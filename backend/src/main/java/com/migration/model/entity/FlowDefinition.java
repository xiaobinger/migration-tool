package com.migration.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 流程定义
 */
@Entity
@Table(name = "flow_definition")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlowDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 流程名称 */
    @Column(nullable = false)
    private String name;

    /** 流程描述 */
    private String description;

    /** 流程版本 */
    @Column(nullable = false)
    @Builder.Default
    private Integer version = 1;

    /** 是否启用 */
    @Builder.Default
    private Boolean enabled = true;

    /** 创建人 */
    private String createdBy;

    /** 创建时间 */
    @Column(updatable = false)
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
