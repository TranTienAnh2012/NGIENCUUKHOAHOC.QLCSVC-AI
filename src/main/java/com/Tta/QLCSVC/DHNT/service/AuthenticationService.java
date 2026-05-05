package com.Tta.QLCSVC.DHNT.service;

import com.Tta.QLCSVC.DHNT.dto.auth.LoginRequest;
import com.Tta.QLCSVC.DHNT.dto.auth.LoginResponse;
import com.Tta.QLCSVC.DHNT.dto.auth.RegisterRequest;
import com.Tta.QLCSVC.DHNT.entity.NguoiDung;
import com.Tta.QLCSVC.DHNT.exception.DuplicateResourceException;
import com.Tta.QLCSVC.DHNT.repository.NguoiDungRepository;
import com.Tta.QLCSVC.DHNT.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final NguoiDungRepository nguoiDungRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public LoginResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String token = jwtTokenProvider.generateToken(userDetails);
        String refreshToken = jwtTokenProvider.generateRefreshToken(userDetails);

        NguoiDung nguoiDung = nguoiDungRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        return new LoginResponse(token, refreshToken, nguoiDung);
    }

    @Transactional
    public LoginResponse register(RegisterRequest request) {
        if (nguoiDungRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email đã được sử dụng");
        }

        // Security: Prevent self-registration as ADMIN
        if ("ADMIN".equals(request.getVaiTro())) {
            throw new IllegalArgumentException("Không thể tự đăng ký với quyền Admin");
        }

        NguoiDung nguoiDung = new NguoiDung();
        nguoiDung.setHoTen(request.getHoTen());
        nguoiDung.setEmail(request.getEmail());
        nguoiDung.setMatKhau(passwordEncoder.encode(request.getPassword()));
        nguoiDung.setSoDienThoai(request.getSoDienThoai());

        // Set role from request (validated by @Pattern in RegisterRequest)
        try {
            nguoiDung.setVaiTro(NguoiDung.VaiTro.valueOf(request.getVaiTro()));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Vai trò không hợp lệ: " + request.getVaiTro());
        }

        nguoiDung.setTrangThai(NguoiDung.TrangThaiNguoiDung.ACTIVE);

        nguoiDung = nguoiDungRepository.save(nguoiDung);

        LoginRequest loginRequest = new LoginRequest(request.getEmail(), request.getPassword());
        return login(loginRequest);
    }
}
