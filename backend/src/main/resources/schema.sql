CREATE DATABASE IF NOT EXISTS migration_tool
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE migration_tool;
DROP TABLE IF EXISTS data_source_config;
CREATE TABLE IF NOT EXISTS `data_source_config` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(255) NOT NULL,
    `type` VARCHAR(255) NOT NULL,
    `host` VARCHAR(255) NOT NULL,
    `port` INT NOT NULL DEFAULT 3306,
    `database` VARCHAR(255) NOT NULL,
    `username` VARCHAR(255) NOT NULL,
    `password` VARCHAR(255) NOT NULL,
    `properties` TEXT,
    `ssh_enabled` BIT(1) DEFAULT 0,
    `ssh_host` VARCHAR(255),
    `ssh_port` INT DEFAULT 22,
    `ssh_username` VARCHAR(255),
    `ssh_password` VARCHAR(255),
    `ssh_auth_key` TEXT,
    `enabled` BIT(1) DEFAULT 1,
    `description` VARCHAR(255),
    `created_at` DATETIME,
    `updated_at` DATETIME,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS flow_definition;
CREATE TABLE IF NOT EXISTS `flow_definition` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(255) NOT NULL,
    `description` VARCHAR(255),
    `version` INT NOT NULL DEFAULT 1,
    `enabled` BIT(1) DEFAULT 1,
    `created_by` VARCHAR(255),
    `created_at` DATETIME,
    `updated_at` DATETIME,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS flow_node;
CREATE TABLE IF NOT EXISTS `flow_node` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `node_id` VARCHAR(255) NOT NULL,
    `flow_definition_id` BIGINT NOT NULL,
    `node_type` VARCHAR(255) NOT NULL,
    `name` VARCHAR(255) NOT NULL,
    `description` VARCHAR(255),
    `config` TEXT,
    `position_x` DOUBLE,
    `position_y` DOUBLE,
    `sort_order` INT,
    `implementation_class` VARCHAR(255),
    `parent_group_id` VARCHAR(255),
    PRIMARY KEY (`id`),
    KEY `idx_flow_node_flow_def_id` (`flow_definition_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS flow_edge;
CREATE TABLE IF NOT EXISTS `flow_edge` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `flow_definition_id` BIGINT NOT NULL,
    `edge_id` VARCHAR(255) NOT NULL,
    `source_node_id` VARCHAR(255) NOT NULL,
    `target_node_id` VARCHAR(255) NOT NULL,
    `condition` VARCHAR(1024),
    `label` VARCHAR(255),
    `edge_style` VARCHAR(255),
    PRIMARY KEY (`id`),
    KEY `idx_flow_edge_flow_def_id` (`flow_definition_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


DROP TABLE IF EXISTS migration_task;
CREATE TABLE IF NOT EXISTS `migration_task` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(255) NOT NULL,
    `flow_definition_id` BIGINT NOT NULL,
    `status` VARCHAR(255) NOT NULL DEFAULT 'PENDING',
    `current_node_id` VARCHAR(255),
    `total_nodes` INT DEFAULT 0,
    `completed_nodes` INT DEFAULT 0,
    `total_records` BIGINT DEFAULT 0,
    `success_records` BIGINT DEFAULT 0,
    `failed_records` BIGINT DEFAULT 0,
    `extracted_records` BIGINT DEFAULT 0,
    `loaded_records` BIGINT DEFAULT 0,
    `loaded_success_records` BIGINT DEFAULT 0,
    `loaded_failed_records` BIGINT DEFAULT 0,
    `error_message` TEXT,
    `params` TEXT,
    `restart_from_node_id` VARCHAR(255),
    `created_by` VARCHAR(255),
    `created_at` DATETIME,
    `started_at` DATETIME,
    `finished_at` DATETIME,
    PRIMARY KEY (`id`),
    KEY `idx_migration_task_flow_def_id` (`flow_definition_id`),
    KEY `idx_migration_task_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS node_execution;
CREATE TABLE IF NOT EXISTS `node_execution` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `task_id` BIGINT NOT NULL,
    `node_id` VARCHAR(255) NOT NULL,
    `node_name` VARCHAR(255),
    `node_type` VARCHAR(255),
    `incoming_edge_id` VARCHAR(255),
    `status` VARCHAR(255) NOT NULL DEFAULT 'PENDING',
    `retry_count` INT DEFAULT 0,
    `max_retry` INT DEFAULT 3,
    `error_message` TEXT,
    `input_summary` TEXT,
    `output_summary` TEXT,
    `started_at` DATETIME,
    `finished_at` DATETIME,
    `duration` BIGINT DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_node_execution_task_id` (`task_id`),
    KEY `idx_node_execution_task_status` (`task_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS task_log;
CREATE TABLE IF NOT EXISTS `task_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `task_id` BIGINT NOT NULL,
    `node_id` VARCHAR(255),
    `node_name` VARCHAR(255),
    `level` VARCHAR(255) NOT NULL DEFAULT 'INFO',
    `message` TEXT NOT NULL,
    `stack_trace` TEXT,
    `record_count` BIGINT DEFAULT 0,
    `duration` BIGINT DEFAULT 0,
    `log_time` DATETIME,
    PRIMARY KEY (`id`),
    KEY `idx_task_log_task_id` (`task_id`),
    KEY `idx_task_log_task_node` (`task_id`, `node_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS load_failure_record;
CREATE TABLE IF NOT EXISTS `load_failure_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `task_id` BIGINT NOT NULL,
    `node_execution_id` BIGINT,
    `node_id` VARCHAR(255),
    `node_name` VARCHAR(255),
    `target_table` VARCHAR(255),
    `row_data` TEXT,
    `error_message` TEXT,
    `retried` BIT(1) DEFAULT 0,
    `failed_at` DATETIME,
    `retried_at` DATETIME,
    PRIMARY KEY (`id`),
    KEY `idx_load_failure_task_id` (`task_id`),
    KEY `idx_load_failure_task_node` (`task_id`, `node_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
