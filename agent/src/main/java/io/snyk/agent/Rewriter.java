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

    static void rewriteMethod(String clazzInternalName, MethodNode method) {
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

            if (!"loadClass".equals(mi.name)) {
                continue;
            }

            // we can't actually filter on `owner` here because a class could extend a class,
            // which extends ClassLoader; we don't have that information available at transform
            // time... Chris believes? ASM might be able to do it for us, but cause way too many
            // classes to be loaded by the bootstrap(?) classloader, which isn't what we want at all.

            // This causes the TODO section in the else block below.

            final String callTag = tag + ":" + mi.owner + ":"
                    + mi.name + ":" + mi.desc + ":" + mi.getOpcode();

            final InsnList launchpad = new InsnList();

            if ("(Ljava/lang/String;)Ljava/lang/Class;".equals(mi.desc)) {
                // stack: "name" classloader
                launchpad.add(new InsnNode(Opcodes.DUP));
                // stack: "name" "name" classloader
            } else if ("(Ljava/lang/String;Z)Ljava/lang/Class;".equals(mi.desc)) {
                // Note the "Z" here ------^ in the visually identical strings above
                // the variant with the extra boolean arg. It's protected. Does it matter?

                // stack: bool "name" classloader
                launchpad.add(new InsnNode(Opcodes.DUP2));
                // stack: bool "name" bool "name" classloader
                launchpad.add(new InsnNode(Opcodes.POP));
                // stack: "name" bool "name" classloader
            } else {
                // TODO: scary.. have they added a new method to ClassLoader we've missed?
                // TODO: or, more likely, has someone named a method loadClass, and we've picked it
                // TODO: up too greedily? Sigh.
                continue;
            }

            // stack: "name" [original stack]
            launchpad.add(new LdcInsnNode(callTag));
            // stack: "tag" "name" [original stack]
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
