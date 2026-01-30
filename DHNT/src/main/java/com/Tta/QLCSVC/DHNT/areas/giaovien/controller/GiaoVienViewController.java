package com.Tta.QLCSVC.DHNT.areas.giaovien.controller;

import com.Tta.QLCSVC.DHNT.areas.giaovien.service.GiaoVienBaoHongService;
import com.Tta.QLCSVC.DHNT.areas.giaovien.service.GiaoVienMuonTraService;
import com.Tta.QLCSVC.DHNT.areas.giaovien.service.GiaoVienProfileService;
import com.Tta.QLCSVC.DHNT.areas.giaovien.service.GiaoVienThietBiService;
import com.Tta.QLCSVC.DHNT.entity.BaoHong;
import com.Tta.QLCSVC.DHNT.entity.MuonTraThietBi;
import com.Tta.QLCSVC.DHNT.entity.NguoiDung;
import com.Tta.QLCSVC.DHNT.entity.ThietBi;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/giao-vien")
@PreAuthorize("hasAnyRole('GIAO_VIEN', 'ADMIN')")
@RequiredArgsConstructor
public class GiaoVienViewController {

    private final GiaoVienThietBiService giaoVienThietBiService;
    private final GiaoVienProfileService giaoVienProfileService;
    private final GiaoVienBaoHongService giaoVienBaoHongService;
    private final GiaoVienMuonTraService giaoVienMuonTraService;

    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("title", "Giáo viên Dashboard");
        return "areas/giaovien/dashboard";
    }

    @GetMapping("/thiet-bi")
    public String danhSachThietBi(Model model) {
        model.addAttribute("title", "Danh sách thiết bị");
        List<ThietBi> thietBis = giaoVienThietBiService.getAllAvailableThietBi();
        model.addAttribute("thietBis", thietBis);
        return "areas/giaovien/DSThietBi/list";
    }

    @GetMapping("/profile")
    public String profile(Model model) {
        model.addAttribute("title", "Thông tin cá nhân");
        NguoiDung user = giaoVienProfileService.getCurrentUserProfile();
        model.addAttribute("user", user);
        return "areas/giaovien/profile";
    }

    @PostMapping("/profile/update")
    public String updateProfile(@RequestParam String hoTen,
            @RequestParam String soDienThoai,
            RedirectAttributes redirectAttributes) {
        try {
            giaoVienProfileService.updateProfile(hoTen, soDienThoai);
            redirectAttributes.addFlashAttribute("success", "Cập nhật thông tin thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra: " + e.getMessage());
        }
        return "redirect:/giao-vien/profile";
    }

    @PostMapping("/profile/change-password")
    public String changePassword(@RequestParam String oldPassword,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            RedirectAttributes redirectAttributes) {
        try {
            if (!newPassword.equals(confirmPassword)) {
                redirectAttributes.addFlashAttribute("error", "Mật khẩu mới không khớp!");
                return "redirect:/giao-vien/profile";
            }
            giaoVienProfileService.changePassword(oldPassword, newPassword);
            redirectAttributes.addFlashAttribute("success", "Đổi mật khẩu thành công!");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra: " + e.getMessage());
        }
        return "redirect:/giao-vien/profile";
    }

    @GetMapping({ "/muon-tra", "/lich-su" })
    public String muonTra(Model model) {
        model.addAttribute("title", "Quản lý mượn trả");
        List<MuonTraThietBi> muonTras = giaoVienMuonTraService.getMyBorrowings();
        model.addAttribute("muonTras", muonTras);
        return "areas/giaovien/muon-tra";
    }

    @GetMapping("/muon-tra/create")
    public String createMuonTraForm(Model model) {
        model.addAttribute("title", "Mượn thiết bị mới");
        List<ThietBi> thietBis = giaoVienThietBiService.getAllAvailableThietBi();
        model.addAttribute("thietBis", thietBis);
        return "areas/giaovien/muon-tra-create";
    }

    @PostMapping("/muon-tra/create")
    public String createMuonTra(@RequestParam Long thietBiId,
            @RequestParam String ngayTraDuKien,
            @RequestParam(required = false) String ghiChu,
            RedirectAttributes redirectAttributes) {
        try {
            java.time.LocalDateTime ngayTra = java.time.LocalDate.parse(ngayTraDuKien).atStartOfDay();
            MuonTraThietBi muonTra = giaoVienMuonTraService.borrowEquipment(thietBiId, ngayTra);
            if (ghiChu != null && !ghiChu.trim().isEmpty()) {
                muonTra.setGhiChu(ghiChu);
            }
            redirectAttributes.addFlashAttribute("success", "Mượn thiết bị thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra: " + e.getMessage());
            return "redirect:/giao-vien/muon-tra/create";
        }
        return "redirect:/giao-vien/muon-tra";
    }

    @PostMapping("/muon-tra/return/{id}")
    public String returnEquipment(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            giaoVienMuonTraService.returnEquipment(id);
            redirectAttributes.addFlashAttribute("success", "Trả thiết bị thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra: " + e.getMessage());
        }
        return "redirect:/giao-vien/muon-tra";
    }

    @GetMapping("/bao-hong")
    public String baoHong(Model model) {
        model.addAttribute("title", "Báo hỏng thiết bị");
        List<BaoHong> baoHongs = giaoVienBaoHongService.getMyReports();
        model.addAttribute("baoHongs", baoHongs);
        return "areas/giaovien/bao-hong";
    }

    @GetMapping("/bao-hong/create")
    public String createBaoHongForm(Model model) {
        model.addAttribute("title", "Báo hỏng mới");
        List<ThietBi> thietBis = giaoVienThietBiService.getAllAvailableThietBi();
        model.addAttribute("thietBis", thietBis);
        return "areas/giaovien/bao-hong-create";
    }

    @PostMapping("/bao-hong/create")
    public String createBaoHong(@RequestParam Long thietBiId,
            @RequestParam String moTaLoi,
            @RequestParam(required = false) String mucDoNghiemTrong,
            RedirectAttributes redirectAttributes) {
        try {
            BaoHong.MucDoNghiemTrong mucDo = mucDoNghiemTrong != null
                    ? BaoHong.MucDoNghiemTrong.valueOf(mucDoNghiemTrong)
                    : BaoHong.MucDoNghiemTrong.TRUNG_BINH;

            giaoVienBaoHongService.reportDamage(thietBiId, moTaLoi, mucDo);
            redirectAttributes.addFlashAttribute("success", "Báo hỏng thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra: " + e.getMessage());
            return "redirect:/giao-vien/bao-hong/create";
        }
        return "redirect:/giao-vien/bao-hong";
    }
}
