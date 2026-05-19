package com.migration.repository;

import com.migration.model.entity.LoadFailureRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoadFailureRecordRepository extends JpaRepository<LoadFailureRecord, Long> {
    List<LoadFailureRecord> findByTaskIdAndNodeId(Long taskId, String nodeId);
    List<LoadFailureRecord> findByTaskId(Long taskId);
    List<LoadFailureRecord> findByTaskIdAndRetriedFalse(Long taskId);
    long countByTaskIdAndNodeId(Long taskId, String nodeId);
    long countByTaskIdAndRetriedFalse(Long taskId);
    void deleteByTaskIdAndNodeId(Long taskId, String nodeId);
}
