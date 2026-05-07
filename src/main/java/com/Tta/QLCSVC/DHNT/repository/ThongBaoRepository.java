package com.Tta.QLCSVC.DHNT.repository;

import com.Tta.QLCSVC.DHNT.entity.NguoiDung;
import com.Tta.QLCSVC.DHNT.entity.ThongBao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ThongBaoRepository extends JpaRepository<ThongBao, Long> {

    @Query("SELECT t FROM ThongBao t LEFT JOIN t.nguoiNhan n WHERE n.id = :userId OR t.roleNhan = :role ORDER BY t.ngayTao DESC")
    List<ThongBao> findNotificationsForUser(@Param("userId") Long userId, @Param("role") NguoiDung.VaiTro role);

    @Query("SELECT COUNT(t) FROM ThongBao t LEFT JOIN t.nguoiNhan n WHERE (n.id = :userId OR t.roleNhan = :role) AND t.daDoc = false")
    long countUnreadNotifications(@Param("userId") Long userId, @Param("role") NguoiDung.VaiTro role);
}
