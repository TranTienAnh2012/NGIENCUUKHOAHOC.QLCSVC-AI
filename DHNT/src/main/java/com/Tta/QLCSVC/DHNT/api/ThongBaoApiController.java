package com.Tta.QLCSVC.DHNT.api;

import com.Tta.QLCSVC.DHNT.entity.NguoiDung;
import com.Tta.QLCSVC.DHNT.entity.ThongBao;
import com.Tta.QLCSVC.DHNT.repository.NguoiDungRepository;
import com.Tta.QLCSVC.DHNT.service.ThongBaoService;
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

    private final ThongBaoService thongBaoService;
    private final NguoiDungRepository nguoiDungRepository;

    private Optional<NguoiDung> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return Optional.empty();
        }
        return nguoiDungRepository.findByEmail(auth.getName());
    }

    @GetMapping("/unread")
    public ResponseEntity<Map<String, Object>> getUnreadNotifications() {
        Optional<NguoiDung> currentUserOpt = getCurrentUser();
        if (currentUserOpt.isEmpty()) {
            return ResponseEntity.status(401).build();
        }

        NguoiDung user = currentUserOpt.get();
        List<ThongBao> unreadList = thongBaoService.getUnreadNotifications(user.getId());
        long count = thongBaoService.getUnreadCount(user.getId());

        Map<String, Object> response = new HashMap<>();
        response.put("count", count);
        response.put("notifications", unreadList);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id) {
        thongBaoService.markAsRead(id);
        return ResponseEntity.ok().build();
    }
}
