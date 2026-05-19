package com.migration.loader;

import cn.hutool.json.JSONUtil;
import com.migration.engine.FlowEngine;
import com.migration.model.entity.FlowNode;
import com.migration.service.DataSourceConfigService;
import lombok.extern.slf4j.Slf4j;

import java.sql.*;
import java.util.*;

@Slf4j
public class PostgreSQLDataLoader implements DataLoader {

    private DataSourceConfigService dataSourceConfigService;

    @Override
    public void setDataSourceConfigService(DataSourceConfigService service) {
        this.dataSourceConfigService = service;
    }

    @Override
    public String getName() { return "PostgreSQLDataLoader"; }

    @Override
    public String getFullName() { return "com.migration.loader.PostgreSQLDataLoader"; }

    @Override
    public String getDescription() { return "PostgreSQL数据库数据加载器"; }

    @Override
    @SuppressWarnings("unchecked")
    public FlowEngine.NodeResult execute(FlowEngine.FlowContext context, FlowNode node) {
        log.info("PostgreSQL数据加载开始: nodeId={}", node.getNodeId());

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
            port = Integer.parseInt(getConfig(node, "port", "5432"));
            database = getConfig(node, "database", "");
            username = getConfig(node, "username", "");
            password = getConfig(node, "password", "");
        }

        String table = getConfig(node, "table", "");
        String writeMode = getConfig(node, "writeMode", "INSERT");
        int batchSize = Integer.parseInt(getConfig(node, "batchSize", "1000"));

        List<Map<String, Object>> dataRows = new ArrayList<>();
        for (Object item : context.getData()) {
            if (item instanceof Map) {
                dataRows.add((Map<String, Object>) item);
            }
        }

        if (dataRows.isEmpty()) {
            return FlowEngine.NodeResult.ok("无数据需要加载", 0, 0, 0);
        }

        String jdbcUrl = String.format("jdbc:postgresql://%s:%d/%s?sslmode=disable&reWriteBatchedInserts=true", host, port, database);
        log.info("加载到PostgreSQL: {}, table={}, mode={}, 数据量={}", jdbcUrl, table, writeMode, dataRows.size());

        long successRecords = 0;
        long failedRecords = 0;
        List<FlowEngine.FailedRow> failedRows = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password)) {
            conn.setAutoCommit(false);

            Map<String, Object> firstRow = dataRows.get(0);
            List<String> columns = new ArrayList<>(firstRow.keySet());

            String sql = buildInsertSql(table, columns, writeMode);
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
                        failedRows.add(FlowEngine.FailedRow.builder()
                                .rowData(JSONUtil.toJsonStr(row))
                                .errorMessage(e.getMessage())
                                .build());
                        log.warn("行写入失败: {}", e.getMessage());
                    }
                }
                if (batchCount > 0) {
                    try {
                        ps.executeBatch();
                        successRecords += batchCount;
                    } catch (BatchUpdateException e) {
                        int[] updateCounts = e.getUpdateCounts();
                        for (int i = 0; i < updateCounts.length; i++) {
                            if (updateCounts[i] == Statement.EXECUTE_FAILED) {
                                failedRecords++;
                                successRecords--;
                            }
                        }
                    }
                }
            }
            conn.commit();
        } catch (SQLException e) {
            log.error("PostgreSQL写入失败: {}", e.getMessage(), e);
            return FlowEngine.NodeResult.fail("PostgreSQL写入失败: " + e.getMessage());
        }

        context.getVariables().put(node.getNodeId() + "_loaded", successRecords);

        String summary = String.format("数据加载到PostgreSQL [%s.%s] 完成，共 %d 条，成功 %d 条，失败 %d 条，模式: %s",
                database, table, dataRows.size(), successRecords, failedRecords, writeMode);
        FlowEngine.NodeResult result = FlowEngine.NodeResult.ok(summary, dataRows.size(), successRecords, failedRecords);
        result.setFailedRows(failedRows);
        return result;
    }

    private String buildInsertSql(String table, List<String> columns, String writeMode) {
        StringBuilder sql = new StringBuilder("INSERT INTO ").append(table).append(" (");
        sql.append(String.join(", ", columns));
        sql.append(") VALUES (");
        sql.append(String.join(", ", Collections.nCopies(columns.size(), "?")));
        sql.append(")");

        if ("UPSERT".equals(writeMode)) {
            sql.append(" ON CONFLICT DO UPDATE SET ");
            List<String> updates = new ArrayList<>();
            for (String col : columns) {
                updates.add(col + " = EXCLUDED." + col);
            }
            sql.append(String.join(", ", updates));
        } else if ("UPDATE".equals(writeMode)) {
            sql.insert(0, "INSERT ").append(" ON CONFLICT DO NOTHING");
        }
        return sql.toString();
    }
}
