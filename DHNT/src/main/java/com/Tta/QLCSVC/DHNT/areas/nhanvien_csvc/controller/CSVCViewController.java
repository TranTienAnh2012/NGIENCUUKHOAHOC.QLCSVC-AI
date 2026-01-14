package com.Tta.QLCSVC.DHNT.areas.nhanvien_csvc.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/nhanvien-csvc")
@PreAuthorize("hasRole('NHAN_VIEN_CSVC')")
public class CSVCViewController {

    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("title", "Dashboard Nhân viên CSVC");
        return "areas/nhanvien_csvc/dashboard";
    }

    @GetMapping("/bao-hong")
    public String baoHong(Model model) {
        model.addAttribute("title", "Quản lý báo hỏng");
        return "areas/nhanvien_csvc/bao-hong";
    }

    @GetMapping("/bao-tri")
    public String baoTri(Model model) {
        model.addAttribute("title", "Lịch trình bảo trì");
        return "areas/nhanvien_csvc/bao-tri";
    }
}
