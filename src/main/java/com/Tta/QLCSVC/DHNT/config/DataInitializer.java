package com.Tta.QLCSVC.DHNT.config;

import com.Tta.QLCSVC.DHNT.entity.NguoiDung;
import com.Tta.QLCSVC.DHNT.repository.NguoiDungRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    private final NguoiDungRepository nguoiDungRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(NguoiDungRepository nguoiDungRepository, PasswordEncoder passwordEncoder) {
        this.nguoiDungRepository = nguoiDungRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Bean
    public CommandLineRunner initData() {
        return args -> {
            if (!nguoiDungRepository.existsByEmail("admin@example.com")) {
                NguoiDung admin = new NguoiDung();
                admin.setHoTen("Admin System");
                admin.setEmail("admin@example.com");
                admin.setMatKhau(passwordEncoder.encode("123456"));
                admin.setVaiTro(NguoiDung.VaiTro.ADMIN);
                admin.setTrangThai(NguoiDung.TrangThaiNguoiDung.ACTIVE);
                nguoiDungRepository.save(admin);
            } else {
                // Update existing user to ADMIN
                nguoiDungRepository.findByEmail("admin@example.com").ifPresent(user -> {
                    user.setVaiTro(NguoiDung.VaiTro.ADMIN);
                    user.setMatKhau(passwordEncoder.encode("123456"));
                    nguoiDungRepository.save(user);
                });
            }
        };
    }
}
