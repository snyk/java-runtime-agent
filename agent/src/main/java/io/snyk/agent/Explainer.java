package io.snyk.agent;

class Explainer implements Runnable {
    @Override
    public void run() {
        while (true) {
            try {
                work();
            } catch (Throwable t) {
                System.err.println("agent issue");
                t.printStackTrace();
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    void work() {
        System.err.println("I've seen " + Tracker.SEEN_SET.size() + " objects");
    }
}
