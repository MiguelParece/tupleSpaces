import sys
sys.path.insert(1, '../Contract/target/generated-sources/protobuf/python')
import grpc
import NameServer_pb2_grpc as pb2_grpc
from NameServerServiceImpl import NameServerServiceImpl
from concurrent import futures


# define the port
PORT = 5001

if __name__ == '__main__':
    try:
        # create server and add service
        server = grpc.server(futures.ThreadPoolExecutor(max_workers=1))
        pb2_grpc.add_NameServerServiceServicer_to_server(NameServerServiceImpl(), server)
        
        # start server
        server.add_insecure_port('[::]:'+str(PORT))
        server.start()

        # wait for server to finish
        server.wait_for_termination()

    except KeyboardInterrupt:
        print("Name Server stopped")
        exit(0)


