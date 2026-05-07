package com.Tta.QLCSVC.DHNT.service;

import com.Tta.QLCSVC.DHNT.entity.NguoiDung;
import com.Tta.QLCSVC.DHNT.entity.ThongBao;
import com.Tta.QLCSVC.DHNT.repository.BaoHongRepository;
import com.Tta.QLCSVC.DHNT.repository.ThietBiRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Service chạy ngầm dự báo và cảnh báo thông minh bằng logic mô phỏng AI.
 * Phân tích dữ liệu hệ thống để sinh ra các thông báo chủ động.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiNotificationScheduler {

    private final NotificationService notificationService;
    private final ThietBiRepository thietBiRepository;
    private final BaoHongRepository baoHongRepository;

    // Chạy tự động mỗi ngày vào 8h sáng (Giả lập: Để test có thể dùng cron="0 * * * * ?" chạy mỗi phút)
    @Scheduled(cron = "0 0 8 * * ?")
    public void generatePredictiveNotifications() {
        log.info("Bắt đầu tiến trình AI phân tích và dự báo thông báo...");
        
        try {
            long damagedCount = baoHongRepository.count();
            long totalDevices = thietBiRepository.count();

            // 1. Cảnh báo cho ADMIN nếu tỷ lệ hỏng hóc cao (> 10%)
            if (totalDevices > 0 && ((double) damagedCount / totalDevices) > 0.1) {
                notificationService.sendToRole(
                        NguoiDung.VaiTro.ADMIN,
                        "⚠️ AI Cảnh báo: Tần suất hỏng hóc cao",
                        "Phân tích dữ liệu cho thấy hệ thống đang có tỷ lệ thiết bị hỏng vượt mốc 10% (" + damagedCount + "/" + totalDevices + "). Đề xuất tổ chức đợt rà soát và bảo trì tổng thể để tránh gián đoạn giảng dạy.",
                        ThongBao.LoaiThongBao.BAO_TRI,
                        "/admin/bao-tri"
                );
            }

            // 2. Nhắc nhở cho NHAN_VIEN_CSVC về công việc tồn đọng
            long pendingRepairs = baoHongRepository.findAll().stream()
                    .filter(b -> b.getTrangThai() == com.Tta.QLCSVC.DHNT.entity.BaoHong.TrangThaiBaoHong.CHO_XU_LY)
                    .count();
            
            if (pendingRepairs >= 5) {
                notificationService.sendToRole(
                        NguoiDung.VaiTro.NHAN_VIEN_CSVC,
                        "🔧 AI Nhắc nhở: Quá tải yêu cầu sửa chữa",
                        "Hệ thống ghi nhận có " + pendingRepairs + " đơn báo hỏng đang chờ xử lý. Đề xuất ưu tiên xử lý các thiết bị thuộc phòng học có lịch sử dụng trong tuần này.",
                        ThongBao.LoaiThongBao.BAO_HONG,
                        "/nhanvien-csvc/bao-hong"
                );
            }

            log.info("Hoàn tất AI dự báo thông báo.");
        } catch (Exception e) {
            log.error("Lỗi khi chạy AI Scheduler: ", e);
        }
    }

    /**
     * Endpoint giả lập để gọi thủ công từ Postman/Browser nhằm mục đích Demo/Chấm điểm.
     */
    public void triggerManualPrediction() {
        generatePredictiveNotifications();
    }
}
