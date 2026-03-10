package com.Tta.QLCSVC.DHNT.areas.admin.controller;

import com.Tta.QLCSVC.DHNT.areas.admin.service.AdminThietBiService;
import com.Tta.QLCSVC.DHNT.dto.ApiResponse;
import com.Tta.QLCSVC.DHNT.dto.PageResponse;
import com.Tta.QLCSVC.DHNT.entity.ThietBi;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/thiet-bi")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Thiết bị", description = "API quản lý thiết bị (Admin)")
@SecurityRequirement(name = "bearer-jwt")
public class AdminThietBiController {

    private final AdminThietBiService adminThietBiService;

    @GetMapping
    @Operation(summary = "Lấy danh sách thiết bị", description = "Lấy danh sách tất cả thiết bị (phân trang)")
    public ResponseEntity<ApiResponse<PageResponse<ThietBi>>> getAllThietBi(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ThietBi> pageResult = adminThietBiService.getAllThietBi(pageable);

        PageResponse<ThietBi> response = new PageResponse<>(
                pageResult.getContent(),
                pageResult.getNumber(),
                pageResult.getSize(),
                pageResult.getTotalElements(),
                pageResult.getTotalPages(),
                pageResult.isLast(),
                pageResult.isFirst());

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy thông tin thiết bị", description = "Lấy thông tin chi tiết một thiết bị")
    public ResponseEntity<ApiResponse<ThietBi>> getThietBiById(@PathVariable Long id) {
        ThietBi thietBi = adminThietBiService.getThietBiById(id);
        return ResponseEntity.ok(ApiResponse.success(thietBi));
    }

    @PostMapping
    @Operation(summary = "Tạo thiết bị mới", description = "Thêm thiết bị mới vào hệ thống")
    public ResponseEntity<ApiResponse<ThietBi>> createThietBi(@Valid @RequestBody ThietBi thietBi) {
        ThietBi created = adminThietBiService.createThietBi(thietBi);
        return ResponseEntity.ok(ApiResponse.success("Tạo thiết bị thành công", created));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật thiết bị", description = "Cập nhật thông tin thiết bị")
    public ResponseEntity<ApiResponse<ThietBi>> updateThietBi(
            @PathVariable Long id,
            @Valid @RequestBody ThietBi thietBi) {
        ThietBi updated = adminThietBiService.updateThietBi(id, thietBi);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật thiết bị thành công", updated));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa thiết bị", description = "Xóa thiết bị khỏi hệ thống")
    public ResponseEntity<ApiResponse<Void>> deleteThietBi(@PathVariable Long id) {
        adminThietBiService.deleteThietBi(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa thiết bị thành công", null));
    }
}
