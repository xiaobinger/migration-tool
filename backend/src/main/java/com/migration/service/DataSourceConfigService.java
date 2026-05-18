package com.migration.service;

import com.migration.model.enums.DatabaseType;
import com.migration.model.entity.DataSourceConfig;
import com.migration.repository.DataSourceConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.*;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataSourceConfigService {

    private final DataSourceConfigRepository dataSourceConfigRepository;

    public List<DataSourceConfig> listAll() {
        return dataSourceConfigRepository.findAll();
    }

    public List<DataSourceConfig> listEnabled() {
        return dataSourceConfigRepository.findByEnabledTrue();
    }

    public DataSourceConfig getById(Long id) {
        return dataSourceConfigRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("数据源配置不存在: " + id));
    }

    @Transactional
    public DataSourceConfig create(DataSourceConfig config) {
        return dataSourceConfigRepository.save(config);
    }

    @Transactional
    public DataSourceConfig update(Long id, DataSourceConfig config) {
        DataSourceConfig existing = getById(id);
        existing.setName(config.getName());
        existing.setType(config.getType());
        existing.setHost(config.getHost());
        existing.setPort(config.getPort());
        existing.setDatabase(config.getDatabase());
        existing.setUsername(config.getUsername());
        if (config.getPassword() != null && !config.getPassword().isEmpty()) {
            existing.setPassword(config.getPassword());
        }
        existing.setProperties(config.getProperties());
        existing.setSshEnabled(config.getSshEnabled());
        existing.setSshHost(config.getSshHost());
        existing.setSshPort(config.getSshPort());
        existing.setSshUsername(config.getSshUsername());
        if (config.getSshPassword() != null && !config.getSshPassword().isEmpty()) {
            existing.setSshPassword(config.getSshPassword());
        }
        existing.setSshAuthKey(config.getSshAuthKey());
        existing.setEnabled(config.getEnabled());
        existing.setDescription(config.getDescription());
        return dataSourceConfigRepository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        dataSourceConfigRepository.deleteById(id);
    }

    public boolean testConnection(Long id) {
        DataSourceConfig config = getById(id);
        return testConnection(config);
    }

    public boolean testConnection(DataSourceConfig config) {
        if (config.getType() == DatabaseType.MONGODB) {
            return testMongoConnection(config);
        }
        String url = buildJdbcUrl(config);
        try (Connection conn = DriverManager.getConnection(url, config.getUsername(), config.getPassword())) {
            return conn.isValid(10);
        } catch (SQLException e) {
            log.error("测试连接失败: {}", e.getMessage());
            return false;
        }
    }

    private boolean testMongoConnection(DataSourceConfig config) {
        try {
            String uri = String.format("mongodb://%s:%s@%s:%d/%s", config.getUsername(), config.getPassword(), config.getHost(), config.getPort(), config.getDatabase());
            com.mongodb.client.MongoClient client = com.mongodb.client.MongoClients.create(uri);
            client.listDatabaseNames().first();
            client.close();
            return true;
        } catch (Exception e) {
            log.error("MongoDB测试连接失败: {}", e.getMessage());
            return false;
        }
    }

    public List<String> getTables(Long id) {
        DataSourceConfig config = getById(id);
        return getTables(config);
    }

    public List<String> getTables(DataSourceConfig config) {
        if (config.getType() == DatabaseType.MONGODB) {
            return getMongoCollections(config);
        }
        String url = buildJdbcUrl(config);
        List<String> tables = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(url, config.getUsername(), config.getPassword())) {
            DatabaseMetaData meta = conn.getMetaData();
            try (ResultSet rs = meta.getTables(config.getDatabase(), null, "%", new String[]{"TABLE"})) {
                while (rs.next()) {
                    tables.add(rs.getString("TABLE_NAME"));
                }
            }
        } catch (SQLException e) {
            log.error("获取表列表失败: {}", e.getMessage());
        }
        return tables;
    }

    private List<String> getMongoCollections(DataSourceConfig config) {
        List<String> collections = new ArrayList<>();
        try {
            String uri = String.format("mongodb://%s:%s@%s:%d/%s", config.getUsername(), config.getPassword(), config.getHost(), config.getPort(), config.getDatabase());
            com.mongodb.client.MongoClient client = com.mongodb.client.MongoClients.create(uri);
            com.mongodb.client.MongoDatabase db = client.getDatabase(config.getDatabase());
            for (String name : db.listCollectionNames()) {
                collections.add(name);
            }
            client.close();
        } catch (Exception e) {
            log.error("获取MongoDB集合列表失败: {}", e.getMessage());
        }
        return collections;
    }

    public List<Map<String, Object>> getColumns(Long id, String tableName) {
        DataSourceConfig config = getById(id);
        return getColumns(config, tableName);
    }

    public List<Map<String, Object>> getColumns(DataSourceConfig config, String tableName) {
        String url = buildJdbcUrl(config);
        List<Map<String, Object>> columns = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(url, config.getUsername(), config.getPassword())) {
            DatabaseMetaData meta = conn.getMetaData();
            try (ResultSet rs = meta.getColumns(config.getDatabase(), null, tableName, null)) {
                while (rs.next()) {
                    Map<String, Object> col = new LinkedHashMap<>();
                    col.put("name", rs.getString("COLUMN_NAME"));
                    col.put("type", rs.getString("TYPE_NAME"));
                    col.put("nullable", rs.getInt("NULLABLE") == DatabaseMetaData.columnNullable);
                    col.put("size", rs.getInt("COLUMN_SIZE"));
                    columns.add(col);
                }
            }
        } catch (SQLException e) {
            log.error("获取列信息失败: {}", e.getMessage());
        }
        return columns;
    }

    private String buildJdbcUrl(DataSourceConfig config) {
        return switch (config.getType()) {
            case MYSQL -> String.format("jdbc:mysql://%s:%d/%s?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC", config.getHost(), config.getPort(), config.getDatabase());
            case POSTGRESQL -> String.format("jdbc:postgresql://%s:%d/%s?sslmode=disable", config.getHost(), config.getPort(), config.getDatabase());
            default -> throw new IllegalArgumentException("不支持的数据库类型: " + config.getType());
        };
    }
}
