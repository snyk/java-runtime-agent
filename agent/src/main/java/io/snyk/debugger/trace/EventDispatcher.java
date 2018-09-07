package io.snyk.debugger.trace;

import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.ClassPrepareRequest;
import com.sun.jdi.request.EventRequestManager;
import io.snyk.agent.filter.Filter;
import io.snyk.agent.filter.PathFilter;
import io.snyk.agent.logic.Config;

import java.util.Collections;
import java.util.List;

public class EventDispatcher extends Thread {

    private final VirtualMachine vm;

    private final Config config;

    private final EventVisitor handler = new EventVisitor() {
        @Override
        public void classPrepareEvent(ClassPrepareEvent event) {
            final ThreadReference thread = event.thread();
            final ReferenceType dt = event.referenceType();
            final ClassLoaderReference loader = dt.classLoader();
            if (null == loader) {
                System.err.println(dt + " has no classloader");
                return;
            }
            final List<Method> getResourceMethods = loader.referenceType()
                    .methodsByName("getResource", "(Ljava/lang/String;)Ljava/net/URL;");
            final Method getResource = getResourceMethods.get(0);
            try {
                System.err.println("going to fetch url for " + dt);
                final ObjectReference maybeUrl = (ObjectReference) loader.invokeMethod(thread,
                        getResource,
                        Collections.singletonList(vm.mirrorOf(dt.name())),
                        0);
                final List<Method> toStringMethods = maybeUrl.referenceType()
                        .methodsByName("toString", "()Ljava/lang/String;");
                final Method toString = toStringMethods.get(0);
                final StringReference maybeString = (StringReference) maybeUrl.invokeMethod(thread,
                        toString,
                        Collections.emptyList(),
                        0);
                System.err.println("url for " + dt + ": " + maybeString.value());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void breakpointEvent(BreakpointEvent event) {
            final Location location = event.location();
            final ReferenceType dt = location.declaringType();
            System.err.println(dt + "#" + location.method().name());
            vm.eventRequestManager().deleteEventRequest(event.request());
        }
    };

    private boolean connectedToVm = true;

    EventDispatcher(VirtualMachine vm, Config config) {
        super("event-dispatcher");
        this.vm = vm;
        this.config = config;

        final ClassPrepareRequest allClasses = vm.eventRequestManager().createClassPrepareRequest();
        allClasses.addClassExclusionFilter("java.*");
        allClasses.enable();
    }

    @Override
    public void run() {
        final EventQueue queue = vm.eventQueue();
        while (connectedToVm) {
            try {
                final EventSet eventSet = queue.remove(4 * 1000);
                if (null == eventSet) {

                    System.err.println("adding watches...");
                    doAddWatches();

                    continue;
                }
                final EventIterator it = eventSet.eventIterator();
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
    }

    private void doAddWatches() {
        final EventRequestManager mgr = vm.eventRequestManager();
        mgr.deleteAllBreakpoints();
        for (Filter filter : config.filters) {
            for (PathFilter pathFilter : filter.pathFilters) {
                final String className = pathFilter.className.replace('/', '.');
                if (className.contains("*")) {
                    throw new IllegalStateException("not currently supported: " + className);
                }
                final List<ReferenceType> referenceTypes = vm.classesByName(className);
                if (null == referenceTypes) {
                    // TODO: probably not reachable
                    continue;
                }

                for (ReferenceType referenceType : referenceTypes) {
                    referenceType.methodsByName(pathFilter.methodName.get())
                            .stream()
                            .filter(method -> !(method.isNative() || method.isAbstract() || method.isBridge()))
                            .forEach(method -> mgr.createBreakpointRequest(method.location()).enable());
                }
            }
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
