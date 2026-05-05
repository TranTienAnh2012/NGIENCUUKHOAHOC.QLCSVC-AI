package com.Tta.QLCSVC.DHNT.areas.giaovien.service;

import com.Tta.QLCSVC.DHNT.entity.NguoiDung;
import com.Tta.QLCSVC.DHNT.exception.ResourceNotFoundException;
import com.Tta.QLCSVC.DHNT.repository.NguoiDungRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GiaoVienProfileService {

    private final NguoiDungRepository nguoiDungRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Lấy thông tin user hiện tại từ SecurityContext
     */
    @Transactional(readOnly = true)
    public NguoiDung getCurrentUserProfile() {
        String email = getCurrentUserEmail();
        return nguoiDungRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("NguoiDung", "email", email));
    }

    /**
     * Cập nhật thông tin cá nhân
     */
    @Transactional
    public NguoiDung updateProfile(String hoTen, String soDienThoai) {
        NguoiDung user = getCurrentUserProfile();

        user.setHoTen(hoTen);
        user.setSoDienThoai(soDienThoai);

        return nguoiDungRepository.save(user);
    }

    /**
     * Đổi mật khẩu
     */
    @Transactional
    public void changePassword(String oldPassword, String newPassword) {
        NguoiDung user = getCurrentUserProfile();

        // Kiểm tra mật khẩu cũ
        if (!passwordEncoder.matches(oldPassword, user.getMatKhau())) {
            throw new IllegalArgumentException("Mật khẩu cũ không đúng");
        }

        // Cập nhật mật khẩu mới
        user.setMatKhau(passwordEncoder.encode(newPassword));
        nguoiDungRepository.save(user);
    }

    /**
     * Lấy email của user hiện tại từ SecurityContext
     */
    private String getCurrentUserEmail() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserDetails) {
            return ((UserDetails) principal).getUsername();
        }
        return principal.toString();
    }
}
