package com.migration.loader;

import cn.hutool.json.JSONUtil;
import com.migration.engine.FlowEngine;
import com.migration.model.entity.FlowNode;
import com.migration.service.DataSourceConfigService;
import lombok.extern.slf4j.Slf4j;

import java.sql.*;
import java.time.*;
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
        List<FlowEngine.FailedRow> failedRows = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password)) {
            conn.setAutoCommit(false);

            Map<String, Object> firstRow = dataRows.get(0);
            List<String> columns = new ArrayList<>(firstRow.keySet());

            Map<String, Integer> columnTypes = getColumnTypes(conn, table, columns);

            String sql = buildInsertSql(table, columns, writeMode, onDuplicateKey);
            log.debug("INSERT SQL: {}", sql);

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                List<Map<String, Object>> currentBatch = new ArrayList<>();
                for (Map<String, Object> row : dataRows) {
                    try {
                        for (int i = 0; i < columns.size(); i++) {
                            String col = columns.get(i);
                            Object val = row.get(col);
                            Integer sqlType = columnTypes.get(col);
                            if (sqlType != null) {
                                ps.setObject(i + 1, convertForTargetColumn(val, sqlType));
                            } else {
                                ps.setObject(i + 1, val);
                            }
                        }
                        ps.addBatch();
                        currentBatch.add(row);

                        if (currentBatch.size() >= batchSize) {
                            successRecords += executeBatchWithTracking(ps, currentBatch, failedRows);
                            currentBatch.clear();
                        }
                    } catch (Exception e) {
                        failedRows.add(FlowEngine.FailedRow.builder()
                                .rowData(JSONUtil.toJsonStr(row))
                                .errorMessage(e.getMessage())
                                .build());
                        log.warn("行写入失败: {}", e.getMessage());
                    }
                }
                if (!currentBatch.isEmpty()) {
                    successRecords += executeBatchWithTracking(ps, currentBatch, failedRows);
                    currentBatch.clear();
                }
            }
            conn.commit();
        } catch (SQLException e) {
            log.error("MySQL写入失败: {}", e.getMessage(), e);
            return FlowEngine.NodeResult.fail("MySQL写入失败: " + e.getMessage());
        }

        context.getVariables().put(node.getNodeId() + "_loaded", successRecords);

        long totalFailed = failedRows.size();
        String summary = String.format("数据加载到MySQL [%s.%s] 完成，共 %d 条，成功 %d 条，失败 %d 条，模式: %s",
                database, table, dataRows.size(), successRecords, totalFailed, writeMode);
        FlowEngine.NodeResult result = FlowEngine.NodeResult.ok(summary, dataRows.size(), successRecords, totalFailed);
        result.setFailedRows(failedRows);
        return result;
    }

    private long executeBatchWithTracking(PreparedStatement ps,
                                           List<Map<String, Object>> currentBatch,
                                           List<FlowEngine.FailedRow> failedRows) throws SQLException {
        long batchSuccess = 0;
        try {
            ps.executeBatch();
            batchSuccess = currentBatch.size();
        } catch (BatchUpdateException e) {
            int[] updateCounts = e.getUpdateCounts();
            for (int i = 0; i < updateCounts.length; i++) {
                if (updateCounts[i] == Statement.EXECUTE_FAILED || updateCounts[i] < 0) {
                    failedRows.add(FlowEngine.FailedRow.builder()
                            .rowData(JSONUtil.toJsonStr(currentBatch.get(i)))
                            .errorMessage(e.getMessage())
                            .build());
                } else {
                    batchSuccess++;
                }
            }
            for (int i = updateCounts.length; i < currentBatch.size(); i++) {
                failedRows.add(FlowEngine.FailedRow.builder()
                        .rowData(JSONUtil.toJsonStr(currentBatch.get(i)))
                        .errorMessage("Batch execution failed")
                        .build());
            }
            log.warn("批量写入部分失败: 成功={}, 失败={}, 原因={}", batchSuccess, currentBatch.size() - batchSuccess, e.getMessage());
        }
        return batchSuccess;
    }

    private Map<String, Integer> getColumnTypes(Connection conn, String table, List<String> columns) {
        Map<String, Integer> columnTypes = new HashMap<>();
        try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM " + table + " WHERE 1=0")) {
            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData meta = rs.getMetaData();
                for (int i = 1; i <= meta.getColumnCount(); i++) {
                    columnTypes.put(meta.getColumnLabel(i), meta.getColumnType(i));
                }
            }
        } catch (SQLException e) {
            log.warn("无法获取目标表列类型信息: {}", e.getMessage());
        }
        return columnTypes;
    }

    private Object convertForTargetColumn(Object value, int targetSqlType) {
        if (value == null || !(value instanceof String)) return value;
        String str = (String) value;
        try {
            switch (targetSqlType) {
                case Types.TIMESTAMP:
                case Types.TIMESTAMP_WITH_TIMEZONE:
                    if (str.contains("T")) {
                        return java.sql.Timestamp.valueOf(LocalDateTime.parse(str));
                    }
                    return java.sql.Timestamp.valueOf(LocalDateTime.parse(str.replace(" ", "T")));
                case Types.DATE:
                    return java.sql.Date.valueOf(LocalDate.parse(str.length() > 10 ? str.substring(0, 10) : str));
                case Types.TIME:
                case Types.TIME_WITH_TIMEZONE:
                    return java.sql.Time.valueOf(LocalTime.parse(str));
                default:
                    return value;
            }
        } catch (Exception e) {
            log.warn("日期时间值转换失败: value={}, targetType={}, error={}", str, targetSqlType, e.getMessage());
            return value;
        }
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
