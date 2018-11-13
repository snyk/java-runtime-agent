package io.snyk.checkdb;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.Bits;
import org.apache.maven.index.*;
import org.apache.maven.index.context.IndexCreator;
import org.apache.maven.index.context.IndexUtils;
import org.apache.maven.index.context.IndexingContext;
import org.apache.maven.index.expr.SourcedSearchExpression;
import org.apache.maven.index.updater.*;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.events.TransferEvent;
import org.apache.maven.wagon.events.TransferListener;
import org.apache.maven.wagon.observers.AbstractTransferListener;
import org.codehaus.plexus.*;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MavenIndex implements Closeable {
    private static final Logger logger = LoggerFactory.getLogger(MavenIndex.class);

    private final Indexer indexer;

    private final IndexUpdater indexUpdater;

    private final Wagon httpWagon;

    private final IndexingContext centralContext;
    private final String home = System.getProperty("user.home");

    public MavenIndex()
            throws PlexusContainerException, ComponentLookupException, IOException {
        // here we create Plexus container, the Maven default IoC container
        // Plexus falls outside of MI scope, just accept the fact that
        // MI is a Plexus component ;)
        // If needed more info, ask on Maven Users list or Plexus Users list
        // google is your friend!
        final DefaultContainerConfiguration config = new DefaultContainerConfiguration();
        config.setClassPathScanning(PlexusConstants.SCANNING_INDEX);
        final PlexusContainer plexusContainer = new DefaultPlexusContainer(config);

        // lookup the indexer components from plexus
        this.indexer = plexusContainer.lookup(Indexer.class);
        this.indexUpdater = plexusContainer.lookup(IndexUpdater.class);
        // lookup wagon used to remotely fetch index
        this.httpWagon = plexusContainer.lookup(Wagon.class, "http");

        final File cache = new File(home, ".m2/snyk-index");
        // Files where local cache is (if any) and Lucene Index should be located
        final File centralLocalCache = new File(cache, "central-cache");
        final File centralIndexDir = new File(cache, "central-index");
        assert centralLocalCache.isDirectory() || centralLocalCache.mkdirs();
        assert centralIndexDir.isDirectory() || centralIndexDir.mkdirs();

        // Creators we want to use (search for fields it defines)
        final List<IndexCreator> indexers = new ArrayList<>();
        indexers.add(plexusContainer.lookup(IndexCreator.class, "min"));
        indexers.add(plexusContainer.lookup(IndexCreator.class, "jarContent"));
        indexers.add(plexusContainer.lookup(IndexCreator.class, "maven-plugin"));

        // Create context for central repository index
        centralContext = indexer.createIndexingContext("central-context", "central", centralLocalCache, centralIndexDir,
                "http://repo1.maven.org/maven2", null, true, true, indexers);
    }

    @Override
    public void close() throws IOException {
        indexer.closeIndexingContext(centralContext, false);
    }

    // Update the index (incremental update will happen if this is not 1st run and files are not deleted)
    // This whole block below should not be executed on every app start, but rather controlled by some configuration
    // since this block will always emit at least one HTTP GET. Central indexes are updated once a week, but
    // other index sources might have different index publishing frequency.
    // Preferred frequency is once a week.
    // TODO: incremental update can take like 20 minutes of CPU time, vs. downloading a 500MB file: poor trade-off
    void maybeUpdateIndex() throws IOException {
        logger.debug("Updating Index...");

        // Create ResourceFetcher implementation to be used with IndexUpdateRequest
        // Here, we use Wagon based one as shorthand, but all we need is a ResourceFetcher implementation
        final TransferListener listener = new AbstractTransferListener() {
            long downloadedBytes = 0;

            public void transferStarted(TransferEvent transferEvent) {
                logger.warn("  Downloading " + transferEvent.getResource().getName());
            }

            public void transferProgress(TransferEvent transferEvent, byte[] buffer, int length) {
                downloadedBytes += length;

                if (downloadedBytes > 5_000_000) {
                    logger.warn("  Still downloading " + transferEvent.getResource().getName());
                    downloadedBytes = 0;
                }
            }

            public void transferCompleted(TransferEvent transferEvent) {
                logger.debug(" - Done");
            }
        };
        final ResourceFetcher resourceFetcher = new WagonHelper.WagonFetcher(httpWagon, listener, null, null);

        final Date centralContextCurrentTimestamp = centralContext.getTimestamp();
        final IndexUpdateRequest updateRequest = new IndexUpdateRequest(centralContext, resourceFetcher);
        final IndexUpdateResult updateResult = indexUpdater.fetchAndUpdateIndex(updateRequest);
        if (updateResult.isFullUpdate()) {
            logger.debug("Full update happened");
        } else if (updateResult.getTimestamp().equals(centralContextCurrentTimestamp)) {
            logger.debug("No update needed, index is up to date");
        } else {
            logger.debug(
                    "Incremental update happened, change covered " + centralContextCurrentTimestamp + " - "
                            + updateResult.getTimestamp() + " period.");
        }
    }

    IteratorSearchResponse find(String group, String artifact) throws IOException {
        final Query groupIdQ =
                indexer.constructQuery(MAVEN.GROUP_ID, new SourcedSearchExpression(group));
        final Query artifactIdQ =
                indexer.constructQuery(MAVEN.ARTIFACT_ID, new SourcedSearchExpression(artifact));
        final BooleanQuery.Builder query = new BooleanQuery.Builder();

        query.add(groupIdQ, Occur.MUST);
        query.add(artifactIdQ, Occur.MUST);

        query.add(new BooleanQuery.Builder()
                .add(indexer.constructQuery(MAVEN.PACKAGING, new SourcedSearchExpression("jar")), Occur.SHOULD)
                .add(indexer.constructQuery(MAVEN.PACKAGING, new SourcedSearchExpression("pom")), Occur.SHOULD)
                .add(indexer.constructQuery(MAVEN.PACKAGING, new SourcedSearchExpression("bundle")), Occur.SHOULD)
                .build(), Occur.MUST);

        // we want main artifacts only (no classifier)
        query.add(indexer.constructQuery(MAVEN.CLASSIFIER, new SourcedSearchExpression(Field.NOT_PRESENT)),
                Occur.MUST_NOT);

        return indexer.searchIterator(new IteratorSearchRequest(query.build(),
                Collections.singletonList(centralContext)));
    }

    void forEach(Consumer<ArtifactInfo> callback) throws IOException {
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

                callback.accept(ai);
            }
        } finally {
            centralContext.releaseIndexSearcher(searcher);
        }
    }

    File fetch(String group, String artifact, String version) throws IOException, InterruptedException {
        // I tried to do this with plugins but I can't get it to work
        // Let's test the idea the horrible way, then maybe fix it later.

        final File wanted = new File(new File(home, ".m2/repository").getAbsolutePath() +
                "/" + group.replace('.', '/') +
                "/" + artifact +
                "/" + version +
                "/" + artifact + "-" + version + ".jar");

        if (wanted.isFile()) {
            return wanted;
        }

        assertEquals(0, new ProcessBuilder("mvn", "org.apache.maven.plugins:maven-dependency-plugin:3.1.1:get",
                "-Dartifact=" + group + ":" + artifact + ":" + version, "-Dtransitive=false")
                .start().waitFor());

        if (!wanted.isFile()) {
            throw new IllegalStateException("download succeeded hasn't produced the file we wanted: " + wanted);
        }

        return wanted;
    }
}
