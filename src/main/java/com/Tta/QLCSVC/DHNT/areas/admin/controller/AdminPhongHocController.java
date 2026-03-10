package com.Tta.QLCSVC.DHNT.areas.admin.controller;

import com.Tta.QLCSVC.DHNT.areas.admin.service.AdminPhongHocService;
import com.Tta.QLCSVC.DHNT.dto.ApiResponse;
import com.Tta.QLCSVC.DHNT.entity.PhongHoc;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/phong-hoc")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Phòng học", description = "API quản lý phòng học (Admin)")
@SecurityRequirement(name = "bearer-jwt")
public class AdminPhongHocController {

    private final AdminPhongHocService adminPhongHocService;

    @GetMapping
    @Operation(summary = "Lấy danh sách phòng học có phân trang", description = "Lấy danh sách phòng học với phân trang")
    public ResponseEntity<ApiResponse<Page<PhongHoc>>> getAllPhongHoc(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<PhongHoc> phongHocs = adminPhongHocService.getAllPhongHoc(pageable);
            return ResponseEntity.ok(ApiResponse.success(phongHocs));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("Lỗi khi tải danh sách: " + e.getMessage()));
        }
    }

    @GetMapping("/all")
    @Operation(summary = "Lấy tất cả phòng học", description = "Lấy danh sách tất cả phòng học không phân trang")
    public ResponseEntity<ApiResponse<List<PhongHoc>>> getAllPhongHocNoPaging() {
        List<PhongHoc> phongHocs = adminPhongHocService.getAllPhongHoc();
        return ResponseEntity.ok(ApiResponse.success(phongHocs));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy thông tin phòng học", description = "Lấy thông tin chi tiết một phòng học")
    public ResponseEntity<ApiResponse<PhongHoc>> getPhongHocById(@PathVariable Long id) {
        PhongHoc phongHoc = adminPhongHocService.getPhongHocById(id);
        return ResponseEntity.ok(ApiResponse.success(phongHoc));
    }

    @PostMapping
    @Operation(summary = "Tạo phòng học mới", description = "Tạo một phòng học mới")
    public ResponseEntity<ApiResponse<PhongHoc>> createPhongHoc(@RequestBody PhongHoc phongHoc) {
        try {
            PhongHoc created = adminPhongHocService.createPhongHoc(phongHoc);
            return ResponseEntity.ok(ApiResponse.success("Thêm phòng học thành công", created));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật phòng học", description = "Cập nhật thông tin phòng học")
    public ResponseEntity<ApiResponse<PhongHoc>> updatePhongHoc(
            @PathVariable Long id,
            @RequestBody PhongHoc phongHoc) {
        try {
            PhongHoc updated = adminPhongHocService.updatePhongHoc(id, phongHoc);
            return ResponseEntity.ok(ApiResponse.success("Cập nhật phòng học thành công", updated));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa phòng học", description = "Xóa một phòng học")
    public ResponseEntity<ApiResponse<Void>> deletePhongHoc(@PathVariable Long id) {
        adminPhongHocService.deletePhongHoc(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
