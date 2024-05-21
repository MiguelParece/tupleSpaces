package pt.ulisboa.tecnico.tuplespaces.client.grpc.async;

import io.grpc.stub.StreamObserver;


public class ResponseObserver<R> implements StreamObserver<R> {

    ResponseCollector<R> collector;
    R response;
    boolean arrived = false;

    public ResponseObserver(ResponseCollector<R> collector) {
        this.collector = collector;
    }

    @Override
    public void onNext(R r) {
        this.response = r;
        this.collector.addResponse(r);
        synchronized (this) {
            this.arrived = true;
            notifyAll();
        }
    }

    @Override
    public void onError(Throwable throwable) {
        System.out.println("Received error: " + throwable);
        this.collector.abortCollection();
    }

    @Override
    public void onCompleted() {}

    public synchronized void waitResponse() {
        if (this.arrived == true)
            return;
        try {
            wait();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public R getResponse() {
        return this.response;
    }
}
