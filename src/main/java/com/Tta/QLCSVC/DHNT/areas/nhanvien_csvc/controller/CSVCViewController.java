package com.Tta.QLCSVC.DHNT.areas.nhanvien_csvc.controller;

import com.Tta.QLCSVC.DHNT.areas.nhanvien_csvc.service.CSVCBaoHongService;
import com.Tta.QLCSVC.DHNT.areas.nhanvien_csvc.service.CSVCBaoTriService;
import com.Tta.QLCSVC.DHNT.entity.BaoHong;
import com.Tta.QLCSVC.DHNT.entity.BaoTri;
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
import com.Tta.QLCSVC.DHNT.areas.nhanvien_csvc.service.CSVCThongKeService;
import com.Tta.QLCSVC.DHNT.areas.nhanvien_csvc.service.CSVCThietBiService;
import com.Tta.QLCSVC.DHNT.areas.nhanvien_csvc.service.CSVCPhongHocService;
import com.Tta.QLCSVC.DHNT.entity.ThietBi;
import com.Tta.QLCSVC.DHNT.entity.PhongHoc;

@Controller
@RequestMapping("/nhanvien-csvc")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('NHAN_VIEN_CSVC', 'ADMIN')")
public class CSVCViewController {

    private final CSVCBaoHongService csvcBaoHongService;
    private final CSVCBaoTriService csvcBaoTriService;
    private final CSVCThongKeService csvcThongKeService;
    private final CSVCThietBiService csvcThietBiService;
    private final CSVCPhongHocService csvcPhongHocService;

    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("title", "Dashboard Nhân viên CSVC");
        return "areas/nhanvien_csvc/dashboard";
    }

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

    @GetMapping("/bao-tri/create")
    public String createBaoTriForm(@RequestParam(required = false) Long baoHongId, Model model) {
        model.addAttribute("title", "Tạo lịch bảo trì");
        if (baoHongId != null) {
            BaoHong baoHong = csvcBaoHongService.getBaoHongById(baoHongId);
            model.addAttribute("baoHong", baoHong);
        } else {
            List<ThietBi> thietBis = csvcThietBiService.getAllThietBiWithDetails();
            model.addAttribute("thietBis", thietBis);
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
            @RequestParam(required = false) String nguoiThucHien,
            @RequestParam(required = false) BigDecimal chiPhi,
            RedirectAttributes redirectAttributes) {
        try {
            BaoTri baoTri = new BaoTri();
            baoTri.setLoaiBaoTri(BaoTri.LoaiBaoTri.valueOf(loaiBaoTri));
            baoTri.setNgayBaoTri(LocalDate.parse(ngayBaoTri));
            baoTri.setNoiDung(noiDung);
            baoTri.setNguoiThucHien(nguoiThucHien);
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

    @GetMapping("/bao-tri")
    public String baoTri(Model model) {
        model.addAttribute("title", "Lịch trình bảo trì");
        List<BaoTri> baoTris = csvcBaoTriService.getAllBaoTri();
        model.addAttribute("baoTris", baoTris);
        return "areas/nhanvien_csvc/bao-tri/list";
    }

    @PostMapping("/bao-tri/update")
    public String updateBaoTriProgress(
            @RequestParam Long baoTriId,
            @RequestParam(required = false) String ketQua,
            @RequestParam(required = false) String ghiChuThem,
            @RequestParam(required = false) BigDecimal chiPhi,
            RedirectAttributes redirectAttributes) {
        try {
            BaoTri.KetQuaBaoTri ketQuaEnum = null;
            if (ketQua != null && !ketQua.isEmpty()) {
                ketQuaEnum = BaoTri.KetQuaBaoTri.valueOf(ketQua);
            }
            csvcBaoTriService.updateTienDoBaoTri(baoTriId, ketQuaEnum, ghiChuThem, chiPhi);
            redirectAttributes.addFlashAttribute("success", "Cập nhật tiến độ thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra: " + e.getMessage());
        }
        return "redirect:/nhanvien-csvc/bao-tri";
    }

    @GetMapping("/bao-tri/{id}")
    public String baoTriDetail(@PathVariable Long id, Model model) {
        model.addAttribute("title", "Chi tiết bảo trì");
        BaoTri baoTri = csvcBaoTriService.getBaoTriById(id);
        model.addAttribute("baoTri", baoTri);
        return "areas/nhanvien_csvc/bao-tri/detail";
    }

    @GetMapping("/thiet-bi")
    public String thietBi(Model model) {
        model.addAttribute("title", "Quản lý thiết bị");
        List<ThietBi> thietBis = csvcThietBiService.getAllThietBiWithDetails();
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

    @GetMapping("/phong-hoc")
    public String phongHoc(Model model) {
        model.addAttribute("title", "Quản lý phòng học");
        List<PhongHoc> phongHocs = csvcPhongHocService.getAllPhongHocWithThietBiStats();
        model.addAttribute("phongHocs", phongHocs);
        return "areas/nhanvien_csvc/phong-hoc";
    }

    @GetMapping("/thong-ke")
    public String thongKe(Model model) {
        model.addAttribute("title", "Thống kê & Báo cáo");
        Map<String, Object> stats = csvcThongKeService.getThongKeTongQuan();
        model.addAttribute("stats", stats);
        return "areas/nhanvien_csvc/thong-ke";
    }

    @GetMapping("/profile")
    public String profile(Model model) {
        model.addAttribute("title", "Thông tin cá nhân");
        return "areas/nhanvien_csvc/profile";
    }
}
