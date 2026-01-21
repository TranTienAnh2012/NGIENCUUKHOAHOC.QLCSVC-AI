package com.Tta.QLCSVC.DHNT.service;

import com.Tta.QLCSVC.DHNT.entity.AiDuDoanBaoTri;
import com.Tta.QLCSVC.DHNT.entity.ThietBi;
import com.Tta.QLCSVC.DHNT.repository.AiDuDoanBaoTriRepository;
import com.Tta.QLCSVC.DHNT.repository.ThietBiRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class AiPredictiveService {

    private final AiDuDoanBaoTriRepository predictionRepository;
    private final ThietBiRepository thietBiRepository;
    private final Random random = new Random();

    public List<AiDuDoanBaoTri> getHighRiskEquipments() {
        return predictionRepository.findHighRiskPredictions();
    }

    public List<AiDuDoanBaoTri> getPredictionsByThietBi(Long thietBiId) {
        return predictionRepository.findByThietBiId(thietBiId);
    }

    @Transactional
    public AiDuDoanBaoTri runPrediction(Long thietBiId) {
        ThietBi thietBi = thietBiRepository.findById(thietBiId)
                .orElseThrow(() -> new RuntimeException("ThietBi not found"));

        // Giả lập logic AI
        double probValue = 0.1 + (random.nextDouble() * 0.8);
        BigDecimal probability = BigDecimal.valueOf(probValue * 100).setScale(2, java.math.RoundingMode.HALF_UP);
        AiDuDoanBaoTri.MucDoRuiRo risk = calculateRisk(probValue);

        AiDuDoanBaoTri prediction = new AiDuDoanBaoTri();
        prediction.setThietBi(thietBi);
        prediction.setNgayDuDoan(LocalDate.now());
        prediction.setXacSuatHong(probability);
        prediction.setMucDoRuiRo(risk);
        prediction.setNgayDuKienHong(LocalDate.now().plusDays(30 + random.nextInt(180)));
        prediction.setChiPhiUocTinh(BigDecimal.valueOf(500000 + random.nextInt(5000000)));
        prediction.setDoTinCay(
                BigDecimal.valueOf(70 + random.nextDouble() * 25).setScale(2, java.math.RoundingMode.HALF_UP));
        prediction.setPhienBanModel("qlcsvc-ai-v1.0");

        String suggestion = risk == AiDuDoanBaoTri.MucDoRuiRo.CAO || risk == AiDuDoanBaoTri.MucDoRuiRo.NGUY_HIEM
                ? "Cảnh báo: Rủi ro hỏng hóc cao. Đề xuất bảo trì bảo dưỡng toàn diện trong vòng 7 ngày tới."
                : "Tình trạng ổn định. Đề xuất kiểm tra định kỳ theo kế hoạch tiêu chuẩn.";
        prediction.setHanhDongDeXuat(suggestion);

        return predictionRepository.save(prediction);
    }

    private AiDuDoanBaoTri.MucDoRuiRo calculateRisk(double probability) {
        if (probability < 0.3)
            return AiDuDoanBaoTri.MucDoRuiRo.THAP;
        if (probability < 0.6)
            return AiDuDoanBaoTri.MucDoRuiRo.TRUNG_BINH;
        if (probability < 0.85)
            return AiDuDoanBaoTri.MucDoRuiRo.CAO;
        return AiDuDoanBaoTri.MucDoRuiRo.NGUY_HIEM;
    }
}
