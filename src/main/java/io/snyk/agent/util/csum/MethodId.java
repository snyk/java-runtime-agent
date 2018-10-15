package io.snyk.agent.util.csum;

import io.snyk.asm.tree.MethodNode;

class MethodId {
    public static int id(MethodNode node) {
        final InstructionHashCodeBuilder builder = new InstructionHashCodeBuilder();
        node.instructions.accept(builder);

        return builder.hashCode;
    }
}
