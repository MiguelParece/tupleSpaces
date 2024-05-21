package pt.ulisboa.tecnico.tuplespaces.sequencer;

import pt.ulisboa.tecnico.sequencer.contract.GetSeqNumberRequest;
import pt.ulisboa.tecnico.sequencer.contract.GetSeqNumberResponse;

import io.grpc.stub.StreamObserver;
import pt.ulisboa.tecnico.sequencer.contract.SequencerGrpc.SequencerImplBase;

import java.util.List;

public class SequencerServiceImpl extends SequencerImplBase {

	int seqNumber;

	public SequencerServiceImpl() {
		seqNumber = 0;
	}

	@Override
	synchronized public void getSeqNumber(GetSeqNumberRequest request,
			StreamObserver<GetSeqNumberResponse> responseObserver) {
		seqNumber++;
		GetSeqNumberResponse response = GetSeqNumberResponse.newBuilder()
				.setSeqNumber(seqNumber).build();
		responseObserver.onNext(response);
		responseObserver.onCompleted();
	}

}
