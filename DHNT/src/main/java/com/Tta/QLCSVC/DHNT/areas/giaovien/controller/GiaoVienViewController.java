package com.Tta.QLCSVC.DHNT.areas.giaovien.controller;

import com.Tta.QLCSVC.DHNT.areas.giaovien.service.GiaoVienThietBiService;
import com.Tta.QLCSVC.DHNT.entity.ThietBi;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/giao-vien")
@PreAuthorize("hasAnyRole('GIAO_VIEN', 'ADMIN')")
@RequiredArgsConstructor
public class GiaoVienViewController {

    private final GiaoVienThietBiService giaoVienThietBiService;

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

    @GetMapping("/muon-tra")
    public String muonTra(Model model) {
        model.addAttribute("title", "Quản lý mượn trả");
        return "areas/giaovien/muon-tra";
    }

    @GetMapping("/bao-hong")
    public String baoHong(Model model) {
        model.addAttribute("title", "Báo hỏng thiết bị");
        return "areas/giaovien/bao-hong";
    }
}
