package com.Tta.QLCSVC.DHNT.api;

import com.Tta.QLCSVC.DHNT.entity.NguoiDung;
import com.Tta.QLCSVC.DHNT.entity.ThongBao;
import com.Tta.QLCSVC.DHNT.repository.NguoiDungRepository;
import com.Tta.QLCSVC.DHNT.service.AiNotificationScheduler;
import com.Tta.QLCSVC.DHNT.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/thong-bao")
@RequiredArgsConstructor
public class ThongBaoApiController {

    private final NotificationService notificationService;
    private final NguoiDungRepository nguoiDungRepository;
    private final AiNotificationScheduler aiNotificationScheduler;

    private Optional<NguoiDung> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return Optional.empty();
        return nguoiDungRepository.findByEmail(auth.getName());
    }

    @GetMapping("/unread")
    public ResponseEntity<Map<String, Object>> getUnreadNotifications() {
        Optional<NguoiDung> currentUserOpt = getCurrentUser();
        if (currentUserOpt.isEmpty()) return ResponseEntity.status(401).build();

        NguoiDung user = currentUserOpt.get();
        // Dùng NotificationService (hỗ trợ role-broadcast)
        List<ThongBao> allList = notificationService.getUserNotifications(user.getId(), user.getVaiTro());
        List<ThongBao> unreadList = allList.stream().filter(t -> !t.isDaDoc()).toList();
        long count = unreadList.size();

        Map<String, Object> response = new HashMap<>();
        response.put("count", count);
        response.put("notifications", unreadList);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/all")
    public ResponseEntity<Map<String, Object>> getAllNotifications() {
        Optional<NguoiDung> currentUserOpt = getCurrentUser();
        if (currentUserOpt.isEmpty()) return ResponseEntity.status(401).build();

        NguoiDung user = currentUserOpt.get();
        List<ThongBao> allList = notificationService.getUserNotifications(user.getId(), user.getVaiTro());
        
        Map<String, Object> response = new HashMap<>();
        response.put("notifications", allList);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead() {
        getCurrentUser().ifPresent(user ->
            notificationService.markAllAsRead(user.getId(), user.getVaiTro())
        );
        return ResponseEntity.ok().build();
    }

    @PostMapping("/trigger-ai-prediction")
    public ResponseEntity<Map<String, Object>> triggerAiPrediction() {
        Optional<NguoiDung> userOpt = getCurrentUser();
        if (userOpt.isEmpty() || userOpt.get().getVaiTro() != NguoiDung.VaiTro.ADMIN) {
            return ResponseEntity.status(403).build();
        }
        aiNotificationScheduler.triggerManualPrediction();
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "Đã kích hoạt AI phân tích hệ thống thành công.");
        return ResponseEntity.ok(result);
    }

    @GetMapping("/debug")
    public ResponseEntity<Map<String, Object>> debugNotifications() {
        Map<String, Object> debug = new HashMap<>();
        try {
            Optional<NguoiDung> userOpt = getCurrentUser();
            debug.put("currentUser", userOpt.isPresent() ? userOpt.get().getEmail() : "null");
            debug.put("currentRole", userOpt.isPresent() ? userOpt.get().getVaiTro() : "null");
            
            List<ThongBao> allAdmin = notificationService.getUserNotifications(0L, NguoiDung.VaiTro.ADMIN);
            debug.put("allAdminNotifs", allAdmin);
            debug.put("adminNotifCount", allAdmin.size());
            
            if (userOpt.isPresent()) {
                List<ThongBao> userNotifs = notificationService.getUserNotifications(userOpt.get().getId(), userOpt.get().getVaiTro());
                debug.put("userNotifCount", userNotifs.size());
            }
        } catch(Exception e) {
            debug.put("error", e.getMessage());
        }
        return ResponseEntity.ok(debug);
    }
    
    @GetMapping("/dump-db")
    public ResponseEntity<List<Map<String, Object>>> dumpDb(@org.springframework.beans.factory.annotation.Autowired org.springframework.jdbc.core.JdbcTemplate jdbcTemplate) {
        return ResponseEntity.ok(jdbcTemplate.queryForList("SELECT id, role_nhan, nguoi_dung_id, tieu_de, da_doc FROM thong_bao WHERE role_nhan = 'ADMIN'"));
    }
}

