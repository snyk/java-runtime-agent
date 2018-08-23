package io.snyk.agent.csum;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.IdentityHashMap;
import java.util.Map;

public class InstructionHashCodeBuilder extends MethodVisitor {
    int hashCode = 0x31415926;

    private Map<Label, Integer> labels = new IdentityHashMap<>();

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
        add(labels.computeIfAbsent(l, _l ->labels.size()));
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
        } else {
            throw new UnsupportedOperationException("TODO");
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
