package io.snyk.agent.dynamic;

import com.sun.tools.attach.VirtualMachine;

import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;

public class Startup {
    public static Startup activate() {
        try {
            final VirtualMachine vm = VirtualMachine.attach(guessPid());
            final URLConnection codeSource = Startup.class.getProtectionDomain().getCodeSource().getLocation().openConnection();
            codeSource.connect();
            final Path tempFile = Files.createTempFile("snyk-runtime", ".jar");
            Files.delete(tempFile);
            try (InputStream in = codeSource.getInputStream()) {
                Files.copy(in, tempFile);
            }
            vm.loadAgent(tempFile.toAbsolutePath().toString());
        } catch (Exception e) {
            throw new IllegalStateException("setup failed", e);
        }
        return null;
    }

    private static String guessPid() {
        final String nameOfRunningVM = ManagementFactory.getRuntimeMXBean().getName();
        final int pos = nameOfRunningVM.indexOf('@');
        if (-1 == pos) {
            throw new IllegalStateException("unsupported vm name: " + nameOfRunningVM);
        }
        return nameOfRunningVM.substring(0, pos);
    }
}
