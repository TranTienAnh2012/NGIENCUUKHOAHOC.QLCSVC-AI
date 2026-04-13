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
     * POST /api/ai-data/report-damage
     * Tiep nhan bao cao hong tu Flask QR form (khong can JWT - dung InternalApiKey).
     * Reporter la nguoi dung khong co tai khoan (khach, hoc sinh, GV chua dang nhap).
     *
     * Body params:
     *   thiet_bi_id   : Long   - ID thiet bi (bat buoc)
     *   mo_ta         : String - Mo ta loi (bat buoc)
     *   muc_do        : String - THAP | TRUNG_BINH | CAO | KHAN_CAP (mac dinh: TRUNG_BINH)
     *   ten_nguoi_bao : String - Ten nguoi bao (tuy chon)
     *   so_dien_thoai : String - SDT nguoi bao (tuy chon)
     */
    @PostMapping("/report-damage")
    public ResponseEntity<Map<String, Object>> reportDamageFromQR(
            @RequestParam Long thiet_bi_id,
            @RequestParam String mo_ta,
            @RequestParam(required = false, defaultValue = "TRUNG_BINH") String muc_do,
            @RequestParam(required = false, defaultValue = "Khách/Người dùng") String ten_nguoi_bao,
            @RequestParam(required = false, defaultValue = "") String so_dien_thoai) {

        Map<String, Object> result = new HashMap<>();

        // Tim thiet bi
        Optional<ThietBi> thietBiOpt = thietBiRepository.findById(thiet_bi_id);
        if (thietBiOpt.isEmpty()) {
            result.put("success", false);
            result.put("message", "Không tìm thấy thiết bị ID=" + thiet_bi_id);
            return ResponseEntity.badRequest().body(result);
        }

        BaoHong baoHong = new BaoHong();
        baoHong.setThietBi(thietBiOpt.get());
        baoHong.setMoTaLoi(mo_ta);
        baoHong.setTrangThai(BaoHong.TrangThaiBaoHong.CHO_XU_LY);

        try {
            BaoHong.MucDoNghiemTrong mucDoEnum = BaoHong.MucDoNghiemTrong.valueOf(muc_do.toUpperCase());
            baoHong.setMucDoNghiemTrong(mucDoEnum);
        } catch (IllegalArgumentException e) {
            baoHong.setMucDoNghiemTrong(BaoHong.MucDoNghiemTrong.TRUNG_BINH);
        }

        // Luu ghi chu nguoi bao (khong co tai khoan)
        String ghiChu = "Báo qua QR Code";
        if (!ten_nguoi_bao.equals("Khách/Người dùng"))
            ghiChu += " | Người báo: " + ten_nguoi_bao;
        if (!so_dien_thoai.isEmpty())
            ghiChu += " | SDT: " + so_dien_thoai;
        baoHong.setGhiChu(ghiChu);

        BaoHong saved = baoHongRepository.save(baoHong);

        result.put("success", true);
        result.put("message", "Báo hỏng thành công! Nhân viên kỹ thuật sẽ xử lý sớm.");
        result.put("bao_hong_id", saved.getId());
        result.put("thiet_bi", thietBiOpt.get().getTenThietBi());
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
        return map;
    }

    private Map<String, Object> mapBorrowToSimple(MuonTraThietBi borrow) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", borrow.getId());
        map.put("device_name", borrow.getThietBi() != null ? borrow.getThietBi().getTenThietBi() : null);
        map.put("device_code", borrow.getThietBi() != null ? borrow.getThietBi().getMaThietBi() : null);
        map.put("borrow_date", borrow.getNgayMuon() != null ? borrow.getNgayMuon().toString() : null);
        map.put("expected_return", borrow.getNgayTraDuKien() != null ? borrow.getNgayTraDuKien().toString() : null);
        map.put("actual_return", borrow.getNgayTraThucTe() != null ? borrow.getNgayTraThucTe().toString() : null);
        map.put("status", borrow.getTrangThai().toString());
        map.put("user_name", borrow.getNguoiMuon() != null ? borrow.getNguoiMuon().getHoTen() : null);
        map.put("ghi_chu", borrow.getGhiChu());
        return map;
    }

    private Map<String, Object> mapDamageToSimple(BaoHong damage) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", damage.getId());
        map.put("device_name", damage.getThietBi() != null ? damage.getThietBi().getTenThietBi() : null);
        map.put("description", damage.getMoTaLoi());
        map.put("status", damage.getTrangThai());
        map.put("severity", damage.getMucDoNghiemTrong());
        map.put("reported_date", damage.getCreatedAt());
        map.put("reporter_name", damage.getNguoiBao() != null ? damage.getNguoiBao().getHoTen() : null);
        return map;
    }
}
