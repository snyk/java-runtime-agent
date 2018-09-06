package io.snyk.debugger.trace;

import com.sun.jdi.Location;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.VMDisconnectedException;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.event.*;
import com.sun.jdi.request.ClassPrepareRequest;
import com.sun.jdi.request.EventRequest;
import com.sun.jdi.request.EventRequestManager;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

public class EventDispatcher extends Thread {

    private final VirtualMachine vm;   // Running VM
    private final PrintWriter writer;  // Where output goes

    private final Map<String, ClassPrepareRequest> byClass = new HashMap<>();

    private final EventVisitor handler = new EventVisitor() {
        @Override
        public void classPrepareEvent(ClassPrepareEvent event) {
            final EventRequestManager mgr = vm.eventRequestManager();
            event.referenceType()
                    // allMethods would be useful, but we clearly don't want java.lang.Object#<init>.
                    .methods()
                    .stream()
                    .filter(method -> !method.isNative() && !method.isAbstract() && !method.isBridge())
                    .forEach(method -> mgr.createBreakpointRequest(method.location()).enable());
        }

        @Override
        public void breakpointEvent(BreakpointEvent event) {
            final Location location = event.location();
            final ReferenceType dt = location.declaringType();
            System.err.println(dt + "#" + location.method().name());
        }
    };

    private boolean connectedToVm = true;

    EventDispatcher(VirtualMachine vm, PrintWriter writer) {
        super("event-dispatcher");
        this.vm = vm;
        this.writer = writer;
    }

    @Override
    public void run() {
        EventQueue queue = vm.eventQueue();
        while (connectedToVm) {
            try {
                EventSet eventSet = queue.remove();
                EventIterator it = eventSet.eventIterator();
                while (it.hasNext()) {
                    dispatchEvent(it.nextEvent());
                }
                eventSet.resume();
            } catch (InterruptedException exc) {
                // Ignore
            } catch (VMDisconnectedException discExc) {
                handleDisconnectedException();
                break;
            }
        }
        writer.flush();
    }

    /**
     * Create the desired event requests, and enable
     * them so that we will get events.
     */
    void addClassWatches(Iterable<String> classPatterns) {
        final EventRequestManager mgr = vm.eventRequestManager();
        for (String pattern : classPatterns) {
            final ClassPrepareRequest prep = mgr.createClassPrepareRequest();
            if (pattern.contains("*")) {
                throw new IllegalStateException("wildcards not currently supported: " + pattern);
            }
            prep.addClassFilter(pattern);
            prep.setSuspendPolicy(EventRequest.SUSPEND_NONE);
            prep.enable();
            byClass.put(pattern, prep);
        }
    }

    private void dispatchEvent(Event event) {
        if (event instanceof ExceptionEvent) {
            handler.exceptionEvent((ExceptionEvent) event);
        } else if (event instanceof ModificationWatchpointEvent) {
            handler.fieldWatchEvent((ModificationWatchpointEvent) event);
        } else if (event instanceof BreakpointEvent) {
            handler.breakpointEvent((BreakpointEvent) event);
        } else if (event instanceof MethodEntryEvent) {
            handler.methodEntryEvent((MethodEntryEvent) event);
        } else if (event instanceof MethodExitEvent) {
            handler.methodExitEvent((MethodExitEvent) event);
        } else if (event instanceof StepEvent) {
            handler.stepEvent((StepEvent) event);
        } else if (event instanceof ThreadDeathEvent) {
            handler.threadDeathEvent((ThreadDeathEvent) event);
        } else if (event instanceof ClassPrepareEvent) {
            handler.classPrepareEvent((ClassPrepareEvent) event);
        } else if (event instanceof VMStartEvent) {
            handler.vmStartEvent((VMStartEvent) event);
        } else if (event instanceof VMDeathEvent) {
            handler.vmDeathEvent((VMDeathEvent) event);
        } else if (event instanceof VMDisconnectEvent) {
            handler.vmDisconnectEvent((VMDisconnectEvent) event);
            connectedToVm = false;
        } else {
            throw new IllegalStateException("Unexpected event type: " + event.getClass());
        }
    }

    /***
     * A VMDisconnectedException has happened while dealing with
     * another event. We need to flush the event queue, dealing only
     * with exit events (VMDeath, VMDisconnect) so that we terminate
     * correctly.
     */
    private synchronized void handleDisconnectedException() {
        EventQueue queue = vm.eventQueue();
        while (connectedToVm) {
            try {
                EventSet eventSet = queue.remove();
                EventIterator iter = eventSet.eventIterator();
                while (iter.hasNext()) {
                    Event event = iter.nextEvent();
                    if (event instanceof VMDeathEvent) {
                        handler.vmDeathEvent((VMDeathEvent) event);
                    } else if (event instanceof VMDisconnectEvent) {
                        handler.vmDisconnectEvent((VMDisconnectEvent) event);
                    }
                }
                eventSet.resume(); // Resume the VM
            } catch (InterruptedException exc) {
                // ignore
            }
        }
    }
}
