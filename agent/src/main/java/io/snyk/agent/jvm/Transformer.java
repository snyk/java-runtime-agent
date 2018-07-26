package io.snyk.agent.jvm;

import io.snyk.agent.logic.InstrumentationFilter;
import io.snyk.agent.logic.Rewriter;
import org.objectweb.asm.ClassReader;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

/**
 * Tie {@link Rewriter} and {@link LandingZone} to the JVM interface.
 */
public class Transformer implements ClassFileTransformer {
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
            return process(className, classfileBuffer);
        } catch (Throwable t) {
            // classpath or jar clash issues are just silently eaten by the JVM,
            // make sure they're shown.
            t.printStackTrace();
            throw t;
        }
    }

    private byte[] process(String className, byte[] classfileBuffer) {
        if (InstrumentationFilter.interestingClassName(className)) {
            return new Rewriter(LandingZone.class, LandingZone.SEEN_SET)
                    .rewrite(new ClassReader(classfileBuffer));
        } else {
            return classfileBuffer;
        }
    }
}
