package io.snyk.debugger.trace;

import com.sun.jdi.Bootstrap;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.connect.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Map;

public class Trace {

    private final VirtualMachine vm;

    private Thread outThread;
    private Thread errThread;

    // Mode for tracing the Trace program (default= 0 off)
    private int debugTraceMode = 0;

    public static void main(String[] args) throws Exception {
        new Trace(Integer.parseInt(args[0])).run(new PrintWriter(System.err));
    }

    private Trace(String arg) throws IllegalConnectorArgumentsException, VMStartException, IOException {
        final LaunchingConnector connector = findLaunchingConnector();
        final Map<String, Connector.Argument> arguments = connector.defaultArguments();

        {
            final Connector.Argument mainArg = arguments.get("main");

            if (mainArg == null) {
                throw new IllegalStateException("LaunchConnector lacks 'main'");
            }
            mainArg.setValue(arg);
        }

        vm = connector.launch(arguments);
    }

    private Trace(int port) throws IOException, IllegalConnectorArgumentsException {
        final AttachingConnector connector = findSocketAttachConnector();

        final Map<String, Connector.Argument> arguments = connector.defaultArguments();

        {
            final Connector.Argument hostnameArg = arguments.get("hostname");
            if (null == hostnameArg) {
                throw new IllegalStateException("AttachConnector lacks 'hostname'");
            }
            hostnameArg.setValue("127.0.0.1");
        }

        {
            final Connector.Argument portArg = arguments.get("port");
            if (null == portArg) {
                throw new IllegalStateException("AttachConnector lacks 'port'");
            }
            portArg.setValue(String.valueOf(port));
        }

        vm = connector.attach(arguments);
    }

    private void run(PrintWriter writer) {
        vm.setDebugTraceMode(debugTraceMode);

        final EventDispatcher eventDispatcher = new EventDispatcher(vm, writer);
        eventDispatcher.addClassWatches(Arrays.asList(
                "org.apache.struts2.dispatcher.multipart.JakartaMultiPartRequest",
                "com.google.common.collect.Lists"
        ));

        eventDispatcher.start();
        if (false) {
            redirectOutput();
        }
        vm.resume();

        try {
            eventDispatcher.join();

            if (null != errThread) {
                errThread.join(); // Make sure output is forwarded
            }

            if (null != outThread) {
                outThread.join(); // before we exit
            }
        } catch (InterruptedException exc) {
            throw new IllegalStateException(exc);
        }
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

    /*
    com.sun.jdi.CommandLineLaunch (defaults: home=/usr/lib/jvm/java-8-openjdk-amd64/jre, options=, main=, suspend=true, quote=", vmexec=java)
    com.sun.jdi.RawCommandLineLaunch (defaults: command=, quote=", address=)
    com.sun.jdi.SocketAttach (defaults: timeout=, hostname=anoia.goeswhere.com, port=)
    com.sun.jdi.SocketListen (defaults: timeout=, port=, localAddress=)
    com.sun.jdi.ProcessAttach (defaults: pid=, timeout=)
     */

    private static LaunchingConnector findLaunchingConnector() {
        return Bootstrap.virtualMachineManager()
                .launchingConnectors()
                .stream()
                .filter(c -> "com.sun.jdi.CommandLineLaunch".equals(c.name()))
                .findFirst()
                .get();
    }

    private static AttachingConnector findSocketAttachConnector() {
        return Bootstrap.virtualMachineManager()
                .attachingConnectors()
                .stream()
                .filter(c -> "com.sun.jdi.SocketAttach".equals(c.name()))
                .findFirst()
                .get();
    }
}
