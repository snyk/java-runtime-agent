package io.snyk.agent.logic;

import io.snyk.agent.filter.Filter;
import io.snyk.agent.util.AsmUtil;
import io.snyk.agent.util.Log;
import io.snyk.asm.ClassReader;
import io.snyk.asm.Opcodes;
import io.snyk.asm.tree.*;

import java.util.List;
import java.util.Optional;
import java.util.function.ToIntFunction;

/**
 * perform rewrites of classes
 */
public class Rewriter {

    private final String ourInternalName;
    private final ToIntFunction<String> allocateNewId;
    private final String sourceLocation;
    private final Config config;
    private final Log log;

    // This Class<?> must implement the same "static interface" as LandingZone.class.
    // There's no way to express this in Java. The marked public static methods must
    // exist.
    public Rewriter(Class<?> tracker,
                    ToIntFunction<String> allocateNewId,
                    String sourceLocation,
                    Config config,
                    Log log) {
        this.ourInternalName = tracker.getName().replace('.', '/');
        this.allocateNewId = allocateNewId;
        this.sourceLocation = sourceLocation;
        this.config = config;
        this.log = log;
    }

    public byte[] rewrite(ClassReader reader) {
        final ClassNode cn = AsmUtil.parse(reader);
        final List<Filter> filters = config.filters.get().filters;
        boolean aMethodHadTheRightName = false;

        for (MethodNode method : cn.methods) {
            final Optional<Filter> matching = filters.stream()
                    .filter(filter -> filter.testMethod(cn.name, method.name))
                    .findAny();

            if (!matching.isPresent()) {
                continue;
            }

            aMethodHadTheRightName = true;

            final Filter filter = matching.get();
            filter.matches.incrementAndGet();

            final String logName = filter.name + ": " + cn.name + "#" + method.name;

            if (InstrumentationFilter.alreadyInstrumented(method, ourInternalName)) {
                log.debug("asked to rewrite, but already instrumented:" + logName);
                continue;
            }

            if (InstrumentationFilter.skipMethod(cn, method)) {
                log.info("rewrite requested, but disallowed: " + logName);
                continue;
            }

            final boolean includeWrtAccessors = config.trackAccessors || !InstrumentationFilter.isAccessor(method);
            if (!includeWrtAccessors) {
                log.info("rewrite requested, but accessor: " + logName);
                continue;
            }

            final boolean includeWrtBranching = config.trackBranchingMethods ||
                    InstrumentationFilter.branches(cn, method);
            if (!includeWrtBranching) {
                log.info("rewrite requested, but branching: " + logName);
                continue;
            }

            log.info("rewrite: " + logName);
            rewriteMethod(cn.name, method);
        }

        if (!aMethodHadTheRightName) {
            log.info("rewrite requested, but no matching method found: " + cn.name);
        }

        return AsmUtil.byteArray(cn);
    }

    private void rewriteMethod(String clazzInternalName, MethodNode method) {
        final String tag = clazzInternalName + ":" + method.name +
                method.desc + // using desc, not signature, as it's always available
                ":" + sourceLocation;
        if (config.trackClassLoading) {
            addInspectionOfLoadClassCalls(method, tag);
        }
        addInspectionOfMethodEntry(method, tag);
    }

    private void addInspectionOfMethodEntry(MethodNode method, String methodLocation) {
        final InsnList launchpad = new InsnList();
        launchpad.add(new LdcInsnNode(allocateNewId.applyAsInt(methodLocation)));
        launchpad.add(new MethodInsnNode(Opcodes.INVOKESTATIC, ourInternalName,
                "registerMethodEntry", "(I)V", false));

        final AbstractInsnNode first = method.instructions.getFirst();
        if (null == first) {
            method.instructions.insert(launchpad);
        } else {
            method.instructions.insertBefore(first, launchpad);
        }
    }

    private void addInspectionOfLoadClassCalls(MethodNode method, String methodLocation) {
        final InsnList insns = method.instructions;
        for (int i = 0; i < insns.size(); ++i) {
            final AbstractInsnNode ins = insns.get(i);
            if (!(ins instanceof MethodInsnNode)) {
                continue;
            }

            final MethodInsnNode mi = (MethodInsnNode) ins;

            if (!InstrumentationFilter.returnsClass(mi)) {
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

            final String callTag = mi.owner + ":"
                    + mi.name + ":" + mi.desc + ":" + mi.getOpcode() + ":"
                    + methodLocation;

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
            launchpad.add(new LdcInsnNode(allocateNewId.applyAsInt(callTag)));
            // stack: tag "name" [original stack]
            launchpad.add(new MethodInsnNode(
                    Opcodes.INVOKESTATIC,
                    ourInternalName,
                    "registerLoadClass",
                    "(Ljava/lang/String;I)V",
                    false));
            // stack: "name" classloader

            // insertBefore clears its input
            i += launchpad.size();

            insns.insertBefore(mi, launchpad);
        }
    }
}
