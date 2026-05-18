package com.migration.repository;

import com.migration.model.entity.TaskLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskLogRepository extends JpaRepository<TaskLog, Long> {
    List<TaskLog> findByTaskIdOrderByLogTimeDesc(Long taskId);
    List<TaskLog> findByTaskIdAndNodeIdOrderByLogTimeDesc(Long taskId, String nodeId);
    long countByTaskId(Long taskId);
}
