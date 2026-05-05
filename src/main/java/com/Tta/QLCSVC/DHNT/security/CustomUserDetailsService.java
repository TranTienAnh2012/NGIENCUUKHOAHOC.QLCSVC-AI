package com.Tta.QLCSVC.DHNT.security;

import com.Tta.QLCSVC.DHNT.entity.NguoiDung;
import com.Tta.QLCSVC.DHNT.repository.NguoiDungRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final NguoiDungRepository nguoiDungRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        NguoiDung nguoiDung = nguoiDungRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        return User.builder()
                .username(nguoiDung.getEmail())
                .password(nguoiDung.getMatKhau())
                .authorities(getAuthorities(nguoiDung))
                .accountExpired(false)
                .accountLocked(nguoiDung.getTrangThai() == NguoiDung.TrangThaiNguoiDung.INACTIVE)
                .credentialsExpired(false)
                .disabled(nguoiDung.getTrangThai() == NguoiDung.TrangThaiNguoiDung.INACTIVE)
                .build();
    }

    private Collection<? extends GrantedAuthority> getAuthorities(NguoiDung nguoiDung) {
        return Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_" + nguoiDung.getVaiTro().name()));
    }
}
