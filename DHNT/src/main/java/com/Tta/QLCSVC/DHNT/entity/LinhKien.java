package com.Tta.QLCSVC.DHNT.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "linh_kien")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LinhKien {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ma_linh_kien", nullable = false, unique = true, length = 50)
    private String maLinhKien;

    @Column(name = "ten_linh_kien", nullable = false, length = 200)
    private String tenLinhKien;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "thiet_bi_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "linhKiens"})
    private ThietBi thietBi;

    @Column(name = "thong_so_ky_thuat", columnDefinition = "TEXT")
    private String thongSoKyThuat;

    @Column(name = "ngay_mua")
    private LocalDate ngayMua;

    @Column(name = "han_bao_hanh")
    private LocalDate hanBaoHanh;

    // Ví dụ: 3000 (giờ), 10000 (trang)
    @Column(name = "tuoi_tho_toi_da")
    private Integer tuoiThoToiDa;

    // Ví dụ: 2500 (giờ đã chạy)
    @Column(name = "thoi_gian_da_su_dung")
    private Integer thoiGianDaSuDung = 0;

    // Đơn vị đo lường: "Giờ", "Trang", "Tháng", "Lần"
    @Column(name = "don_vi_tinh", length = 20)
    private String donViTinh;

    @Enumerated(EnumType.STRING)
    @Column(name = "trang_thai", nullable = false)
    private TrangThaiLinhKien trangThai = TrangThaiLinhKien.HOAT_DONG;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum TrangThaiLinhKien {
        HOAT_DONG,       // Đang chạy tốt
        CAN_THAY_THE,    // Gần đến hoặc đã vượt quá tuổi thọ tối đa
        DANG_BAO_HANH,   // Đang được tháo ra đi bảo hành
        DA_HU_HONG       // Đã hỏng hoàn toàn
    }
}
