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

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationApiController {

    private final NotificationService notificationService;
    private final NguoiDungRepository nguoiDungRepository;
    private final AiNotificationScheduler aiNotificationScheduler;

    private NguoiDung getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        return nguoiDungRepository.findByEmail(auth.getName()).orElse(null);
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getNotifications() {
        NguoiDung user = getCurrentUser();
        if (user == null) return ResponseEntity.status(401).build();

        List<ThongBao> notifications = notificationService.getUserNotifications(user.getId(), user.getVaiTro());
        long unreadCount = notificationService.getUnreadCount(user.getId(), user.getVaiTro());

        Map<String, Object> responseData = new HashMap<>();
        responseData.put("notifications", notifications);
        responseData.put("unreadCount", unreadCount);
        responseData.put("success", true);

        return ResponseEntity.ok(responseData);
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead() {
        NguoiDung user = getCurrentUser();
        if (user != null) {
            notificationService.markAllAsRead(user.getId(), user.getVaiTro());
        }
        return ResponseEntity.ok().build();
    }

    @PostMapping("/trigger-ai-prediction")
    public ResponseEntity<Map<String, Object>> triggerAiPrediction() {
        NguoiDung user = getCurrentUser();
        if (user == null || user.getVaiTro() != NguoiDung.VaiTro.ADMIN) {
            return ResponseEntity.status(403).build();
        }
        aiNotificationScheduler.triggerManualPrediction();
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "Đã kích hoạt AI phân tích hệ thống thành công.");
        return ResponseEntity.ok(result);
    }
}
