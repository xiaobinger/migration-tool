package com.migration.extractor;

import com.migration.engine.FlowEngine;
import com.migration.model.entity.FlowNode;
import com.migration.service.DataSourceConfigService;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;

import java.time.*;
import java.util.*;

@Slf4j
public class MongoDBDataExtractor implements DataExtractor {

    private DataSourceConfigService dataSourceConfigService;

    @Override
    public void setDataSourceConfigService(DataSourceConfigService service) {
        this.dataSourceConfigService = service;
    }

    @Override
    public String getName() { return "MongoDBDataExtractor"; }

    @Override
    public String getFullName() { return "com.migration.extractor.MongoDBDataExtractor"; }

    @Override
    public String getDescription() { return "MongoDB数据提取器"; }

    @Override
    public FlowEngine.NodeResult execute(FlowEngine.FlowContext context, FlowNode node) {
        log.info("MongoDB数据提取开始: nodeId={}", node.getNodeId());

        String connectionString, database;

        String connectionId = getConfig(node, "connectionId", "");
        if (!connectionId.isEmpty() && dataSourceConfigService != null) {
            com.migration.model.entity.DataSourceConfig ds = dataSourceConfigService.getById(Long.parseLong(connectionId));
            connectionString = String.format("mongodb://%s:%s@%s:%d/%s", ds.getUsername(), ds.getPassword(), ds.getHost(), ds.getPort(), ds.getDatabase());
            database = ds.getDatabase();
        } else {
            connectionString = getConfig(node, "connectionString", "mongodb://localhost:27017");
            database = getConfig(node, "database", "");
        }

        String collection = getConfig(node, "collection", "");
        String queryStr = getConfig(node, "query", "{}");
        int batchSize = Integer.parseInt(getConfig(node, "batchSize", "1000"));

        log.info("连接MongoDB: database={}, collection={}", database, collection);

        List<Map<String, Object>> rows = new ArrayList<>();
        long totalRecords = 0;

        try (com.mongodb.client.MongoClient client = com.mongodb.client.MongoClients.create(connectionString)) {
            com.mongodb.client.MongoDatabase db = client.getDatabase(database);
            com.mongodb.client.MongoCollection<Document> coll = db.getCollection(collection);

            Document queryDoc = Document.parse(queryStr);
            try (com.mongodb.client.MongoCursor<Document> cursor = coll.find(queryDoc).batchSize(batchSize).iterator()) {
                while (cursor.hasNext()) {
                    Document doc = cursor.next();
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (Map.Entry<String, Object> entry : doc.entrySet()) {
                        if (!"_id".equals(entry.getKey())) {
                            row.put(entry.getKey(), normalizeValue(entry.getValue()));
                        } else {
                            row.put("_id", entry.getValue().toString());
                        }
                    }
                    rows.add(row);
                    totalRecords++;
                }
            }
        } catch (Exception e) {
            log.error("MongoDB查询失败: {}", e.getMessage(), e);
            return FlowEngine.NodeResult.fail("MongoDB查询失败: " + e.getMessage());
        }

        context.getData().addAll(rows);
        context.getVariables().put(node.getNodeId() + "_extracted", totalRecords);

        String summary = String.format("从MongoDB [%s.%s] 提取数据完成，共 %d 条记录", database, collection, totalRecords);
        return FlowEngine.NodeResult.ok(summary, totalRecords, totalRecords, 0);
    }

    private static Object normalizeValue(Object value) {
        if (value == null) return null;
        if (value instanceof java.util.Date jd) {
            return jd.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime().toString();
        }
        if (value instanceof java.sql.Timestamp ts) {
            return ts.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime().toString();
        }
        if (value instanceof java.sql.Date d) {
            return d.toLocalDate().toString();
        }
        if (value instanceof java.sql.Time t) {
            return t.toLocalTime().toString();
        }
        if (value instanceof LocalDateTime ldt) {
            return ldt.toString();
        }
        if (value instanceof LocalDate ld) {
            return ld.toString();
        }
        if (value instanceof LocalTime lt) {
            return lt.toString();
        }
        if (value instanceof ZonedDateTime zdt) {
            return zdt.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime().toString();
        }
        if (value instanceof Instant instant) {
            return instant.atZone(ZoneId.systemDefault()).toLocalDateTime().toString();
        }
        if (value instanceof OffsetDateTime odt) {
            return odt.toLocalDateTime().toString();
        }
        return value;
    }
}
