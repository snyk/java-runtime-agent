package io.snyk.agent.util.csum;

import io.snyk.asm.*;

import java.util.IdentityHashMap;
import java.util.Map;

class InstructionHashCodeBuilder extends MethodVisitor {
    int hashCode = 0x31415926;

    /**
     * `Label` instances are unique, but don't have any interior unique identifier,
     * i.e. the `offset` isn't available while iterating an InsnList, which isn't ideal.
     * <p>
     * The `toString` method appears to compute a unique id; like "L123123123", but it's
     * just the identityHashCode and not stable across runs.
     * <p>
     * This map is used to generate a monotonic ID for each label (first seen: 0,
     * second seen: 1, etc.), which we use as as the label number, for the `hashCode`.
     */
    private Map<Label, Integer> labelSerialiser = new IdentityHashMap<>();

    private void add(int val) {
        hashCode = hashCode * 37 + val;
    }

    private void add(String o) {
        add(o.hashCode());
    }

    private void add(boolean b) {
        add(b ? 13 : 7);
    }

    private void add(Label l) {
        add(labelSerialiser.computeIfAbsent(l, _l -> labelSerialiser.size()));
    }

    InstructionHashCodeBuilder() {
        super(Opcodes.ASM5);
    }

    @Override
    public void visitInsn(int opcode) {
        add(opcode);
    }

    @Override
    public void visitIntInsn(int opcode, int operand) {
        add(operand);
    }

    @Override
    public void visitVarInsn(int opcode, int var) {
        add(var);
    }

    @Override
    public void visitTypeInsn(int opcode, String type) {
        add(type);
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
        add(owner);
        add(name);
        add(desc);
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
        add(owner);
        add(name);
        add(desc);
        add(itf);
    }

    @Override
    public void visitInvokeDynamicInsn(String name, String desc, Handle bsm, Object... bsmArgs) {
        add(name);
        add(desc);

        // TODO: do we not care about some of these?
        add(bsm.getTag());
        add(bsm.getName());
        add(bsm.getOwner());
        add(bsm.getDesc());

        // TODO: args?
    }

    @Override
    public void visitJumpInsn(int opcode, Label label) {
        add(label);
    }

    @Override
    public void visitLabel(Label label) {
        add(label);
    }

    @Override
    public void visitLdcInsn(Object cst) {
        if (cst instanceof Number || cst instanceof String) {
            add(cst.hashCode());
        } else if (cst instanceof Type) {
            add(((Type) cst).getInternalName());
        } else {
            throw new UnsupportedOperationException("impossible ldc: " + cst.getClass());
        }
    }

    @Override
    public void visitIincInsn(int var, int increment) {
        add(increment);
    }

    @Override
    public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
        add(min);
        add(max);
        add(dflt);
        for (Label l : labels) {
            add(l);
        }
    }

    @Override
    public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
        add(dflt);
        for (int i : keys) {
            add(i);
        }
        for (Label l : labels) {
            add(l);
        }
    }

    @Override
    public void visitMultiANewArrayInsn(String desc, int dims) {
        add(desc);
        add(dims);
    }
}
