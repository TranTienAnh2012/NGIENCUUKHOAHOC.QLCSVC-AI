package com.Tta.QLCSVC.DHNT.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

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
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler", "hinhAnhs", "baoHongs", "muonTras" })
    private ThietBi thietBi;

    @Column(name = "url_hinh_anh", nullable = false, length = 500)
    private String urlHinhAnh;

    @Enumerated(EnumType.STRING)
    @Column(name = "loai_hinh_anh", columnDefinition = "VARCHAR(50)")
    private LoaiHinhAnh loaiHinhAnh;

    @Column(name = "danh_gia_ai", columnDefinition = "JSON")
    private String danhGiaAi;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nguoi_chup_id")
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler", "password" })
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
        QR_CODE, // Mã QR để nhận diện thiết bị
        HINH_ANH_CHINH, // Hình ảnh đại diện chính của thiết bị
        KIEM_TRA_TINH_TRANG, // Hình ảnh kiểm tra tình trạng thiết bị
        BAO_HONG // Hình ảnh đính kèm khi báo hỏng
    }
}
