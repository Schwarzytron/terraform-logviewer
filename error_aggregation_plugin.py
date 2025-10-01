import grpc
from concurrent import futures
import logging
import re

import log_plugin_pb2
import log_plugin_pb2_grpc

class ErrorAggregationPlugin(log_plugin_pb2_grpc.LogPluginServicer):
    def GetPluginInfo(self, request, context):
        return log_plugin_pb2.PluginInfo(
            name="error-aggregator",
            version="1.0",
            description="Агрегирует ошибки по типу и частоте",
            supported_parameters=["min_count", "time_window"]
        )

    def Process(self, request, context):
        error_stats = {}

        for entry in request.entries:
            if entry.level == "ERROR":
                error_type = self.extract_error_type(entry.message)
                error_stats[error_type] = error_stats.get(error_type, 0) + 1

        # Применяем параметры
        min_count = int(request.parameters.get("min_count", "1"))
        filtered_stats = {k: v for k, v in error_stats.items() if v >= min_count}

        return log_plugin_pb2.PluginResponse(
            statistics=filtered_stats
        )

    def extract_error_type(self, message):
        # Простая эвристика для классификации ошибок
        if "timeout" in message.lower():
            return "timeout_error"
        elif "connection" in message.lower():
            return "connection_error"
        elif "permission" in message.lower():
            return "permission_error"
        elif "not found" in message.lower():
            return "not_found_error"
        else:
            return "other_error"

def serve():
    server = grpc.server(futures.ThreadPoolExecutor(max_workers=10))
    log_plugin_pb2_grpc.add_LogPluginServicer_to_server(ErrorAggregationPlugin(), server)
    server.add_insecure_port('[::]:50051')
    server.start()
    print("Plugin server started on port 50051")
    server.wait_for_termination()

if __name__ == '__main__':
    logging.basicConfig()
    serve()