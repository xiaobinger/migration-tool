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

    @Column(nullable = false,name = "`name`")
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DatabaseType type;

    @Column(nullable = false,name = "`host`")
    private String host;

    @Column(nullable = false,name = "`port`")
    @Builder.Default
    private Integer port = 3306;

    @Column(nullable = false,name = "`database`")
    private String database;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false,name = "`password`")
    private String password;

    @Column(columnDefinition = "TEXT",name = "`properties`")
    private String properties;

    @Builder.Default
    private Boolean sshEnabled = false;

    private String sshHost;

    @Builder.Default
    private Integer sshPort = 22;

    private String sshUsername;

    private String sshPassword;

    @Column(columnDefinition = "TEXT",name = "`ssh_auth_key`")
    private String sshAuthKey;

    @Builder.Default
    @Column(name = "`enabled`")
    private Boolean enabled = true;

    @Column(name = "`description`")
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
