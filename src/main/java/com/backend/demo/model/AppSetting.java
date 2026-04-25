package com.backend.demo.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "app_settings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppSetting {

    @Id
    @Column(name = "setting_key", length = 120)
    private String key;

    @Column(name = "setting_group", nullable = false, length = 60)
    private String group;

    @Column(name = "setting_value", columnDefinition = "TEXT")
    private String value;

    @Column(name = "value_type", nullable = false, length = 30)
    @Builder.Default
    private String valueType = "string";

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    protected void onWrite() {
        updatedAt = Instant.now();
    }
}
