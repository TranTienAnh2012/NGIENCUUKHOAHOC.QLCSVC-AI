package com.Tta.QLCSVC.DHNT.areas.admin.service;

import com.Tta.QLCSVC.DHNT.entity.AiDuLieuHuanLuyen;
import com.Tta.QLCSVC.DHNT.entity.AiModelMetrics;
import com.Tta.QLCSVC.DHNT.repository.AiDuLieuHuanLuyenRepository;
import com.Tta.QLCSVC.DHNT.repository.AiModelMetricsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminAiService {

    private final AiDuLieuHuanLuyenRepository trainingRepository;
    private final AiModelMetricsRepository metricsRepository;

    public Page<AiDuLieuHuanLuyen> getAllTrainingData(Pageable pageable) {
        return trainingRepository.findAll(pageable);
    }

    public Page<AiModelMetrics> getAllMetrics(Pageable pageable) {
        return metricsRepository.findAll(pageable);
    }

    public void deleteTrainingData(Long id) {
        trainingRepository.deleteById(id);
    }

    public void deleteMetric(Long id) {
        metricsRepository.deleteById(id);
    }
}
