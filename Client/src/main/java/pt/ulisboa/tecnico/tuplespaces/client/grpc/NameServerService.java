package pt.ulisboa.tecnico.tuplespaces.client.grpc;

import io.grpc.StatusRuntimeException;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import pt.ulisboa.tecnico.nameserver.contract.*;
import java.util.List;
import java.util.ArrayList;

public class NameServerService {

    NameServerServiceGrpc.NameServerServiceBlockingStub stub;
    ManagedChannel channel;
    private final String host = "localhost";
    private final String port = "5001";
    private boolean offline = true;


    public NameServerService() {
        this.connect();
    }

    public void endService() {
        if (!this.offline)
            this.channel.shutdownNow();
    }

    public void register(String service_name, String qualifier, String host, String port) {
        if (this.offline && !this.connect()) {
            System.out.println("Name Server is not currently available. Please try to reconnect later.\n");
            return;
        }

        String service_address = host + ":" + port;
        try {

            // Send the gRPC request
            RegisterRequest request = RegisterRequest
                .newBuilder()
                .setServiceName(service_name)
                .setQualifier(qualifier)
                .setServerAddress(service_address)
                .build();

            EmptyResponse response = this.stub.register(request);

        } catch (StatusRuntimeException e) {
            System.out.println("Caught exception with description: " + e.getStatus().getDescription());
        } catch (Exception e) {
            // Handle exceptions
            e.printStackTrace();
        }
    }

    public List<ClientServiceSingleServer> lookup(String service_name) {
        return this.lookup(service_name, "");
    }

    public List<ClientServiceSingleServer> lookup(String service_name, String qualifier) {
        if (this.offline && !this.connect()) {
            System.out.println("Name Server is not currently available. Please try to reconnect later.\n");
            return new ArrayList<>();
        }

        List<ClientServiceSingleServer> servers = new ArrayList<>();
        try {

            // Send the gRPC request
            LookupRequest.Builder _builder = LookupRequest.newBuilder().setServiceName(service_name);
            if (qualifier != "") {
                _builder.setQualifier(qualifier);
            }
            LookupRequest request = _builder.build();

            LookupResponse response = this.stub.lookup(request);
            List<String> aux = response.getServersList();

            for (String _serverData : aux) {
                String[] parts = _serverData.split("\\|", 2);
                servers.add( new ClientServiceSingleServer(parts[1], parts[0]));
            }

        } catch (StatusRuntimeException e) {
            System.out.println("Caught exception with description: " + e.getStatus().getDescription());
        } catch (Exception e) {
            // Handle exceptions
            e.printStackTrace();
        }

        return servers;
    }

    
    public void delete(String service_name, String host, String port) {
        if (this.offline && !this.connect()) {
            System.out.println("Name Server is not currently available. Please try to reconnect later.\n");
            return;
        }

        String service_address = host + ":" + port;
        try {

            // Send the gRPC request
            DeleteRequest request = DeleteRequest
                .newBuilder()
                .setServiceName(service_name)
                .setServerAddress(service_address)
                .build();

            EmptyResponse response = this.stub.delete(request);

        } catch (StatusRuntimeException e) {
            System.out.println("Caught exception with description: " + e.getStatus().getDescription());
        } catch (Exception e) {
            // Handle exceptions
            e.printStackTrace();
        }
    }


    public boolean connect() {
        if (this.offline) {
            final String target = this.host + ":" + this.port;
            try {

                this.channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
                // Here we create a blocking stub, but an async stub,
                // or an async stub with Future are also possible.
                this.stub = NameServerServiceGrpc.newBlockingStub(this.channel);
                this.offline = false;

            } catch (Exception e) {
                System.out.println("Name Server is not currently available. Please try to reconnect later.\n");
            }
        }
        return !this.offline;
    }
    
}
