package pt.ulisboa.tecnico.tuplespaces.client.grpc.async;

public abstract class ResponseCollector<R> {

    private int counter = 0, waitingFor = 0;
    private boolean abort = false;

    public abstract void addResponse(R r);
    public abstract void processResponses();

    public synchronized void incrementResponse() {
        this.counter += 1;
        if (this.counter >= this.waitingFor) {
            notifyAll();
        }
    }

    public synchronized void waitUntilAllReceived(int waitingFor) {
        this.waitingFor = waitingFor;
        try {
            if (this.abort == false && this.counter < this.waitingFor)
                wait();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public synchronized void resetCounter() {
        this.counter = 0;
        this.abort = false;
    }

    public synchronized void abortCollection() {
        // errors were found connecting to servers
        // release whoever is waiting
        this.abort = true;
        notifyAll();
    }

    public boolean aborted() {
        return this.abort;
    }
}

