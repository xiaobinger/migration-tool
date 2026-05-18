package com.migration.model.entity;

import com.migration.model.enums.DatabaseType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "data_source_config")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DataSourceConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DatabaseType type;

    @Column(nullable = false)
    private String host;

    @Column(nullable = false)
    @Builder.Default
    private Integer port = 3306;

    @Column(nullable = false)
    private String database;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(columnDefinition = "TEXT")
    private String properties;

    @Builder.Default
    private Boolean sshEnabled = false;

    private String sshHost;

    @Builder.Default
    private Integer sshPort = 22;

    private String sshUsername;

    private String sshPassword;

    @Column(columnDefinition = "TEXT")
    private String sshAuthKey;

    @Builder.Default
    private Boolean enabled = true;

    private String description;

    @Column(updatable = false)
    private LocalDateTime createdAt;

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
