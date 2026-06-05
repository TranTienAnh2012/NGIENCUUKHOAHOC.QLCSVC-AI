package com.Tta.QLCSVC.DHNT.areas.admin.controller;

import com.Tta.QLCSVC.DHNT.areas.admin.service.AdminBaoHongService;
import com.Tta.QLCSVC.DHNT.dto.ApiResponse;
import com.Tta.QLCSVC.DHNT.dto.PageResponse;
import com.Tta.QLCSVC.DHNT.dto.PhanCongRequest;
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
    @Operation(summary = "Lấy danh sách báo hỏng")
    public ResponseEntity<ApiResponse<PageResponse<BaoHong>>> getAllBaoHong(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<BaoHong> pageResult = adminBaoHongService.getAllBaoHong(pageable);
        PageResponse<BaoHong> response = new PageResponse<>(
                pageResult.getContent(), pageResult.getNumber(), pageResult.getSize(),
                pageResult.getTotalElements(), pageResult.getTotalPages(),
                pageResult.isLast(), pageResult.isFirst());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy chi tiết báo hỏng")
    public ResponseEntity<ApiResponse<BaoHong>> getBaoHongById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(adminBaoHongService.getBaoHongById(id)));
    }

    @GetMapping("/thiet-bi/{thietBiId}")
    @Operation(summary = "Báo hỏng theo thiết bị")
    public ResponseEntity<ApiResponse<List<BaoHong>>> getBaoHongByThietBi(@PathVariable Long thietBiId) {
        return ResponseEntity.ok(ApiResponse.success(adminBaoHongService.getBaoHongByThietBi(thietBiId)));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Báo hỏng theo trạng thái")
    public ResponseEntity<ApiResponse<List<BaoHong>>> getBaoHongByStatus(@PathVariable String status) {
        BaoHong.TrangThaiBaoHong trangThai = BaoHong.TrangThaiBaoHong.valueOf(status);
        return ResponseEntity.ok(ApiResponse.success(adminBaoHongService.getBaoHongByTrangThai(trangThai)));
    }

    @GetMapping("/urgent")
    @Operation(summary = "Báo hỏng khẩn cấp")
    public ResponseEntity<ApiResponse<List<BaoHong>>> getUrgentBaoHong() {
        return ResponseEntity.ok(ApiResponse.success(
                adminBaoHongService.getBaoHongByMucDo(BaoHong.MucDoNghiemTrong.KHAN_CAP)));
    }

    @PostMapping
    @Operation(summary = "Tạo phiếu báo hỏng mới")
    public ResponseEntity<ApiResponse<BaoHong>> createBaoHong(@RequestBody BaoHong baoHong) {
        return ResponseEntity.ok(ApiResponse.success("Tạo phiếu báo hỏng thành công",
                adminBaoHongService.createBaoHong(baoHong)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật phiếu báo hỏng")
    public ResponseEntity<ApiResponse<BaoHong>> updateBaoHong(@PathVariable Long id, @RequestBody BaoHong baoHong) {
        return ResponseEntity.ok(ApiResponse.success("Cập nhật thành công",
                adminBaoHongService.updateBaoHong(id, baoHong)));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Cập nhật trạng thái")
    public ResponseEntity<ApiResponse<BaoHong>> updateStatus(@PathVariable Long id, @RequestParam String status) {
        BaoHong.TrangThaiBaoHong trangThai = BaoHong.TrangThaiBaoHong.valueOf(status);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái thành công",
                adminBaoHongService.updateTrangThai(id, trangThai)));
    }

    /**
     * POST /api/admin/bao-hong/{id}/phan-cong
     * Body: { "nhanVienId": 3, "ghiChuAdmin": "Ưu tiên xử lý trước 5h" }
     * Strict validation: nhanVienId bắt buộc có VaiTro = NHAN_VIEN_CSVC.
     */
    @PostMapping("/{id}/phan-cong")
    @Operation(summary = "Phân công nhân viên xử lý báo hỏng",
               description = "Admin chỉ định nhân viên CSVC. Nhân viên nhận notification và cần xác nhận/từ chối.")
    public ResponseEntity<ApiResponse<BaoHong>> phanCongNhanVien(
            @PathVariable Long id,
            @RequestBody PhanCongRequest req) {
        BaoHong updated = adminBaoHongService.phanCongNhanVien(id, req);
        return ResponseEntity.ok(ApiResponse.success("Phân công thành công, đang chờ nhân viên xác nhận", updated));
    }

    /**
     * DELETE /api/admin/bao-hong/{id}/phan-cong
     * Thu hồi phân công, reset về CHUA_PHAN_CONG.
     */
    @DeleteMapping("/{id}/phan-cong")
    @Operation(summary = "Thu hồi phân công",
               description = "Admin hủy phân công và đặt lại trạng thái về CHUA_PHAN_CONG để gán người khác")
    public ResponseEntity<ApiResponse<BaoHong>> huyCongViec(@PathVariable Long id) {
        BaoHong updated = adminBaoHongService.huyCongViec(id);
        return ResponseEntity.ok(ApiResponse.success("Thu hồi phân công thành công", updated));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa báo hỏng")
    public ResponseEntity<ApiResponse<Void>> deleteBaoHong(@PathVariable Long id) {
        adminBaoHongService.deleteBaoHong(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa báo hỏng thành công", null));
    }
}
