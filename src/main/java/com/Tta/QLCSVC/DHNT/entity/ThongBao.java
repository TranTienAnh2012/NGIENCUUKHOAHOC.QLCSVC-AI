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
    @JoinColumn(name = "nguoi_nhan_id")
    private NguoiDung nguoiNhan;

    @Enumerated(EnumType.STRING)
    @Column(name = "role_nhan")
    private NguoiDung.VaiTro roleNhan;

    @Column(name = "tieu_de", nullable = false)
    private String tieuDe;

    @Column(name = "noi_dung", columnDefinition = "TEXT", nullable = false)
    private String noiDung;

    @Enumerated(EnumType.STRING)
    @Column(name = "loai_thong_bao")
    private LoaiThongBao loaiThongBao;

    @Column(name = "url_detail")
    private String urlDetail;

    @Column(name = "da_doc")
    private boolean daDoc = false;

    @Column(name = "ngay_tao", updatable = false)
    private LocalDateTime ngayTao;

    @PrePersist
    protected void onCreate() {
        ngayTao = LocalDateTime.now();
    }

    public enum LoaiThongBao {
        HE_THONG,
        MUON_TRA,
        BAO_HONG,
        BAO_TRI
    }
}
