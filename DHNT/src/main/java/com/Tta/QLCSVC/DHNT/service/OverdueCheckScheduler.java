package com.Tta.QLCSVC.DHNT.service;

import com.Tta.QLCSVC.DHNT.entity.MuonTraThietBi;
import com.Tta.QLCSVC.DHNT.repository.MuonTraThietBiRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OverdueCheckScheduler {

    private final MuonTraThietBiRepository muonTraThietBiRepository;
    private final ThongBaoService thongBaoService;
    private final RestTemplate restTemplate = new RestTemplate();

    private static final String FLASK_AI_URL = "http://localhost:5000/api/ai/chatbot";

    // Chạy ngầm mỗi 1 phút để test (có thể đổi thành 0 0 0 * * * để chạy lúc nửa đêm)
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void scanAndNotifyOverdue() {
        System.out.println("[Scheduler] Bắt đầu quét các thiết bị mượn quá hạn...");
        
        // Lấy danh sách các phiếu mượn đang mượn nhưng đã quá ngày trả dự kiến
        List<MuonTraThietBi> overdueRecords = muonTraThietBiRepository.findOverdueRecords(LocalDateTime.now());
        
        if (overdueRecords.isEmpty()) {
            System.out.println("[Scheduler] Không có thiết bị nào quá hạn.");
            return;
        }

        System.out.println("[Scheduler] Phát hiện " + overdueRecords.size() + " phiếu mượn quá hạn.");

        for (MuonTraThietBi record : overdueRecords) {
            // Cập nhật trạng thái thành QUA_HAN
            record.setTrangThai(MuonTraThietBi.TrangThaiMuonTra.QUA_HAN);
            muonTraThietBiRepository.save(record);

            String tieuDe = "Thông báo: Thiết bị quá hạn trả";
            String noiDung = generateMessageWithAI(record);

            // Tạo thông báo cho người mượn
            thongBaoService.createNotification(record.getNguoiMuon(), tieuDe, noiDung, "OVERDUE");
            System.out.println("[Scheduler] Đã tạo thông báo cho: " + record.getNguoiMuon().getHoTen());
        }
    }

    private String generateMessageWithAI(MuonTraThietBi record) {
        String defaultMsg = String.format("Chào %s, thiết bị '%s' bạn mượn vào ngày %s đã quá thời hạn trả dự kiến (%s). Vui lòng hoàn trả sớm nhất có thể. Cảm ơn!",
                record.getNguoiMuon().getHoTen(),
                record.getThietBi().getTenThietBi(),
                record.getNgayMuon().toLocalDate().toString(),
                record.getNgayTraDuKien().toLocalDate().toString());

        try {
            // Gọi Flask AI API để nhờ sinh ra một câu nhắc nhở mượt mà
            Map<String, Object> request = new HashMap<>();
            String prompt = String.format("Hãy viết một tin nhắn ngắn gọn (dưới 50 từ), lịch sự nhắc nhở giáo viên '%s' rằng họ đã mượn thiết bị '%s' quá hạn (Hạn trả: %s). Chỉ trả về đúng nội dung thông báo, không kèm theo giải thích gì thêm.", 
                record.getNguoiMuon().getHoTen(), 
                record.getThietBi().getTenThietBi(), 
                record.getNgayTraDuKien().toLocalDate().toString());
                
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
