package com.Tta.QLCSVC.DHNT.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "muon_tra_thiet_bi")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class MuonTraThietBi {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "thiet_bi_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private ThietBi thietBi;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nguoi_muon_id", nullable = false)
    private NguoiDung nguoiMuon;

    @Column(name = "ngay_muon", nullable = false)
    private LocalDateTime ngayMuon;

    @Column(name = "ngay_tra_du_kien")
    private LocalDateTime ngayTraDuKien;

    @Column(name = "ngay_tra_thuc_te")
    private LocalDateTime ngayTraThucTe;

    @Enumerated(EnumType.STRING)
    @Column(name = "trang_thai")
    private TrangThaiMuonTra trangThai = TrangThaiMuonTra.DANG_MUON;

    @Column(name = "ghi_chu", columnDefinition = "TEXT")
    private String ghiChu;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum TrangThaiMuonTra {
        DANG_MUON,
        DA_TRA,
        QUA_HAN
    }
}
