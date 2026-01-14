package com.Tta.QLCSVC.DHNT.areas.nhanvien_csvc.controller;

import com.Tta.QLCSVC.DHNT.areas.nhanvien_csvc.service.CSVCBaoHongService;
import com.Tta.QLCSVC.DHNT.dto.ApiResponse;
import com.Tta.QLCSVC.DHNT.entity.BaoHong;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/nhanvien-csvc/bao-hong")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('NHAN_VIEN_CSVC', 'ADMIN')")
@Tag(name = "CSVC - Báo hỏng", description = "API xử lý báo hỏng (CSVC)")
@SecurityRequirement(name = "bearer-jwt")
public class CSVCBaoHongController {

    private final CSVCBaoHongService csvcBaoHongService;

    @GetMapping("/pending")
    @Operation(summary = "Phiếu chờ xử lý", description = "Lấy danh sách phiếu báo hỏng chờ xử lý")
    public ResponseEntity<ApiResponse<List<BaoHong>>> getPendingReports() {
        List<BaoHong> reports = csvcBaoHongService.getPendingReports();
        return ResponseEntity.ok(ApiResponse.success(reports));
    }

    @GetMapping("/urgent")
    @Operation(summary = "Phiếu khẩn cấp", description = "Lấy danh sách phiếu báo hỏng khẩn cấp")
    public ResponseEntity<ApiResponse<List<BaoHong>>> getUrgentReports() {
        List<BaoHong> reports = csvcBaoHongService.getUrgentReports();
        return ResponseEntity.ok(ApiResponse.success(reports));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Cập nhật trạng thái", description = "Cập nhật trạng thái phiếu báo hỏng")
    public ResponseEntity<ApiResponse<BaoHong>> updateStatus(
            @PathVariable Long id,
            @RequestParam String trangThai) {
        BaoHong.TrangThaiBaoHong status = BaoHong.TrangThaiBaoHong.valueOf(trangThai);
        BaoHong updated = csvcBaoHongService.updateStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái thành công", updated));
    }
}
