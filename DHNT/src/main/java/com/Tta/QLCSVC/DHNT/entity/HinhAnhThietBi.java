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
@Table(name = "hinh_anh_thiet_bi")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HinhAnhThietBi {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "thiet_bi_id", nullable = false)
    private ThietBi thietBi;

    @Column(name = "url_hinh_anh", nullable = false, length = 500)
    private String urlHinhAnh;

    @Enumerated(EnumType.STRING)
    @Column(name = "loai_hinh_anh")
    private LoaiHinhAnh loaiHinhAnh;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "danh_gia_ai", columnDefinition = "JSON")
    private Map<String, Object> danhGiaAi;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nguoi_chup_id")
    private NguoiDung nguoiChup;

    @Column(name = "ngay_chup")
    private LocalDateTime ngayChup;

    @PrePersist
    protected void onCreate() {
        if (ngayChup == null) {
            ngayChup = LocalDateTime.now();
        }
    }

    public enum LoaiHinhAnh {
        QR_CODE,
        KIEM_TRA_TINH_TRANG,
        BAO_HONG,
        MUON_TRA
    }
}
