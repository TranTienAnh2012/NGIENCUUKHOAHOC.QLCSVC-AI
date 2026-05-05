package com.Tta.QLCSVC.DHNT.api;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Custom Error Controller để thay thế Spring Whitelabel Error Page.
 * Hiển thị trang lỗi thân thiện cho người dùng khi gặp 403/404/500.
 */
@Controller
public class CustomErrorController implements ErrorController {

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        Object statusCode = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);

        if (statusCode != null) {
            int code = Integer.parseInt(statusCode.toString());

            if (code == HttpStatus.FORBIDDEN.value()) { // 403
                model.addAttribute("errorCode", "403");
                model.addAttribute("errorTitle", "Truy cập bị từ chối");
                model.addAttribute("errorMessage",
                        "Bạn không có quyền truy cập trang này. " +
                                "Vui lòng liên hệ quản trị viên nếu bạn cho rằng đây là nhầm lẫn.");
                model.addAttribute("errorEmoji", "🔒");
                return "error/403";
            }

            if (code == HttpStatus.NOT_FOUND.value()) { // 404
                model.addAttribute("errorCode", "404");
                model.addAttribute("errorTitle", "Trang không tìm thấy");
                model.addAttribute("errorMessage",
                        "Trang bạn đang tìm kiếm không tồn tại hoặc đã bị di chuyển.");
                model.addAttribute("errorEmoji", "🔍");
                return "error/404";
            }

            if (code == HttpStatus.INTERNAL_SERVER_ERROR.value()) { // 500
                model.addAttribute("errorCode", "500");
                model.addAttribute("errorTitle", "Lỗi máy chủ");
                model.addAttribute("errorMessage",
                        "Hệ thống gặp sự cố. Vui lòng thử lại sau hoặc liên hệ quản trị viên.");
                model.addAttribute("errorEmoji", "⚙️");
                return "error/error";
            }
        }

        // Generic error fallback
        model.addAttribute("errorCode", statusCode != null ? statusCode.toString() : "Lỗi");
        model.addAttribute("errorTitle", "Đã xảy ra lỗi");
        model.addAttribute("errorMessage", "Hệ thống gặp sự cố không mong muốn. Vui lòng thử lại.");
        model.addAttribute("errorEmoji", "⚠️");
        return "error/error";
    }
}
