package com.Tta.QLCSVC.DHNT.areas.admin.controller;

import com.Tta.QLCSVC.DHNT.areas.admin.service.AdminLinhKienService;
import com.Tta.QLCSVC.DHNT.dto.ApiResponse;
import com.Tta.QLCSVC.DHNT.entity.LinhKien;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/linh-kien")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Linh kiện", description = "API quản lý linh kiện thiết bị (Admin)")
@SecurityRequirement(name = "bearer-jwt")
public class AdminLinhKienController {

    private final AdminLinhKienService adminLinhKienService;

    @GetMapping
    @Operation(summary = "Lấy danh sách linh kiện", description = "Lấy tất cả linh kiện trong hệ thống")
    public ResponseEntity<ApiResponse<List<LinhKien>>> getAllLinhKien() {
        List<LinhKien> list = adminLinhKienService.getAllLinhKien();
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy thông tin linh kiện", description = "Lấy chi tiết một linh kiện theo ID")
    public ResponseEntity<ApiResponse<LinhKien>> getLinhKienById(@PathVariable Long id) {
        LinhKien linhKien = adminLinhKienService.getLinhKienById(id);
        return ResponseEntity.ok(ApiResponse.success(linhKien));
    }

    @GetMapping("/thiet-bi/{thietBiId}")
    @Operation(summary = "Lấy linh kiện theo thiết bị", description = "Lấy tất cả linh kiện thuộc một thiết bị")
    public ResponseEntity<ApiResponse<List<LinhKien>>> getLinhKienByThietBi(@PathVariable Long thietBiId) {
        List<LinhKien> list = adminLinhKienService.getLinhKienByThietBi(thietBiId);
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @PostMapping
    @Operation(summary = "Tạo linh kiện mới", description = "Thêm linh kiện mới vào một thiết bị")
    public ResponseEntity<ApiResponse<LinhKien>> createLinhKien(
            @RequestBody LinhKien linhKien,
            @RequestParam Long thietBiId) {
        LinhKien created = adminLinhKienService.createLinhKien(linhKien, thietBiId);
        return ResponseEntity.ok(ApiResponse.success("Tạo linh kiện thành công", created));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật linh kiện", description = "Cập nhật thông tin linh kiện")
    public ResponseEntity<ApiResponse<LinhKien>> updateLinhKien(
            @PathVariable Long id,
            @RequestBody LinhKien linhKien) {
        LinhKien updated = adminLinhKienService.updateLinhKien(id, linhKien);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật linh kiện thành công", updated));
    }

    @PatchMapping("/{id}/su-dung")
    @Operation(summary = "Cập nhật số giờ sử dụng", description = "Cộng thêm số giờ/trang đã sử dụng cho linh kiện")
    public ResponseEntity<ApiResponse<LinhKien>> capNhatSuDung(
            @PathVariable Long id,
            @RequestParam Integer soGioThem) {
        LinhKien updated = adminLinhKienService.capNhatSuDung(id, soGioThem);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật sử dụng thành công", updated));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa linh kiện", description = "Xóa linh kiện khỏi hệ thống")
    public ResponseEntity<ApiResponse<Void>> deleteLinhKien(@PathVariable Long id) {
        adminLinhKienService.deleteLinhKien(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa linh kiện thành công", null));
    }
}
