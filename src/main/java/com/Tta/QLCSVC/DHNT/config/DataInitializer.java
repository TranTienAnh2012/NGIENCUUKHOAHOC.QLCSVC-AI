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
    public CommandLineRunner initData(org.springframework.jdbc.core.JdbcTemplate jdbcTemplate) {
        return args -> {
            try {
                // Fix lỗi Data Truncated cho Enum mới thêm bằng cách convert sang VARCHAR(50)
                jdbcTemplate.execute("ALTER TABLE muon_tra_thiet_bi MODIFY COLUMN trang_thai VARCHAR(50)");
            } catch (Exception e) {
                // Bỏ qua nếu bảng chưa tồn tại
            }

            if (!nguoiDungRepository.existsByEmail("admin@example.com")) {
                NguoiDung admin = new NguoiDung();
                admin.setHoTen("Admin System");
                admin.setEmail("admin@example.com");
                admin.setMatKhau(passwordEncoder.encode("123456"));
                admin.setVaiTro(NguoiDung.VaiTro.ADMIN);
                admin.setTrangThai(NguoiDung.TrangThaiNguoiDung.ACTIVE);
                nguoiDungRepository.save(admin);
            } else {
                nguoiDungRepository.findByEmail("admin@example.com").ifPresent(user -> {
                    user.setVaiTro(NguoiDung.VaiTro.ADMIN);
                    user.setTrangThai(NguoiDung.TrangThaiNguoiDung.ACTIVE);
                    nguoiDungRepository.save(user);
                });
            }
        };
    }
}
