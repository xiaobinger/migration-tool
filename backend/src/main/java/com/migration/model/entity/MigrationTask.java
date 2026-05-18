package com.migration.model.entity;

import com.migration.model.enums.TaskStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 迁移任务实例
 */
@Entity
@Table(name = "migration_task")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MigrationTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 任务名称 */
    @Column(nullable = false)
    private String name;

    /** 关联的流程定义ID */
    @Column(nullable = false)
    private Long flowDefinitionId;

    /** 任务状态 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TaskStatus status = TaskStatus.PENDING;

    /** 当前执行到的节点ID */
    private String currentNodeId;

    /** 总节点数 */
    @Builder.Default
    private Integer totalNodes = 0;

    /** 已完成节点数 */
    @Builder.Default
    private Integer completedNodes = 0;

    /** 总处理记录数 */
    @Builder.Default
    private Long totalRecords = 0L;

    /** 成功记录数 */
    @Builder.Default
    private Long successRecords = 0L;

    /** 失败记录数 */
    @Builder.Default
    private Long failedRecords = 0L;

    /** 提取记录数 */
    @Builder.Default
    private Long extractedRecords = 0L;

    /** 加载记录数 */
    @Builder.Default
    private Long loadedRecords = 0L;

    /** 加载成功记录数 */
    @Builder.Default
    private Long loadedSuccessRecords = 0L;

    /** 加载失败记录数 */
    @Builder.Default
    private Long loadedFailedRecords = 0L;

    /** 错误信息 */
    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    /** 任务参数（JSON） */
    @Column(columnDefinition = "TEXT")
    private String params;

    /** 可从哪个节点重启（断点续传） */
    private String restartFromNodeId;

    /** 创建人 */
    private String createdBy;

    /** 创建时间 */
    @Column(updatable = false)
    private LocalDateTime createdAt;

    /** 开始执行时间 */
    private LocalDateTime startedAt;

    /** 结束时间 */
    private LocalDateTime finishedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    /** 计算进度百分比 */
    @Transient
    public int getProgress() {
        if (totalNodes == null || totalNodes == 0) return 0;
        return (int) ((completedNodes * 100.0) / totalNodes);
    }
}
