package com.migration.repository;

import com.migration.model.enums.TaskStatus;
import com.migration.model.entity.MigrationTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MigrationTaskRepository extends JpaRepository<MigrationTask, Long> {
    List<MigrationTask> findByFlowDefinitionId(Long flowDefinitionId);
    List<MigrationTask> findByStatusIn(List<TaskStatus> statuses);
    void deleteByFlowDefinitionId(Long flowDefinitionId);
}
