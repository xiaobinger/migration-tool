package com.migration.controller;

import com.migration.model.entity.DataSourceConfig;
import com.migration.service.DataSourceConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/datasources")
@RequiredArgsConstructor
public class DataSourceConfigController {

    private final DataSourceConfigService dataSourceConfigService;

    @GetMapping
    public ResponseEntity<List<DataSourceConfig>> list() {
        return ResponseEntity.ok(dataSourceConfigService.listAll());
    }

    @GetMapping("/enabled")
    public ResponseEntity<List<DataSourceConfig>> listEnabled() {
        return ResponseEntity.ok(dataSourceConfigService.listEnabled());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DataSourceConfig> get(@PathVariable Long id) {
        return ResponseEntity.ok(dataSourceConfigService.getById(id));
    }

    @PostMapping
    public ResponseEntity<DataSourceConfig> create(@RequestBody DataSourceConfig config) {
        return ResponseEntity.ok(dataSourceConfigService.create(config));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DataSourceConfig> update(@PathVariable Long id, @RequestBody DataSourceConfig config) {
        return ResponseEntity.ok(dataSourceConfigService.update(id, config));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        dataSourceConfigService.delete(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/test")
    public ResponseEntity<Map<String, Object>> testConnection(@PathVariable Long id) {
        boolean success = dataSourceConfigService.testConnection(id);
        return ResponseEntity.ok(Map.of("success", success, "message", success ? "连接成功" : "连接失败"));
    }

    @PostMapping("/test")
    public ResponseEntity<Map<String, Object>> testConnectionDirect(@RequestBody DataSourceConfig config) {
        boolean success = dataSourceConfigService.testConnection(config);
        return ResponseEntity.ok(Map.of("success", success, "message", success ? "连接成功" : "连接失败"));
    }

    @GetMapping("/{id}/tables")
    public ResponseEntity<List<String>> getTables(@PathVariable Long id) {
        return ResponseEntity.ok(dataSourceConfigService.getTables(id));
    }

    @GetMapping("/{id}/tables/{tableName}/columns")
    public ResponseEntity<List<Map<String, Object>>> getColumns(@PathVariable Long id, @PathVariable String tableName) {
        return ResponseEntity.ok(dataSourceConfigService.getColumns(id, tableName));
    }
}
