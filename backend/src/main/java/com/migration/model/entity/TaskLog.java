package com.migration.model.entity;

import com.migration.model.enums.LogLevel;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 任务执行日志
 */
@Entity
@Table(name = "task_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 关联任务ID */
    @Column(nullable = false)
    private Long taskId;

    /** 关联节点ID */
    private String nodeId;

    /** 节点名称 */
    private String nodeName;

    /** 日志级别 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private LogLevel level = LogLevel.INFO;

    /** 日志消息 */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    /** 错误堆栈 */
    @Column(columnDefinition = "TEXT")
    private String stackTrace;

    /** 处理记录数 */
    @Builder.Default
    private Long recordCount = 0L;

    /** 耗时(ms) */
    @Builder.Default
    private Long duration = 0L;

    /** 日志时间 */
    @Column(updatable = false)
    private LocalDateTime logTime;

    @PrePersist
    protected void onCreate() {
        logTime = LocalDateTime.now();
    }
}
