import os, threading

class NonExistingServerError(Exception):
    def __init__(self):
        super().__init__("Not possible to remove the server")


class ServerEntry():

    def __init__(self, host, port, qualifier):
        self.qualifier = qualifier
        self.host, self.port = host , port

    def __init__(self, qualifier, server_address):
        self.qualifier = qualifier
        self.host, self.port = server_address.split(':')

    def get_address(self):
        return f"{self.host}:{self.port}"

    def __eq__(self, other):
        return (
            isinstance(other, ServerEntry) and
            self.host == other.host and
            self.port == other.port and
            self.qualifier == other.qualifier
        )
    
    def __lt__(self, other):
        # Sort servers based on qualifier, and then port
        if self.qualifier == other.qualifier:
            return self.port < other.port
        return self.qualifier < other.qualifier 

    def __hash__(self):
        return hash((self.host, self.port, self.qualifier))

    def __str__(self):
        return f"{self.qualifier} on {self.host}:{self.port}"



class ServiceEntry:
    def __init__(self, service_name):
        self.service_name = service_name
        self.servers = list()
        self.ordered = True

    def add_server_entry(self, server_entry):
        if server_entry not in self.servers:
            self.servers.append(server_entry)
        self.ordered = False

    def lookup_servers(self, qualifier:str = ""):
        if self.ordered == False:
            self.order()
    
        result = list()
        for server in self.servers:
            if qualifier == "" or qualifier == server.qualifier:
                result.append(f"{server.qualifier}|{server.get_address()}")
        return result


    def delete_servers(self, server_address):
        servers_to_remove = []

        for server in self.servers:
            if server_address == server.get_address():
                servers_to_remove.append(server)

        for server in servers_to_remove:
            self.servers.remove(server)

        if not servers_to_remove:
            raise NonExistingServerError()


    def order(self):
        self.servers = sorted(self.servers)
        self.ordered = True

    def __str__(self):
        if self.ordered == False:
            self.order()

        result = f"Service: {self.service_name}\n"
        for server in self.servers:
            result += f"\tServer: {server}\n"
        return result
    


class NamingServer():

    def __init__(self):
        self.service_map = {}
        self.lock = threading.Lock()
        self.printState()

    def register_service(self, service_name:str, qualifier:str, server_address:str):
        _new_server = ServerEntry(qualifier, server_address)
        with self.lock:
            if service_name not in self.service_map:
                self.service_map[service_name] = ServiceEntry(service_name)
            self.service_map[service_name].add_server_entry(_new_server)
        self.printState()


    def lookup_service(self, service_name:str, qualifier:str = ""):
        with self.lock:
            if service_name in self.service_map:
                return self.service_map[service_name].lookup_servers(qualifier)
            return list()
    

    def delete_service(self, service_name, server_address):
        with self.lock:
            if service_name not in self.service_map:
                raise NonExistingServerError()
            self.service_map[service_name].delete_servers(server_address)
        self.printState()


    def printState(self):
        print(self.__str__(), flush=True)

    def __str__(self):
        os.system('cls' if os.name == 'nt' else 'clear')
        result = "\n                 State\n\n"
        result += "****************************************\n"
        with self.lock:
            for _, service_entry in self.service_map.items():
                result += service_entry.__str__()
            return result

