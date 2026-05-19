package com.migration.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "load_failure_record")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoadFailureRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long taskId;

    private Long nodeExecutionId;

    private String nodeId;

    private String nodeName;

    private String targetTable;

    @Column(columnDefinition = "TEXT")
    private String rowData;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Builder.Default
    private Boolean retried = false;

    private LocalDateTime failedAt;

    private LocalDateTime retriedAt;

    @PrePersist
    protected void onCreate() {
        failedAt = LocalDateTime.now();
    }
}
