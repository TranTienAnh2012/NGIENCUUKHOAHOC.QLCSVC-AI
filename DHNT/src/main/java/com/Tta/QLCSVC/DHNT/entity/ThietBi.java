package com.Tta.QLCSVC.DHNT.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "thiet_bi")
public class ThietBi {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ma_thiet_bi", nullable = false, unique = true, length = 50)
    private String maThietBi;

    @Column(name = "ten_thiet_bi", nullable = false, length = 200)
    private String tenThietBi;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loai_thiet_bi_id", nullable = false)
    private LoaiThietBi loaiThietBi;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "phong_id")
    private PhongHoc phong;

    @Column(name = "hang_san_xuat", length = 100)
    private String hangSanXuat;

    @Column(name = "model", length = 100)
    private String model;

    @Column(name = "nam_san_xuat")
    private Integer namSanXuat;

    @Column(name = "ngay_mua")
    private LocalDate ngayMua;

    @Column(name = "gia_mua", precision = 15, scale = 2)
    private BigDecimal giaMua;

    @Enumerated(EnumType.STRING)
    @Column(name = "trang_thai")
    private TrangThaiThietBi trangThai = TrangThaiThietBi.TOT;

    @Column(name = "ghi_chu", columnDefinition = "TEXT")
    private String ghiChu;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public ThietBi() {
    }

    public ThietBi(Long id, String maThietBi, String tenThietBi, LoaiThietBi loaiThietBi, PhongHoc phong,
            String hangSanXuat, String model, Integer namSanXuat, LocalDate ngayMua, BigDecimal giaMua,
            TrangThaiThietBi trangThai, String ghiChu, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.maThietBi = maThietBi;
        this.tenThietBi = tenThietBi;
        this.loaiThietBi = loaiThietBi;
        this.phong = phong;
        this.hangSanXuat = hangSanXuat;
        this.model = model;
        this.namSanXuat = namSanXuat;
        this.ngayMua = ngayMua;
        this.giaMua = giaMua;
        this.trangThai = trangThai;
        this.ghiChu = ghiChu;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMaThietBi() {
        return maThietBi;
    }

    public void setMaThietBi(String maThietBi) {
        this.maThietBi = maThietBi;
    }

    public String getTenThietBi() {
        return tenThietBi;
    }

    public void setTenThietBi(String tenThietBi) {
        this.tenThietBi = tenThietBi;
    }

    public LoaiThietBi getLoaiThietBi() {
        return loaiThietBi;
    }

    public void setLoaiThietBi(LoaiThietBi loaiThietBi) {
        this.loaiThietBi = loaiThietBi;
    }

    public PhongHoc getPhong() {
        return phong;
    }

    public void setPhong(PhongHoc phong) {
        this.phong = phong;
    }

    public String getHangSanXuat() {
        return hangSanXuat;
    }

    public void setHangSanXuat(String hangSanXuat) {
        this.hangSanXuat = hangSanXuat;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Integer getNamSanXuat() {
        return namSanXuat;
    }

    public void setNamSanXuat(Integer namSanXuat) {
        this.namSanXuat = namSanXuat;
    }

    public LocalDate getNgayMua() {
        return ngayMua;
    }

    public void setNgayMua(LocalDate ngayMua) {
        this.ngayMua = ngayMua;
    }

    public BigDecimal getGiaMua() {
        return giaMua;
    }

    public void setGiaMua(BigDecimal giaMua) {
        this.giaMua = giaMua;
    }

    public TrangThaiThietBi getTrangThai() {
        return trangThai;
    }

    public void setTrangThai(TrangThaiThietBi trangThai) {
        this.trangThai = trangThai;
    }

    public String getGhiChu() {
        return ghiChu;
    }

    public void setGhiChu(String ghiChu) {
        this.ghiChu = ghiChu;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public enum TrangThaiThietBi {
        TOT,
        BAO_TRI,
        HONG,
        THANH_LY
    }
}
