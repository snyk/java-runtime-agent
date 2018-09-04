package io.snyk.debugger.trace;

import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EventDispatcher extends Thread {

    private final VirtualMachine vm;   // Running VM
    private final String[] excludes;   // Packages to exclude
    private final PrintWriter writer;  // Where output goes

    private static String nextBaseIndent = ""; // Starting indent for next thread

    private boolean connected = true;  // Connected to VM
    private boolean vmDied = true;     // VMDeath occurred

    private Map<ThreadReference, ThreadTrace> traceMap = new HashMap<>();

    EventDispatcher(VirtualMachine vm, String[] excludes, PrintWriter writer) {
        super("event-dispatcher");
        this.vm = vm;
        this.excludes = excludes;
        this.writer = writer;
    }

    @Override
    public void run() {
        EventQueue queue = vm.eventQueue();
        while (connected) {
            try {
                EventSet eventSet = queue.remove();
                EventIterator it = eventSet.eventIterator();
                while (it.hasNext()) {
                    handleEvent(it.nextEvent());
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
     *
     * @param watchFields Do we want to watch assignments to fields
     */
    void setEventRequests(boolean watchFields) {
        EventRequestManager mgr = vm.eventRequestManager();

        // want all exceptions
        ExceptionRequest excReq = mgr.createExceptionRequest(null,
                true, true);
        // suspend so we can step
        excReq.setSuspendPolicy(EventRequest.SUSPEND_ALL);
        excReq.enable();

        MethodEntryRequest menr = mgr.createMethodEntryRequest();
        for (String exclude : excludes) {
            menr.addClassExclusionFilter(exclude);
        }
        menr.setSuspendPolicy(EventRequest.SUSPEND_NONE);
        menr.enable();

        MethodExitRequest mexr = mgr.createMethodExitRequest();
        for (String exclude : excludes) {
            mexr.addClassExclusionFilter(exclude);
        }
        mexr.setSuspendPolicy(EventRequest.SUSPEND_NONE);
        mexr.enable();

        ThreadDeathRequest tdr = mgr.createThreadDeathRequest();
        // Make sure we sync on thread death
        tdr.setSuspendPolicy(EventRequest.SUSPEND_ALL);
        tdr.enable();

        if (watchFields) {
            ClassPrepareRequest cpr = mgr.createClassPrepareRequest();
            for (String exclude : excludes) {
                cpr.addClassExclusionFilter(exclude);
            }
            cpr.setSuspendPolicy(EventRequest.SUSPEND_ALL);
            cpr.enable();
        }
    }

    /**
     * This class keeps context on events in one thread.
     * In this implementation, context is the indentation prefix.
     */
    class ThreadTrace {
        final ThreadReference thread;
        final String baseIndent;
        static final String threadDelta = "                     ";
        StringBuffer indent;

        ThreadTrace(ThreadReference thread) {
            this.thread = thread;
            this.baseIndent = nextBaseIndent;
            indent = new StringBuffer(baseIndent);
            nextBaseIndent += threadDelta;
            println("====== " + thread.name() + " ======");
        }

        private void println(String str) {
            writer.print(indent);
            writer.println(str);
        }

        void methodEntryEvent(MethodEntryEvent event) {
            println(event.method().name() + "  --  "
                    + event.method().declaringType().name());
            indent.append("| ");
        }

        void methodExitEvent(MethodExitEvent event) {
            indent.setLength(indent.length() - 2);
        }

        void fieldWatchEvent(ModificationWatchpointEvent event) {
            Field field = event.field();
            Value value = event.valueToBe();
            println("    " + field.name() + " = " + value);
        }

        void exceptionEvent(ExceptionEvent event) {
            println("Exception: " + event.exception() +
                    " catch: " + event.catchLocation());

            // Step to the catch
            EventRequestManager mgr = vm.eventRequestManager();
            StepRequest req = mgr.createStepRequest(thread,
                    StepRequest.STEP_MIN,
                    StepRequest.STEP_INTO);
            req.addCountFilter(1);  // next step only
            req.setSuspendPolicy(EventRequest.SUSPEND_ALL);
            req.enable();
        }

        // Step to exception catch
        void stepEvent(StepEvent event) {
            // Adjust call depth
            int cnt = 0;
            indent = new StringBuffer(baseIndent);
            try {
                cnt = thread.frameCount();
            } catch (IncompatibleThreadStateException ignored) {
            }
            while (cnt-- > 0) {
                indent.append("| ");
            }

            EventRequestManager mgr = vm.eventRequestManager();
            mgr.deleteEventRequest(event.request());
        }

        void threadDeathEvent(ThreadDeathEvent event) {
            indent = new StringBuffer(baseIndent);
            println("====== " + thread.name() + " end ======");
        }
    }

    /**
     * Returns the ThreadTrace instance for the specified thread,
     * creating one if needed.
     */
    private ThreadTrace threadTrace(ThreadReference thread) {
        ThreadTrace trace = traceMap.get(thread);
        if (trace == null) {
            trace = new ThreadTrace(thread);
            traceMap.put(thread, trace);
        }
        return trace;
    }

    /**
     * Dispatch incoming events
     */
    private void handleEvent(Event event) {
        if (event instanceof ExceptionEvent) {
            exceptionEvent((ExceptionEvent) event);
        } else if (event instanceof ModificationWatchpointEvent) {
            fieldWatchEvent((ModificationWatchpointEvent) event);
        } else if (event instanceof MethodEntryEvent) {
            methodEntryEvent((MethodEntryEvent) event);
        } else if (event instanceof MethodExitEvent) {
            methodExitEvent((MethodExitEvent) event);
        } else if (event instanceof StepEvent) {
            stepEvent((StepEvent) event);
        } else if (event instanceof ThreadDeathEvent) {
            threadDeathEvent((ThreadDeathEvent) event);
        } else if (event instanceof ClassPrepareEvent) {
            classPrepareEvent((ClassPrepareEvent) event);
        } else if (event instanceof VMStartEvent) {
            vmStartEvent((VMStartEvent) event);
        } else if (event instanceof VMDeathEvent) {
            vmDeathEvent((VMDeathEvent) event);
        } else if (event instanceof VMDisconnectEvent) {
            vmDisconnectEvent((VMDisconnectEvent) event);
        } else {
            throw new Error("Unexpected event type");
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
        while (connected) {
            try {
                EventSet eventSet = queue.remove();
                EventIterator iter = eventSet.eventIterator();
                while (iter.hasNext()) {
                    Event event = iter.nextEvent();
                    if (event instanceof VMDeathEvent) {
                        vmDeathEvent((VMDeathEvent) event);
                    } else if (event instanceof VMDisconnectEvent) {
                        vmDisconnectEvent((VMDisconnectEvent) event);
                    }
                }
                eventSet.resume(); // Resume the VM
            } catch (InterruptedException exc) {
                // ignore
            }
        }
    }

    private void vmStartEvent(VMStartEvent event) {
        writer.println("-- VM Started --");
    }

    private void methodEntryEvent(MethodEntryEvent event) {
        threadTrace(event.thread()).methodEntryEvent(event);
    }

    private void methodExitEvent(MethodExitEvent event) {
        threadTrace(event.thread()).methodExitEvent(event);
    }

    private void stepEvent(StepEvent event) {
        threadTrace(event.thread()).stepEvent(event);
    }

    private void fieldWatchEvent(ModificationWatchpointEvent event) {
        threadTrace(event.thread()).fieldWatchEvent(event);
    }

    void threadDeathEvent(ThreadDeathEvent event) {
        ThreadTrace trace = traceMap.get(event.thread());
        if (trace != null) {  // only want threads we care about
            trace.threadDeathEvent(event);   // Forward event
        }
    }

    /**
     * A new class has been loaded.
     * Set watchpoints on each of its fields
     */
    private void classPrepareEvent(ClassPrepareEvent event) {
        EventRequestManager mgr = vm.eventRequestManager();
        List<Field> fields = event.referenceType().visibleFields();
        for (Field field : fields) {
            ModificationWatchpointRequest req =
                    mgr.createModificationWatchpointRequest(field);
            for (String exclude : excludes) {
                req.addClassExclusionFilter(exclude);
            }
            req.setSuspendPolicy(EventRequest.SUSPEND_NONE);
            req.enable();
        }
    }

    private void exceptionEvent(ExceptionEvent event) {
        ThreadTrace trace = traceMap.get(event.thread());
        if (trace != null) {  // only want threads we care about
            trace.exceptionEvent(event);      // Forward event
        }
    }

    private void vmDeathEvent(VMDeathEvent event) {
        vmDied = true;
        writer.println("-- The application exited --");
    }

    private void vmDisconnectEvent(VMDisconnectEvent event) {
        connected = false;
        if (!vmDied) {
            writer.println("-- The application has been disconnected --");
        }
    }
}
