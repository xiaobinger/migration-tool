package com.migration.extractor;

import com.migration.engine.FlowEngine;
import com.migration.model.entity.FlowNode;
import com.migration.service.DataSourceConfigService;
import lombok.extern.slf4j.Slf4j;

import java.sql.*;
import java.util.*;

@Slf4j
public class PostgreSQLDataExtractor implements DataExtractor {

    private DataSourceConfigService dataSourceConfigService;

    @Override
    public void setDataSourceConfigService(DataSourceConfigService service) {
        this.dataSourceConfigService = service;
    }

    @Override
    public String getName() { return "PostgreSQLDataExtractor"; }

    @Override
    public String getFullName() { return "com.migration.extractor.PostgreSQLDataExtractor"; }

    @Override
    public String getDescription() { return "PostgreSQL数据库数据提取器"; }

    @Override
    public FlowEngine.NodeResult execute(FlowEngine.FlowContext context, FlowNode node) {
        log.info("PostgreSQL数据提取开始: nodeId={}", node.getNodeId());

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

        String queryMode = getConfig(node, "queryMode", "table");
        String table = getConfig(node, "table", "");
        String script = getConfig(node, "script", "");
        int batchSize = Integer.parseInt(getConfig(node, "batchSize", "1000"));
        String whereClause = getConfig(node, "whereClause", "");

        String sql;
        String source;
        if ("script".equals(queryMode) && !script.isEmpty()) {
            sql = script;
            source = "custom SQL";
        } else {
            StringBuilder sb = new StringBuilder("SELECT * FROM ").append(table);
            if (!whereClause.isEmpty()) {
                sb.append(" WHERE ").append(whereClause);
            }
            sql = sb.toString();
            source = table;
        }

        String jdbcUrl = String.format("jdbc:postgresql://%s:%d/%s?sslmode=disable", host, port, database);
        log.info("连接PostgreSQL: {}, 查询: {}", jdbcUrl, sql);

        List<Map<String, Object>> rows = new ArrayList<>();
        long totalRecords = 0;

        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password)) {
            try (PreparedStatement ps = conn.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {
                ps.setFetchSize(batchSize);
                try (ResultSet rs = ps.executeQuery()) {
                    ResultSetMetaData meta = rs.getMetaData();
                    int colCount = meta.getColumnCount();

                    while (rs.next()) {
                        Map<String, Object> row = new LinkedHashMap<>();
                        for (int i = 1; i <= colCount; i++) {
                            row.put(meta.getColumnLabel(i), rs.getObject(i));
                        }
                        rows.add(row);
                        totalRecords++;
                    }
                }
            }
        } catch (SQLException e) {
            log.error("PostgreSQL查询失败: {}", e.getMessage(), e);
            return FlowEngine.NodeResult.fail("PostgreSQL查询失败: " + e.getMessage());
        }

        context.getData().addAll(rows);
        context.getVariables().put(node.getNodeId() + "_extracted", totalRecords);

        String summary = String.format("从PostgreSQL [%s.%s] 提取数据完成(queryMode=%s)，共 %d 条记录", database, source, queryMode, totalRecords);
        return FlowEngine.NodeResult.ok(summary, totalRecords, totalRecords, 0);
    }
}
