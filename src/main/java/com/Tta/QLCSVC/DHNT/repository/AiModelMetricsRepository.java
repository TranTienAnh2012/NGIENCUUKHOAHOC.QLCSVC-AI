package com.Tta.QLCSVC.DHNT.repository;

import com.Tta.QLCSVC.DHNT.entity.AiModelMetrics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AiModelMetricsRepository extends JpaRepository<AiModelMetrics, Long> {

    List<AiModelMetrics> findByTenModel(String tenModel);

    List<AiModelMetrics> findByTenModelAndPhienBan(String tenModel, String phienBan);

    @Query("SELECT a FROM AiModelMetrics a WHERE a.tenModel = :tenModel ORDER BY a.ngayDanhGia DESC")
    List<AiModelMetrics> findLatestMetricsByModel(String tenModel);
}
