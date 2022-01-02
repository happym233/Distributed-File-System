package storage;

import rmi.Status;

public class StorageServerStatusController {

    private Status status;

    public StorageServerStatusController() {
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
        if (isStopped()) {
            System.out.println("This skeleton have been stopped");
        }
        this.status = Status.STOPPED;
    }

    public synchronized void start() {
        if (isRunning()) {
            System.out.println("This skeleton is running.");
        }
        this.status = Status.RUNNING;
    }


}
