package com.Tta.QLCSVC.DHNT.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "chatbot_hoi_thoai")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatbotHoiThoai {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nguoi_dung_id")
    private NguoiDung nguoiDung;

    @Column(name = "session_id", length = 100)
    private String sessionId;

    @Column(name = "tin_nhan", nullable = false, columnDefinition = "TEXT")
    private String tinNhan;

    @Column(name = "intent", length = 100)
    private String intent;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "entities", columnDefinition = "JSON")
    private Map<String, Object> entities;

    @Column(name = "phan_hoi", columnDefinition = "TEXT")
    private String phanHoi;

    @Column(name = "do_tin_cay", precision = 5, scale = 2)
    private BigDecimal doTinCay;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
