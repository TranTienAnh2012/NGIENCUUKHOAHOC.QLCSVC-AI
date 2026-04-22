package com.Tta.QLCSVC.DHNT.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.Hibernate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "loai_thiet_bi")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoaiThietBi {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ten_loai", nullable = false, length = 100)
    private String tenLoai;

    @Column(name = "mo_ta", columnDefinition = "TEXT")
    private String moTa;

    @Column(name = "thoi_gian_bao_hanh_mac_dinh")
    private Integer thoiGianBaoHanhMacDinh;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "loaiThietBi")
    @JsonIgnore
    private List<ThietBi> thietBis = new ArrayList<>();

    @JsonProperty("soThietBi")
    public int getSoThietBi() {
        try {
            if (thietBis != null && Hibernate.isInitialized(thietBis)) {
                return thietBis.size();
            }
        } catch (Exception e) {
            // Lazy collection not available outside transaction
        }
        return 0;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
