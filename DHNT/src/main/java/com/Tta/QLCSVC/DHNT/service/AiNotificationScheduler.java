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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Scheduler AI tich hop Flask - tu dong phan tich du lieu va
 * gui thong bao thong minh cho 3 role: Admin, Nhan vien CSVC, Giao vien.
 *
 * Luong:
 *  1. Thu thap du lieu tu DB
 *  2. Goi Flask AI de sinh noi dung tu nhien, thong minh
 *  3. Luu ThongBao vao DB qua NotificationService
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
    // STARTUP: Chay ngay khi server khoi dong
    // ================================================================
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void onStartup() {
        log.info("[AI] Server khoi dong - kich hoat phan tich tu dong...");
        runFullAiAnalysis();
    }

    // ================================================================
    // SCHEDULED: Moi 8h sang (daily report)
    // ================================================================
    @Scheduled(cron = "0 0 8 * * ?")
    @Transactional
    public void dailySchedule() {
        log.info("[AI] Bat dau phan tich tu dong hang ngay...");
        runFullAiAnalysis();
    }

    // ================================================================
    // CORE: Phan tich toan he thong, AI sinh noi dung, gui 3 role
    // ================================================================
    public void runFullAiAnalysis() {
        try {
            long tongThietBi = thietBiRepository.count();
            long dangMuon    = muonTraThietBiRepository.countByTrangThai(MuonTraThietBi.TrangThaiMuonTra.DANG_MUON);
            long quaHan      = muonTraThietBiRepository.countByTrangThai(MuonTraThietBi.TrangThaiMuonTra.QUA_HAN);
            long choBaoHong  = baoHongRepository.findByTrangThai(BaoHong.TrangThaiBaoHong.CHO_XU_LY).size();
            long dangBaoHong = baoHongRepository.findByTrangThai(BaoHong.TrangThaiBaoHong.DANG_XU_LY).size();
            List<BaoHong> urgentList = baoHongRepository.findUrgentReports();

            LocalDateTime oneDayAgo = LocalDateTime.now().minusDays(1);
            List<BaoTri> recentBaoTri = baoTriRepository.findAll().stream()
                    .filter(b -> b.getCreatedAt() != null && b.getCreatedAt().isAfter(oneDayAgo))
                    .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                    .limit(5).toList();

            notifyAdmin(tongThietBi, dangMuon, quaHan, choBaoHong, urgentList.size());
            notifyNhanVien(choBaoHong, dangBaoHong, urgentList, recentBaoTri);
            remindDueSoonBorrowings();

            log.info("[AI] Phan tich hoan tat - da gui thong bao cho 3 roles.");
        } catch (Exception e) {
            log.error("[AI] Loi khi phan tich: {}", e.getMessage());
        }
    }

    // ================================================================
    // ADMIN: Bao cao tong quan he thong
    // ================================================================
    private void notifyAdmin(long tongThietBi, long dangMuon, long quaHan, long choBaoHong, long urgent) {
        String dataContext = String.format(
            "Du lieu he thong QLCSVC:\n" +
            "- Tong thiet bi: %d\n" +
            "- Dang duoc muon: %d\n" +
            "- Muon qua han chua tra: %d\n" +
            "- Bao hong cho xu ly: %d (khan cap: %d)\n",
            tongThietBi, dangMuon, quaHan, choBaoHong, urgent
        );

        String title = "\uD83D\uDCCA AI Báo cáo hệ thống tự động";
        if (notificationService.canSendToRole(NguoiDung.VaiTro.ADMIN, title)) {
            String aiContent = callFlaskAI(
                "Dua vao du lieu sau, hay viet mot bao cao ngan gon (3-4 cau) cho Quan tri vien:\n" + dataContext,
                "ADMIN",
                String.format("He thong hien co %d thiet bi, dang muon: %d, qua han: %d, bao hong cho xu ly: %d.",
                    tongThietBi, dangMuon, quaHan, choBaoHong)
            );
            notificationService.sendToRole(NguoiDung.VaiTro.ADMIN, title, aiContent, "HE_THONG", "/admin");
        }

        if (urgent > 0) {
            String urgentTitle = "\uD83D\uDEA8 AI Canh bao: " + urgent + " thiet bi hong khan cap";
            if (notificationService.canSendToRole(NguoiDung.VaiTro.ADMIN, urgentTitle)) {
                String urgentMsg = callFlaskAI(
                    "He thong co " + urgent + " thiet bi bao hong muc KHAN CAP. Viet canh bao ngan (2 cau).",
                    "ADMIN",
                    "Phat hien " + urgent + " thiet bi hong muc nghiem trong. Vui long kiem tra va xu ly ngay."
                );
                notificationService.sendToRole(NguoiDung.VaiTro.ADMIN, urgentTitle, urgentMsg, "BAO_HONG", "/admin/bao-hong");
            }
        }

        if (quaHan > 0) {
            String overDueTitle = "\u26A0\uFE0F AI: " + quaHan + " thiet bi muon qua han";
            if (notificationService.canSendToRole(NguoiDung.VaiTro.ADMIN, overDueTitle)) {
                notificationService.sendToRole(
                    NguoiDung.VaiTro.ADMIN, overDueTitle,
                    callFlaskAI(
                        "Co " + quaHan + " thiet bi muon da qua ngay tra. Viet 2 cau nhac nho quan tri vien.",
                        "ADMIN",
                        "Co " + quaHan + " phieu muon da qua han. De nghi lien he nguoi muon de thu hoi thiet bi."
                    ),
                    "MUON_TRA", "/admin/muon-tra"
                );
            }
        }
    }

    // ================================================================
    // NHAN VIEN CSVC: Bao hong + Bao tri
    // ================================================================
    private void notifyNhanVien(long choBaoHong, long dangXuLy,
                                 List<BaoHong> urgentList, List<BaoTri> recentBaoTri) {
        String workSummary = String.format(
            "- Don bao hong cho xu ly: %d\n- Don dang xu ly: %d\n- Thiet bi hong khan cap: %d",
            choBaoHong, dangXuLy, urgentList.size()
        );

        String workSummaryTitle = "\uD83D\uDCCB AI Tóm tắt công việc hôm nay";
        if (notificationService.canSendToRole(NguoiDung.VaiTro.NHAN_VIEN_CSVC, workSummaryTitle)) {
            String aiWorkMsg = callFlaskAI(
                "Tinh trang cong viec nhan vien CSVC:\n" + workSummary + "\nViet tom tat (3-4 cau), neu uu tien xu ly.",
                "NHAN_VIEN_CSVC",
                String.format("Hom nay co %d don bao hong cho xu ly, %d don dang xu ly.", choBaoHong, dangXuLy)
            );
            notificationService.sendToRole(NguoiDung.VaiTro.NHAN_VIEN_CSVC, workSummaryTitle, aiWorkMsg, "HE_THONG", "/nhanvien-csvc");
        }

        for (BaoHong bh : urgentList) {
            String tenThietBi = bh.getThietBi() != null ? bh.getThietBi().getTenThietBi() : "Thiet bi";
            String moTa = bh.getMoTaLoi() != null ? bh.getMoTaLoi() : "khong co mo ta";
            String mucDo = bh.getMucDoNghiemTrong() != null ? bh.getMucDoNghiemTrong().name() : "CAO";

            String urgentBHTitle = "\uD83D\uDD34 Báo hỏng khẩn: " + tenThietBi;
            if (notificationService.canSendToRole(NguoiDung.VaiTro.NHAN_VIEN_CSVC, urgentBHTitle)) {
                String aiMsg = callFlaskAI(
                    "Thiet bi '" + tenThietBi + "' bi bao hong muc '" + mucDo + "'. Mo ta: " + moTa + ". Viet 2 cau thong bao ngan.",
                    "NHAN_VIEN_CSVC",
                    "Thiet bi " + tenThietBi + " vua duoc bao hong (muc " + mucDo + "): " + moTa
                );
                notificationService.sendToRole(NguoiDung.VaiTro.NHAN_VIEN_CSVC, urgentBHTitle, aiMsg, "BAO_HONG", "/nhanvien-csvc/bao-hong");
            }
        }

        for (BaoTri bt : recentBaoTri) {
            if (bt.getKetQua() == null) continue;
            String tenThietBi = bt.getThietBi() != null ? bt.getThietBi().getTenThietBi() : "Thiet bi";
            String ketQua = bt.getKetQua().name();

            String maintDoneTitle = "\uD83D\uDD27 Báo trì hoàn tất: " + tenThietBi;
            if (notificationService.canSendToRole(NguoiDung.VaiTro.NHAN_VIEN_CSVC, maintDoneTitle)) {
                String aiMsg = callFlaskAI(
                    "Phieu bao tri thiet bi '" + tenThietBi + "' hoan tat. Ket qua: " + ketQua + ". Viet 2 cau thong bao.",
                    "NHAN_VIEN_CSVC",
                    "Bao tri " + tenThietBi + " da hoan tat voi ket qua: " + ketQua
                );
                notificationService.sendToRole(NguoiDung.VaiTro.NHAN_VIEN_CSVC, maintDoneTitle, aiMsg, "BAO_TRI", "/nhanvien-csvc/bao-tri");
            }
        }
    }

    // ================================================================
    // GIAO VIEN: Nhac han tra (ca nhan)
    // ================================================================
    @Transactional
    public void remindDueSoonBorrowings() {
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime in120h = now.plusHours(120);
            List<MuonTraThietBi> dueSoon = muonTraThietBiRepository.findDueSoonRecords(now, in120h);
            if (dueSoon.isEmpty()) return;

            log.info("[AI] Nhac han tra: {} phieu sap het han trong 5 ngay", dueSoon.size());

            for (MuonTraThietBi phieu : dueSoon) {
                String tenThietBi = phieu.getThietBi().getTenThietBi();
                String hanTra     = phieu.getNgayTraDuKien().format(DATE_FMT);
                String tenGV      = phieu.getNguoiMuon().getHoTen();
                long daysLeft = java.time.temporal.ChronoUnit.DAYS.between(
                        now.toLocalDate(), phieu.getNgayTraDuKien().toLocalDate());
                String timeLabel = daysLeft > 0 ? "con " + daysLeft + " ngay" : "hom nay";

                String remindTitle = "\u23F0 Nhắc nhở: Hạn trả thiết bị (" + timeLabel + ")";
                if (notificationService.canSendToUser(phieu.getNguoiMuon().getId(), remindTitle)) {
                    String aiMsg = callFlaskAI(
                        "Giao vien '" + tenGV + "' dang muon thiet bi '" + tenThietBi +
                        "', han tra " + hanTra + " (" + timeLabel + "). Viet 2 cau nhac nho lich su.",
                        "GIAO_VIEN",
                        "Nhac nho: Thiet bi \"" + tenThietBi + "\" se het han vao ngay " + hanTra + " (" + timeLabel + "). Vui long hoan tra dung han!"
                    );
                    notificationService.sendToUser(phieu.getNguoiMuon().getId(), remindTitle, aiMsg, "MUON_TRA", "/giao-vien/muon-tra");
                }
            }
        } catch (Exception e) {
            log.error("[AI] Loi nhac han tra: {}", e.getMessage());
        }
    }

    // ================================================================
    // HELPER: Goi Flask AI de sinh noi dung. Neu Flask offline -> dung fallback.
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
            log.warn("[AI] Flask offline hoac loi: {} - dung noi dung mac dinh.", e.getMessage());
        }
        return fallbackMsg;
    }

    /**
     * Trigger thu cong tu API endpoint (Admin dung khi demo).
     */
    @Transactional
    public void triggerManualPrediction() {
        runFullAiAnalysis();
    }
}
