package io.snyk.agent.util.csum;

import io.snyk.asm.tree.MethodNode;

/**
 * Build a unique identifier from a method (instead of a class).
 * ASM won't give us the bytecode (it's not as stable across versions?),
 * so walk the method like this, and manually track the values.
 */
class MethodId {
    public static int id(MethodNode node) {
        final InstructionHashCodeBuilder builder = new InstructionHashCodeBuilder();
        node.instructions.accept(builder);

        return builder.hashCode;
    }
}
