package io.snyk.agent.bp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

class DuplicateStreams extends Thread {
    private static final int BUFFER_SIZE = 4096;
    private final InputStream in;
    private final OutputStream out;

    DuplicateStreams(String name, InputStream in, OutputStream out) {
        super(name);
        this.in = in;
        this.out = out;
    }

    @Override
    public void run() {
        try {
            byte[] cbuf = new byte[BUFFER_SIZE];
            while (true) {
                int count = in.read(cbuf, 0, BUFFER_SIZE);

                if (count <= 0) {
                    break;
                }

                out.write(cbuf, 0, count);
            }
        } catch (IOException exc) {
            System.err.println(Thread.currentThread().getName() + ": error transferring");
            exc.printStackTrace();
        }
    }
}
