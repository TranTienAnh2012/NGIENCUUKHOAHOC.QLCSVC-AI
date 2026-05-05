package com.Tta.QLCSVC.DHNT.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_model_metrics")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiModelMetrics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ten_model", nullable = false, length = 100)
    private String tenModel;

    @Column(name = "phien_ban", nullable = false, length = 50)
    private String phienBan;

    @Column(name = "loai_metric", length = 50)
    private String loaiMetric;

    @Column(name = "gia_tri", precision = 10, scale = 4)
    private BigDecimal giaTri;

    @Column(name = "ngay_danh_gia")
    private LocalDate ngayDanhGia;

    @Column(name = "ghi_chu", columnDefinition = "TEXT")
    private String ghiChu;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
