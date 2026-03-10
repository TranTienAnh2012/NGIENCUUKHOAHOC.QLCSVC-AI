package com.Tta.QLCSVC.DHNT.areas.admin.controller;

import com.Tta.QLCSVC.DHNT.areas.admin.service.AdminBaoTriService;
import com.Tta.QLCSVC.DHNT.dto.ApiResponse;
import com.Tta.QLCSVC.DHNT.dto.PageResponse;
import com.Tta.QLCSVC.DHNT.entity.BaoTri;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin/bao-tri")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Bảo trì", description = "API quản lý bảo trì (Admin)")
@SecurityRequirement(name = "bearer-jwt")
public class AdminBaoTriController {

    private final AdminBaoTriService adminBaoTriService;

    @GetMapping
    @Operation(summary = "Lấy danh sách bảo trì", description = "Lấy danh sách tất cả phiếu bảo trì (phân trang)")
    public ResponseEntity<ApiResponse<PageResponse<BaoTri>>> getAllBaoTri(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<BaoTri> pageResult = adminBaoTriService.getAllBaoTri(pageable);

        PageResponse<BaoTri> response = new PageResponse<>(
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
    @Operation(summary = "Lấy thông tin bảo trì", description = "Lấy thông tin chi tiết một phiếu bảo trì")
    public ResponseEntity<ApiResponse<BaoTri>> getBaoTriById(@PathVariable Long id) {
        BaoTri baoTri = adminBaoTriService.getBaoTriById(id);
        return ResponseEntity.ok(ApiResponse.success(baoTri));
    }

    @GetMapping("/thiet-bi/{thietBiId}")
    @Operation(summary = "Lấy bảo trì theo thiết bị", description = "Lấy danh sách bảo trì của một thiết bị")
    public ResponseEntity<ApiResponse<List<BaoTri>>> getBaoTriByThietBi(@PathVariable Long thietBiId) {
        List<BaoTri> baoTriList = adminBaoTriService.getBaoTriByThietBi(thietBiId);
        return ResponseEntity.ok(ApiResponse.success(baoTriList));
    }

    @GetMapping("/date-range")
    @Operation(summary = "Lấy bảo trì theo khoảng thời gian", description = "Lấy danh sách bảo trì trong khoảng thời gian")
    public ResponseEntity<ApiResponse<List<BaoTri>>> getBaoTriByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        List<BaoTri> baoTriList = adminBaoTriService.getBaoTriByDateRange(start, end);
        return ResponseEntity.ok(ApiResponse.success(baoTriList));
    }

    @PostMapping
    @Operation(summary = "Tạo phiếu bảo trì mới", description = "Thêm phiếu bảo trì mới vào hệ thống")
    public ResponseEntity<ApiResponse<BaoTri>> createBaoTri(@Valid @RequestBody BaoTri baoTri) {
        BaoTri created = adminBaoTriService.createBaoTri(baoTri);
        return ResponseEntity.ok(ApiResponse.success("Tạo phiếu bảo trì thành công", created));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật bảo trì", description = "Cập nhật thông tin phiếu bảo trì")
    public ResponseEntity<ApiResponse<BaoTri>> updateBaoTri(
            @PathVariable Long id,
            @Valid @RequestBody BaoTri baoTri) {
        BaoTri updated = adminBaoTriService.updateBaoTri(id, baoTri);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật bảo trì thành công", updated));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa bảo trì", description = "Xóa phiếu bảo trì khỏi hệ thống")
    public ResponseEntity<ApiResponse<Void>> deleteBaoTri(@PathVariable Long id) {
        adminBaoTriService.deleteBaoTri(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa bảo trì thành công", null));
    }
}
