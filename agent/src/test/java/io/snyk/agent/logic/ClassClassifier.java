package io.snyk.agent.logic;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import io.snyk.agent.util.IterableJar;
import io.snyk.asm.tree.AbstractInsnNode;
import io.snyk.asm.tree.ClassNode;
import io.snyk.asm.tree.MethodInsnNode;
import io.snyk.asm.tree.MethodNode;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class ClassClassifier {
    private static final List<String> STDLIB_CLASS_LOADERS = new ArrayList<>();

    static {
        STDLIB_CLASS_LOADERS.add("java/lang/ClassLoader");
        STDLIB_CLASS_LOADERS.add("java/net/URLClassLoader");
        STDLIB_CLASS_LOADERS.add("java/security/SecureClassLoader");
        STDLIB_CLASS_LOADERS.add("javax/management/loading/MLet");
        STDLIB_CLASS_LOADERS.add("javax/management/loading/PrivateMLet");
        STDLIB_CLASS_LOADERS.add("jdk/nashorn/internal/runtime/NashornLoader");
        STDLIB_CLASS_LOADERS.add("jdk/nashorn/internal/runtime/ScriptLoader");
        STDLIB_CLASS_LOADERS.add("jdk/nashorn/internal/runtime/StructureLoader");
        STDLIB_CLASS_LOADERS.add("java/rmi/server/RMIClassLoader");
        STDLIB_CLASS_LOADERS.add("javax/xml/stream/FactoryFinder$ClassLoaderFinder");
        // TODO: can you get AppClassLoader?
    }

    public static void main(String[] args) throws Exception {
        final Multimap<String, File> map = HashMultimap.create();
        Files.walk(Paths.get(args[0]))
                .parallel()
                .filter(path -> Files.isRegularFile(path))
                .map(Path::toFile)
                // 'Path#endsWith' is not your friend
                .filter(path -> path.toString().endsWith(".jar"))
                //.collect(Collectors.toMap(Function.identity(), ClassClassifier::inspect));
                .forEach(file -> inspect(file).forEach(callee -> {
                    synchronized (map) {
                        map.put(callee, file);
                    }
                }));

        for (String knownLoader : STDLIB_CLASS_LOADERS) {
            final Set<File> libs = new HashSet<>();
            for (Map.Entry<String, Collection<File>> en : map.asMap().entrySet()) {
                if (!en.getKey().startsWith(knownLoader)) {
                    continue;
                }

                libs.addAll(en.getValue());
            }

            System.out.println(knownLoader);
            System.out.println("===");

            final ArrayList<File> libsSorted = new ArrayList<>(libs);
            Collections.sort(libsSorted);
            for (File lib : libsSorted) {
                System.out.println(" * " + lib.toString().replaceAll("^deb/[^/]*/", ""));
            }
            System.out.println();
        }

        if (false) {
            List<String> classLoaders = new ArrayList<>();
            for (String key : map.keySet()) {
                if (key.split("#", 2)[0].contains("ClassLoader")) {
                    classLoaders.add(key);
                }
            }
            Collections.sort(classLoaders);
            for (String classLoader : classLoaders) {
                System.out.println(classLoader);
            }
        }
    }

    private static Set<String> inspect(File jar) {
        try {
            return callsIn(jar);
        } catch (RuntimeException e) {
            throw new IllegalStateException("processing " + jar, e);
        }
    }

    private static Set<String> callsIn(File jar) {
        final Multimap<String, String> calls = HashMultimap.create();
        final Set<String> classes = new HashSet<>();

        for (ClassNode clazz : new IterableJar(() -> {
            try {
                return new FileInputStream(jar);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        })) {
            if (jarMayContain(clazz.name)) {
                classes.add(clazz.name);
            }
            for (MethodNode method : clazz.methods) {
                final ListIterator<AbstractInsnNode> insns = method.instructions.iterator();
                while (insns.hasNext()) {
                    final AbstractInsnNode insn = insns.next();
                    if (insn instanceof MethodInsnNode) {
                        final MethodInsnNode call = (MethodInsnNode) insn;
                        if (call.owner.startsWith("[")) {
                            // we don't need to look at the methods called on "array" (e.g. clone, or... clone)
                            continue;
                        }
                        calls.put(call.owner, call.name + call.desc);
                    }
                }
            }
        }

        for (String clazz : classes) {
            calls.removeAll(clazz);
        }

        return calls.entries().stream().map(en -> en.getKey() + "#" + en.getValue()).collect(Collectors.toSet());
    }

    private static boolean jarMayContain(String name) {
        return !name.startsWith("java/") && !name.startsWith("javax/");
    }
}
