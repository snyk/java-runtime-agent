package io.snyk.agent.jvm;

import io.snyk.agent.logic.ClassSource;
import io.snyk.agent.logic.Config;
import io.snyk.agent.logic.InstrumentationFilter;
import io.snyk.agent.logic.Rewriter;
import io.snyk.agent.util.Log;
import org.objectweb.asm.ClassReader;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

/**
 * Tie {@link Rewriter} and {@link LandingZone} to the JVM interface.
 */
public class Transformer implements ClassFileTransformer {

    private final Log log;
    private final Config config;
    private final ClassSource classSource;

    Transformer(Log log, Config config, ClassSource classSource) {
        this.log = log;
        this.config = config;
        this.classSource = classSource;
    }

    @Override
    public byte[] transform(
            ClassLoader loader,
            String className,
            Class<?> classBeingRedefined,
            ProtectionDomain protectionDomain,
            byte[] classfileBuffer) {
        // `null` here represents the system classloader
        // TODO: is this only going to load core classes? Who knows.
        if (null == loader) {
            return classfileBuffer;
        }
        try {
            return process(loader, classfileBuffer);
        } catch (Throwable t) {
            // classpath or jar clash issues are just silently eaten by the JVM,
            // make sure they're shown.
            // note that this class name can be null, but.. what else can we do?
            classSource.addError("transform:" + className, t);
            t.printStackTrace();
            throw t;
        }
    }

    private byte[] process(ClassLoader loader, byte[] classfileBuffer) {
        final ClassReader reader = new ClassReader(classfileBuffer);

        // grab the class name from the buffer, not using the passed-in class name,
        // which is `null` for synthetic classes like lambdas and anonymous inner classes,
        // it appears. I haven't seen documentation for why this would be the case.

        final String className = reader.getClassName();
        if (!InstrumentationFilter.bannedClassName(className)) {
            return null;
        }

        final ClassSource.ExtraInfo info = classSource.findSourceInfo(loader, className, classfileBuffer);

        if (!shouldProcessClass(className, info)) {
            return null;
        }

        return new Rewriter(LandingZone.class, LandingZone.SEEN_SET::add, info.toLocation())
                .rewrite(reader);
    }

    private boolean shouldProcessClass(String className, ClassSource.ExtraInfo info) {
        if (config.filters.isEmpty()) {
            return true;
        }

        return config.filters.stream()
                .anyMatch(f -> f.testArtifacts(log, info.extra) && f.testClassName(log, className));
    }
}
