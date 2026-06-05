package com.Tta.QLCSVC.DHNT.areas.nhanvien_csvc.controller;

import com.Tta.QLCSVC.DHNT.areas.nhanvien_csvc.service.CSVCBaoHongService;
import com.Tta.QLCSVC.DHNT.dto.ApiResponse;
import com.Tta.QLCSVC.DHNT.entity.BaoHong;
import com.Tta.QLCSVC.DHNT.entity.BaoTri;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/nhanvien-csvc/bao-hong")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('NHAN_VIEN_CSVC', 'ADMIN')")
@Tag(name = "CSVC - Báo hỏng", description = "API xử lý báo hỏng (CSVC)")
@SecurityRequirement(name = "bearer-jwt")
public class CSVCBaoHongController {

    private final CSVCBaoHongService csvcBaoHongService;

    @GetMapping("/pending")
    @Operation(summary = "Phiếu chờ xử lý")
    public ResponseEntity<ApiResponse<List<BaoHong>>> getPendingReports() {
        return ResponseEntity.ok(ApiResponse.success(csvcBaoHongService.getPendingReports()));
    }

    @GetMapping("/urgent")
    @Operation(summary = "Phiếu khẩn cấp")
    public ResponseEntity<ApiResponse<List<BaoHong>>> getUrgentReports() {
        return ResponseEntity.ok(ApiResponse.success(csvcBaoHongService.getUrgentReports()));
    }

    /** Lấy danh sách phiếu được phân công cho nhân viên đang login */
    @GetMapping("/assigned")
    @Operation(summary = "Phiếu được phân công cho tôi",
               description = "Trả về các phiếu Admin đã gán cho nhân viên hiện tại (CHO_XAC_NHAN hoặc DA_NHAN)")
    public ResponseEntity<ApiResponse<List<BaoHong>>> getAssignedToMe() {
        return ResponseEntity.ok(ApiResponse.success(csvcBaoHongService.getAssignedToCurrentUser()));
    }

    /** Badge count — dùng cho sidebar */
    @GetMapping("/assignment/count")
    @Operation(summary = "Số lượng phiếu chờ xác nhận")
    public ResponseEntity<ApiResponse<Long>> countPendingAcceptance() {
        return ResponseEntity.ok(ApiResponse.success(csvcBaoHongService.countPendingAcceptanceForCurrentUser()));
    }

    /**
     * POST /api/nhanvien-csvc/bao-hong/{id}/accept
     * Nhân viên XÁC NHẬN nhận việc — IDOR guard bên trong service.
     */
    @PostMapping("/{id}/accept")
    @Operation(summary = "Xác nhận nhận việc",
               description = "Nhân viên chấp nhận phiếu báo hỏng được Admin gán. " +
                             "Chỉ nhân viên được gán mới thực hiện được (IDOR protected).")
    public ResponseEntity<ApiResponse<BaoHong>> acceptAssignment(@PathVariable Long id) {
        BaoHong updated = csvcBaoHongService.acceptAssignment(id);
        return ResponseEntity.ok(ApiResponse.success("Đã xác nhận nhận việc thành công", updated));
    }

    /**
     * POST /api/nhanvien-csvc/bao-hong/{id}/reject
     * Body: { "lyDoTuChoi": "Đang bận xử lý thiết bị khác" }
     */
    @PostMapping("/{id}/reject")
    @Operation(summary = "Từ chối nhận việc",
               description = "Nhân viên từ chối phiếu. Admin sẽ nhận notification để gán người khác.")
    public ResponseEntity<ApiResponse<BaoHong>> rejectAssignment(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String lyDoTuChoi = body.getOrDefault("lyDoTuChoi", "Không có lý do cụ thể");
        BaoHong updated = csvcBaoHongService.rejectAssignment(id, lyDoTuChoi);
        return ResponseEntity.ok(ApiResponse.success("Đã từ chối phiếu, Admin sẽ gán người khác", updated));
    }

    /**
     * POST /api/nhanvien-csvc/bao-hong/{baoHongId}/ai-auto-assign/{nhanVienId}
     * Chatbot AI gọi endpoint này khi nhân viên đồng ý xử lý qua chatbot.
     * Transaction ACID: Accept + Tạo BaoTri trong 1 transaction.
     */
    @PostMapping("/{baoHongId}/ai-auto-assign/{nhanVienId}")
    @Operation(summary = "AI tự động nhận phiếu và tạo lịch bảo trì",
               description = "Được gọi bởi chatbot AI khi nhân viên xác nhận 'Có' muốn nhận deal. " +
                             "Tự động: chấp nhận phân công + tạo phiếu BaoTri trong 1 ACID transaction.")
    public ResponseEntity<ApiResponse<BaoTri>> aiAutoAssign(
            @PathVariable Long baoHongId,
            @PathVariable Long nhanVienId) {
        BaoTri baoTri = csvcBaoHongService.aiAutoAcceptAndCreateBaoTri(baoHongId, nhanVienId);
        return ResponseEntity.ok(ApiResponse.success(
            "✅ AI đã tự động tạo lịch bảo trì cho bạn!", baoTri));
    }

    @PostMapping("/{baoHongId}/ai-auto-schedule")
    @Operation(summary = "Nhân viên bấm nút AI Tự động tạo lịch bảo trì",
               description = "Tự động phân tích lỗi, accept assignment và tạo lịch bảo trì.")
    public ResponseEntity<ApiResponse<BaoTri>> aiAutoSchedule(@PathVariable Long baoHongId) {
        BaoTri result = csvcBaoHongService.aiAutoScheduleFromUI(baoHongId);
        return ResponseEntity.ok(ApiResponse.success("✅ AI đã tự động nhận việc và tạo lịch bảo trì!", result));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Cập nhật trạng thái báo hỏng")
    public ResponseEntity<ApiResponse<BaoHong>> updateStatus(
            @PathVariable Long id,
            @RequestParam String trangThai) {
        BaoHong.TrangThaiBaoHong status = BaoHong.TrangThaiBaoHong.valueOf(trangThai);
        BaoHong updated = csvcBaoHongService.updateStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái thành công", updated));
    }
}
