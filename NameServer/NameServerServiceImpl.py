import sys, re, socket
sys.path.insert(1, '../contract/target/generated-sources/protobuf/python')
import NameServer_pb2 as pb2
import NameServer_pb2_grpc as pb2_grpc
import grpc

from NameServerDomain import NamingServer, NonExistingServerError

class NameServerServiceImpl(pb2_grpc.NameServerServiceServicer):

    def __init__(self, *args, **kwargs):
        self.name_server = NamingServer()

    def register(self, request, context):
        # Check if 'service_name', 'qualifier', and 'server_address' are present in the request
        if not hasattr(request, 'service_name') or not hasattr(request, 'qualifier') or not hasattr(request, 'server_address'):
            context.set_code(grpc.StatusCode.INVALID_ARGUMENT)
            context.set_details("Missing required fields in the request.")
            return pb2.EmptyResponse()
        
        if not self.nonEmptyString(request.service_name, "service_name", context):
            return pb2.EmptyResponse()
        if not self.nonEmptyString(request.qualifier, "qualifier", context):
            return pb2.EmptyResponse()
        if not self.nonEmptyString(request.server_address, "server_address", context):
            return pb2.EmptyResponse()
        elif not self.validServerAddress(request.server_address, context):
            return pb2.EmptyResponse()

        # If all checks pass, proceed with registering the service
        self.name_server.register_service(request.service_name, request.qualifier, request.server_address)

        # return response
        return pb2.EmptyResponse()
    

    def lookup(self, request, context):
        # Check if 'service_name' or 'qualifier' are present in the request
        if not ( hasattr(request, 'service_name') and self.nonEmptyString(request.service_name, "service_name", context) ):
            return pb2.LookupResponse(servers=list())
        
        if hasattr(request, 'qualifier') and isinstance(request.qualifier, str) and request.qualifier.strip():
            result = self.name_server.lookup_service(request.service_name, request.qualifier)
        else:
            result = self.name_server.lookup_service(request.service_name)

        # return response
        return pb2.LookupResponse(servers=result)


    def delete(self, request, context):
        # Check if 'service_name' and 'server_address' are present in the request
        if not hasattr(request, 'service_name') or not hasattr(request, 'server_address'):
            context.set_code(grpc.StatusCode.INVALID_ARGUMENT)
            context.set_details("Missing required fields in the request.")
            return pb2.EmptyResponse()
        
        if not self.nonEmptyString(request.service_name, "service_name", context):
            return pb2.EmptyResponse()
        if not self.nonEmptyString(request.server_address, "server_address", context):
            return pb2.EmptyResponse()
        elif not self.validServerAddress(request.server_address, context):
            return pb2.EmptyResponse()

        try:
            self.name_server.delete_service(request.service_name, request.server_address)
        except NonExistingServerError as error:
            context.set_code(grpc.StatusCode.NOT_FOUND)
            context.set_details(str(error))

        # return response
        return pb2.EmptyResponse()


    def nonEmptyString(self, my_string:str, text:str, context):
        if not isinstance(my_string, str) or not my_string.strip():
            context.set_code(grpc.StatusCode.INVALID_ARGUMENT)
            context.set_details(f"'{text}' field must be a non-empty string.")
            return False
        return True
    


    def validServerAddress(self, address: str, context):
        def went_wrong(message):
            context.set_code(grpc.StatusCode.INVALID_ARGUMENT)
            context.set_details(message)
            return False

        if ':' not in address:
            return went_wrong("'server_address' must have the format host:port.")
        host, port_str = address.split(':')

        # Validate host (can be "localhost" or an IP address)
        if host.lower() != "localhost":
            try:
                socket.inet_aton(host)
            except socket.error:
                return went_wrong("Invalid host IP address.")

        # Validate port number
        try:
            if not (0 <= int(port_str) <= 65535):
                return went_wrong("Port number must be in the range 0-65535.")
        except ValueError:
            return went_wrong("Invalid port number.")

        return True

    
