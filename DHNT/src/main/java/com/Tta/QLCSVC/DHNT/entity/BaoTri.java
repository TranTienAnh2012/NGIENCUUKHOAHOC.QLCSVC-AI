package com.Tta.QLCSVC.DHNT.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "bao_tri")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BaoTri {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "thiet_bi_id", nullable = false)
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler", "hinhAnhs", "baoHongs", "muonTras", "baoTris" })
    private ThietBi thietBi;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bao_hong_id")
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler", "thietBi" })
    private BaoHong baoHong;

    // Track component being maintained/replaced (optional)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "linh_kien_id")
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler", "thietBi" })
    private LinhKien linhKien;

    @Enumerated(EnumType.STRING)
    @Column(name = "loai_bao_tri", nullable = false)
    private LoaiBaoTri loaiBaoTri;

    @Column(name = "ngay_bao_tri", nullable = false)
    private LocalDate ngayBaoTri;

    @Column(name = "noi_dung", columnDefinition = "TEXT")
    private String noiDung;

    @Column(name = "chi_phi", precision = 15, scale = 2)
    private BigDecimal chiPhi;

    /** Nhân viên CSVC thực hiện bảo trì (FK, không còn là plain String) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nguoi_thuc_hien_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "matKhau", "thongBaos"})
    private NguoiDung nguoiThucHien;

    @Enumerated(EnumType.STRING)
    @Column(name = "ket_qua")
    private KetQuaBaoTri ketQua;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum LoaiBaoTri {
        DINH_KY,
        SUA_CHUA,
        PHONG_NGUA
    }

    public enum KetQuaBaoTri {
        THANH_CONG,
        THAT_BAI,
        CAN_THAY_THE
    }
}
