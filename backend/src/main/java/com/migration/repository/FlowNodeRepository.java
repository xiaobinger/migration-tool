package com.migration.repository;

import com.migration.model.entity.FlowNode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FlowNodeRepository extends JpaRepository<FlowNode, Long> {
    List<FlowNode> findByFlowDefinitionIdOrderBySortOrder(Long flowDefinitionId);
    void deleteByFlowDefinitionId(Long flowDefinitionId);
}
