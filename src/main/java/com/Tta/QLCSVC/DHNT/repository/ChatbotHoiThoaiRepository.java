package com.Tta.QLCSVC.DHNT.repository;

import com.Tta.QLCSVC.DHNT.entity.ChatbotHoiThoai;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ChatbotHoiThoaiRepository extends JpaRepository<ChatbotHoiThoai, Long> {

    List<ChatbotHoiThoai> findBySessionId(String sessionId);

    List<ChatbotHoiThoai> findByNguoiDungId(Long nguoiDungId);

    List<ChatbotHoiThoai> findByIntent(String intent);

    // Lấy lịch sử chat theo session, sắp xếp theo thời gian
    List<ChatbotHoiThoai> findBySessionIdOrderByCreatedAtAsc(String sessionId);

    // Lấy chat gần đây của user (trong khoảng thời gian)
    List<ChatbotHoiThoai> findByNguoiDungIdAndCreatedAtAfterOrderByCreatedAtDesc(
            Long nguoiDungId,
            LocalDateTime after);

    // Kiểm tra user có sở hữu session không (để bảo mật)
    @Query("SELECT COUNT(c) > 0 FROM ChatbotHoiThoai c WHERE c.sessionId = :sessionId AND c.nguoiDung.id = :userId")
    boolean isSessionOwnedByUser(@Param("sessionId") String sessionId, @Param("userId") Long userId);
}
