package com.Tta.QLCSVC.DHNT.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "iot_du_lieu_cam_bien")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IotDuLieuCamBien {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "phong_id", nullable = false)
    private PhongHoc phong;

    @Column(name = "nhiet_do", precision = 5, scale = 2)
    private BigDecimal nhietDo;

    @Column(name = "do_am", precision = 5, scale = 2)
    private BigDecimal doAm;

    @Column(name = "timestamp")
    private LocalDateTime timestamp;

    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }
}
