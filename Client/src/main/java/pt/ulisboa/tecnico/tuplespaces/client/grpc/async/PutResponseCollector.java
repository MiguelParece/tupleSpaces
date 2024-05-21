package pt.ulisboa.tecnico.tuplespaces.client.grpc.async;

public class PutResponseCollector<R> extends ResponseCollector<R> {

    @Override
    public void addResponse(R r) {
        super.incrementResponse();
    }

    @Override
    public void processResponses() {
        if(this.aborted()) {
            System.out.println("Operation failed.");
        }
    }

}

