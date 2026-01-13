package com.Tta.QLCSVC.DHNT.areas.admin.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminViewController {

    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("title", "Admin Dashboard");
        return "areas/admin/dashboard";
    }

    @GetMapping("/users")
    public String users(Model model) {
        model.addAttribute("title", "Quản lý người dùng");
        return "areas/admin/users";
    }

    @GetMapping("/thiet-bi")
    public String thietBi(Model model) {
        model.addAttribute("title", "Quản lý thiết bị");
        return "areas/admin/thiet-bi";
    }

    @GetMapping("/phong-hoc")
    public String phongHoc(Model model) {
        model.addAttribute("title", "Quản lý phòng học");
        return "areas/admin/phong-hoc";
    }
}
