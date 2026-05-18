package com.migration.model.entity;

import com.migration.model.enums.TaskStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 节点执行记录（每个节点每次执行一条）
 */
@Entity
@Table(name = "node_execution")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NodeExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 关联任务ID */
    @Column(nullable = false)
    private Long taskId;

    /** 节点ID */
    @Column(nullable = false)
    private String nodeId;

    /** 节点名称 */
    private String nodeName;

    /** 节点类型 */
    private String nodeType;

    /** 进入该节点的边ID */
    private String incomingEdgeId;

    /** 执行状态 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TaskStatus status = TaskStatus.PENDING;

    /** 重试次数 */
    @Builder.Default
    private Integer retryCount = 0;

    /** 最大重试次数 */
    @Builder.Default
    private Integer maxRetry = 3;

    /** 错误信息 */
    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    /** 节点输入数据摘要 */
    @Column(columnDefinition = "TEXT")
    private String inputSummary;

    /** 节点输出数据摘要 */
    @Column(columnDefinition = "TEXT")
    private String outputSummary;

    /** 开始时间 */
    private LocalDateTime startedAt;

    /** 结束时间 */
    private LocalDateTime finishedAt;

    /** 耗时(ms) */
    @Builder.Default
    private Long duration = 0L;
}
