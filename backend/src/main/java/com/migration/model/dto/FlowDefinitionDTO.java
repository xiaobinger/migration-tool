package com.migration.model.dto;

import lombok.Data;

import java.util.List;

/**
 * 流程定义完整DTO（含节点和边）
 */
@Data
public class FlowDefinitionDTO {

    private Long id;
    private String name;
    private String description;
    private Integer version;
    private Boolean enabled;
    private String createdBy;

    /** 节点列表 */
    private List<NodeDTO> nodes;

    /** 边列表 */
    private List<EdgeDTO> edges;

    @Data
    public static class NodeDTO {
        private String nodeId;
        private String nodeType;
        private String name;
        private String description;
        private String config;
        private Double positionX;
        private Double positionY;
        private String implementationClass;
        private String parentGroupId;
    }

    @Data
    public static class EdgeDTO {
        private String edgeId;
        private String sourceNodeId;
        private String targetNodeId;
        private String condition;
        private String label;
        private String edgeStyle;
    }
}
