package io.snyk.agent;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

/**
 * perform rewrites of classes
 */
public class Rewriter {

    private static final int DEFAULT_PARSING_OPTIONS = 0;
    public static final String OUR_INTERNAL_NAME = Tracker.class.getName().replace('.', '/');

    static byte[] rewrite(ClassReader reader) {
        final ClassNode cn = new ClassNode();
        reader.accept(cn, DEFAULT_PARSING_OPTIONS);
        for (MethodNode method : cn.methods) {
            if (Interesting.interesting(method)) {
                rewriteMethod(cn.name, method);
            }
        }
        return AsmUtil.byteArray(cn);
    }

    private static void rewriteMethod(String clazzInternalName, MethodNode method) {
        final String tag = clazzInternalName + ":" + method.name;
        addCallsTracking(method, tag);
        addCalleeTracking(method, tag);
    }

    private static void addCalleeTracking(MethodNode method, String tag) {
        final InsnList launchpad = generateRegistrationSnippet("registerCall", tag);

        final AbstractInsnNode first = method.instructions.getFirst();
        if (null == first) {
            method.instructions.insert(launchpad);
        } else {
            method.instructions.insertBefore(first, launchpad);
        }
    }

    private static InsnList generateRegistrationSnippet(String targetMethod, String tag) {
        final InsnList launchpad = new InsnList();
        launchpad.add(new LdcInsnNode(tag));
        launchpad.add(new MethodInsnNode(Opcodes.INVOKESTATIC, OUR_INTERNAL_NAME,
                targetMethod, "(Ljava/lang/String;)V", false));
        return launchpad;
    }

    private static void addCallsTracking(MethodNode method, String tag) {
        final InsnList insns = method.instructions;
        for (int i = 0; i < insns.size(); ++i) {
            final AbstractInsnNode ins = insns.get(i);
            if (!(ins instanceof MethodInsnNode)) {
                continue;
            }

            final MethodInsnNode mi = (MethodInsnNode) ins;

            if (!Interesting.interesting(mi)) {
                continue;
            }

            if (!("loadClass".equals(mi.name)
                    && "java/lang/ClassLoader".equals(mi.owner)
                    && "(Ljava/lang/String;)Ljava/lang/Class;".equals(mi.desc))) {
                // TODO: There are overloads of this method.
                // TODO: Ignoring them is plain wrong.
                continue;
            }

            final String callTag = tag + ":" + mi.owner + ":"
                    + mi.name + ":" + mi.desc + ":" + mi.getOpcode();
            final InsnList launchpad = new InsnList();

            // stack: "name" classloader
            launchpad.add(new InsnNode(Opcodes.DUP));
            // stack: "name" "name" classloader
            launchpad.add(new LdcInsnNode(callTag));
            // stack: "tag" "name" "name" classloader
            launchpad.add(new MethodInsnNode(
                    Opcodes.INVOKESTATIC,
                    OUR_INTERNAL_NAME,
                    "registerCallee",
                    "(Ljava/lang/String;Ljava/lang/String;)V",
                    false));
            // stack: "name" classloader

            // insertBefore clears its input
            i += launchpad.size();

            insns.insertBefore(mi, launchpad);
        }
    }
}
