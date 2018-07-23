package io.snyk.agent;

import org.objectweb.asm.ClassReader;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;


public class Rewrite implements ClassFileTransformer {
    @Override
    public byte[] transform(
            ClassLoader loader,
            String className,
            Class<?> classBeingRedefined,
            ProtectionDomain protectionDomain,
            byte[] classfileBuffer) throws IllegalClassFormatException {
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
        ClassReader reader = new ClassReader(classfileBuffer);
        System.err.println("actual: " + reader.getClassName());
    }
}
