package com.Tta.QLCSVC.DHNT.areas.admin.service;

import com.Tta.QLCSVC.DHNT.entity.NguoiDung;
import com.Tta.QLCSVC.DHNT.exception.DuplicateResourceException;
import com.Tta.QLCSVC.DHNT.exception.ResourceNotFoundException;
import com.Tta.QLCSVC.DHNT.repository.NguoiDungRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminNguoiDungService {

    private final NguoiDungRepository nguoiDungRepository;
    private final PasswordEncoder passwordEncoder;

    public Page<NguoiDung> getAllUsers(Pageable pageable) {
        return nguoiDungRepository.findAll(pageable);
    }

    public NguoiDung getUserById(Long id) {
        return nguoiDungRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("NguoiDung", "id", id));
    }

    public NguoiDung getUserByEmail(String email) {
        return nguoiDungRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("NguoiDung", "email", email));
    }

    @Transactional
    public NguoiDung createUser(NguoiDung nguoiDung) {
        if (nguoiDungRepository.existsByEmail(nguoiDung.getEmail())) {
            throw new DuplicateResourceException("Email đã được sử dụng");
        }
        nguoiDung.setMatKhau(passwordEncoder.encode(nguoiDung.getMatKhau()));
        return nguoiDungRepository.save(nguoiDung);
    }

    @Transactional
    public NguoiDung updateUser(Long id, NguoiDung nguoiDungDetails) {
        NguoiDung nguoiDung = getUserById(id);

        if (!nguoiDung.getEmail().equals(nguoiDungDetails.getEmail()) &&
                nguoiDungRepository.existsByEmail(nguoiDungDetails.getEmail())) {
            throw new DuplicateResourceException("Email đã được sử dụng");
        }

        nguoiDung.setHoTen(nguoiDungDetails.getHoTen());
        nguoiDung.setEmail(nguoiDungDetails.getEmail());
        nguoiDung.setSoDienThoai(nguoiDungDetails.getSoDienThoai());
        nguoiDung.setVaiTro(nguoiDungDetails.getVaiTro());
        nguoiDung.setTrangThai(nguoiDungDetails.getTrangThai());

        // Chỉ đổi mật khẩu nếu admin nhập vào (không để trống)
        String newPassword = nguoiDungDetails.getMatKhau();
        if (newPassword != null && !newPassword.isBlank()) {
            nguoiDung.setMatKhau(passwordEncoder.encode(newPassword));
        }

        return nguoiDungRepository.save(nguoiDung);
    }

    @Transactional
    public void deleteUser(Long id) {
        if (!nguoiDungRepository.existsById(id)) {
            throw new ResourceNotFoundException("NguoiDung", "id", id);
        }
        nguoiDungRepository.deleteById(id);
    }
}
