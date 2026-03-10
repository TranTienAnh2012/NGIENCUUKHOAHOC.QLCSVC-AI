package com.Tta.QLCSVC.DHNT.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "ai_du_lieu_huan_luyen")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiDuLieuHuanLuyen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "thiet_bi_id", nullable = false)
    private ThietBi thietBi;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "features", nullable = false, columnDefinition = "JSON")
    private Map<String, Object> features;

    @Column(name = "label", length = 50)
    private String label;

    @Enumerated(EnumType.STRING)
    @Column(name = "loai_du_lieu")
    private LoaiDuLieu loaiDuLieu = LoaiDuLieu.TRAIN;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum LoaiDuLieu {
        TRAIN,
        TEST,
        VALIDATION
    }
}
