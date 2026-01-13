package com.Tta.QLCSVC.DHNT.areas.csvc.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/csvc")
@PreAuthorize("hasRole('NHAN_VIEN_CSVC')")
public class CSVCViewController {

    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("title", "CSVC Dashboard");
        return "areas/csvc/dashboard";
    }

    @GetMapping("/bao-hong")
    public String baoHong(Model model) {
        model.addAttribute("title", "Quản lý báo hỏng");
        return "areas/csvc/bao-hong";
    }

    @GetMapping("/bao-tri")
    public String baoTri(Model model) {
        model.addAttribute("title", "Lịch trình bảo trì");
        return "areas/csvc/bao-tri";
    }
}
