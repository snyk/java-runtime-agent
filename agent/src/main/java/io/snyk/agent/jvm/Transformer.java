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
            return process(classfileBuffer);
        } catch (Throwable t) {
            // classpath or jar clash issues are just silently eaten by the JVM,
            // make sure they're shown.
            t.printStackTrace();
            throw t;
        }
    }

    private byte[] process(byte[] classfileBuffer) {
        final ClassReader reader = new ClassReader(classfileBuffer);

        // grab the class name from the buffer, not using the passed-in class name,
        // which is `null` for synthetic classes like lambdas and anonymous inner classes,
        // it appears. I haven't seen documentation for why this would be the case.

        if (!InstrumentationFilter.interestingClassName(reader.getClassName())) {
            return null;
        }

        return new Rewriter(LandingZone.class, LandingZone.SEEN_SET::add)
                .rewrite(reader);
    }
}
