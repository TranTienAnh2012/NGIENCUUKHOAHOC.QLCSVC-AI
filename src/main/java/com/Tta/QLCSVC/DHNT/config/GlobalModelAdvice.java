package com.Tta.QLCSVC.DHNT.config;

import com.Tta.QLCSVC.DHNT.entity.NguoiDung;
import com.Tta.QLCSVC.DHNT.repository.NguoiDungRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalModelAdvice {

    private final NguoiDungRepository nguoiDungRepository;

    /**
     * Inject currentUser vào mọi Model (cho Thymeleaf dùng trực tiếp).
     * Nếu chưa đăng nhập hoặc lỗi → trả null.
     */
    @ModelAttribute("currentUser")
    public NguoiDung currentUser() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()
                    || "anonymousUser".equals(auth.getPrincipal())) {
                return null;
            }
            return nguoiDungRepository.findByEmail(auth.getName()).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }
}
