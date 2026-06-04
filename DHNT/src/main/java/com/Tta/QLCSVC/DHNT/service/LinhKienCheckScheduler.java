package com.Tta.QLCSVC.DHNT.service;

import com.Tta.QLCSVC.DHNT.entity.LinhKien;
import com.Tta.QLCSVC.DHNT.entity.NguoiDung;
import com.Tta.QLCSVC.DHNT.repository.LinhKienRepository;
import com.Tta.QLCSVC.DHNT.repository.NguoiDungRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class LinhKienCheckScheduler {

    private final LinhKienRepository linhKienRepository;
    private final NguoiDungRepository nguoiDungRepository;
    private final ThongBaoService thongBaoService;
    private final RestTemplate restTemplate = new RestTemplate();

    private static final String FLASK_AI_URL = "http://localhost:5000/api/ai/chatbot";

    // Chạy ngầm định kỳ 1 lần mỗi ngày vào lúc 7h00 sáng
    @Scheduled(cron = "0 0 7 * * ?")
    @Transactional
    public void scanAndNotifyComponentReplacement() {
        System.out.println("[Scheduler] Bắt đầu quét các linh kiện cần thay thế...");

        // Lấy danh sách linh kiện đã dùng >= 90% tuổi thọ và đang ở trạng thái HOAT_DONG
        List<LinhKien> needsReplacement = linhKienRepository.findComponentsNeedingReplacement();

        if (needsReplacement.isEmpty()) {
            System.out.println("[Scheduler] Không có linh kiện nào cần thay thế ngay.");
            return;
        }

        System.out.println("[Scheduler] Phát hiện " + needsReplacement.size() + " linh kiện cần thay thế.");

        // Lấy danh sách những người cần thông báo (Admin và Nhân viên CSVC)
        List<NguoiDung> targetUsers = nguoiDungRepository.findByVaiTroIn(
                Arrays.asList(NguoiDung.VaiTro.ADMIN, NguoiDung.VaiTro.NHAN_VIEN_CSVC)
        );

        for (LinhKien lk : needsReplacement) {
            // Đánh dấu cần thay thế để lần sau không bị quét lại
            lk.setTrangThai(LinhKien.TrangThaiLinhKien.CAN_THAY_THE);
            linhKienRepository.save(lk);

            String thietBiName = lk.getThietBi() != null ? lk.getThietBi().getTenThietBi() : "Thiết bị không xác định";
            String tieuDe = "⚠️ AI Gợi Ý: Cần thay thế linh kiện " + lk.getTenLinhKien();
            String noiDung = generateSuggestionWithAI(lk, thietBiName);

            // Gửi thông báo cho từng người
            for (NguoiDung user : targetUsers) {
                thongBaoService.createNotification(user, tieuDe, noiDung, "MAINTENANCE");
            }
            System.out.println("[Scheduler] Đã phát cảnh báo cho linh kiện: " + lk.getTenLinhKien());
        }
    }

    private String generateSuggestionWithAI(LinhKien lk, String thietBiName) {
        String defaultMsg = String.format("Linh kiện '%s' của '%s' đã đạt %d/%d %s. Vui lòng kiểm tra và lên kế hoạch thay thế.",
                lk.getTenLinhKien(),
                thietBiName,
                lk.getThoiGianDaSuDung(),
                lk.getTuoiThoToiDa(),
                lk.getDonViTinh() != null ? lk.getDonViTinh() : "");

        try {
            Map<String, Object> request = new HashMap<>();
            String prompt = String.format("Đóng vai một trợ lý AI thông minh chuyên về quản lý cơ sở vật chất. Linh kiện '%s' nằm trong '%s' đã sử dụng được %d/%d (%s). Hãy viết một đoạn thông báo cảnh báo ngắn gọn (khoảng 30-40 từ) khuyên nhân viên bảo trì nên lập kế hoạch thay thế để tránh gián đoạn. Trả về nội dung, không giải thích.",
                    lk.getTenLinhKien(),
                    thietBiName,
                    lk.getThoiGianDaSuDung(),
                    lk.getTuoiThoToiDa(),
                    lk.getDonViTinh() != null ? lk.getDonViTinh() : "đơn vị");

            request.put("message", prompt);

            Map<String, Object> context = new HashMap<>();
            context.put("user_role", "SYSTEM_AUTO");
            request.put("context", context);

            ResponseEntity<Map> response = restTemplate.postForEntity(FLASK_AI_URL, request, Map.class);

            if (response.getBody() != null && response.getBody().get("response") != null) {
                return (String) response.getBody().get("response");
            }
        } catch (Exception e) {
            System.err.println("[Scheduler] Lỗi khi gọi Flask AI API: " + e.getMessage());
        }

        return defaultMsg;
    }
}
