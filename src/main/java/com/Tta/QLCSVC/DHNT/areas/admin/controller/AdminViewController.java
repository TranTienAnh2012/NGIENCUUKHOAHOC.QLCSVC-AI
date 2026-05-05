package com.Tta.QLCSVC.DHNT.areas.admin.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    // Thiết bị CRUD routes
    @GetMapping("/thiet-bi")
    public String thietBiList(Model model) {
        model.addAttribute("title", "Danh sách thiết bị");
        return "areas/admin/thiet-bi/list";
    }

    @GetMapping("/thiet-bi/create")
    public String thietBiCreate(Model model) {
        model.addAttribute("title", "Thêm thiết bị mới");
        return "areas/admin/thiet-bi/create";
    }

    @GetMapping("/thiet-bi/edit/{id}")
    public String thietBiEdit(@PathVariable Long id, Model model) {
        model.addAttribute("title", "Sửa thiết bị");
        model.addAttribute("id", id);
        return "areas/admin/thiet-bi/edit";
    }

    @GetMapping("/thiet-bi/view/{id}")
    public String thietBiView(@PathVariable Long id, Model model) {
        model.addAttribute("title", "Chi tiết thiết bị");
        model.addAttribute("id", id);
        return "areas/admin/thiet-bi/view";
    }

    // Users CRUD routes
    @GetMapping("/users")
    public String usersList(Model model) {
        model.addAttribute("title", "Danh sách người dùng");
        return "areas/admin/users/list";
    }

    @GetMapping("/users/create")
    public String usersCreate(Model model) {
        model.addAttribute("title", "Thêm người dùng mới");
        return "areas/admin/users/create";
    }

    @GetMapping("/users/edit/{id}")
    public String usersEdit(@PathVariable Long id, Model model) {
        model.addAttribute("title", "Sửa người dùng");
        model.addAttribute("id", id);
        return "areas/admin/users/edit";
    }

    @GetMapping("/users/view/{id}")
    public String usersView(@PathVariable Long id, Model model) {
        model.addAttribute("title", "Chi tiết người dùng");
        model.addAttribute("id", id);
        return "areas/admin/users/view";
    }

    // Phòng học CRUD routes
    @GetMapping("/phong-hoc")
    public String phongHocList(Model model) {
        model.addAttribute("title", "Danh sách phòng học");
        return "areas/admin/phong-hoc/list";
    }

    @GetMapping("/phong-hoc/create")
    public String phongHocCreate(Model model) {
        model.addAttribute("title", "Thêm phòng học mới");
        return "areas/admin/phong-hoc/create";
    }

    @GetMapping("/phong-hoc/edit/{id}")
    public String phongHocEdit(@PathVariable Long id, Model model) {
        model.addAttribute("title", "Sửa phòng học");
        model.addAttribute("id", id);
        return "areas/admin/phong-hoc/edit";
    }

    @GetMapping("/phong-hoc/view/{id}")
    public String phongHocView(@PathVariable Long id, Model model) {
        model.addAttribute("title", "Chi tiết phòng học");
        model.addAttribute("id", id);
        return "areas/admin/phong-hoc/view";
    }

    // Loại phòng CRUD routes
    @GetMapping("/loai-phong")
    public String loaiPhongList(Model model) {
        model.addAttribute("title", "Danh sách loại phòng");
        return "areas/admin/loai-phong/list";
    }

    @GetMapping("/loai-phong/create")
    public String loaiPhongCreate(Model model) {
        model.addAttribute("title", "Thêm loại phòng mới");
        return "areas/admin/loai-phong/create";
    }

    @GetMapping("/loai-phong/edit/{id}")
    public String loaiPhongEdit(@PathVariable Long id, Model model) {
        model.addAttribute("title", "Sửa loại phòng");
        model.addAttribute("id", id);
        return "areas/admin/loai-phong/edit";
    }

    @GetMapping("/loai-phong/view/{id}")
    public String loaiPhongView(@PathVariable Long id, Model model) {
        model.addAttribute("title", "Chi tiết loại phòng");
        model.addAttribute("id", id);
        return "areas/admin/loai-phong/view";
    }

    // Loại thiết bị CRUD routes
    @GetMapping("/loai-thiet-bi")
    public String loaiThietBiList(Model model) {
        model.addAttribute("title", "Danh sách loại thiết bị");
        return "areas/admin/loai-thiet-bi/list";
    }

    @GetMapping("/loai-thiet-bi/create")
    public String loaiThietBiCreate(Model model) {
        model.addAttribute("title", "Thêm loại thiết bị mới");
        return "areas/admin/loai-thiet-bi/create";
    }

    @GetMapping("/loai-thiet-bi/edit/{id}")
    public String loaiThietBiEdit(@PathVariable Long id, Model model) {
        model.addAttribute("title", "Sửa loại thiết bị");
        model.addAttribute("id", id);
        return "areas/admin/loai-thiet-bi/edit";
    }

    @GetMapping("/loai-thiet-bi/view/{id}")
    public String loaiThietBiView(@PathVariable Long id, Model model) {
        model.addAttribute("title", "Chi tiết loại thiết bị");
        model.addAttribute("id", id);
        return "areas/admin/loai-thiet-bi/view";
    }

    // Mượn trả CRUD routes
    @GetMapping("/muon-tra")
    public String muonTraList(Model model) {
        model.addAttribute("title", "Danh sách mượn trả");
        return "areas/admin/muon-tra/list";
    }

    @GetMapping("/muon-tra/create")
    public String muonTraCreate(Model model) {
        model.addAttribute("title", "Thêm phiếu mượn mới");
        return "areas/admin/muon-tra/create";
    }

    @GetMapping("/muon-tra/edit/{id}")
    public String muonTraEdit(@PathVariable Long id, Model model) {
        model.addAttribute("title", "Sửa phiếu mượn");
        model.addAttribute("id", id);
        return "areas/admin/muon-tra/edit";
    }

    @GetMapping("/muon-tra/view/{id}")
    public String muonTraView(@PathVariable Long id, Model model) {
        model.addAttribute("title", "Chi tiết phiếu mượn");
        model.addAttribute("id", id);
        return "areas/admin/muon-tra/view";
    }

    // Báo hỏng CRUD routes
    @GetMapping("/bao-hong")
    public String baoHongList(Model model) {
        model.addAttribute("title", "Danh sách báo hỏng");
        return "areas/admin/bao-hong/list";
    }

    @GetMapping("/bao-hong/create")
    public String baoHongCreate(Model model) {
        model.addAttribute("title", "Thêm báo hỏng mới");
        return "areas/admin/bao-hong/create";
    }

    @GetMapping("/bao-hong/edit/{id}")
    public String baoHongEdit(@PathVariable Long id, Model model) {
        model.addAttribute("title", "Sửa báo hỏng");
        model.addAttribute("id", id);
        return "areas/admin/bao-hong/edit";
    }

    @GetMapping("/bao-hong/view/{id}")
    public String baoHongView(@PathVariable Long id, Model model) {
        model.addAttribute("title", "Chi tiết báo hỏng");
        model.addAttribute("id", id);
        return "areas/admin/bao-hong/view";
    }

    // Bảo trì CRUD routes
    @GetMapping("/bao-tri")
    public String baoTriList(Model model) {
        model.addAttribute("title", "Danh sách bảo trì");
        return "areas/admin/bao-tri/list";
    }

    @GetMapping("/bao-tri/create")
    public String baoTriCreate(Model model) {
        model.addAttribute("title", "Thêm phiếu bảo trì mới");
        return "areas/admin/bao-tri/create";
    }

    @GetMapping("/bao-tri/edit/{id}")
    public String baoTriEdit(@PathVariable Long id, Model model) {
        model.addAttribute("title", "Sửa phiếu bảo trì");
        model.addAttribute("id", id);
        return "areas/admin/bao-tri/edit";
    }

    @GetMapping("/bao-tri/view/{id}")
    public String baoTriView(@PathVariable Long id, Model model) {
        model.addAttribute("title", "Chi tiết phiếu bảo trì");
        model.addAttribute("id", id);
        return "areas/admin/bao-tri/view";
    }

    // IoT Monitoring routes
    @GetMapping("/iot")
    public String iotList(Model model) {
        model.addAttribute("title", "Giám sát dữ liệu cảm biến");
        return "areas/admin/iot/list";
    }

    // AI Management routes
    @GetMapping("/ai/training-data")
    public String aiTrainingData(Model model) {
        model.addAttribute("title", "Dữ liệu huấn luyện AI");
        return "areas/admin/ai/training-data";
    }

    @GetMapping("/ai/metrics")
    public String aiMetrics(Model model) {
        model.addAttribute("title", "Chỉ số Model AI");
        return "areas/admin/ai/metrics";
    }

    // Chatbot Logs routes
    @GetMapping("/chatbot-logs")
    public String chatbotLogs(Model model) {
        model.addAttribute("title", "Nhật ký hội thoại Chatbot");
        return "areas/admin/chatbot/list";
    }

    // Hình ảnh quản lý routes
    @GetMapping("/hinh-anh")
    public String hinhAnhGallery(Model model) {
        model.addAttribute("title", "Quản lý hình ảnh");
        return "areas/admin/hinh-anh/list";
    }
}
