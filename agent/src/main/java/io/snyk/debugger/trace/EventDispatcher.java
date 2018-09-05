package io.snyk.debugger.trace;

import com.sun.jdi.Method;
import com.sun.jdi.VMDisconnectedException;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.event.*;
import com.sun.jdi.request.EventRequest;
import com.sun.jdi.request.EventRequestManager;
import com.sun.jdi.request.MethodEntryRequest;

import java.io.PrintWriter;

public class EventDispatcher extends Thread {

    private final VirtualMachine vm;   // Running VM
    private final PrintWriter writer;  // Where output goes

    private final EventVisitor handler = new EventVisitor() {
        @Override
        public void methodEntryEvent(MethodEntryEvent event) {
            final Method method = event.method();
            System.err.println(method.declaringType() + "#" + method.name());
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
            final MethodEntryRequest menr = mgr.createMethodEntryRequest();
            menr.addClassFilter(pattern);
            menr.setSuspendPolicy(EventRequest.SUSPEND_NONE);
            menr.enable();
        }
    }

    private void dispatchEvent(Event event) {
        if (event instanceof ExceptionEvent) {
            handler.exceptionEvent((ExceptionEvent) event);
        } else if (event instanceof ModificationWatchpointEvent) {
            handler.fieldWatchEvent((ModificationWatchpointEvent) event);
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
