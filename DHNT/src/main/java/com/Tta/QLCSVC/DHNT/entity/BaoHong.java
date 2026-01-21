package com.Tta.QLCSVC.DHNT.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

@Entity
@Table(name = "bao_hong")
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
    private ThietBi thietBi;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nguoi_bao_id", nullable = false)
    private NguoiDung nguoiBao;

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
}
