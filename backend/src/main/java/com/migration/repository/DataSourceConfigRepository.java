package com.migration.repository;

import com.migration.model.entity.DataSourceConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DataSourceConfigRepository extends JpaRepository<DataSourceConfig, Long> {
    List<DataSourceConfig> findByEnabledTrue();
    List<DataSourceConfig> findByType(com.migration.model.enums.DatabaseType type);
}
