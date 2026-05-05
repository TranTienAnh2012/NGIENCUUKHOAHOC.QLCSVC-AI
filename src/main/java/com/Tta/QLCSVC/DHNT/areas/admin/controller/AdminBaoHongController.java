package com.Tta.QLCSVC.DHNT.areas.admin.controller;

import com.Tta.QLCSVC.DHNT.areas.admin.service.AdminBaoHongService;
import com.Tta.QLCSVC.DHNT.dto.ApiResponse;
import com.Tta.QLCSVC.DHNT.dto.PageResponse;
import com.Tta.QLCSVC.DHNT.entity.BaoHong;
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
@RequestMapping("/api/admin/bao-hong")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Báo hỏng", description = "API quản lý báo hỏng (Admin)")
@SecurityRequirement(name = "bearer-jwt")
public class AdminBaoHongController {

    private final AdminBaoHongService adminBaoHongService;

    @GetMapping
    @Operation(summary = "Lấy danh sách báo hỏng", description = "Lấy danh sách tất cả phiếu báo hỏng (phân trang)")
    public ResponseEntity<ApiResponse<PageResponse<BaoHong>>> getAllBaoHong(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<BaoHong> pageResult = adminBaoHongService.getAllBaoHong(pageable);

        PageResponse<BaoHong> response = new PageResponse<>(
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
    @Operation(summary = "Lấy thông tin báo hỏng", description = "Lấy thông tin chi tiết một phiếu báo hỏng")
    public ResponseEntity<ApiResponse<BaoHong>> getBaoHongById(@PathVariable Long id) {
        BaoHong baoHong = adminBaoHongService.getBaoHongById(id);
        return ResponseEntity.ok(ApiResponse.success(baoHong));
    }

    @GetMapping("/thiet-bi/{thietBiId}")
    @Operation(summary = "Lấy báo hỏng theo thiết bị", description = "Lấy danh sách báo hỏng của một thiết bị")
    public ResponseEntity<ApiResponse<List<BaoHong>>> getBaoHongByThietBi(@PathVariable Long thietBiId) {
        List<BaoHong> baoHongList = adminBaoHongService.getBaoHongByThietBi(thietBiId);
        return ResponseEntity.ok(ApiResponse.success(baoHongList));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Lấy báo hỏng theo trạng thái", description = "Lấy danh sách báo hỏng theo trạng thái (CHO_XU_LY, DANG_XU_LY, HOAN_THANH, HUY)")
    public ResponseEntity<ApiResponse<List<BaoHong>>> getBaoHongByStatus(@PathVariable String status) {
        BaoHong.TrangThaiBaoHong trangThai = BaoHong.TrangThaiBaoHong.valueOf(status);
        List<BaoHong> baoHongList = adminBaoHongService.getBaoHongByTrangThai(trangThai);
        return ResponseEntity.ok(ApiResponse.success(baoHongList));
    }

    @GetMapping("/urgent")
    @Operation(summary = "Lấy báo hỏng khẩn cấp", description = "Lấy danh sách báo hỏng có mức độ khẩn cấp")
    public ResponseEntity<ApiResponse<List<BaoHong>>> getUrgentBaoHong() {
        List<BaoHong> baoHongList = adminBaoHongService.getBaoHongByMucDo(BaoHong.MucDoNghiemTrong.KHAN_CAP);
        return ResponseEntity.ok(ApiResponse.success(baoHongList));
    }

    @PostMapping
    @Operation(summary = "Tạo phiếu báo hỏng mới", description = "Thêm phiếu báo hỏng mới vào hệ thống")
    public ResponseEntity<ApiResponse<BaoHong>> createBaoHong(@RequestBody BaoHong baoHong) {
        BaoHong created = adminBaoHongService.createBaoHong(baoHong);
        return ResponseEntity.ok(ApiResponse.success("Tạo phiếu báo hỏng thành công", created));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật phiếu báo hỏng", description = "Cập nhật toàn bộ thông tin phiếu báo hỏng")
    public ResponseEntity<ApiResponse<BaoHong>> updateBaoHong(
            @PathVariable Long id,
            @RequestBody BaoHong baoHong) {
        BaoHong updated = adminBaoHongService.updateBaoHong(id, baoHong);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật báo hỏng thành công", updated));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Cập nhật trạng thái", description = "Cập nhật trạng thái phiếu báo hỏng")
    public ResponseEntity<ApiResponse<BaoHong>> updateStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        BaoHong.TrangThaiBaoHong trangThai = BaoHong.TrangThaiBaoHong.valueOf(status);
        BaoHong updated = adminBaoHongService.updateTrangThai(id, trangThai);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái thành công", updated));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa báo hỏng", description = "Xóa phiếu báo hỏng khỏi hệ thống")
    public ResponseEntity<ApiResponse<Void>> deleteBaoHong(@PathVariable Long id) {
        adminBaoHongService.deleteBaoHong(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa báo hỏng thành công", null));
    }
}
