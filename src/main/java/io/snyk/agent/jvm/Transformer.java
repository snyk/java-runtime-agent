package io.snyk.agent.jvm;

import io.snyk.agent.logic.*;
import io.snyk.agent.util.Log;
import io.snyk.asm.ClassReader;

import java.lang.instrument.ClassFileTransformer;
import java.net.URLClassLoader;
import java.security.ProtectionDomain;
import java.util.Arrays;

/**
 * Tie {@link Rewriter} and {@link LandingZone} to the JVM interface.
 */
class Transformer implements ClassFileTransformer {

    private static final String LANDING_ZONE_NAME = LandingZone.class.getName().replace('.', '/') + ".class";
    private final Log log;
    private final Config config;
    private final DataTracker dataTracker;

    Transformer(Log log, Config config, DataTracker dataTracker) {
        this.log = log;
        this.config = config;
        this.dataTracker = dataTracker;
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
            // `null` here means "don't rewrite"
            return null;
        }

        // if the classloader loading the class can't load us,
        // then something weird has gone on. NewRelic intentionally strips
        // other jars out of the classpath when loading itself, to void problems.
        if (null == loader.getResource(LANDING_ZONE_NAME)) {
            if (false) {
                if (loader instanceof URLClassLoader) {
                    System.err.println(className + " // " + Arrays.toString(((URLClassLoader) loader).getURLs()));
                }
            }

            log.debug("classloader futzing detected: " + className);
            return null;
        }

        try {
            return process(loader, classfileBuffer);
        } catch (Throwable t) {
            // classpath or jar clash issues are just silently eaten by the JVM,
            // make sure they're shown.
            // note that this class name can be null, but.. what else can we do?
            dataTracker.addError("transform:" + className, t);
            log.warn("transform failed: " + className);
            log.stackTrace(t);
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

        final ClassInfo.ExtraInfo info = dataTracker.classInfo.findSourceInfo(loader, className, classfileBuffer);

        if (!shouldProcessClass(className, info)) {
            return null;
        }

        return new Rewriter(LandingZone.class, LandingZone.SEEN_SET::add, info.toLocation(), config, log)
                .rewrite(reader);
    }

    private boolean shouldProcessClass(String className, ClassInfo.ExtraInfo info) {
        return config.filters.stream()
                .anyMatch(f -> f.testArtifacts(log, info.extra) && f.testClassName(className));
    }
}
