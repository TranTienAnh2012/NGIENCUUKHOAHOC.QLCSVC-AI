package com.Tta.QLCSVC.DHNT.repository;

import com.Tta.QLCSVC.DHNT.entity.LinhKien;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LinhKienRepository extends JpaRepository<LinhKien, Long> {
    Optional<LinhKien> findByMaLinhKien(String maLinhKien);
    List<LinhKien> findByThietBiId(Long thietBiId);
    List<LinhKien> findByTrangThai(LinhKien.TrangThaiLinhKien trangThai);

    @org.springframework.data.jpa.repository.Query("SELECT l FROM LinhKien l WHERE l.tuoiThoToiDa IS NOT NULL AND l.tuoiThoToiDa > 0 " +
            "AND (CAST(l.thoiGianDaSuDung AS double) / l.tuoiThoToiDa) >= 0.9 " +
            "AND l.trangThai = 'HOAT_DONG'")
    List<LinhKien> findComponentsNeedingReplacement();
}
