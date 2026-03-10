package com.Tta.QLCSVC.DHNT.repository;

import com.Tta.QLCSVC.DHNT.entity.AiDuDoanBaoTri;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AiDuDoanBaoTriRepository extends JpaRepository<AiDuDoanBaoTri, Long> {

    List<AiDuDoanBaoTri> findByThietBiId(Long thietBiId);

    List<AiDuDoanBaoTri> findByMucDoRuiRo(AiDuDoanBaoTri.MucDoRuiRo mucDoRuiRo);

    @Query("SELECT a FROM AiDuDoanBaoTri a WHERE a.thietBi.id = :thietBiId ORDER BY a.ngayDuDoan DESC LIMIT 1")
    Optional<AiDuDoanBaoTri> findLatestPredictionByThietBi(Long thietBiId);

    @Query("SELECT a FROM AiDuDoanBaoTri a WHERE a.mucDoRuiRo IN ('CAO', 'NGUY_HIEM') ORDER BY a.xacSuatHong DESC")
    List<AiDuDoanBaoTri> findHighRiskPredictions();
}
