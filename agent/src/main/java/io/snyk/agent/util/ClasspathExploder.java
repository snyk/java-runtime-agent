package io.snyk.agent.util;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ClasspathExploder {
    public void walkClassPath() {
        final String classpath = System.getProperty("java.class.path");
        for (String item : classpath.split(File.pathSeparator)) {
            try (final JarFile file = new JarFile(item, false)) {
                final Enumeration<JarEntry> entries = file.entries();
                while (entries.hasMoreElements()) {
                    final JarEntry entry = entries.nextElement();
                    if (entry.isDirectory()) {
                        continue;
                    }

                    System.err.println(item + ":" + entry.getName());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
