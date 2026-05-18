package com.migration.repository;

import com.migration.model.entity.FlowDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FlowDefinitionRepository extends JpaRepository<FlowDefinition, Long> {
    List<FlowDefinition> findByEnabledTrue();
}
