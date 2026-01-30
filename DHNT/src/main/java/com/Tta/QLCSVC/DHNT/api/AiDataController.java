package com.Tta.QLCSVC.DHNT.api;

import com.Tta.QLCSVC.DHNT.entity.*;
import com.Tta.QLCSVC.DHNT.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * REST API Controller for AI Chatbot Data
 * Provides real database data for AI to give accurate responses
 */
@RestController
@RequestMapping("/api/ai-data")
@CrossOrigin(origins = "http://localhost:5000") // Allow Flask AI API
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
     * Get active borrows
     */
    @GetMapping("/borrows/active")
    public ResponseEntity<List<Map<String, Object>>> getActiveBorrows() {
        List<MuonTraThietBi> borrows = muonTraThietBiRepository
                .findByTrangThai(MuonTraThietBi.TrangThaiMuonTra.DANG_MUON);

        List<Map<String, Object>> result = borrows.stream()
                .limit(20)
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
        return map;
    }

    private Map<String, Object> mapBorrowToSimple(MuonTraThietBi borrow) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", borrow.getId());
        map.put("device_name", borrow.getThietBi() != null ? borrow.getThietBi().getTenThietBi() : null);
        map.put("borrow_date", borrow.getNgayMuon());
        map.put("expected_return", borrow.getNgayTraDuKien());
        map.put("status", borrow.getTrangThai().toString());
        map.put("user_name", borrow.getNguoiMuon() != null ? borrow.getNguoiMuon().getHoTen() : null);
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
