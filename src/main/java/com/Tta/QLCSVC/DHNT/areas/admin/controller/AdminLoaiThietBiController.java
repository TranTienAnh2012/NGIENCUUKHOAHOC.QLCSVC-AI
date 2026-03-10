package com.Tta.QLCSVC.DHNT.areas.admin.controller;

import com.Tta.QLCSVC.DHNT.areas.admin.service.AdminLoaiThietBiService;
import com.Tta.QLCSVC.DHNT.dto.ApiResponse;
import com.Tta.QLCSVC.DHNT.dto.PageResponse;
import com.Tta.QLCSVC.DHNT.entity.LoaiThietBi;
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

import java.util.List;

@RestController
@RequestMapping("/api/admin/loai-thiet-bi")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Loại thiết bị", description = "API quản lý loại thiết bị (Admin)")
@SecurityRequirement(name = "bearer-jwt")
public class AdminLoaiThietBiController {

    private final AdminLoaiThietBiService adminLoaiThietBiService;

    @GetMapping
    @Operation(summary = "Lấy danh sách loại thiết bị", description = "Lấy danh sách tất cả loại thiết bị (phân trang)")
    public ResponseEntity<ApiResponse<PageResponse<LoaiThietBi>>> getAllLoaiThietBi(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<LoaiThietBi> pageResult = adminLoaiThietBiService.getAllLoaiThietBi(pageable);

        PageResponse<LoaiThietBi> response = new PageResponse<>(
                pageResult.getContent(),
                pageResult.getNumber(),
                pageResult.getSize(),
                pageResult.getTotalElements(),
                pageResult.getTotalPages(),
                pageResult.isLast(),
                pageResult.isFirst());

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/all")
    @Operation(summary = "Lấy tất cả loại thiết bị", description = "Lấy danh sách tất cả loại thiết bị (không phân trang)")
    public ResponseEntity<ApiResponse<List<LoaiThietBi>>> getAllLoaiThietBiNoPaging() {
        List<LoaiThietBi> loaiThietBis = adminLoaiThietBiService.getAllLoaiThietBi();
        return ResponseEntity.ok(ApiResponse.success(loaiThietBis));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy thông tin loại thiết bị", description = "Lấy thông tin chi tiết một loại thiết bị")
    public ResponseEntity<ApiResponse<LoaiThietBi>> getLoaiThietBiById(@PathVariable Long id) {
        LoaiThietBi loaiThietBi = adminLoaiThietBiService.getLoaiThietBiById(id);
        return ResponseEntity.ok(ApiResponse.success(loaiThietBi));
    }

    @PostMapping
    @Operation(summary = "Tạo loại thiết bị mới", description = "Thêm loại thiết bị mới vào hệ thống")
    public ResponseEntity<ApiResponse<LoaiThietBi>> createLoaiThietBi(@Valid @RequestBody LoaiThietBi loaiThietBi) {
        LoaiThietBi created = adminLoaiThietBiService.createLoaiThietBi(loaiThietBi);
        return ResponseEntity.ok(ApiResponse.success("Tạo loại thiết bị thành công", created));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật loại thiết bị", description = "Cập nhật thông tin loại thiết bị")
    public ResponseEntity<ApiResponse<LoaiThietBi>> updateLoaiThietBi(
            @PathVariable Long id,
            @Valid @RequestBody LoaiThietBi loaiThietBi) {
        LoaiThietBi updated = adminLoaiThietBiService.updateLoaiThietBi(id, loaiThietBi);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật loại thiết bị thành công", updated));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa loại thiết bị", description = "Xóa loại thiết bị khỏi hệ thống")
    public ResponseEntity<ApiResponse<Void>> deleteLoaiThietBi(@PathVariable Long id) {
        adminLoaiThietBiService.deleteLoaiThietBi(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa loại thiết bị thành công", null));
    }
}
