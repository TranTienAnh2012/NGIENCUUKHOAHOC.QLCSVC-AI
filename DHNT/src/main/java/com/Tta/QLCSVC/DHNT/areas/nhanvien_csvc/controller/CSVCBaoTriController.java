package com.Tta.QLCSVC.DHNT.areas.nhanvien_csvc.controller;

import com.Tta.QLCSVC.DHNT.areas.nhanvien_csvc.service.CSVCBaoTriService;
import com.Tta.QLCSVC.DHNT.dto.ApiResponse;
import com.Tta.QLCSVC.DHNT.entity.BaoTri;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/nhanvien-csvc/bao-tri")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('NHAN_VIEN_CSVC', 'ADMIN')")
@Tag(name = "CSVC - Bảo trì", description = "API quản lý bảo trì thiết bị (CSVC)")
@SecurityRequirement(name = "bearer-jwt")
public class CSVCBaoTriController {

    private final CSVCBaoTriService baoTriService;

    @GetMapping
    @Operation(summary = "Lấy danh sách tất cả phiếu bảo trì", description = "Lấy toàn bộ lịch sử và kế hoạch bảo trì")
    public ResponseEntity<ApiResponse<List<BaoTri>>> getAllBaoTri() {
        return ResponseEntity.ok(ApiResponse.success(baoTriService.getAllBaoTri()));
    }

    @PostMapping("/thiet-bi/{thietBiId}")
    @Operation(summary = "Tạo phiếu bảo trì", description = "Lập kế hoạch hoặc ghi nhận bảo trì cho thiết bị")
    public ResponseEntity<ApiResponse<BaoTri>> taoLichBaoTri(
            @PathVariable Long thietBiId,
            @RequestBody BaoTri baoTri) {
        BaoTri result = baoTriService.taoLichBaoTri(thietBiId, baoTri);
        return ResponseEntity.ok(ApiResponse.success("Tạo phiếu bảo trì thành công", result));
    }

    @PostMapping("/sync-trang-thai")
    @Operation(summary = "Đồng bộ trạng thái thiết bị theo kết quả bảo trì",
               description = "Quét toàn bộ phiếu bảo trì và cập nhật trạng thái thiết bị cho khớp. " +
                             "Dùng để sửa dữ liệu cũ bị không nhất quán.")
    public ResponseEntity<ApiResponse<String>> syncTrangThai() {
        int fixedCount = baoTriService.syncThietBiTrangThai();
        return ResponseEntity.ok(ApiResponse.success(
            "✅ Đã đồng bộ " + fixedCount + " thiết bị về đúng trạng thái", null));
    }

    @PutMapping("/{id}/hoan-thanh")
    @Operation(summary = "Hoàn thành bảo trì", description = "Cập nhật kết quả và chi phí sau khi bảo trì xong")
    public ResponseEntity<ApiResponse<BaoTri>> hoanThanhBaoTri(
            @PathVariable Long id,
            @RequestParam String ketQua,
            @RequestParam BigDecimal chiPhi,
            @RequestParam(required = false) String ghiChu) {
        BaoTri.KetQuaBaoTri resultEnum = BaoTri.KetQuaBaoTri.valueOf(ketQua);
        BaoTri result = baoTriService.hoanThanhBaoTri(id, resultEnum, chiPhi, ghiChu);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật hoàn thành bảo trì thành công", result));
    }

    @PutMapping("/{id}/cap-nhat-tien-do")
    @Operation(summary = "Cập nhật tiến độ bảo trì", description = "Cập nhật ghi chú và chi phí phát sinh mà chưa kết thúc lịch")
    public ResponseEntity<ApiResponse<BaoTri>> capNhatTienDo(
            @PathVariable Long id,
            @RequestParam(required = false) BigDecimal chiPhiThem,
            @RequestParam(required = false) String ghiChu) {
        BaoTri result = baoTriService.capNhatTienDo(id, chiPhiThem, ghiChu);
        return ResponseEntity.ok(ApiResponse.success("Đã ghi nhận tiến độ bảo trì", result));
    }
}
