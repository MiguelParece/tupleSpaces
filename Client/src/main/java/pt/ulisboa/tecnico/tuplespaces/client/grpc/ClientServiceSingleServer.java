package pt.ulisboa.tecnico.tuplespaces.client.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import pt.ulisboa.tecnico.tuplespaces.replicaTotalOrder.contract.*;
import pt.ulisboa.tecnico.tuplespaces.client.grpc.async.*;

import java.util.List;

public class ClientServiceSingleServer {

  private String address, qualifier;
  private TupleSpacesReplicaGrpc.TupleSpacesReplicaStub stub;
  private ManagedChannel channel;

  ClientServiceSingleServer(String address, String qualifier) {
    this.channel = ManagedChannelBuilder.forTarget(address).usePlaintext().build();
    this.stub = TupleSpacesReplicaGrpc.newStub(this.channel);
    this.address = address;
    this.qualifier = qualifier;
  }

  void endService() {
    this.channel.shutdownNow();
  }

  String getAddress() {
    return this.address;
  }

  String getQualifier() {
    return this.qualifier;
  }

  public void put(PutRequest request, ResponseCollector<PutResponse> responseCollector) {
    try {
      this.stub.put(request, new ResponseObserver<PutResponse>(responseCollector));

    } catch (StatusRuntimeException e) {
      System.out.println("Caught exception with description: " + e.getStatus().getDescription());
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void take(TakeRequest request, ResponseCollector<TakeResponse> responseCollector) {
    try {
      this.stub.take(request, new ResponseObserver<TakeResponse>(responseCollector));
    } catch (StatusRuntimeException e) {
      System.out.println("Caught exception with description: " + e.getStatus().getDescription());
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  void read(ReadRequest request, ResponseCollector<ReadResponse> responseCollector) {
    try {
      this.stub.read(request, new ResponseObserver<ReadResponse>(responseCollector));
    } catch (StatusRuntimeException e) {
      System.out.println("Caught exception with description: " + e.getStatus().getDescription());
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void getTupleSpacesState() {
    try {
      // Send the gRPC request
      getTupleSpacesStateRequest request = getTupleSpacesStateRequest.newBuilder().build();
      GetTupleSpacesStateRequestCollector<getTupleSpacesStateResponse> mycollector = new GetTupleSpacesStateRequestCollector<getTupleSpacesStateResponse>();

      this.stub.getTupleSpacesState(request, new ResponseObserver<getTupleSpacesStateResponse>(mycollector));

      // Process the response
      mycollector.waitUntilAllReceived(1);
      List<String> _existing_tuples = mycollector.getResponses();
      System.out.println("OK");
      System.out.println(_existing_tuples.toString());

    } catch (StatusRuntimeException e) {
      System.out.println("Caught exception with description: " +
          e.getStatus().getDescription());
    } catch (Exception e) {
      // Handle exceptions
      e.printStackTrace();
    }
  }
}
