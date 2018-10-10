package io.snyk.agent.util;

import io.snyk.asm.tree.AbstractInsnNode;
import io.snyk.asm.tree.InsnList;

import java.util.ListIterator;
import java.util.function.Predicate;

import static io.snyk.agent.logic.InstrumentationFilter.isNonsenseNode;

public class InsnIter {

    private final ListIterator<AbstractInsnNode> inner;

    public InsnIter(InsnList instructions) {
        inner = instructions.iterator();
    }

    public boolean nextIs(Predicate<AbstractInsnNode> filter) {
        while (inner.hasNext()) {
            final AbstractInsnNode node = inner.next();
            if (isNonsenseNode(node)) {
                continue;
            }

            return filter.test(node);
        }

        return false;
    }
}
