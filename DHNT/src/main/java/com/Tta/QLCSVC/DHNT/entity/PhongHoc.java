package com.Tta.QLCSVC.DHNT.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "phong_hoc")
public class PhongHoc {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ma_phong", nullable = false, unique = true, length = 20)
    private String maPhong;

    @Column(name = "ten_phong", nullable = false, length = 100)
    private String tenPhong;

    @Column(name = "toa_nha", length = 50)
    private String toaNha;

    @Column(name = "tang")
    private Integer tang;

    @Column(name = "suc_chua")
    private Integer sucChua;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "loai_phong_id", nullable = true)
    private LoaiPhong loaiPhong;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public PhongHoc() {
    }

    public PhongHoc(Long id, String maPhong, String tenPhong, String toaNha, Integer tang, Integer sucChua,
            LoaiPhong loaiPhong, LocalDateTime createdAt) {
        this.id = id;
        this.maPhong = maPhong;
        this.tenPhong = tenPhong;
        this.toaNha = toaNha;
        this.tang = tang;
        this.sucChua = sucChua;
        this.loaiPhong = loaiPhong;
        this.createdAt = createdAt;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMaPhong() {
        return maPhong;
    }

    public void setMaPhong(String maPhong) {
        this.maPhong = maPhong;
    }

    public String getTenPhong() {
        return tenPhong;
    }

    public void setTenPhong(String tenPhong) {
        this.tenPhong = tenPhong;
    }

    public String getToaNha() {
        return toaNha;
    }

    public void setToaNha(String toaNha) {
        this.toaNha = toaNha;
    }

    public Integer getTang() {
        return tang;
    }

    public void setTang(Integer tang) {
        this.tang = tang;
    }

    public Integer getSucChua() {
        return sucChua;
    }

    public void setSucChua(Integer sucChua) {
        this.sucChua = sucChua;
    }

    public LoaiPhong getLoaiPhong() {
        return loaiPhong;
    }

    public void setLoaiPhong(LoaiPhong loaiPhong) {
        this.loaiPhong = loaiPhong;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
