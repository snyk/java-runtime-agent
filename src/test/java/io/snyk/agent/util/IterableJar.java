package io.snyk.agent.util;

import io.snyk.asm.ClassReader;
import io.snyk.asm.tree.ClassNode;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.function.Supplier;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

public class IterableJar implements Iterable<ClassNode> {

    private final Supplier<InputStream> jarOpener;

    public IterableJar(Supplier<InputStream> jarOpener) {
        this.jarOpener = jarOpener;
    }

    @Override
    public Iterator<ClassNode> iterator() {
        try {
            return new JarIterator(new JarInputStream(jarOpener.get()));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}

class JarIterator implements Iterator<ClassNode> {

    private final JarInputStream jar;

    JarEntry nextEntry;

    public JarIterator(JarInputStream jarInputStream) {
        this.jar = jarInputStream;
    }

    @Override
    public boolean hasNext() {
        final boolean b = thereIsNext();
        if (!b) {
            // close the stream if we're at the end
            // double-closing is fine, but I wouldn't expect to hit it anyway.
            try {
                jar.close();
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
        return b;
    }

    private boolean thereIsNext() {
        if (null == nextEntry) {
            do {
                try {
                    nextEntry = jar.getNextJarEntry();
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }

                if (null == nextEntry) {
                    return false;
                }

            } while (nextEntry.isDirectory() ||
                    !nextEntry.getName().endsWith(".class") ||
                    // hack: some of these trip up ASM,
                    // e.g. deb/j/json-smart/libjson-smart-java_2.2-2_all/accessors-smart.jar
                    nextEntry.getName().equals("module-info.class"));
        }

        return null != nextEntry;
    }

    @Override
    public ClassNode next() {
        try {
            final ClassNode parsed = AsmUtil.parse(new ClassReader(jar));
            nextEntry = null;
            return parsed;
        } catch (Exception e) {
            final String name = nextEntry.getName();
            nextEntry = null;
            throw new IllegalStateException("stream probably corrupt, illegal state: error parsing: " + name, e);
        }
    }
}
