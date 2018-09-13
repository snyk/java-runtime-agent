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

            } while (nextEntry.isDirectory() || !nextEntry.getName().endsWith(".class"));
        }

        return null != nextEntry;
    }

    @Override
    public ClassNode next() {
        try {
            nextEntry = null;
            return AsmUtil.parse(new ClassReader(jar));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
