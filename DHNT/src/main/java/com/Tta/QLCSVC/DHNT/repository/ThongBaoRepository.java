package com.Tta.QLCSVC.DHNT.repository;

import com.Tta.QLCSVC.DHNT.entity.NguoiDung;
import com.Tta.QLCSVC.DHNT.entity.ThongBao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ThongBaoRepository extends JpaRepository<ThongBao, Long> {
    // Queries cũ (per-user, dùng bởi ThongBaoService)
    List<ThongBao> findByNguoiDungIdOrderByCreatedAtDesc(Long nguoiDungId);
    List<ThongBao> findByNguoiDungIdAndDaDocFalse(Long nguoiDungId);
    long countByNguoiDungIdAndDaDocFalse(Long nguoiDungId);

    // Queries mới (kết hợp per-user + role-broadcast, dùng bởi NotificationService)
    @Query("SELECT t FROM ThongBao t LEFT JOIN t.nguoiDung u WHERE (u.id = :userId OR t.roleNhan = :role) ORDER BY t.createdAt DESC")
    List<ThongBao> findNotificationsForUser(@Param("userId") Long userId, @Param("role") NguoiDung.VaiTro role);

    @Query("SELECT COUNT(t) FROM ThongBao t LEFT JOIN t.nguoiDung u WHERE (u.id = :userId OR t.roleNhan = :role) AND t.daDoc = false")
    long countUnreadNotifications(@Param("userId") Long userId, @Param("role") NguoiDung.VaiTro role);

    // Deduplication: kiểm tra đã gửi thông báo tương tự chưa (trong N giờ gần đây)
    @Query("SELECT COUNT(t) > 0 FROM ThongBao t WHERE t.nguoiDung.id = :userId AND t.tieuDe = :tieuDe AND t.createdAt >= :since")
    boolean existsByUserAndTitleSince(@Param("userId") Long userId, @Param("tieuDe") String tieuDe, @Param("since") java.time.LocalDateTime since);

    @Query("SELECT COUNT(t) > 0 FROM ThongBao t WHERE t.roleNhan = :role AND t.tieuDe = :tieuDe AND t.createdAt >= :since")
    boolean existsByRoleAndTitleSince(@Param("role") NguoiDung.VaiTro role, @Param("tieuDe") String tieuDe, @Param("since") java.time.LocalDateTime since);
}
