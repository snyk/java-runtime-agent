package io.snyk.debugger.trace;

import com.sun.jdi.Bootstrap;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;
import com.sun.jdi.connect.LaunchingConnector;
import com.sun.jdi.connect.VMStartException;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

public class Trace {

    private final VirtualMachine vm;

    private Thread outThread;
    private Thread errThread;

    // Mode for tracing the Trace program (default= 0 off)
    private int debugTraceMode = 0;

    //  Do we want to watch assignments to fields
    private boolean watchFields = false;

    // Class patterns for which we don't want events
    private String[] excludes = {"javax.*", "sun.*",
            "com.sun.*"};

    public static void main(String[] args) throws Exception {
        new Trace(args[0]).run(new PrintWriter(System.err));
    }


    private Trace(String arg) throws IllegalConnectorArgumentsException, VMStartException, IOException {
        vm = launchTarget(arg);
    }

    private void run(PrintWriter writer) {
        vm.setDebugTraceMode(debugTraceMode);

        final EventDispatcher eventDispatcher = new EventDispatcher(vm, excludes, writer);
        eventDispatcher.setEventRequests(watchFields);
        eventDispatcher.start();
        redirectOutput();
        vm.resume();

        try {
            eventDispatcher.join();
            errThread.join(); // Make sure output is forwarded
            outThread.join(); // before we exit
        } catch (InterruptedException exc) {
            throw new IllegalStateException(exc);
        }
    }

    private VirtualMachine launchTarget(String mainArgs)
            throws VMStartException, IllegalConnectorArgumentsException, IOException {
        final LaunchingConnector connector = findLaunchingConnector();
        final Map<String, Connector.Argument> arguments =
                connectorArguments(connector, mainArgs);
        return connector.launch(arguments);
    }

    private void redirectOutput() {
        final Process process = vm.process();

        // Copy target's output and error to our output and error.
        errThread = new DuplicateStreams("error reader",
                process.getErrorStream(),
                System.err);
        outThread = new DuplicateStreams("output reader",
                process.getInputStream(),
                System.out);
        errThread.start();
        outThread.start();
    }

    private static LaunchingConnector findLaunchingConnector() {
        return (LaunchingConnector) Bootstrap.virtualMachineManager()
                .allConnectors()
                .stream()
                .filter(c -> "com.sun.jdi.CommandLineLaunch".equals(c.name()))
                .findFirst()
                .get();
    }

    private Map<String, Connector.Argument> connectorArguments(LaunchingConnector connector, String mainArgs) {
        final Map<String, Connector.Argument> arguments = connector.defaultArguments();
        final Connector.Argument mainArg = arguments.get("main");

        if (mainArg == null) {
            throw new Error("Bad launching connector");
        }
        mainArg.setValue(mainArgs);

        if (watchFields) {
            // We need a VM that supports watchpoints
            Connector.Argument optionArg = arguments.get("options");
            if (optionArg == null) {
                throw new Error("Bad launching connector");
            }
            optionArg.setValue("-classic");
        }
        return arguments;
    }
}
