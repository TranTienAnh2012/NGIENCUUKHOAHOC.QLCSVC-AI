package com.Tta.QLCSVC.DHNT.repository;

import com.Tta.QLCSVC.DHNT.entity.AiDuLieuHuanLuyen;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AiDuLieuHuanLuyenRepository extends JpaRepository<AiDuLieuHuanLuyen, Long> {

    List<AiDuLieuHuanLuyen> findByLoaiDuLieu(AiDuLieuHuanLuyen.LoaiDuLieu loaiDuLieu);

    List<AiDuLieuHuanLuyen> findByThietBiId(Long thietBiId);

    List<AiDuLieuHuanLuyen> findByLabel(String label);
}
