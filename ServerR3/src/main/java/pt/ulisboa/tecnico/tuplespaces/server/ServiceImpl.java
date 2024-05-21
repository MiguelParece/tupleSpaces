package pt.ulisboa.tecnico.tuplespaces.server;

import pt.ulisboa.tecnico.tuplespaces.server.domain.ServerState;
import io.grpc.stub.StreamObserver;
import pt.ulisboa.tecnico.tuplespaces.replicaTotalOrder.contract.*;
import java.util.regex.*;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.ArrayList;

import static io.grpc.Status.INVALID_ARGUMENT;

class Waiting {
	private Condition matchCondition;
	private String pattern;

	Waiting(String pattern, ReentrantReadWriteLock lock) {
		this.matchCondition = lock.writeLock().newCondition();
		this.pattern = pattern;
	}

	boolean matches(String pattern) {
		return pattern.matches(this.pattern);
	}

	void wakeUp() {
		this.matchCondition.signal();
	}

	void waitTuple() throws InterruptedException {
		this.matchCondition.await();
	}
}

public class ServiceImpl extends TupleSpacesReplicaGrpc.TupleSpacesReplicaImplBase {

	/** Server Domain implementation. */
	private ServerState tuple_space = new ServerState();
	private int serverNextState = 1;
	private List<Waiting> waitingList = new ArrayList<>();
	private List<Waiting> waitingTakeList = new ArrayList<>();
	private ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
	private Condition ServerStateOrder = lock.writeLock().newCondition();
	private List<String> WaitingTakeListB = new ArrayList<>();

	@Override
	public void put(PutRequest request, StreamObserver<PutResponse> responseObserver) {
		// get tuple
		String _new_tuple = request.getNewTuple();
		// get sequence number
		int _seq_number = request.getSeqNumber();

		if (_new_tuple.isEmpty() || !isValidTuple(_new_tuple)) {
			responseObserver.onError(
					INVALID_ARGUMENT.withDescription("Input must have a tuple format: <param1,param2,...,paramN>")
							.asRuntimeException());
		} else {

			this.lock.writeLock().lock();
			try {
				while (_seq_number != serverNextState) {
					this.ServerStateOrder.await();
				}

				System.out.println("exec Put");

				// check if waitingtakelist has any tuple that matches the new tuple
				for (Waiting w : waitingTakeList) {
					if (w.matches(_new_tuple)) {
						System.out.println("Wake up waiting takes");
						w.wakeUp();
						PutResponse result = PutResponse.newBuilder().build();
						responseObserver.onNext(result);
						responseObserver.onCompleted();
						return;
					}
				}

				tuple_space.put(_new_tuple);
				// todo notificar waiting reads and takes
				for (Waiting w : waitingList) {

					if (w.matches(_new_tuple)) {
						System.out.println("Wake up waiting read");
						w.wakeUp();
					}

				}

				serverNextState++;
				this.ServerStateOrder.signal();

			} catch (InterruptedException e) {
				System.out.println("Error in put");
			} finally {
				this.lock.writeLock().unlock();
			}

			PutResponse result = PutResponse.newBuilder().build();
			responseObserver.onNext(result);
			responseObserver.onCompleted();

		}
	}

	@Override
	public void take(TakeRequest request, StreamObserver<TakeResponse> responseObserver) {
		// get tuple
		String pattern = request.getSearchPattern();
		// get sequence number
		int _seq_number = request.getSeqNumber();

		if (pattern.isEmpty() || !isValidPattern(pattern)) {
			responseObserver.onError(
					INVALID_ARGUMENT
							.withDescription("Pattern must be coherent with a tuple format: <param1,param2,...,paramN>")
							.asRuntimeException());
		} else {
			this.lock.writeLock().lock();
			String result;

			while (_seq_number != serverNextState) {
				try {
					this.ServerStateOrder.await();
				} catch (InterruptedException e) {
					System.out.println("Error in take");
				}
			}
			result = tuple_space.take(pattern);

			this.serverNextState++;
			this.ServerStateOrder.signal();

			if (result == null) {
				Waiting w = new Waiting(pattern, this.lock);

				waitingTakeList.add(w);

				System.out.println("Waiting for tuple");
				try {
					w.waitTuple();
				} catch (InterruptedException e) {
					System.out.println("Error in take");
				}
				result = pattern;

				this.waitingTakeList.remove(w);
			}

			this.lock.writeLock().unlock();

			TakeResponse response = TakeResponse.newBuilder().setResult(result).build();
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

			this.lock.writeLock().lock();
			String result;

			result = tuple_space.read(pattern);
			if (result == null) {

				Waiting w = new Waiting(pattern, this.lock);
				waitingList.add(w);

				while (result == null) {
					try {
						w.waitTuple();
						result = tuple_space.read(pattern);
					} catch (InterruptedException e) {
						System.out.println("Error in read");
					}
				}
				this.waitingList.remove(w);
			}

			this.lock.writeLock().unlock();
			ReadResponse response = ReadResponse.newBuilder().setResult(result).build();
			responseObserver.onNext(response);
			responseObserver.onCompleted();
		}
	}

	@Override
	public synchronized void getTupleSpacesState(getTupleSpacesStateRequest request,
			StreamObserver<getTupleSpacesStateResponse> responseObserver) {

		this.lock.readLock().lock();

		getTupleSpacesStateResponse response = getTupleSpacesStateResponse.newBuilder()
				.addAllTuple(tuple_space.getTupleSpacesState()).build();

		this.lock.readLock().unlock();
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
