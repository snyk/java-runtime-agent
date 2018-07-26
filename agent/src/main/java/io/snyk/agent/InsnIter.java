package io.snyk.agent;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;

import java.util.ListIterator;
import java.util.function.Predicate;

import static io.snyk.agent.Interesting.isNonsenseNode;

public class InsnIter {

    private final ListIterator<AbstractInsnNode> inner;

    InsnIter(InsnList instructions) {
        inner = instructions.iterator();
    }

    boolean nextIs(Predicate<AbstractInsnNode> filter) {
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
