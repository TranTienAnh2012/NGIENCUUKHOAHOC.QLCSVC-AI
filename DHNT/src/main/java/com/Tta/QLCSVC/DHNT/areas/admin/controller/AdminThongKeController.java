package com.Tta.QLCSVC.DHNT.areas.admin.controller;

import com.Tta.QLCSVC.DHNT.areas.admin.service.AdminThongKeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Map;

@Controller
@RequestMapping("/admin/thong-ke")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminThongKeController {

    private final AdminThongKeService adminThongKeService;

    @GetMapping
    public String thongKe(Model model) {
        model.addAttribute("title", "Thống kê & Báo cáo");
        Map<String, Object> stats = adminThongKeService.getThongKeTongQuan();
        model.addAttribute("stats", stats);
        return "areas/admin/thong-ke";
    }
}
