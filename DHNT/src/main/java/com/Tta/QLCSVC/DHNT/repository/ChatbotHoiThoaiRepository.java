package com.Tta.QLCSVC.DHNT.repository;

import com.Tta.QLCSVC.DHNT.entity.ChatbotHoiThoai;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatbotHoiThoaiRepository extends JpaRepository<ChatbotHoiThoai, Long> {

    List<ChatbotHoiThoai> findBySessionId(String sessionId);

    List<ChatbotHoiThoai> findByNguoiDungId(Long nguoiDungId);

    List<ChatbotHoiThoai> findByIntent(String intent);
}
