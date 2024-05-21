package pt.ulisboa.tecnico.tuplespaces.server;

import pt.ulisboa.tecnico.tuplespaces.server.domain.ServerState;
import io.grpc.stub.StreamObserver;
import pt.ulisboa.tecnico.tuplespaces.replicaTotalOrder.contract.*;
import java.util.regex.*;
import java.util.List;

import static io.grpc.Status.INVALID_ARGUMENT;

public class ServiceImpl extends TupleSpacesReplicaGrpc.TupleSpacesReplicaImplBase {

	/** Server Domain implementation. */
	private ServerState tuple_space = new ServerState();

	@Override
	public void put(PutRequest request, StreamObserver<PutResponse> responseObserver) {
		String _new_tuple = request.getNewTuple();
		if (_new_tuple.isEmpty() || !isValidTuple(_new_tuple)) {
			responseObserver.onError(
					INVALID_ARGUMENT.withDescription("Input must have a tuple format: <param1,param2,...,paramN>")
							.asRuntimeException());
		} else {

			synchronized (this) {
				tuple_space.put(_new_tuple);
				// Notify clients waiting for tuples with a specific format
				notifyAll();
			}

			PutResponse response = PutResponse.newBuilder().build();
			responseObserver.onNext(response);
			responseObserver.onCompleted();
		}
	}

	@Override
	public void read(ReadRequest request, StreamObserver<ReadResponse> responseObserver) {
		String pattern = request.getSearchPattern();

		if (pattern.isEmpty() || (!isValidTuple(pattern) && !isValidPattern(pattern))) {
			responseObserver.onError(INVALID_ARGUMENT
					.withDescription("Pattern must be coherent with a tuple format: <param1,param2,...,paramN>")
					.asRuntimeException());
		} else {

			String result;
			synchronized (this) {
				result = tuple_space.read(pattern);
				while (result == null) {
					try {
						wait();
					} catch (InterruptedException e) {
					}
					result = tuple_space.read(pattern);
				}
			}


			ReadResponse response = ReadResponse.newBuilder().setResult(result).build();
			responseObserver.onNext(response);
			responseObserver.onCompleted();
		}
	}

	@Override
	public void takePhase1(TakePhase1Request request, StreamObserver<TakePhase1Response> responseObserver) {
		String pattern = request.getSearchPattern();

		if (pattern.isEmpty() || (!isValidTuple(pattern) && !isValidPattern(pattern))) {
			responseObserver.onError(INVALID_ARGUMENT
					.withDescription("Pattern must be coherent with a tuple format: <param1,param2,...,paramN>")
					.asRuntimeException());
		} else {
			List<String> result;
			synchronized (this) {
				result = tuple_space.lock(pattern, request.getClientId());
			}

			TakePhase1Response response = TakePhase1Response.newBuilder().addAllReservedTuples(result).build();
			responseObserver.onNext(response);
			responseObserver.onCompleted();
		}
	}

	@Override
	public void takePhase1Release(TakePhase1ReleaseRequest request,
			StreamObserver<TakePhase1ReleaseResponse> responseObserver) {
		int clientId = request.getClientId();
		String tuple = request.getTuple();

		if (tuple.isEmpty() || !isValidTuple(tuple)) {
			synchronized (this) {
				tuple_space.release(clientId);
			}
		} else {
			synchronized (this) {
				tuple_space.release(clientId, tuple);
			}
		}

		TakePhase1ReleaseResponse response = TakePhase1ReleaseResponse.newBuilder().build();
		responseObserver.onNext(response);
		responseObserver.onCompleted();
	}

	@Override
	public void takePhase2(TakePhase2Request request, StreamObserver<TakePhase2Response> responseObserver) {
		int clientId = request.getClientId();
		String tuple = request.getTuple();

		if (tuple.isEmpty() || !isValidTuple(tuple)) {
			responseObserver.onError(INVALID_ARGUMENT
					.withDescription("Tuple must be coherent with a tuple format: <param1,param2,...,paramN>")
					.asRuntimeException());
		} else {
			synchronized (this) {
				tuple_space.take(tuple, clientId);
			}

			TakePhase2Response response = TakePhase2Response.newBuilder().build();
			responseObserver.onNext(response);
			responseObserver.onCompleted();
		}
	}

	@Override
	public synchronized void getTupleSpacesState(getTupleSpacesStateRequest request,
			StreamObserver<getTupleSpacesStateResponse> responseObserver) {
		getTupleSpacesStateResponse response = getTupleSpacesStateResponse.newBuilder()
				.addAllTuple(tuple_space.getTupleSpacesState()).build();
		responseObserver.onNext(response);
		responseObserver.onCompleted();
	}

	// This method verifies if given tuple has indeed a tuple format
	private static boolean isValidTuple(String possible_tuple) {
		String pattern = "<(?:[a-zA-Z0-9]+,)*[a-zA-Z0-9]+>";
		return possible_tuple.matches(pattern) || possible_tuple.equals("<>");
	}

	private static boolean isValidPattern(String possible_pattern) {
		String patternPattern = "<(?:\\\\.|[^\\\\>])+>";
		return possible_pattern.matches(patternPattern);
	}
}
