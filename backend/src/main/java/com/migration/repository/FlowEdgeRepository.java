package com.migration.repository;

import com.migration.model.entity.FlowEdge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FlowEdgeRepository extends JpaRepository<FlowEdge, Long> {
    List<FlowEdge> findByFlowDefinitionId(Long flowDefinitionId);
    void deleteByFlowDefinitionId(Long flowDefinitionId);
}
