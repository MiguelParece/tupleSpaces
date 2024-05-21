package pt.ulisboa.tecnico.tuplespaces.sequencer;

import pt.ulisboa.tecnico.sequencer.contract.*;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class SequencerService {
    // this will serve like an api for using the sequencer
    private SequencerGrpc.SequencerBlockingStub stub;
    private ManagedChannel channel;
    private String service_address;

    public SequencerService() {
        // get the server data from the name server
        // assume that there is always 1 sequencer available
        final NameServerService nameServer = new NameServerService();

        service_address = nameServer.lookup("Sequencer").get(0);
        // create channel
        channel = ManagedChannelBuilder.forTarget(service_address).usePlaintext().build();
        stub = SequencerGrpc.newBlockingStub(channel);
        nameServer.endService();
    }

    public void endService() {
        // close the channel
        channel.shutdownNow();
    }

    public int getSeqNumber() {
        // get the next sequence number
        GetSeqNumberRequest request = GetSeqNumberRequest.newBuilder().build();
        GetSeqNumberResponse response = this.stub.getSeqNumber(request);
        System.out.println("Sequence number: " + response.getSeqNumber());

        return response.getSeqNumber();
    }

}
