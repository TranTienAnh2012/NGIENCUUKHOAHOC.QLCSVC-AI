package com.Tta.QLCSVC.DHNT.areas.giaovien.controller;

import com.Tta.QLCSVC.DHNT.areas.giaovien.service.GiaoVienBaoHongService;
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
@RequestMapping("/api/giao-vien/bao-hong")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('GIAO_VIEN', 'ADMIN')")
@Tag(name = "Giáo viên - Báo hỏng", description = "API báo hỏng thiết bị (Giáo viên)")
@SecurityRequirement(name = "bearer-jwt")
public class GiaoVienBaoHongController {

    private final GiaoVienBaoHongService giaoVienBaoHongService;

    @GetMapping("/my-reports")
    @Operation(summary = "Phiếu báo hỏng của tôi", description = "Xem danh sách phiếu báo hỏng")
    public ResponseEntity<ApiResponse<List<BaoHong>>> getMyReports() {
        List<BaoHong> reports = giaoVienBaoHongService.getMyReports();
        return ResponseEntity.ok(ApiResponse.success(reports));
    }

    @PostMapping
    @Operation(summary = "Báo hỏng thiết bị", description = "Tạo phiếu báo hỏng thiết bị")
    public ResponseEntity<ApiResponse<BaoHong>> reportDamage(
            @RequestParam Long thietBiId,
            @RequestParam String moTa,
            @RequestParam(required = false) String mucDoNghiemTrong) {

        BaoHong.MucDoNghiemTrong mucDo = mucDoNghiemTrong != null
                ? BaoHong.MucDoNghiemTrong.valueOf(mucDoNghiemTrong)
                : BaoHong.MucDoNghiemTrong.TRUNG_BINH;

        BaoHong baoHong = giaoVienBaoHongService.reportDamage(thietBiId, moTa, mucDo);
        return ResponseEntity.ok(ApiResponse.success("Báo hỏng thành công", baoHong));
    }
}
