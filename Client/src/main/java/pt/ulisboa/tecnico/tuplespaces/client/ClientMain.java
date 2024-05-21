package pt.ulisboa.tecnico.tuplespaces.client;

import pt.ulisboa.tecnico.tuplespaces.client.grpc.ClientService;

public class ClientMain {
    public static void main(String[] args) {
        CommandProcessor parser = new CommandProcessor(new ClientService());
        parser.parseInput();
    }
}
