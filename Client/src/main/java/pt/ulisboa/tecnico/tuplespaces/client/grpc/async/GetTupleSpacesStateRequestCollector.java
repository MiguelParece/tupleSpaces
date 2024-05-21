package pt.ulisboa.tecnico.tuplespaces.client.grpc.async;

import java.util.List;
import java.util.ArrayList;
import pt.ulisboa.tecnico.tuplespaces.replicaTotalOrder.contract.getTupleSpacesStateResponse;

public class GetTupleSpacesStateRequestCollector<R> extends ResponseCollector<R> {

    private List<String> responses = new ArrayList<>();

    @Override
    public void addResponse(R r) {
        getTupleSpacesStateResponse _aux = (getTupleSpacesStateResponse) r;
        this.responses = _aux.getTupleList();
        super.incrementResponse();
    }

    @Override
    public void processResponses() {}

    public List<String> getResponses() {
        return this.responses;
    }

}

