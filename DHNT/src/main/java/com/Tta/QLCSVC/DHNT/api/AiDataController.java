package com.Tta.QLCSVC.DHNT.api;

import com.Tta.QLCSVC.DHNT.entity.*;
import com.Tta.QLCSVC.DHNT.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * REST API Controller for AI Chatbot Data
 * Provides real database data for AI to give accurate responses.
 *
 * Bảo mật: Endpoint này yêu cầu xác thực:
 * - Flask AI server: gửi header X-Internal-API-Key (xác thực bởi
 * InternalApiKeyFilter → ROLE_INTERNAL_SERVICE)
 * - User: đã đăng nhập với JWT/session (ADMIN, GIAO_VIEN, NHAN_VIEN_CSVC)
 */
@RestController
@RequestMapping("/api/ai-data")
public class AiDataController {

    @Autowired
    private ThietBiRepository thietBiRepository;

    @Autowired
    private MuonTraThietBiRepository muonTraThietBiRepository;

    @Autowired
    private BaoHongRepository baoHongRepository;

    @Autowired
    private BaoTriRepository baoTriRepository;

    @Autowired
    private NguoiDungRepository nguoiDungRepository;

    @Autowired
    private ThongBaoRepository thongBaoRepository;

    /**
     * Mark all notifications as read
     */
    @PostMapping("/notifications/{userId}/mark-read")
    public ResponseEntity<Map<String, Object>> markAllRead(@PathVariable Long userId) {
        List<ThongBao> notifications = thongBaoRepository.findByNguoiDungIdAndDaDocFalse(userId);
        notifications.forEach(n -> n.setDaDoc(true));
        thongBaoRepository.saveAll(notifications);
        return ResponseEntity.ok(Map.of("success", true));
    }

    /**
     * Get system statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        Map<String, Object> stats = new HashMap<>();

        // Total devices
        long totalDevices = thietBiRepository.count();
        stats.put("total_devices", totalDevices);

        // Devices by status
        long totDevices = thietBiRepository.findByTrangThai(ThietBi.TrangThaiThietBi.TOT).size();
        long baoTriDevices = thietBiRepository.findByTrangThai(ThietBi.TrangThaiThietBi.BAO_TRI).size();
        long hongDevices = thietBiRepository.findByTrangThai(ThietBi.TrangThaiThietBi.HONG).size();
        long thanhLyDevices = thietBiRepository.findByTrangThai(ThietBi.TrangThaiThietBi.THANH_LY).size();

        stats.put("available_devices", totDevices);
        stats.put("maintenance_devices", baoTriDevices);
        stats.put("damaged_devices", hongDevices);
        stats.put("retired_devices", thanhLyDevices);

        // Active borrows
        long activeBorrows = muonTraThietBiRepository.findByTrangThai(MuonTraThietBi.TrangThaiMuonTra.DANG_MUON).size();
        stats.put("active_borrows", activeBorrows);

        // Total borrows and damages
        stats.put("total_borrows", muonTraThietBiRepository.count());
        stats.put("total_damages", baoHongRepository.count());
        stats.put("total_maintenances", baoTriRepository.count());

        return ResponseEntity.ok(stats);
    }

    /**
     * Get devices with optional status filtering
     */
    @GetMapping("/devices")
    public ResponseEntity<List<Map<String, Object>>> getDevices(@RequestParam(required = false) String status) {
        List<ThietBi> devices;

        if (status != null && !status.isEmpty()) {
            try {
                ThietBi.TrangThaiThietBi trangThaiEnum = ThietBi.TrangThaiThietBi.valueOf(status.toUpperCase());
                devices = thietBiRepository.findByTrangThai(trangThaiEnum);
            } catch (IllegalArgumentException e) {
                devices = thietBiRepository.findAll();
            }
        } else {
            devices = thietBiRepository.findAll();
        }

        List<Map<String, Object>> result = devices.stream()
                .limit(50)
                .map(this::mapDeviceToSimple)
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    /**
     * Get devices that need maintenance
     */
    @GetMapping("/devices/maintenance-needed")
    public ResponseEntity<List<Map<String, Object>>> getMaintenanceNeeded() {
        List<ThietBi> devices = thietBiRepository.findByTrangThai(ThietBi.TrangThaiThietBi.BAO_TRI);

        List<Map<String, Object>> result = devices.stream()
                .map(this::mapDeviceToSimple)
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    /**
     * Get recent damage reports
     */
    @GetMapping("/damages/recent")
    public ResponseEntity<List<Map<String, Object>>> getRecentDamages(
            @RequestParam(defaultValue = "10") int limit) {

        List<BaoHong> damages = baoHongRepository.findAll().stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .limit(limit)
                .collect(Collectors.toList());

        List<Map<String, Object>> result = damages.stream()
                .map(this::mapDamageToSimple)
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    /**
     * Get active borrows (DANG_MUON)
     */
    @GetMapping("/borrows/active")
    public ResponseEntity<List<Map<String, Object>>> getActiveBorrows() {
        List<MuonTraThietBi> borrows = muonTraThietBiRepository
                .findByTrangThai(MuonTraThietBi.TrangThaiMuonTra.DANG_MUON);

        List<Map<String, Object>> result = borrows.stream()
                .limit(50)
                .map(this::mapBorrowToSimple)
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    /**
     * Get returned borrows (DA_TRA), sorted by actual return date desc
     */
    @GetMapping("/borrows/returned")
    public ResponseEntity<List<Map<String, Object>>> getReturnedBorrows(
            @RequestParam(defaultValue = "20") int limit) {
        List<MuonTraThietBi> borrows = muonTraThietBiRepository
                .findByTrangThai(MuonTraThietBi.TrangThaiMuonTra.DA_TRA)
                .stream()
                .sorted((a, b) -> {
                    if (a.getNgayTraThucTe() == null)
                        return 1;
                    if (b.getNgayTraThucTe() == null)
                        return -1;
                    return b.getNgayTraThucTe().compareTo(a.getNgayTraThucTe());
                })
                .limit(limit)
                .collect(Collectors.toList());

        List<Map<String, Object>> result = borrows.stream()
                .map(this::mapBorrowToSimple)
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    /**
     * Get all borrows (all statuses), sorted by borrow date desc
     */
    @GetMapping("/borrows/all")
    public ResponseEntity<List<Map<String, Object>>> getAllBorrows(
            @RequestParam(defaultValue = "50") int limit) {
        List<MuonTraThietBi> borrows = muonTraThietBiRepository.findAll()
                .stream()
                .sorted((a, b) -> b.getNgayMuon().compareTo(a.getNgayMuon()))
                .limit(limit)
                .collect(Collectors.toList());

        List<Map<String, Object>> result = borrows.stream()
                .map(this::mapBorrowToSimple)
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    /**
     * Get overdue borrows (DANG_MUON and expected return date < now)
     */
    @GetMapping("/borrows/overdue")
    public ResponseEntity<List<Map<String, Object>>> getOverdueBorrows() {
        System.out.println("Debug: Checking overdue borrows at " + java.time.LocalDateTime.now());
        List<MuonTraThietBi> overdueBorrows = muonTraThietBiRepository
                .findOverdueRecords(java.time.LocalDateTime.now());
        
        System.out.println("Debug: Found " + overdueBorrows.size() + " overdue records");
        for (MuonTraThietBi m : overdueBorrows) {
            System.out.println("Debug: Record ID " + m.getId() + " - User ID: " + (m.getNguoiMuon() != null ? m.getNguoiMuon().getId() : "NULL"));
        }

        List<Map<String, Object>> result = overdueBorrows.stream()
                .map(this::mapBorrowToSimple)
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    // Helper methods to map entities to simple objects

    private Map<String, Object> mapDeviceToSimple(ThietBi device) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", device.getId());
        map.put("name", device.getTenThietBi());
        map.put("code", device.getMaThietBi());
        map.put("status", device.getTrangThai().toString());
        map.put("room", device.getPhong() != null ? device.getPhong().getTenPhong() : null);
        map.put("category", device.getLoaiThietBi() != null ? device.getLoaiThietBi().getTenLoai() : null);
        map.put("hinh_anh", resolveDeviceImage(device));
        return map;
    }

    private Map<String, Object> mapBorrowToSimple(MuonTraThietBi borrow) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", borrow.getId());
        map.put("device_name", borrow.getThietBi() != null ? borrow.getThietBi().getTenThietBi() : null);
        map.put("device_code", borrow.getThietBi() != null ? borrow.getThietBi().getMaThietBi() : null);
        map.put("device_id", borrow.getThietBi() != null ? borrow.getThietBi().getId() : null);
        map.put("device_image", borrow.getThietBi() != null ? resolveDeviceImage(borrow.getThietBi()) : null);
        map.put("borrow_date", borrow.getNgayMuon() != null ? borrow.getNgayMuon().toString() : null);
        map.put("expected_return", borrow.getNgayTraDuKien() != null ? borrow.getNgayTraDuKien().toString() : null);
        map.put("actual_return", borrow.getNgayTraThucTe() != null ? borrow.getNgayTraThucTe().toString() : null);
        map.put("status", borrow.getTrangThai().toString());
        map.put("user_name", borrow.getNguoiMuon() != null ? borrow.getNguoiMuon().getHoTen() : null);
        map.put("user_id", borrow.getNguoiMuon() != null ? borrow.getNguoiMuon().getId() : null);
        map.put("ghi_chu", borrow.getGhiChu());
        return map;
    }

    private String resolveDeviceImage(ThietBi device) {
        if (device == null)
            return null;

        // Ưu tiên 1: Lấy từ trường hinh_anh_chinh ở bảng thiet_bi
        if (device.getHinhAnhChinh() != null && !device.getHinhAnhChinh().trim().isEmpty()) {
            return device.getHinhAnhChinh();
        }

        // Ưu tiên 2: Lấy từ danh sách hinhAnhs (bảng hinh_anh_thiet_bi)
        if (device.getHinhAnhs() != null && !device.getHinhAnhs().isEmpty()) {
            // Tìm ảnh có loại là HINH_ANH_CHINH
            for (HinhAnhThietBi img : device.getHinhAnhs()) {
                if (img.getLoaiHinhAnh() == HinhAnhThietBi.LoaiHinhAnh.HINH_ANH_CHINH) {
                    return img.getUrlHinhAnh();
                }
            }
            // Nếu không có loại HINH_ANH_CHINH, lấy ảnh đầu tiên
            return device.getHinhAnhs().get(0).getUrlHinhAnh();
        }

        return "N/A";
    }

    private Map<String, Object> mapDamageToSimple(BaoHong damage) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", damage.getId());
        map.put("device_name", damage.getThietBi() != null ? damage.getThietBi().getTenThietBi() : null);
        map.put("device_image", damage.getThietBi() != null ? resolveDeviceImage(damage.getThietBi()) : null);
        map.put("description", damage.getMoTaLoi());
        map.put("status", damage.getTrangThai());
        map.put("severity", damage.getMucDoNghiemTrong());
        map.put("reported_date", damage.getCreatedAt());
        map.put("reporter_name", damage.getNguoiBao() != null ? damage.getNguoiBao().getHoTen() : null);
        return map;
    }

    /**
     * Create a new notification
     */
    @PostMapping("/notifications")
    public ResponseEntity<Map<String, Object>> createNotification(@RequestBody Map<String, Object> payload) {
        try {
            Long userId = Long.valueOf(payload.get("user_id").toString());
            String title = payload.get("title").toString();
            String content = payload.get("content").toString();
            String type = payload.getOrDefault("type", "SYSTEM").toString();

            NguoiDung user = nguoiDungRepository.findById(userId).orElse(null);
            if (user == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "User not found"));
            }

            ThongBao notification = new ThongBao();
            notification.setNguoiDung(user);
            notification.setTieuDe(title);
            notification.setNoiDung(content);
            notification.setLoaiThongBao(type);
            notification.setDaDoc(false);

            thongBaoRepository.save(notification);
            System.out.println("Debug: Saved notification for User ID: " + userId + " - Title: " + title);

            return ResponseEntity.ok(Map.of("success", true, "id", notification.getId()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get unread notifications for a user
     */
    @GetMapping("/notifications/{userId}")
    public ResponseEntity<List<Map<String, Object>>> getNotifications(@PathVariable Long userId) {
        System.out.println("Debug: Fetching notifications for user ID: " + userId);
        List<ThongBao> notifications = thongBaoRepository.findByNguoiDungIdOrderByCreatedAtDesc(userId);
        System.out.println("Debug: Found " + notifications.size() + " notifications");

        List<Map<String, Object>> result = notifications.stream().limit(10).map(n -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", n.getId());
            map.put("title", n.getTieuDe());
            map.put("content", n.getNoiDung());
            map.put("is_read", n.isDaDoc());
            map.put("type", n.getLoaiThongBao());
            map.put("created_at", n.getCreatedAt() != null ? n.getCreatedAt().toString() : null);
            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    /**
     * Get unread count
     */
    @GetMapping("/notifications/{userId}/unread-count")
    public ResponseEntity<Map<String, Object>> getUnreadCount(@PathVariable Long userId) {
        long count = thongBaoRepository.countByNguoiDungIdAndDaDocFalse(userId);
        return ResponseEntity.ok(Map.of("count", count));
    }
}
