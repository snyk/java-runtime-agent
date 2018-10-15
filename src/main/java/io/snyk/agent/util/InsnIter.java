package io.snyk.agent.util;

import io.snyk.asm.tree.AbstractInsnNode;
import io.snyk.asm.tree.InsnList;

import java.util.ListIterator;
import java.util.function.Predicate;

import static io.snyk.agent.logic.InstrumentationFilter.isNonsenseNode;

/**
 * Working around inconveniences in the ASM API:
 * iterate over instructions in a method, skipping ones that are worthless.
 */
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
