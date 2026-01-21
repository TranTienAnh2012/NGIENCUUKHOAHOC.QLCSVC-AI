package com.Tta.QLCSVC.DHNT.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "loai_phong")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoaiPhong {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ten_loai", nullable = false, unique = true, length = 100)
    private String tenLoai;

    @Column(name = "mo_ta", columnDefinition = "TEXT")
    private String moTa;

    @OneToMany(mappedBy = "loaiPhong")
    @JsonIgnore
    private List<PhongHoc> phongHocs = new ArrayList<>();

    @Column(name = "so_phong")
    private Integer soPhong;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
