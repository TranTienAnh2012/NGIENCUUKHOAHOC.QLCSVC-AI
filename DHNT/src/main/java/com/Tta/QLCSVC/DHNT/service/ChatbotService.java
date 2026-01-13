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

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatbotService {

    private final ChatbotHoiThoaiRepository chatbotRepository;
    private final NguoiDungRepository nguoiDungRepository;

    public List<ChatbotHoiThoai> getMyChatHistory() {
        NguoiDung currentUser = getCurrentUser();
        return chatbotRepository.findByNguoiDungId(currentUser.getId());
    }

    @Transactional
    public ChatbotHoiThoai askChatbot(String cauHoi) {
        NguoiDung currentUser = getCurrentUser();

        String traLoi = generateMockResponse(cauHoi);

        ChatbotHoiThoai hoiThoai = new ChatbotHoiThoai();
        hoiThoai.setNguoiDung(currentUser);
        hoiThoai.setSessionId(UUID.randomUUID().toString());
        hoiThoai.setTinNhan(cauHoi);
        hoiThoai.setPhanHoi(traLoi);
        hoiThoai.setDoTinCay(BigDecimal.valueOf(0.95));
        hoiThoai.setIntent(detectIntent(cauHoi));

        return chatbotRepository.save(hoiThoai);
    }

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

    private String generateMockResponse(String cauHoi) {
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
        return "Xin lỗi, tôi chưa hiểu câu hỏi của bạn. Tôi có thể giúp bạn về mượn thiết bị, báo hỏng hoặc thông tin phòng học.";
    }

    private NguoiDung getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return nguoiDungRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new ResourceNotFoundException("NguoiDung", "email", auth.getName()));
    }
}
