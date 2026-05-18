package com.migration.model.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 流程边（节点连线）
 */
@Entity
@Table(name = "flow_edge")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlowEdge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 所属流程定义ID */
    @Column(nullable = false)
    private Long flowDefinitionId;

    /** 边ID */
    @Column(nullable = false)
    private String edgeId;

    /** 源节点ID */
    @Column(nullable = false)
    private String sourceNodeId;

    /** 目标节点ID */
    @Column(nullable = false)
    private String targetNodeId;

    /** 条件表达式（条件分支时使用） */
    @Column(name = "`condition`")
    private String condition;

    /** 边标签 */
    private String label;

    /** 连线样式 */
    private String edgeStyle;
}
