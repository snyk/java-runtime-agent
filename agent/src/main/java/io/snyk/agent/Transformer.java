package io.snyk.agent;

import org.objectweb.asm.ClassReader;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;


public class Transformer implements ClassFileTransformer {
    @Override
    public byte[] transform(
            ClassLoader loader,
            String className,
            Class<?> classBeingRedefined,
            ProtectionDomain protectionDomain,
            byte[] classfileBuffer) throws IllegalClassFormatException {
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
        if (Interesting.interesting(className)) {
            return new Rewriter(Tracker.class, Tracker.SEEN_SET).rewrite(new ClassReader(classfileBuffer));
        } else {
            return classfileBuffer;
        }
    }
}
