package pt.ulisboa.tecnico.tuplespaces.server;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import java.io.IOException;

public class ServerMain {

  public static void main(String[] args) throws IOException, InterruptedException {

    if (args.length < 4) {
        System.err.println("Insufficient arguments!");
        System.err.println("Usage: java " + ServerMain.class.getName() + " <host> <port> <qual> <service>");
        return;
    }

    final String host = args[0];
    final int port = Integer.parseInt(args[1]);
    final String qual = args[2];
    final String service = args[3];

    final BindableService impl = new ServiceImpl();

    // Create a new server to listen on host:port
    Server server = ServerBuilder.forPort(port).addService(impl).build();
    try {
      server.start();
    } catch (IOException e) {
      System.err.println("Failed to start server: " + e.getMessage());
      System.exit(0);
    }

    System.out.println("Server started on " + host + ":" + port);
    System.out.println("Qual: " + qual);
    System.out.println("Service: " + service);

    // register server on Name Server System
    final NameServerService _nameServerService = new NameServerService();
    _nameServerService.register("TupleSpace", qual, host, args[1]);

    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      // This code will be executed on program termination (including Ctrl-C)
      System.out.println("Shutting Down...");
      _nameServerService.delete("TupleSpace", host, args[1]);
      _nameServerService.endService();
    }));

    server.awaitTermination();

  }


}

