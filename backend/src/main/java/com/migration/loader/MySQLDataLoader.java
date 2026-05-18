package com.migration.loader;

import com.migration.engine.FlowEngine;
import com.migration.model.entity.FlowNode;
import com.migration.service.DataSourceConfigService;
import lombok.extern.slf4j.Slf4j;

import java.sql.*;
import java.util.*;

@Slf4j
public class MySQLDataLoader implements DataLoader {

    private DataSourceConfigService dataSourceConfigService;

    @Override
    public void setDataSourceConfigService(DataSourceConfigService service) {
        this.dataSourceConfigService = service;
    }

    @Override
    public String getName() { return "MySQLDataLoader"; }

    @Override
    public String getFullName() { return "com.migration.loader.MySQLDataLoader"; }

    @Override
    public String getDescription() { return "MySQL数据库数据加载器"; }

    @Override
    @SuppressWarnings("unchecked")
    public FlowEngine.NodeResult execute(FlowEngine.FlowContext context, FlowNode node) {
        log.info("MySQL数据加载开始: nodeId={}", node.getNodeId());

        String host, database, username, password;
        int port;

        String connectionId = getConfig(node, "connectionId", "");
        if (!connectionId.isEmpty() && dataSourceConfigService != null) {
            com.migration.model.entity.DataSourceConfig ds = dataSourceConfigService.getById(Long.parseLong(connectionId));
            host = ds.getHost();
            port = ds.getPort();
            database = ds.getDatabase();
            username = ds.getUsername();
            password = ds.getPassword();
        } else {
            host = getConfig(node, "host", "localhost");
            port = Integer.parseInt(getConfig(node, "port", "3306"));
            database = getConfig(node, "database", "");
            username = getConfig(node, "username", "");
            password = getConfig(node, "password", "");
        }

        String table = getConfig(node, "table", "");
        String writeMode = getConfig(node, "writeMode", "INSERT");
        int batchSize = Integer.parseInt(getConfig(node, "batchSize", "1000"));
        String onDuplicateKey = getConfig(node, "onDuplicateKey", "");

        List<Map<String, Object>> dataRows = new ArrayList<>();
        for (Object item : context.getData()) {
            if (item instanceof Map) {
                dataRows.add((Map<String, Object>) item);
            }
        }

        if (dataRows.isEmpty()) {
            return FlowEngine.NodeResult.ok("无数据需要加载", 0, 0, 0);
        }

        String jdbcUrl = String.format("jdbc:mysql://%s:%d/%s?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&rewriteBatchedStatements=true", host, port, database);
        log.info("加载到MySQL: {}, table={}, mode={}, 数据量={}", jdbcUrl, table, writeMode, dataRows.size());

        long successRecords = 0;
        long failedRecords = 0;

        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password)) {
            conn.setAutoCommit(false);

            Map<String, Object> firstRow = dataRows.get(0);
            List<String> columns = new ArrayList<>(firstRow.keySet());

            String sql = buildInsertSql(table, columns, writeMode, onDuplicateKey);
            log.debug("INSERT SQL: {}", sql);

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                int batchCount = 0;
                for (Map<String, Object> row : dataRows) {
                    try {
                        for (int i = 0; i < columns.size(); i++) {
                            ps.setObject(i + 1, row.get(columns.get(i)));
                        }
                        ps.addBatch();
                        batchCount++;

                        if (batchCount >= batchSize) {
                            ps.executeBatch();
                            successRecords += batchCount;
                            batchCount = 0;
                        }
                    } catch (Exception e) {
                        failedRecords++;
                        log.warn("行写入失败: {}", e.getMessage());
                    }
                }
                if (batchCount > 0) {
                    ps.executeBatch();
                    successRecords += batchCount;
                }
            }
            conn.commit();
        } catch (SQLException e) {
            log.error("MySQL写入失败: {}", e.getMessage(), e);
            return FlowEngine.NodeResult.fail("MySQL写入失败: " + e.getMessage());
        }

        context.getVariables().put(node.getNodeId() + "_loaded", successRecords);

        String summary = String.format("数据加载到MySQL [%s.%s] 完成，共 %d 条，成功 %d 条，失败 %d 条，模式: %s",
                database, table, dataRows.size(), successRecords, failedRecords, writeMode);
        return FlowEngine.NodeResult.ok(summary, dataRows.size(), successRecords, failedRecords);
    }

    private String buildInsertSql(String table, List<String> columns, String writeMode, String onDuplicateKey) {
        StringBuilder sql = new StringBuilder("INSERT ");
        if ("UPDATE".equals(writeMode)) {
            sql.append("IGNORE ");
        }
        sql.append("INTO ").append(table).append(" (");
        sql.append(String.join(", ", columns));
        sql.append(") VALUES (");
        sql.append(String.join(", ", Collections.nCopies(columns.size(), "?")));
        sql.append(")");

        if ("UPSERT".equals(writeMode) && !onDuplicateKey.isEmpty()) {
            sql.append(" ON DUPLICATE KEY UPDATE ");
            List<String> updates = new ArrayList<>();
            for (String col : onDuplicateKey.split(",")) {
                col = col.trim();
                if (!col.isEmpty()) {
                    updates.add(col + " = VALUES(" + col + ")");
                }
            }
            if (!updates.isEmpty()) {
                sql.append(String.join(", ", updates));
            } else {
                for (String col : columns) {
                    updates.add(col + " = VALUES(" + col + ")");
                }
                sql.append(String.join(", ", updates));
            }
        }
        return sql.toString();
    }
}
