package rmi;

public class StatusController {

    private Status status;

    public StatusController() {
        this.status = Status.STOPPED;
    }

    public boolean isStopped() {
        return this.status == Status.STOPPED;
    }

    public boolean isStopping() { return this.status == Status.STOPPING; }

    public boolean isRunning() {
        return this.status == Status.RUNNING;
    }


    public synchronized void stop() {
        if (isStopped() || isStopping()) {
            System.out.println("This skeleton have been stopped");
        }
        this.status = Status.STOPPING;
    }

    public synchronized void start() {
        if (isRunning()) {
            System.out.println("This skeleton is running.");
        }
        if (isStopping()) {
            System.out.println("This skeleton is stopping.");
        }
        this.status = Status.RUNNING;
    }

    public synchronized void stopped() {
        if (isRunning()) {
            System.out.println("This skeleton is running. Please stop first.");
        }
        if (isStopped()) {
            System.out.println("This skeleton have been stopped.");
        }

        this.status = Status.STOPPED;
    }

}
