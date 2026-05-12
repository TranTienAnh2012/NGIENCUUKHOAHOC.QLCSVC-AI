package com.Tta.QLCSVC.DHNT.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "thong_bao")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ThongBao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nguoi_dung_id", nullable = false)
    private NguoiDung nguoiDung;

    @Column(name = "tieu_de", nullable = false)
    private String tieuDe;

    @Column(name = "noi_dung", columnDefinition = "TEXT", nullable = false)
    private String noiDung;

    @Column(name = "da_doc")
    private boolean daDoc = false;

    @Column(name = "loai_thong_bao")
    private String loaiThongBao; // e.g., "OVERDUE", "SYSTEM"

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
