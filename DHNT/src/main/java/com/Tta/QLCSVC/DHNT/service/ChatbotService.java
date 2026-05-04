package com.Tta.QLCSVC.DHNT.service;

import com.Tta.QLCSVC.DHNT.entity.ChatbotHoiThoai;
import com.Tta.QLCSVC.DHNT.entity.NguoiDung;
import com.Tta.QLCSVC.DHNT.exception.ResourceNotFoundException;
import com.Tta.QLCSVC.DHNT.repository.ChatbotHoiThoaiRepository;
import com.Tta.QLCSVC.DHNT.repository.NguoiDungRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatbotService {

    private final ChatbotHoiThoaiRepository chatbotRepository;
    private final NguoiDungRepository nguoiDungRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    private static final String FLASK_AI_URL = "http://localhost:5000/api/ai/chatbot";

    /**
     * Lấy lịch sử chat của user hiện tại
     */
    public List<ChatbotHoiThoai> getMyChatHistory() {
        NguoiDung currentUser = getCurrentUser();
        return chatbotRepository.findByNguoiDungId(currentUser.getId());
    }

    /**
     * Lấy hoặc tạo session_id cho user
     * Session sẽ được tái sử dụng trong 24 giờ
     */
    public String getOrCreateSession() {
        NguoiDung currentUser = getCurrentUser();

        // Tìm session gần nhất của user (trong 24h)
        LocalDateTime yesterday = LocalDateTime.now().minusHours(24);
        List<ChatbotHoiThoai> recentChats = chatbotRepository
                .findByNguoiDungIdAndCreatedAtAfterOrderByCreatedAtDesc(
                        currentUser.getId(),
                        yesterday);

        if (!recentChats.isEmpty()) {
            return recentChats.get(0).getSessionId();
        }

        // Tạo session mới
        return UUID.randomUUID().toString();
    }

    /**
     * Hỏi chatbot với session management
     */
    @Transactional
    public ChatbotHoiThoai askChatbot(String cauHoi, String sessionId) {
        NguoiDung currentUser = getCurrentUser();

        // Validate session ownership (bảo mật)
        if (sessionId != null && !sessionId.isEmpty()) {
            List<ChatbotHoiThoai> existingChats = chatbotRepository.findBySessionId(sessionId);
            if (!existingChats.isEmpty()) {
                // Kiểm tra session có thuộc về user hiện tại không
                if (!chatbotRepository.isSessionOwnedByUser(sessionId, currentUser.getId())) {
                    throw new SecurityException("Unauthorized access to session");
                }
            }
        }

        String aiResponse;
        try {
            // Gọi Flask AI API
            Map<String, Object> request = new HashMap<>();
            request.put("message", cauHoi);
            request.put("user_id", currentUser.getId());
            request.put("session_id", sessionId);

            Map<String, Object> context = new HashMap<>();
            context.put("user_role", currentUser.getVaiTro().name());
            context.put("user_name", currentUser.getHoTen());
            context.put("user_email", currentUser.getEmail());
            request.put("context", context);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    FLASK_AI_URL,
                    request,
                    Map.class);

            if (response.getBody() != null && response.getBody().get("response") != null) {
                aiResponse = (String) response.getBody().get("response");
            } else {
                aiResponse = "Xin lỗi, tôi không thể xử lý câu hỏi của bạn lúc này.";
            }

        } catch (Exception e) {
            // Fallback nếu Flask API lỗi
            System.err.println("Flask AI API error: " + e.getMessage());
            aiResponse = generateFallbackResponse(cauHoi);
        }

        // Lưu vào database
        ChatbotHoiThoai hoiThoai = new ChatbotHoiThoai();
        hoiThoai.setNguoiDung(currentUser);
        hoiThoai.setSessionId(sessionId);
        hoiThoai.setTinNhan(cauHoi);
        hoiThoai.setPhanHoi(aiResponse);
        hoiThoai.setDoTinCay(BigDecimal.valueOf(0.95));
        hoiThoai.setIntent(detectIntent(cauHoi));

        return chatbotRepository.save(hoiThoai);
    }

    /**
     * Phát hiện intent từ câu hỏi
     */
    private String detectIntent(String cauHoi) {
        String lower = cauHoi.toLowerCase();
        if (lower.contains("mượn"))
            return "BORROW_EQUIPMENT";
        if (lower.contains("hỏng"))
            return "REPORT_DAMAGE";
        if (lower.contains("bảo trì"))
            return "MAINTENANCE_INFO";
        return "GENERAL_INQUIRY";
    }

    /**
     * Tạo response dự phòng khi Flask API lỗi
     */
    private String generateFallbackResponse(String cauHoi) {
        String lower = cauHoi.toLowerCase();
        if (lower.contains("mượn thiết bị")) {
            return "Để mượn thiết bị, bạn hãy vào mục 'Giáo viên' -> 'Mượn thiết bị', chọn thiết bị còn trống và nhấn nút 'Mượn'.";
        }
        if (lower.contains("báo hỏng")) {
            return "Nếu thấy thiết bị hỏng, bạn có thể gửi yêu cầu trong phần 'Báo hỏng'. Nhân viên CSVC sẽ kiểm tra và sửa chữa sớm nhất.";
        }
        if (lower.contains("giờ làm việc")) {
            return "Phòng quản trị CSVC làm việc từ 7:30 đến 17:00 các ngày trong tuần.";
        }
        return "Xin lỗi, hệ thống AI tạm thời không khả dụng. Vui lòng thử lại sau hoặc liên hệ phòng CSVC để được hỗ trợ.";
    }

    /**
     * Lấy thông tin user hiện tại từ Security Context
     */
    private NguoiDung getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return nguoiDungRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new ResourceNotFoundException("NguoiDung", "email", auth.getName()));
    }
}
