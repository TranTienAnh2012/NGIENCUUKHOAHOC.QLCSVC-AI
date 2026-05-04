package com.Tta.QLCSVC.DHNT.repository;

import com.Tta.QLCSVC.DHNT.entity.ThongBao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ThongBaoRepository extends JpaRepository<ThongBao, Long> {
    List<ThongBao> findByNguoiDungIdOrderByCreatedAtDesc(Long nguoiDungId);
    List<ThongBao> findByNguoiDungIdAndDaDocFalse(Long nguoiDungId);
    long countByNguoiDungIdAndDaDocFalse(Long nguoiDungId);
}
