package ru.konkurst1.ekb.terraform_logviewer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.konkurst1.ekb.terraform_logviewer.model.LogEntry;
import ru.konkurst1.ekb.terraform_logviewer.plugin.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class PluginManagerService {
    private static final Logger logger = LoggerFactory.getLogger(PluginManagerService.class);

    private final Map<String, ManagedChannel> pluginChannels = new ConcurrentHashMap<>();
    private final Map<String, LogPluginGrpc.LogPluginBlockingStub> pluginStubs = new ConcurrentHashMap<>();

    public void registerPlugin(String name, String host, int port) {
        try {
            ManagedChannel channel = ManagedChannelBuilder
                    .forAddress(host, port)
                    .usePlaintext()
                    .build();

            pluginChannels.put(name, channel);
            LogPluginGrpc.LogPluginBlockingStub stub = LogPluginGrpc.newBlockingStub(channel);
            pluginStubs.put(name, stub);

            // Проверяем доступность плагина
            PluginInfo info = getPluginInfo(name);
            logger.info("Registered gRPC plugin: {} v{} - {}", name, info.getVersion(), info.getDescription());

        } catch (Exception e) {
            logger.error("Failed to register gRPC plugin {}: {}", name, e.getMessage());
            ManagedChannel channel = pluginChannels.remove(name);
            if (channel != null) {
                channel.shutdown();
            }
            pluginStubs.remove(name);
            throw new RuntimeException("Failed to register gRPC plugin: " + e.getMessage(), e);
        }
    }

    public PluginResponse processWithPlugin(String pluginName, List<LogEntry> entries, Map<String, String> parameters) {
        LogPluginGrpc.LogPluginBlockingStub stub = pluginStubs.get(pluginName);
        if (stub == null) {
            return PluginResponse.newBuilder()
                    .setErrorMessage("Plugin not found: " + pluginName)
                    .build();
        }

        try {
            PluginRequest request = buildPluginRequest(entries, parameters);
            PluginResponse response = stub.withDeadlineAfter(30, TimeUnit.SECONDS).process(request);

            logger.info("gRPC plugin {} processed {} entries", pluginName, entries.size());
            return response;

        } catch (StatusRuntimeException e) {
            logger.error("gRPC plugin {} failed with status: {}", pluginName, e.getStatus());
            return PluginResponse.newBuilder()
                    .setErrorMessage("gRPC plugin failed: " + e.getStatus())
                    .build();
        } catch (Exception e) {
            logger.error("gRPC plugin execution failed for {}: {}", pluginName, e.getMessage());
            return PluginResponse.newBuilder()
                    .setErrorMessage("gRPC plugin execution failed: " + e.getMessage())
                    .build();
        }
    }
    public PluginInfo getPluginInfo(String pluginName) {
        LogPluginGrpc.LogPluginBlockingStub stub = pluginStubs.get(pluginName);
        if (stub == null) {
            throw new RuntimeException("gRPC plugin not found: " + pluginName);
        }

        try {
            return stub.withDeadlineAfter(10, TimeUnit.SECONDS).getPluginInfo(Empty.getDefaultInstance());
        } catch (StatusRuntimeException e) {
            logger.error("Failed to get gRPC plugin info for {}: {}", pluginName, e.getStatus());
            throw new RuntimeException("Failed to get gRPC plugin info: " + e.getStatus(), e);
        } catch (Exception e) {
            logger.error("Failed to get gRPC plugin info for {}: {}", pluginName, e.getMessage());
            throw new RuntimeException("Failed to get gRPC plugin info: " + e.getMessage(), e);
        }
    }

    public List<String> getRegisteredPlugins() {
        return new ArrayList<>(pluginStubs.keySet());
    }

    public void unregisterPlugin(String name) {
        try {
            ManagedChannel channel = pluginChannels.remove(name);
            pluginStubs.remove(name);
            if (channel != null) {
                channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
            }
            logger.info("Unregistered gRPC plugin: {}", name);
        } catch (Exception e) {
            logger.error("Failed to unregister gRPC plugin {}: {}", name, e.getMessage());
        }
    }

    private PluginRequest buildPluginRequest(List<LogEntry> entries, Map<String, String> parameters) {
        PluginRequest.Builder requestBuilder = PluginRequest.newBuilder();

        for (LogEntry entry : entries) {
            LogEntryProto.Builder entryBuilder = LogEntryProto.newBuilder()
                    .setId(entry.getId() != null ? entry.getId() : "")
                    .setTimestamp(entry.getTimestamp() != null ? entry.getTimestamp().toString() : "")
                    .setLevel(entry.getLevel() != null ? entry.getLevel() : "")
                    .setSection(entry.getSection() != null ? entry.getSection() : "")
                    .setMessage(entry.getMessage() != null ? entry.getMessage() : "")
                    .setLineNumber(entry.getLineNumber() != null ? entry.getLineNumber() : 0)
                    .setLogFileId(entry.getLogFileId() != null ? entry.getLogFileId() : "");

            // Используем rawJson вместо удаленных полей
            if (entry.getRawJson() != null) {
                entryBuilder.setJsonBody(entry.getRawJson().toString());
            }

            // Terraform-specific поля
            if (entry.getTfResourceType() != null) {
                entryBuilder.setTfResourceType(entry.getTfResourceType());
            }
            if (entry.getTfReqId() != null) {
                entryBuilder.setTfReqId(entry.getTfReqId());
            }
            if (entry.getRequestType() != null) {
                entryBuilder.setRequestType(entry.getRequestType());
            }
            if (entry.getModule() != null) {
                // Можно добавить поле module в proto если нужно
            }
            if (entry.getTfProviderAddr() != null) {
                // Можно добавить поле tfProviderAddr в proto если нужно
            }

            // Новые поля из обогащения
            if (entry.getRequestDurationMs() != null) {
                // Можно добавить в statistics или как отдельное поле
            }
            if (entry.getRequestStatus() != null) {
                // Можно добавить в statistics или как отдельное поле
            }
            if (entry.getOperationType() != null) {
                // Можно добавить в statistics или как отдельное поле
            }
            if (entry.getSeverityScore() != null) {
                // Можно добавить в statistics или как отдельное поле
            }

            requestBuilder.addEntries(entryBuilder.build());
        }

        if (parameters != null) {
            requestBuilder.putAllParameters(parameters);
        }

        return requestBuilder.build();
    }

    private List<LogEntry> convertFromProto(List<LogEntryProto> protoEntries) {
        List<LogEntry> entries = new ArrayList<>();
        for (LogEntryProto proto : protoEntries) {
            LogEntry entry = new LogEntry();
            entry.setId(proto.getId());

            // Базовые поля
            if (!proto.getTimestamp().isEmpty()) {
                try {
                    entry.setTimestamp(java.time.Instant.parse(proto.getTimestamp()));
                } catch (Exception e) {
                    logger.warn("Failed to parse timestamp from proto: {}", proto.getTimestamp());
                }
            }

            entry.setLevel(proto.getLevel());
            entry.setSection(proto.getSection());
            entry.setMessage(proto.getMessage());
            entry.setLineNumber(proto.getLineNumber());
            entry.setLogFileId(proto.getLogFileId());

            // Terraform-specific поля
            entry.setTfResourceType(proto.getTfResourceType());
            entry.setTfReqId(proto.getTfReqId());
            entry.setRequestType(proto.getRequestType());

            // Восстанавливаем rawJson из jsonBody
            if (!proto.getJsonBody().isEmpty()) {
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    entry.setRawJson(mapper.readValue(proto.getJsonBody(), Object.class));
                } catch (Exception e) {
                    logger.warn("Failed to parse jsonBody from proto: {}", e.getMessage());
                }
            }

            entries.add(entry);
        }
        return entries;
    }

    // Метод для демонстрации работы
    public Map<String, Object> testPluginConnection(String pluginName) {
        try {
            PluginInfo info = getPluginInfo(pluginName);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("pluginInfo", Map.of(
                    "name", info.getName(),
                    "version", info.getVersion(),
                    "description", info.getDescription(),
                    "supportedParameters", info.getSupportedParametersList()
            ));
            return result;
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("error", e.getMessage());
            return result;
        }
    }
}