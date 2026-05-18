package com.migration.loader;

import com.migration.engine.FlowEngine;
import com.migration.model.entity.FlowNode;
import com.migration.service.DataSourceConfigService;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;

import java.util.*;

@Slf4j
public class MongoDBDataLoader implements DataLoader {

    private DataSourceConfigService dataSourceConfigService;

    @Override
    public void setDataSourceConfigService(DataSourceConfigService service) {
        this.dataSourceConfigService = service;
    }

    @Override
    public String getName() { return "MongoDBDataLoader"; }

    @Override
    public String getFullName() { return "com.migration.loader.MongoDBDataLoader"; }

    @Override
    public String getDescription() { return "MongoDB数据库数据加载器"; }

    @Override
    @SuppressWarnings("unchecked")
    public FlowEngine.NodeResult execute(FlowEngine.FlowContext context, FlowNode node) {
        log.info("MongoDB数据加载开始: nodeId={}", node.getNodeId());

        String connectionString, database;
        String username = "", password = "";
        String host;
        int port;

        String connectionId = getConfig(node, "connectionId", "");
        if (!connectionId.isEmpty() && dataSourceConfigService != null) {
            com.migration.model.entity.DataSourceConfig ds = dataSourceConfigService.getById(Long.parseLong(connectionId));
            host = ds.getHost();
            port = ds.getPort();
            database = ds.getDatabase();
            username = ds.getUsername();
            password = ds.getPassword();
            connectionString = String.format("mongodb://%s:%s@%s:%d/%s", username, password, host, port, database);
        } else {
            host = getConfig(node, "host", "localhost");
            port = Integer.parseInt(getConfig(node, "port", "27017"));
            database = getConfig(node, "database", "");
            connectionString = getConfig(node, "connectionString", "mongodb://localhost:27017/" + database);
        }

        String collection = getConfig(node, "collection", "");
        String writeMode = getConfig(node, "writeMode", "INSERT");
        String upsertKey = getConfig(node, "upsertKey", "");

        List<Map<String, Object>> dataRows = new ArrayList<>();
        for (Object item : context.getData()) {
            if (item instanceof Map) {
                dataRows.add((Map<String, Object>) item);
            }
        }

        if (dataRows.isEmpty()) {
            return FlowEngine.NodeResult.ok("无数据需要加载", 0, 0, 0);
        }

        log.info("加载到MongoDB: database={}, collection={}, mode={}, 数据量={}", database, collection, writeMode, dataRows.size());

        long successRecords = 0;
        long failedRecords = 0;

        try (com.mongodb.client.MongoClient client = com.mongodb.client.MongoClients.create(connectionString)) {
            com.mongodb.client.MongoDatabase db = client.getDatabase(database);
            com.mongodb.client.MongoCollection<Document> coll = db.getCollection(collection);

            List<Document> batchDocs = new ArrayList<>();
            int batchSize = 1000;

            for (Map<String, Object> row : dataRows) {
                try {
                    Document doc = new Document(row);

                    if ("UPSERT".equals(writeMode) && !upsertKey.isEmpty()) {
                        Object keyValue = row.get(upsertKey);
                        if (keyValue != null) {
                            coll.replaceOne(
                                    com.mongodb.client.model.Filters.eq(upsertKey, keyValue),
                                    doc,
                                    new com.mongodb.client.model.ReplaceOptions().upsert(true)
                            );
                            successRecords++;
                        } else {
                            coll.insertOne(doc);
                            successRecords++;
                        }
                    } else {
                        batchDocs.add(doc);
                        if (batchDocs.size() >= batchSize) {
                            coll.insertMany(batchDocs);
                            successRecords += batchDocs.size();
                            batchDocs.clear();
                        }
                    }
                } catch (Exception e) {
                    failedRecords++;
                    log.warn("MongoDB行写入失败: {}", e.getMessage());
                }
            }

            if (!batchDocs.isEmpty()) {
                coll.insertMany(batchDocs);
                successRecords += batchDocs.size();
            }
        } catch (Exception e) {
            log.error("MongoDB写入失败: {}", e.getMessage(), e);
            return FlowEngine.NodeResult.fail("MongoDB写入失败: " + e.getMessage());
        }

        context.getVariables().put(node.getNodeId() + "_loaded", successRecords);

        String summary = String.format("数据加载到MongoDB [%s.%s] 完成，共 %d 条，成功 %d 条，失败 %d 条，模式: %s",
                database, collection, dataRows.size(), successRecords, failedRecords, writeMode);
        return FlowEngine.NodeResult.ok(summary, dataRows.size(), successRecords, failedRecords);
    }
}
