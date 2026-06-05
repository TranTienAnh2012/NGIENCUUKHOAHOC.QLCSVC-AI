package com.Tta.QLCSVC.DHNT.areas.nhanvien_csvc.controller;

import com.Tta.QLCSVC.DHNT.areas.nhanvien_csvc.service.CSVCBaoHongService;
import com.Tta.QLCSVC.DHNT.areas.nhanvien_csvc.service.CSVCBaoTriService;
import com.Tta.QLCSVC.DHNT.areas.nhanvien_csvc.service.CSVCPhongHocService;
import com.Tta.QLCSVC.DHNT.areas.nhanvien_csvc.service.CSVCThietBiService;
import com.Tta.QLCSVC.DHNT.areas.nhanvien_csvc.service.CSVCThongKeService;
import com.Tta.QLCSVC.DHNT.entity.BaoHong;
import com.Tta.QLCSVC.DHNT.entity.BaoTri;
import com.Tta.QLCSVC.DHNT.entity.ThietBi;
import com.Tta.QLCSVC.DHNT.entity.PhongHoc;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/nhanvien-csvc")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('NHAN_VIEN_CSVC', 'ADMIN')")
public class CSVCViewController {

    private final CSVCBaoHongService csvcBaoHongService;
    private final CSVCBaoTriService csvcBaoTriService;
    private final CSVCThietBiService csvcThietBiService;
    private final CSVCPhongHocService csvcPhongHocService;
    private final CSVCThongKeService csvcThongKeService;

    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("title", "Dashboard Nhân viên CSVC");
        
        // Thống kê chung
        Map<String, Object> stats = csvcThongKeService.getThongKeTongQuan();
        model.addAttribute("stats", stats);
        
        // Công việc được phân công chờ nhận (CHO_XAC_NHAN)
        List<BaoHong> assignedTasks = csvcBaoHongService.getPendingAcceptanceForCurrentUser();
        model.addAttribute("assignedTasks", assignedTasks);
        
        // Công việc chưa ai nhận (để hiển thị gợi ý)
        List<BaoHong> unassignedTasks = csvcBaoHongService.getPendingReports();
        // Lọc lấy những task CHUA_PHAN_CONG (hoặc null)
        unassignedTasks.removeIf(t -> t.getTrangThaiPhanCong() != null && t.getTrangThaiPhanCong() != BaoHong.TrangThaiPhanCong.CHUA_PHAN_CONG);
        model.addAttribute("unassignedTasks", unassignedTasks.size() > 5 ? unassignedTasks.subList(0, 5) : unassignedTasks);
        
        // Lấy list bảo trì của user hiện tại (để AI đánh giá)
        List<BaoTri> myBaoTris = csvcBaoTriService.getAllBaoTri().stream()
            .filter(b -> b.getNguoiThucHien() != null && b.getNguoiThucHien().getEmail().equals(org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName()))
            .toList();
        model.addAttribute("myBaoTrisCount", myBaoTris.size());
        
        return "areas/nhanvien_csvc/dashboard";
    }

    // ==================== Báo Hỏng ====================

    @GetMapping("/bao-hong")
    public String baoHongList(Model model) {
        model.addAttribute("title", "Quản lý báo hỏng");
        List<BaoHong> baoHongs = csvcBaoHongService.getAllBaoHong();
        model.addAttribute("baoHongs", baoHongs);
        return "areas/nhanvien_csvc/bao-hong/list";
    }

    @GetMapping("/bao-hong/{id}")
    public String baoHongDetail(@PathVariable Long id, Model model) {
        model.addAttribute("title", "Chi tiết báo hỏng");
        BaoHong baoHong = csvcBaoHongService.getBaoHongById(id);
        model.addAttribute("baoHong", baoHong);
        return "areas/nhanvien_csvc/bao-hong/detail";
    }

    // ==================== Bảo Trì ====================

    @GetMapping("/bao-tri")
    public String baoTri(Model model) {
        model.addAttribute("title", "Lịch trình bảo trì");
        List<BaoTri> baoTris = csvcBaoTriService.getAllBaoTri();
        model.addAttribute("baoTris", baoTris);
        return "areas/nhanvien_csvc/bao-tri/list";
    }

    @GetMapping("/bao-tri/{id}")
    public String baoTriDetail(@PathVariable Long id, Model model) {
        model.addAttribute("title", "Chi tiết bảo trì");
        BaoTri baoTri = csvcBaoTriService.getBaoTriById(id);
        model.addAttribute("baoTri", baoTri);
        return "areas/nhanvien_csvc/bao-tri/detail";
    }

    @GetMapping("/bao-tri/create")
    public String createBaoTriForm(@RequestParam(required = false) Long baoHongId, Model model) {
        model.addAttribute("title", "Tạo lịch bảo trì");
        if (baoHongId != null) {
            BaoHong baoHong = csvcBaoHongService.getBaoHongById(baoHongId);
            model.addAttribute("baoHong", baoHong);
        }
        return "areas/nhanvien_csvc/bao-tri/create";
    }

    @PostMapping("/bao-tri/create")
    public String createBaoTri(
            @RequestParam(required = false) Long baoHongId,
            @RequestParam Long thietBiId,
            @RequestParam String loaiBaoTri,
            @RequestParam String ngayBaoTri,
            @RequestParam String noiDung,
            @RequestParam(required = false) BigDecimal chiPhi,
            RedirectAttributes redirectAttributes) {
        try {
            BaoTri baoTri = new BaoTri();
            baoTri.setLoaiBaoTri(BaoTri.LoaiBaoTri.valueOf(loaiBaoTri));
            baoTri.setNgayBaoTri(LocalDate.parse(ngayBaoTri));
            baoTri.setNoiDung(noiDung);
            // nguoiThucHien giờ là FK NguoiDung, service sẽ tự set từ SecurityContext
            baoTri.setChiPhi(chiPhi);

            if (baoHongId != null) {
                csvcBaoTriService.taoLichBaoTriTuBaoHong(baoHongId, baoTri);
            } else {
                csvcBaoTriService.taoLichBaoTri(thietBiId, baoTri);
            }

            redirectAttributes.addFlashAttribute("success", "Tạo lịch bảo trì thành công!");
            return "redirect:/nhanvien-csvc/bao-tri";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra: " + e.getMessage());
            return "redirect:/nhanvien-csvc/bao-tri/create" + (baoHongId != null ? "?baoHongId=" + baoHongId : "");
        }
    }

    // ==================== Thiết Bị ====================

    @GetMapping("/thiet-bi")
    public String thietBiList(Model model) {
        model.addAttribute("title", "Quản lý thiết bị");
        List<ThietBi> thietBis = csvcThietBiService.getAllThietBi();
        model.addAttribute("thietBis", thietBis);
        return "areas/nhanvien_csvc/thiet-bi";
    }

    @GetMapping("/thiet-bi/{id}")
    public String thietBiDetail(@PathVariable Long id, Model model) {
        model.addAttribute("title", "Chi tiết thiết bị");
        ThietBi thietBi = csvcThietBiService.getThietBiById(id);
        model.addAttribute("thietBi", thietBi);
        return "areas/nhanvien_csvc/thiet-bi/detail";
    }

    // ==================== Phòng Học ====================

    @GetMapping("/phong-hoc")
    public String phongHocList(Model model) {
        model.addAttribute("title", "Quản lý phòng học");
        List<PhongHoc> phongHocs = csvcPhongHocService.getAllPhongHocWithThietBiStats();
        model.addAttribute("phongHocs", phongHocs);
        return "areas/nhanvien_csvc/phong-hoc";
    }

    // ==================== Thống Kê ====================

    @GetMapping("/thong-ke")
    public String thongKe(Model model) {
        model.addAttribute("title", "Thống kê & Báo cáo");
        Map<String, Object> stats = csvcThongKeService.getThongKeTongQuan();
        model.addAttribute("stats", stats);
        return "areas/nhanvien_csvc/thong-ke";
    }
}

