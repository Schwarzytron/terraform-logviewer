package ru.konkurst1.ekb.terraform_logviewer.service;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.konkurst1.ekb.terraform_logviewer.model.LogEntry;
import ru.konkurst1.ekb.terraform_logviewer.plugin.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PluginManagerService {
    private static final Logger logger = LoggerFactory.getLogger(PluginManagerService.class);

    private Map<String, ManagedChannel> pluginChannels = new ConcurrentHashMap<>();
    private Map<String, LogPluginGrpc.LogPluginBlockingStub> pluginStubs = new ConcurrentHashMap<>();

    public void registerPlugin(String name, String host, int port) {
        try {
            ManagedChannel channel = ManagedChannelBuilder
                    .forAddress(host, port)
                    .usePlaintext()
                    .build();

            pluginChannels.put(name, channel);
            pluginStubs.put(name, LogPluginGrpc.newBlockingStub(channel));

            logger.info("Registered plugin: {} at {}:{}", name, host, port);
        } catch (Exception e) {
            logger.error("Failed to register plugin {}: {}", name, e.getMessage());
        }
    }

    public PluginResponse processWithPlugin(String pluginName, List<LogEntry> entries, Map<String, String> parameters) {
        try {
            LogPluginGrpc.LogPluginBlockingStub stub = pluginStubs.get(pluginName);
            if (stub == null) {
                throw new RuntimeException("Plugin not found: " + pluginName);
            }

            PluginRequest request = buildPluginRequest(entries, parameters);
            return stub.process(request);
        } catch (Exception e) {
            logger.error("Plugin execution failed for {}: {}", pluginName, e.getMessage());
            throw new RuntimeException("Plugin execution failed", e);
        }
    }

    public PluginInfo getPluginInfo(String pluginName) {
        try {
            LogPluginGrpc.LogPluginBlockingStub stub = pluginStubs.get(pluginName);
            if (stub == null) {
                throw new RuntimeException("Plugin not found: " + pluginName);
            }
            return stub.getPluginInfo(Empty.getDefaultInstance());
        } catch (Exception e) {
            logger.error("Failed to get plugin info for {}: {}", pluginName, e.getMessage());
            throw new RuntimeException("Failed to get plugin info", e);
        }
    }

    public List<String> getRegisteredPlugins() {
        return new ArrayList<>(pluginStubs.keySet());
    }

    private PluginRequest buildPluginRequest(List<LogEntry> entries, Map<String, String> parameters) {
        PluginRequest.Builder requestBuilder = PluginRequest.newBuilder();

        // Конвертируем LogEntry в LogEntryProto
        for (LogEntry entry : entries) {
            LogEntryProto.Builder entryBuilder = LogEntryProto.newBuilder()
                    .setId(entry.getId() != null ? entry.getId() : "")
                    .setRawMessage(entry.getRawMessage() != null ? entry.getRawMessage() : "")
                    .setTimestamp(entry.getTimestamp() != null ? entry.getTimestamp().toString() : "")
                    .setLevel(entry.getLevel() != null ? entry.getLevel().name() : "")
                    .setSection(entry.getSection() != null ? entry.getSection() : "")
                    .setMessage(entry.getMessage() != null ? entry.getMessage() : "")
                    .setHasJson(entry.getHasJson() != null ? entry.getHasJson() : false)
                    .setParsingError(entry.getParsingError() != null ? entry.getParsingError() : false)
                    .setLineNumber(entry.getLineNumber() != null ? entry.getLineNumber() : 0)
                    .setLogFileId(entry.getLogFileId() != null ? entry.getLogFileId() : "")
                    .setIsRead(entry.getIsRead() != null ? entry.getIsRead() : false);

            if (entry.getParsingErrorMessage() != null) {
                entryBuilder.setParsingErrorMessage(entry.getParsingErrorMessage());
            }
            if (entry.getTfResourceType() != null) {
                entryBuilder.setTfResourceType(entry.getTfResourceType());
            }
            if (entry.getTfReqId() != null) {
                entryBuilder.setTfReqId(entry.getTfReqId());
            }
            if (entry.getRequestType() != null) {
                entryBuilder.setRequestType(entry.getRequestType());
            }
            if (entry.getJsonBody() != null) {
                entryBuilder.setJsonBody(entry.getJsonBody());
            }

            requestBuilder.addEntries(entryBuilder.build());
        }

        // Добавляем параметры
        if (parameters != null) {
            requestBuilder.putAllParameters(parameters);
        }

        return requestBuilder.build();
    }
}