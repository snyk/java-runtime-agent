package io.snyk.agent;

import io.snyk.agent.util.org.apache.maven.artifact.versioning.ComparableVersion;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.util.Bits;
import org.apache.maven.index.ArtifactInfo;
import org.apache.maven.index.Indexer;
import org.apache.maven.index.context.IndexCreator;
import org.apache.maven.index.context.IndexUtils;
import org.apache.maven.index.context.IndexingContext;
import org.apache.maven.index.updater.*;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.events.TransferEvent;
import org.apache.maven.wagon.events.TransferListener;
import org.apache.maven.wagon.observers.AbstractTransferListener;
import org.codehaus.plexus.*;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.eclipse.aether.version.InvalidVersionSpecificationException;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;

import static java.time.format.DateTimeFormatter.ISO_DATE;

public class FindMavens {
    public static void main(String[] args)
            throws Exception {
        new FindMavens().perform();
    }

    private final PlexusContainer plexusContainer;

    private final Indexer indexer;

    private final IndexUpdater indexUpdater;

    private final Wagon httpWagon;

    private IndexingContext centralContext;

    public FindMavens()
            throws PlexusContainerException, ComponentLookupException {
        // here we create Plexus container, the Maven default IoC container
        // Plexus falls outside of MI scope, just accept the fact that
        // MI is a Plexus component ;)
        // If needed more info, ask on Maven Users list or Plexus Users list
        // google is your friend!
        final DefaultContainerConfiguration config = new DefaultContainerConfiguration();
        config.setClassPathScanning(PlexusConstants.SCANNING_INDEX);
        this.plexusContainer = new DefaultPlexusContainer(config);

        // lookup the indexer components from plexus
        this.indexer = plexusContainer.lookup(Indexer.class);
        this.indexUpdater = plexusContainer.lookup(IndexUpdater.class);
        // lookup wagon used to remotely fetch index
        this.httpWagon = plexusContainer.lookup(Wagon.class, "http");

    }

    private void perform()
            throws IOException, ComponentLookupException, InvalidVersionSpecificationException {
        final File cache = new File(System.getProperty("user.home"), ".cache/snyk-index");
        // Files where local cache is (if any) and Lucene Index should be located
        final File centralLocalCache = new File(cache, "central-cache");
        final File centralIndexDir = new File(cache, "central-index");
        assert centralLocalCache.mkdirs();
        assert centralIndexDir.mkdirs();

        // Creators we want to use (search for fields it defines)
        final List<IndexCreator> indexers = new ArrayList<>();
        indexers.add(plexusContainer.lookup(IndexCreator.class, "min"));
        indexers.add(plexusContainer.lookup(IndexCreator.class, "jarContent"));
        indexers.add(plexusContainer.lookup(IndexCreator.class, "maven-plugin"));

        // Create context for central repository index
        centralContext = indexer.createIndexingContext("central-context", "central", centralLocalCache, centralIndexDir,
                "http://repo1.maven.org/maven2", null, true, true, indexers);

        maybeUpdateIndex();
        walk();

        // close cleanly
        indexer.closeIndexingContext(centralContext, false);
    }

    static class DatedVersion {
        final long date;
        final ComparableVersion version;

        DatedVersion(long date, ComparableVersion version) {
            this.date = date;
            this.version = version;
        }
    }

    private void walk() throws IOException {
        // two years
        final long MAX_AGE = 1000L * 60 * 60 * 24 * 365 * 2;

        final Map<String, DatedVersion> latest = new HashMap<>(16 * 4096);

        final IndexSearcher searcher = centralContext.acquireIndexSearcher();
        try {
            final IndexReader ir = searcher.getIndexReader();
            final Bits liveDocs = MultiFields.getLiveDocs(ir);
            for (int i = 0; i < ir.maxDoc(); i++) {
                if (liveDocs != null && !liveDocs.get(i)) {
                    continue;
                }

                final Document doc = ir.document(i);
                final ArtifactInfo ai = IndexUtils.constructArtifactInfo(doc, centralContext);

                if (null == ai) {
                    // TODO: I don't understand why this happens; the demo code doesn't expect it
                    continue;
                }

                if (null != ai.getClassifier() && !"main".equals(ai.getClassifier())) {
                    continue;
                }

                final long modified = ai.getLastModified();
                final ComparableVersion version = new ComparableVersion(ai.getVersion());

                final String key = ai.getGroupId() + ":" + ai.getArtifactId();

                latest.compute(key, (_key, old) -> {
                    if (null == old) {
                        // no existing version, so use this
                        return new DatedVersion(modified, version);
                    }

                    // positive for this being newer
                    final long timeDifference = modified - old.date;
                    if (version.compareTo(old.version) > 0) {
                        if (timeDifference > -MAX_AGE) {
                            // we're newer, and not depressingly old
                            return new DatedVersion(modified, version);
                        } else {
                            // e.g. https://search.maven.org/search?q=g:org.glassfish.web%20AND%20a:webtier-all&core=gav
                            System.err.println("update too old: " + key + ": "
                                    + old.version + " -> " + version + " in "
                                    + toDate(old.date)
                                    + " -> "
                                    + toDate(modified));
                        }
                    }

                    return old;
                });
            }
        } finally {
            centralContext.releaseIndexSearcher(searcher);
        }

        latest.forEach((k, v) -> System.out.println(k + ": " + v.version));

        System.out.println(latest.size());
    }

    private String toDate(long date) {
        // I really don't like these APIs.
        return ISO_DATE.format(Instant.ofEpochMilli(date).atZone(ZoneOffset.UTC));
    }

    // Update the index (incremental update will happen if this is not 1st run and files are not deleted)
    // This whole block below should not be executed on every app start, but rather controlled by some configuration
    // since this block will always emit at least one HTTP GET. Central indexes are updated once a week, but
    // other index sources might have different index publishing frequency.
    // Preferred frequency is once a week.
    private void maybeUpdateIndex() throws IOException {
        System.out.println("Updating Index...");

        // Create ResourceFetcher implementation to be used with IndexUpdateRequest
        // Here, we use Wagon based one as shorthand, but all we need is a ResourceFetcher implementation
        final TransferListener listener = new AbstractTransferListener() {
            public void transferStarted(TransferEvent transferEvent) {
                System.out.print("  Downloading " + transferEvent.getResource().getName());
            }

            public void transferProgress(TransferEvent transferEvent, byte[] buffer, int length) {
            }

            public void transferCompleted(TransferEvent transferEvent) {
                System.out.println(" - Done");
            }
        };
        final ResourceFetcher resourceFetcher = new WagonHelper.WagonFetcher(httpWagon, listener, null, null);

        final Date centralContextCurrentTimestamp = centralContext.getTimestamp();
        final IndexUpdateRequest updateRequest = new IndexUpdateRequest(centralContext, resourceFetcher);
        final IndexUpdateResult updateResult = indexUpdater.fetchAndUpdateIndex(updateRequest);
        if (updateResult.isFullUpdate()) {
            System.out.println("Full update happened");
        } else if (updateResult.getTimestamp().equals(centralContextCurrentTimestamp)) {
            System.out.println("No update needed, index is up to date");
        } else {
            System.out.println(
                    "Incremental update happened, change covered " + centralContextCurrentTimestamp + " - "
                            + updateResult.getTimestamp() + " period.");
        }
    }

}
