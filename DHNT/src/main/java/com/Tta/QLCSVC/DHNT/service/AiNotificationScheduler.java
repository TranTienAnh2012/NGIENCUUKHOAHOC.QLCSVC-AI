package com.Tta.QLCSVC.DHNT.service;

import com.Tta.QLCSVC.DHNT.entity.BaoHong;
import com.Tta.QLCSVC.DHNT.entity.BaoTri;
import com.Tta.QLCSVC.DHNT.entity.MuonTraThietBi;
import com.Tta.QLCSVC.DHNT.entity.NguoiDung;
import com.Tta.QLCSVC.DHNT.repository.BaoHongRepository;
import com.Tta.QLCSVC.DHNT.repository.BaoTriRepository;
import com.Tta.QLCSVC.DHNT.repository.MuonTraThietBiRepository;
import com.Tta.QLCSVC.DHNT.repository.ThietBiRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Scheduler AI tích hợp Flask — tự động phân tích dữ liệu và
 * gửi thông báo thông minh cho 3 role: Admin, Nhân viên CSVC, Giáo viên.
 *
 * Luồng:
 *  1. Thu thập dữ liệu từ DB
 *  2. Gọi Flask AI để sinh nội dung tự nhiên, thông minh
 *  3. Lưu ThongBao vào DB qua NotificationService
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiNotificationScheduler {

    private final NotificationService notificationService;
    private final ThietBiRepository thietBiRepository;
    private final BaoHongRepository baoHongRepository;
    private final BaoTriRepository baoTriRepository;
    private final MuonTraThietBiRepository muonTraThietBiRepository;

    @Value("${flask.ai.url:http://localhost:5000}")
    private String flaskUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ================================================================
    // STARTUP: Chạy ngay khi server khởi động để có thông báo ngay
    // ================================================================
    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        log.info("[AI] Server khởi động — kích hoạt phân tích tự động...");
        runFullAiAnalysis();
    }

    // ================================================================
    // SCHEDULED: Mỗi 8h sáng (daily report) + mỗi giờ (kiểm tra hạn trả)
    // ================================================================
    @Scheduled(cron = "0 0 8 * * ?")
    public void dailySchedule() {
        log.info("[AI] Bắt đầu phân tích tự động hàng ngày...");
        runFullAiAnalysis();
    }

    @Scheduled(fixedRate = 3_600_000)
    public void hourlyReminder() {
        remindDueSoonBorrowings();
    }

    // ================================================================
    // CORE: Phân tích toàn hệ thống, AI sinh nội dung, gửi 3 role
    // ================================================================
    public void runFullAiAnalysis() {
        try {
            // 1. Thu thập dữ liệu thực từ DB
            long tongThietBi = thietBiRepository.count();
            long dangMuon    = muonTraThietBiRepository.countByTrangThai(MuonTraThietBi.TrangThaiMuonTra.DANG_MUON);
            long quaHan      = muonTraThietBiRepository.countByTrangThai(MuonTraThietBi.TrangThaiMuonTra.QUA_HAN);
            long choBaoHong  = baoHongRepository.findByTrangThai(BaoHong.TrangThaiBaoHong.CHO_XU_LY).size();
            long dangBaoHong = baoHongRepository.findByTrangThai(BaoHong.TrangThaiBaoHong.DANG_XU_LY).size();
            List<BaoHong> urgentList = baoHongRepository.findUrgentReports();
            List<BaoTri> recentBaoTri = baoTriRepository.findAll().stream()
                    .sorted((a, b) -> {
                        if (a.getCreatedAt() == null) return 1;
                        if (b.getCreatedAt() == null) return -1;
                        return b.getCreatedAt().compareTo(a.getCreatedAt());
                    }).limit(3).toList();

            // 2. Gửi thông báo AI cho từng role
            notifyAdmin(tongThietBi, dangMuon, quaHan, choBaoHong, urgentList.size());
            notifyNhanVien(choBaoHong, dangBaoHong, urgentList, recentBaoTri);

            log.info("[AI] Phân tích hoàn tất — đã gửi thông báo cho Admin và Nhân viên CSVC.");
        } catch (Exception e) {
            log.error("[AI] Lỗi khi phân tích: {}", e.getMessage());
        }
    }

    // ================================================================
    // ADMIN: Báo cáo tổng quan hệ thống
    // ================================================================
    private void notifyAdmin(long tongThietBi, long dangMuon, long quaHan, long choBaoHong, long urgent) {
        String dataContext = String.format(
            "Dữ liệu hệ thống QLCSVC:\n" +
            "- Tổng thiết bị: %d\n" +
            "- Đang được mượn: %d\n" +
            "- Mượn quá hạn chưa trả: %d\n" +
            "- Báo hỏng chờ xử lý: %d (trong đó khẩn cấp: %d)\n",
            tongThietBi, dangMuon, quaHan, choBaoHong, urgent
        );

        String title = "📊 AI Báo cáo hệ thống tự động";
        if (notificationService.canSendToRole(NguoiDung.VaiTro.ADMIN, title)) {
            String aiContent = callFlaskAI(
                "Dựa vào dữ liệu sau, hãy viết một báo cáo ngắn gọn (3-4 câu) " +
                "cho Quản trị viên hệ thống QLCSVC, nêu bật các vấn đề cần chú ý:\n" + dataContext,
                "ADMIN",
                String.format("Hệ thống hiện có %d thiết bị, đang mượn: %d, quá hạn: %d, báo hỏng chờ xử lý: %d.",
                    tongThietBi, dangMuon, quaHan, choBaoHong)
            );

            notificationService.sendToRole(
                NguoiDung.VaiTro.ADMIN,
                title,
                aiContent,
                "HE_THONG",
                "/admin"
            );
        }

        // Cảnh báo riêng nếu có sự cố nghiêm trọng
        if (urgent > 0) {
            String urgentTitle = "🚨 AI Cảnh báo: " + urgent + " thiết bị hỏng khẩn cấp";
            if (notificationService.canSendToRole(NguoiDung.VaiTro.ADMIN, urgentTitle)) {
                String urgentMsg = callFlaskAI(
                    "Hệ thống có " + urgent + " thiết bị báo hỏng mức KHẨN CẤP hoặc CAO. " +
                    "Viết cảnh báo ngắn (2 câu) cho quản trị viên, yêu cầu xử lý ngay.",
                    "ADMIN",
                    "Phát hiện " + urgent + " thiết bị hỏng mức nghiêm trọng. Vui lòng kiểm tra và xử lý ngay để đảm bảo hoạt động giảng dạy."
                );
                notificationService.sendToRole(
                    NguoiDung.VaiTro.ADMIN,
                    urgentTitle,
                    urgentMsg,
                    "BAO_HONG",
                    "/admin/bao-hong"
                );
            }
        }

        if (quaHan > 0) {
            String overDueTitle = "⚠️ AI: " + quaHan + " thiết bị mượn quá hạn";
            if (notificationService.canSendToRole(NguoiDung.VaiTro.ADMIN, overDueTitle)) {
                notificationService.sendToRole(
                    NguoiDung.VaiTro.ADMIN,
                    overDueTitle,
                    callFlaskAI(
                        "Có " + quaHan + " thiết bị mượn đã quá ngày trả. " +
                        "Viết 2 câu nhắc nhở cho quản trị viên về việc theo dõi và thu hồi.",
                        "ADMIN",
                        "Có " + quaHan + " phiếu mượn đã quá hạn trả. Đề nghị liên hệ người mượn để thu hồi thiết bị kịp thời."
                    ),
                    "MUON_TRA",
                    "/admin/muon-tra"
                );
            }
        }
    }

    // ================================================================
    // NHÂN VIÊN CSVC: Báo hỏng + Bảo trì + Ghi chú
    // ================================================================
    private void notifyNhanVien(long choBaoHong, long dangXuLy,
                                 List<BaoHong> urgentList, List<BaoTri> recentBaoTri) {
        // Tổng kết công việc hôm nay
        String workSummary = String.format(
            "- Đơn báo hỏng chờ xử lý: %d\n" +
            "- Đơn đang xử lý: %d\n" +
            "- Thiết bị hỏng khẩn cấp/cao: %d",
            choBaoHong, dangXuLy, urgentList.size()
        );

        String workSummaryTitle = "📋 AI Tóm tắt công việc hôm nay";
        if (notificationService.canSendToRole(NguoiDung.VaiTro.NHAN_VIEN_CSVC, workSummaryTitle)) {
            String aiWorkMsg = callFlaskAI(
                "Dựa vào tình trạng công việc nhân viên CSVC:\n" + workSummary +
                "\nViết tóm tắt công việc hôm nay (3-4 câu), nêu ưu tiên cần xử lý.",
                "NHAN_VIEN_CSVC",
                String.format("Hôm nay có %d đơn báo hỏng chờ xử lý, %d đơn đang xử lý. Ưu tiên xử lý các thiết bị khẩn cấp trước.",
                    choBaoHong, dangXuLy)
            );

            notificationService.sendToRole(
                NguoiDung.VaiTro.NHAN_VIEN_CSVC,
                workSummaryTitle,
                aiWorkMsg,
                "HE_THONG",
                "/nhanvien-csvc"
            );
        }

        // Thông báo từng thiết bị hỏng KHẨN CẤP
        for (BaoHong bh : urgentList) {
            String tenThietBi = bh.getThietBi() != null ? bh.getThietBi().getTenThietBi() : "Thiết bị";
            String moTa = bh.getMoTaLoi() != null ? bh.getMoTaLoi() : "không có mô tả";
            String mucDo = bh.getMucDoNghiemTrong() != null ? bh.getMucDoNghiemTrong().name() : "CAO";

            String urgentBHTitle = "🔴 Báo hỏng khẩn: " + tenThietBi;
            if (notificationService.canSendToRole(NguoiDung.VaiTro.NHAN_VIEN_CSVC, urgentBHTitle)) {
                String aiMsg = callFlaskAI(
                    "Thiết bị '" + tenThietBi + "' bị báo hỏng mức '" + mucDo + "'. " +
                    "Mô tả lỗi: " + moTa + ". " +
                    "Viết 2 câu thông báo ngắn cho nhân viên CSVC, nêu rõ tên thiết bị và yêu cầu xử lý khẩn.",
                    "NHAN_VIEN_CSVC",
                    "Thiết bị " + tenThietBi + " vừa được báo hỏng (mức " + mucDo + "): " + moTa + ". Vui lòng kiểm tra và xử lý sớm."
                );

                notificationService.sendToRole(
                    NguoiDung.VaiTro.NHAN_VIEN_CSVC,
                    urgentBHTitle,
                    aiMsg,
                    "BAO_HONG",
                    "/nhanvien-csvc/bao-hong"
                );
            }
        }

        // Thông báo bảo trì gần đây (ghi chú kết quả)
        for (BaoTri bt : recentBaoTri) {
            if (bt.getKetQua() == null) continue;   // enum — không dùng isBlank()
            String tenThietBi = bt.getThietBi() != null ? bt.getThietBi().getTenThietBi() : "Thiết bị";
            String ketQua = bt.getKetQua().name();   // THANH_CONG | THAT_BAI | CAN_THAY_THE

            String maintDoneTitle = "🔧 Bảo trì hoàn tất: " + tenThietBi;
            if (notificationService.canSendToRole(NguoiDung.VaiTro.NHAN_VIEN_CSVC, maintDoneTitle)) {
                String aiMsg = callFlaskAI(
                    "Phiếu bảo trì thiết bị '" + tenThietBi + "' vừa hoàn tất. " +
                    "Kết quả: " + ketQua + ". " +
                    "Viết 2 câu thông báo kết quả bảo trì cho nhân viên CSVC.",
                    "NHAN_VIEN_CSVC",
                    "Bảo trì thiết bị " + tenThietBi + " đã hoàn tất với kết quả: " + ketQua + ". Cập nhật trạng thái thiết bị nếu cần thiết."
                );

                notificationService.sendToRole(
                    NguoiDung.VaiTro.NHAN_VIEN_CSVC,
                    maintDoneTitle,
                    aiMsg,
                    "BAO_TRI",
                    "/nhanvien-csvc/bao-tri"
                );
            }
        }
    }

    // ================================================================
    // GIÁO VIÊN: Nhắc hạn trả (cá nhân, có AI sinh nội dung)
    // ================================================================
    public void remindDueSoonBorrowings() {
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime in120h = now.plusHours(120); // 5 ngày
            List<MuonTraThietBi> dueSoon = muonTraThietBiRepository.findDueSoonRecords(now, in120h);
            if (dueSoon.isEmpty()) return;

            log.info("[AI] Nhắc hạn trả: {} phiếu sắp hết hạn trong 5 ngày", dueSoon.size());

            for (MuonTraThietBi phieu : dueSoon) {
                String tenThietBi = phieu.getThietBi().getTenThietBi();
                String hanTra     = phieu.getNgayTraDuKien().format(DATE_FMT);
                String tenGV      = phieu.getNguoiMuon().getHoTen();
                long daysLeft = java.time.temporal.ChronoUnit.DAYS.between(
                        now.toLocalDate(), phieu.getNgayTraDuKien().toLocalDate());
                String timeLabel = daysLeft > 0 ? "còn " + daysLeft + " ngày" : "hôm nay";

                String remindTitle = "⏰ Nhắc nhở: Hạn trả thiết bị " + timeLabel;
                if (notificationService.canSendToUser(phieu.getNguoiMuon().getId(), remindTitle)) {
                    String aiMsg = callFlaskAI(
                        "Giáo viên '" + tenGV + "' đang mượn thiết bị '" + tenThietBi +
                        "', hạn trả ngày " + hanTra + " (" + timeLabel + "). " +
                        "Viết 2 câu nhắc nhở lịch sự, thân thiện để gửi cho giáo viên này.",
                        "GIAO_VIEN",
                        "Nhắc nhở: Thiết bị \"" + tenThietBi + "\" bạn đang mượn sẽ hết hạn vào ngày " + hanTra +
                        " (" + timeLabel + "). Vui lòng hoàn trả đúng hạn, cảm ơn bạn!"
                    );

                    notificationService.sendToUser(
                        phieu.getNguoiMuon().getId(),
                        remindTitle,
                        aiMsg,
                        "MUON_TRA",
                        "/giao-vien/muon-tra"
                    );
                }
            }
        } catch (Exception e) {
            log.error("[AI] Lỗi nhắc hạn trả: {}", e.getMessage());
        }
    }

    // ================================================================
    // HELPER: Gọi Flask AI API để sinh nội dung thông minh
    // Nếu Flask offline → dùng fallbackMsg thay vì dump raw prompt
    // ================================================================
    private String callFlaskAI(String prompt, String role, String fallbackMsg) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("message", prompt);
            Map<String, Object> context = new HashMap<>();
            context.put("user_role", role);
            body.put("context", context);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                flaskUrl + "/api/ai/chatbot", body, Map.class
            );

            if (response.getBody() != null && response.getBody().get("response") != null) {
                String aiReply = (String) response.getBody().get("response");
                if (aiReply != null && !aiReply.trim().isEmpty()) return aiReply;
            }
        } catch (Exception e) {
            log.warn("[AI] Flask offline hoặc lỗi: {} — dùng nội dung mặc định.", e.getMessage());
        }
        return fallbackMsg;
    }

    /**
     * Trigger thủ công từ API endpoint (Admin dùng khi demo).
     */
    public void triggerManualPrediction() {
        runFullAiAnalysis();
    }
}
