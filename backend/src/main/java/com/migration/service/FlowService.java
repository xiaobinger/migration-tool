package com.migration.service;

import com.migration.model.dto.FlowDefinitionDTO;
import com.migration.model.entity.FlowDefinition;
import com.migration.model.entity.FlowEdge;
import com.migration.model.entity.FlowNode;
import com.migration.repository.FlowDefinitionRepository;
import com.migration.repository.FlowEdgeRepository;
import com.migration.repository.FlowNodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FlowService {

    private final FlowDefinitionRepository flowDefRepository;
    private final FlowNodeRepository flowNodeRepository;
    private final FlowEdgeRepository flowEdgeRepository;

    /**
     * 获取所有流程定义
     */
    public List<FlowDefinitionDTO> listFlows() {
        return flowDefRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取流程定义详情（含节点和边）
     */
    public FlowDefinitionDTO getFlowDetail(Long id) {
        FlowDefinition def = flowDefRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("流程定义不存在: " + id));
        return toDTO(def);
    }

    /**
     * 创建流程定义
     */
    @Transactional
    public FlowDefinitionDTO createFlow(FlowDefinitionDTO dto) {
        log.info("创建流程定义: {}", dto);
        FlowDefinition def = FlowDefinition.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .version(dto.getVersion() != null ? dto.getVersion() : 1)
                .enabled(dto.getEnabled() != null ? dto.getEnabled() : true)
                .createdBy(dto.getCreatedBy())
                .build();
        def = flowDefRepository.save(def);

        // 保存节点
        if (dto.getNodes() != null) {
            for (int i = 0; i < dto.getNodes().size(); i++) {
                FlowDefinitionDTO.NodeDTO nodeDTO = dto.getNodes().get(i);
                FlowNode node = FlowNode.builder()
                        .nodeId(nodeDTO.getNodeId())
                        .flowDefinitionId(def.getId())
                        .nodeType(com.migration.model.enums.NodeType.fromCode(nodeDTO.getNodeType()))
                        .name(nodeDTO.getName())
                        .description(nodeDTO.getDescription())
                        .config(nodeDTO.getConfig())
                        .positionX(nodeDTO.getPositionX())
                        .positionY(nodeDTO.getPositionY())
                        .sortOrder(i)
                        .implementationClass(nodeDTO.getImplementationClass())
                        .parentGroupId(nodeDTO.getParentGroupId())
                        .build();
                flowNodeRepository.save(node);
            }
        }

        // 保存边
        if (dto.getEdges() != null) {
            for (FlowDefinitionDTO.EdgeDTO edgeDTO : dto.getEdges()) {
                FlowEdge edge = FlowEdge.builder()
                        .flowDefinitionId(def.getId())
                        .edgeId(edgeDTO.getEdgeId())
                        .sourceNodeId(edgeDTO.getSourceNodeId())
                        .targetNodeId(edgeDTO.getTargetNodeId())
                        .condition(edgeDTO.getCondition())
                        .label(edgeDTO.getLabel())
                        .edgeStyle(edgeDTO.getEdgeStyle())
                        .build();
                flowEdgeRepository.save(edge);
            }
        }

        return toDTO(def);
    }

    /**
     * 更新流程定义
     */
    @Transactional
    public FlowDefinitionDTO updateFlow(Long id, FlowDefinitionDTO dto) {
        FlowDefinition def = flowDefRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("流程定义不存在: " + id));

        def.setName(dto.getName());
        def.setDescription(dto.getDescription());
        def.setEnabled(dto.getEnabled());
        def.setVersion(def.getVersion() + 1);
        flowDefRepository.save(def);

        // 删除旧的节点和边，重新保存
        flowNodeRepository.deleteByFlowDefinitionId(id);
        flowEdgeRepository.deleteByFlowDefinitionId(id);

        if (dto.getNodes() != null) {
            for (int i = 0; i < dto.getNodes().size(); i++) {
                FlowDefinitionDTO.NodeDTO nodeDTO = dto.getNodes().get(i);
                FlowNode node = FlowNode.builder()
                        .nodeId(nodeDTO.getNodeId())
                        .flowDefinitionId(id)
                        .nodeType(com.migration.model.enums.NodeType.fromCode(nodeDTO.getNodeType()))
                        .name(nodeDTO.getName())
                        .description(nodeDTO.getDescription())
                        .config(nodeDTO.getConfig())
                        .positionX(nodeDTO.getPositionX())
                        .positionY(nodeDTO.getPositionY())
                        .sortOrder(i)
                        .implementationClass(nodeDTO.getImplementationClass())
                        .parentGroupId(nodeDTO.getParentGroupId())
                        .build();
                flowNodeRepository.save(node);
            }
        }

        if (dto.getEdges() != null) {
            for (FlowDefinitionDTO.EdgeDTO edgeDTO : dto.getEdges()) {
                FlowEdge edge = FlowEdge.builder()
                        .flowDefinitionId(id)
                        .edgeId(edgeDTO.getEdgeId())
                        .sourceNodeId(edgeDTO.getSourceNodeId())
                        .targetNodeId(edgeDTO.getTargetNodeId())
                        .condition(edgeDTO.getCondition())
                        .label(edgeDTO.getLabel())
                        .edgeStyle(edgeDTO.getEdgeStyle())
                        .build();
                flowEdgeRepository.save(edge);
            }
        }

        return toDTO(def);
    }

    /**
     * 删除流程定义
     */
    @Transactional
    public void deleteFlow(Long id) {
        flowNodeRepository.deleteByFlowDefinitionId(id);
        flowEdgeRepository.deleteByFlowDefinitionId(id);
        flowDefRepository.deleteById(id);
    }

    private FlowDefinitionDTO toDTO(FlowDefinition def) {
        FlowDefinitionDTO dto = new FlowDefinitionDTO();
        dto.setId(def.getId());
        dto.setName(def.getName());
        dto.setDescription(def.getDescription());
        dto.setVersion(def.getVersion());
        dto.setEnabled(def.getEnabled());
        dto.setCreatedBy(def.getCreatedBy());

        List<FlowNode> nodes = flowNodeRepository.findByFlowDefinitionIdOrderBySortOrder(def.getId());
        dto.setNodes(nodes.stream().map(n -> {
            FlowDefinitionDTO.NodeDTO nd = new FlowDefinitionDTO.NodeDTO();
            nd.setNodeId(n.getNodeId());
            nd.setNodeType(n.getNodeType().getCode());
            nd.setName(n.getName());
            nd.setDescription(n.getDescription());
            nd.setConfig(n.getConfig());
            nd.setPositionX(n.getPositionX());
            nd.setPositionY(n.getPositionY());
            nd.setImplementationClass(n.getImplementationClass());
            nd.setParentGroupId(n.getParentGroupId());
            return nd;
        }).collect(Collectors.toList()));

        List<FlowEdge> edges = flowEdgeRepository.findByFlowDefinitionId(def.getId());
        dto.setEdges(edges.stream().map(e -> {
            FlowDefinitionDTO.EdgeDTO ed = new FlowDefinitionDTO.EdgeDTO();
            ed.setEdgeId(e.getEdgeId());
            ed.setSourceNodeId(e.getSourceNodeId());
            ed.setTargetNodeId(e.getTargetNodeId());
            ed.setCondition(e.getCondition());
            ed.setLabel(e.getLabel());
            ed.setEdgeStyle(e.getEdgeStyle());
            return ed;
        }).collect(Collectors.toList()));

        return dto;
    }
}
