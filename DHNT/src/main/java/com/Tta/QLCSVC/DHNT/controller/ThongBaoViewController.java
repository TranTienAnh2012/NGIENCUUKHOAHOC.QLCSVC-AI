package com.Tta.QLCSVC.DHNT.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@PreAuthorize("isAuthenticated()")
public class ThongBaoViewController {

    @GetMapping("/thong-bao")
    public String index(Model model, org.springframework.security.core.Authentication auth) {
        model.addAttribute("title", "Trung tâm Thông báo AI");
        String layoutName = "layout/main";
        if (auth != null) {
            boolean isGiaoVien = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_GIAO_VIEN"));
            boolean isNhanVien = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_NHAN_VIEN_CSVC"));
            if (isGiaoVien) layoutName = "layout/giaovien-layout";
            else if (isNhanVien) layoutName = "layout/nhanvien-layout";
        }
        model.addAttribute("layoutName", layoutName);
        return "thong-bao/index";
    }
}
