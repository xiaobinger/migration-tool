package com.migration.model.entity;

import com.migration.model.enums.NodeType;
import jakarta.persistence.*;
import lombok.*;

/**
 * 流程节点定义
 */
@Entity
@Table(name = "flow_node")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlowNode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 节点ID（流程内唯一，如 "node_1"） */
    @Column(nullable = false)
    private String nodeId;

    /** 所属流程定义ID */
    @Column(nullable = false)
    private Long flowDefinitionId;

    /** 节点类型 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NodeType nodeType;

    /** 节点名称 */
    @Column(nullable = false)
    private String name;

    /** 节点描述 */
    private String description;

    /** 节点配置（JSON格式，不同类型节点有不同配置） */
    @Column(columnDefinition = "TEXT")
    private String config;

    /** 画布X坐标 */
    private Double positionX;

    /** 画布Y坐标 */
    private Double positionY;

    /** 排序号 */
    private Integer sortOrder;

    /** 业务实现类名称 */
    @Column(name = "implementation_class")
    private String implementationClass;

    /** 所属并行组的节点ID（为空表示不属于任何并行组） */
    private String parentGroupId;
}
