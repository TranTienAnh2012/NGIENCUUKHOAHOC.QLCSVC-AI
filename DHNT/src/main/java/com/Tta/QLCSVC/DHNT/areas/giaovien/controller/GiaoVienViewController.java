package com.Tta.QLCSVC.DHNT.areas.giaovien.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/giao-vien")
@PreAuthorize("hasRole('GIAO_VIEN')")
public class GiaoVienViewController {

    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("title", "Giáo viên Dashboard");
        return "areas/giaovien/dashboard";
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
