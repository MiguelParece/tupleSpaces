package pt.ulisboa.tecnico.tuplespaces.client.grpc;

import io.grpc.StatusRuntimeException;
import pt.ulisboa.tecnico.tuplespaces.replicaTotalOrder.contract.*;
import java.util.Random;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.stream.Collectors;

import pt.ulisboa.tecnico.tuplespaces.client.grpc.async.*;
import pt.ulisboa.tecnico.tuplespaces.client.util.OrderedDelayer;

//import sequencer
import pt.ulisboa.tecnico.tuplespaces.sequencer.*;

public class ClientService {

  private List<ClientServiceSingleServer> servers;
  private OrderedDelayer delayer;
  // create a stub for the sequencer
  private SequencerService sequencer;

  public ClientService() {

    // Get TupleSpace Server's Address
    final NameServerService _nameServerService = new NameServerService();

    this.servers = _nameServerService.lookup("TupleSpace");

    // get a sequencer server

    sequencer = new SequencerService();

    _nameServerService.endService();

    if (this.servers.size() < 1) {
      System.out.println("No Server Available");
      System.exit(0);
    }

    delayer = new OrderedDelayer(this.servers.size());

  }

  /*
   * This method allows the command processor to set the request delay assigned to
   * a given server
   */
  public void setDelay(int id, int delay) {
    if (id >= this.servers.size()) {
      System.out.println("Invalid server Id");
      return;
    }
    delayer.setDelay(id, delay);
  }

  public void endService() {
    this.servers.stream().forEach(ClientServiceSingleServer::endService);
    sequencer.endService();
  }

  public void getTupleSpacesState(int qualifier) {
    if (qualifier >= this.servers.size()) {
      System.out.println("Invalid server Id");
      return;
    }
    this.servers.get(qualifier).getTupleSpacesState();
    System.out.println();
  }

  public void put(String Pattern) {
    try {
      // get sequence number
      int i = sequencer.getSeqNumber();

      // Send the put request with the sequence number
      PutRequest request = PutRequest.newBuilder().setNewTuple(Pattern).setSeqNumber(i).build();
      PutResponseCollector<PutResponse> mycollector = new PutResponseCollector<PutResponse>();

      Iterator<ClientServiceSingleServer> _iterator = this.servers.iterator();
      for (Integer id : delayer) {
        _iterator.next().put(request, mycollector);
      }

      System.out.println("OK");

      mycollector.waitUntilAllReceived(this.servers.size());
      mycollector.processResponses();

    } catch (StatusRuntimeException e) {
      System.out.println("Caught exception with description: " + e.getStatus().getDescription());
    } catch (Exception e) {
      // Handle exceptions
      e.printStackTrace();
    }
    System.out.println();
  }

  public void take(String Pattern) {

    try {
      // get sequence number
      int i = sequencer.getSeqNumber();
      // Send the gRPC request
      TakeRequest request = TakeRequest.newBuilder().setSearchPattern(Pattern).setSeqNumber(i).build();
      StringCollector<TakeResponse> mycollector = new StringCollector<TakeResponse>(this.servers.size());

      Iterator<ClientServiceSingleServer> _iterator = this.servers.iterator();
      for (Integer id : delayer) {
        _iterator.next().take(request, mycollector);
      }
      System.out.println("OK");

      mycollector.waitUntilAllReceived(this.servers.size());
      mycollector.processResponses();

    } catch (StatusRuntimeException e) {
      System.out.println("Caught exception with description: " + e.getStatus().getDescription());
    } catch (Exception e) {
      e.printStackTrace();
    }

    System.out.println();
  }

  public void read(String Pattern) {
    try {
      // Send the gRPC request
      ReadRequest request = ReadRequest.newBuilder().setSearchPattern(Pattern).build();
      StringCollector<ReadResponse> mycollector = new StringCollector<ReadResponse>(1);

      Iterator<ClientServiceSingleServer> _iterator = this.servers.iterator();
      for (Integer id : delayer) {
        _iterator.next().read(request, mycollector);
      }
      System.out.println("OK");

      mycollector.waitUntilAllReceived(1);
      mycollector.processResponses();

    } catch (StatusRuntimeException e) {
      System.out.println("Caught exception with description: " + e.getStatus().getDescription());
    } catch (Exception e) {
      e.printStackTrace();
    }
    System.out.println();
  }

}
