package com.Tta.QLCSVC.DHNT.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_du_doan_bao_tri")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiDuDoanBaoTri {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "thiet_bi_id", nullable = false)
    private ThietBi thietBi;

    @Column(name = "ngay_du_doan", nullable = false)
    private LocalDate ngayDuDoan;

    @Column(name = "xac_suat_hong", precision = 5, scale = 2)
    private BigDecimal xacSuatHong;

    @Enumerated(EnumType.STRING)
    @Column(name = "muc_do_rui_ro")
    private MucDoRuiRo mucDoRuiRo;

    @Column(name = "ngay_du_kien_hong")
    private LocalDate ngayDuKienHong;

    @Column(name = "chi_phi_uoc_tinh", precision = 15, scale = 2)
    private BigDecimal chiPhiUocTinh;

    @Column(name = "hanh_dong_de_xuat", columnDefinition = "TEXT")
    private String hanhDongDeXuat;

    @Column(name = "do_tin_cay", precision = 5, scale = 2)
    private BigDecimal doTinCay;

    @Column(name = "phien_ban_model", length = 50)
    private String phienBanModel;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum MucDoRuiRo {
        THAP,
        TRUNG_BINH,
        CAO,
        NGUY_HIEM
    }
}
