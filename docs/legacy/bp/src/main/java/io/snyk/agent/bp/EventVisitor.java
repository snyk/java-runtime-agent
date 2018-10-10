package io.snyk.agent.bp;

import com.sun.jdi.event.*;

interface EventVisitor {
    default void vmStartEvent(VMStartEvent event) {
    }

    default void methodEntryEvent(MethodEntryEvent event) {
    }

    default void methodExitEvent(MethodExitEvent event) {
    }

    default void stepEvent(StepEvent event) {
    }

    default void fieldWatchEvent(ModificationWatchpointEvent event) {
    }

    default void threadDeathEvent(ThreadDeathEvent event) {
    }

    default void classPrepareEvent(ClassPrepareEvent event) {
    }

    default void exceptionEvent(ExceptionEvent event) {
    }

    default void vmDeathEvent(VMDeathEvent event) {
    }

    default void vmDisconnectEvent(VMDisconnectEvent event) {
    }

    default void breakpointEvent(BreakpointEvent event) {
    }
}
