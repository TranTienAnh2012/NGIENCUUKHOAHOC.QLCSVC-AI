package com.Tta.QLCSVC.DHNT.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

@Entity
@Table(name = "bao_hong", indexes = {
    @Index(name = "idx_bao_hong_phan_cong", columnList = "nguoi_phu_trach_id, trang_thai_phan_cong")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BaoHong {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "thiet_bi_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "hinhAnhs", "baoHongs", "muonTras", "baoTris", "phongHoc"})
    private ThietBi thietBi;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nguoi_bao_id", nullable = true)  // nullable: cho phep bao hong qua QR
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "matKhau", "thongBaos"})
    private NguoiDung nguoiBao;

    // === PHÂN CÔNG NHÂN VIÊN ===
    /** Nhân viên CSVC được Admin phân công xử lý phiếu này */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nguoi_phu_trach_id", nullable = true)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "matKhau", "thongBaos"})
    private NguoiDung nguoiPhuTrach;

    /** Trạng thái phân công (tách biệt với trạng thái xử lý báo hỏng) */
    @Enumerated(EnumType.STRING)
    @Column(name = "trang_thai_phan_cong")
    private TrangThaiPhanCong trangThaiPhanCong = TrangThaiPhanCong.CHUA_PHAN_CONG;

    /** Lý do nhân viên từ chối (nếu có) */
    @Column(name = "ly_do_tu_choi", columnDefinition = "TEXT")
    private String lyDoTuChoi;
    // =============================

    @Column(name = "ngay_bao")
    private LocalDateTime ngayBao;

    @Column(name = "mo_ta_loi", columnDefinition = "TEXT")
    private String moTaLoi;

    @Enumerated(EnumType.STRING)
    @Column(name = "muc_do_nghiem_trong")
    private MucDoNghiemTrong mucDoNghiemTrong = MucDoNghiemTrong.TRUNG_BINH;

    @Enumerated(EnumType.STRING)
    @Column(name = "trang_thai")
    private TrangThaiBaoHong trangThai = TrangThaiBaoHong.CHO_XU_LY;

    @Column(name = "hinh_anh_url", length = 500)
    private String hinhAnhUrl;

    /**
     * Ghi chu them: luu ten/SDT nguoi bao khi bao hong qua QR khong co tai khoan.
     */
    @Column(name = "ghi_chu", columnDefinition = "TEXT")
    private String ghiChu;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (ngayBao == null) {
            ngayBao = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum MucDoNghiemTrong {
        THAP,
        TRUNG_BINH,
        CAO,
        KHAN_CAP
    }

    public enum TrangThaiBaoHong {
        CHO_XU_LY,
        DANG_XU_LY,
        HOAN_THANH,
        HUY
    }

    /** Tách biệt hoàn toàn với TrangThaiBaoHong để tránh state machine conflict */
    public enum TrangThaiPhanCong {
        CHUA_PHAN_CONG,   // Admin chưa gán ai
        CHO_XAC_NHAN,     // Admin đã gán, nhân viên chưa xác nhận
        DA_NHAN,          // Nhân viên đã chấp nhận
        TU_CHOI           // Nhân viên từ chối, chờ Admin gán lại
    }
}
