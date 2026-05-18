package com.migration.repository;

import com.migration.model.entity.NodeExecution;
import com.migration.model.enums.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NodeExecutionRepository extends JpaRepository<NodeExecution, Long> {
    List<NodeExecution> findByTaskIdOrderByStartedAt(Long taskId);
    NodeExecution findByTaskIdAndNodeId(Long taskId, String nodeId);
    List<NodeExecution> findByTaskIdAndStatus(Long taskId, TaskStatus status);
    void deleteByTaskId(Long taskId);
    void deleteByTaskIdAndNodeIdIn(Long taskId, List<String> nodeIds);
}