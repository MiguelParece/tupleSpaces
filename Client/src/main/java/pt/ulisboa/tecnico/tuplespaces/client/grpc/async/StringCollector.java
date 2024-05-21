package pt.ulisboa.tecnico.tuplespaces.client.grpc.async;

import java.util.List;
import java.util.ArrayList;
import pt.ulisboa.tecnico.tuplespaces.replicaTotalOrder.contract.ReadResponse;
import pt.ulisboa.tecnico.tuplespaces.replicaTotalOrder.contract.TakeResponse;

public class StringCollector<R> extends ResponseCollector<R> {

    private List<String> responses = new ArrayList<>();
    private int expectedResponses = 0;

    public StringCollector(int maxNumberOfResponses) {
        this.expectedResponses = maxNumberOfResponses;
    }

    @Override
    public void addResponse(R r) {
        if (r instanceof TakeResponse) {
            TakeResponse _aux = (TakeResponse) r;
            this.responses.add(_aux.getResult().toString());
        } else {
            ReadResponse _aux = (ReadResponse) r;
            this.responses.add(_aux.getResult().toString());
        }
        super.incrementResponse();
    }

    @Override
    public void processResponses() {
        if (responses.size() >= this.expectedResponses) {
            System.out.println(this.responses.get(0).toString());
        } else if(this.aborted()) {
            System.out.println("Operation failed.");
        } else {
            System.out.println("No response was given.");
        }
    }

    @Override
    public synchronized void abortCollection() {
        // only abort if we know all servers are down
        this.expectedResponses -= 1;
        if(this.expectedResponses <= 0) {
            super.abortCollection();
        }
    }

}

