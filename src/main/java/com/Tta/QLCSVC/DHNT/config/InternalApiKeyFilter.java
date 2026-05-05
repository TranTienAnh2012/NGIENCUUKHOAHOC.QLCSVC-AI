package com.Tta.QLCSVC.DHNT.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Filter xác thực Internal API Key cho Flask AI Server.
 *
 * Flask AI (localhost:5000) gọi /api/ai-data/** bằng header:
 * X-Internal-API-Key: <giá trị trong application.properties>
 *
 * Nếu key hợp lệ → tạo authentication với role ROLE_INTERNAL_SERVICE
 * và cho phép request đi tiếp (SecurityConfig sẽ check role này).
 *
 * Nếu không có header này, Spring Security vẫn kiểm tra JWT như bình thường.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InternalApiKeyFilter extends OncePerRequestFilter {

    /** Header name Flask phải gửi kèm */
    public static final String API_KEY_HEADER = "X-Internal-API-Key";

    /** Role ảo gán cho Flask service account */
    public static final String INTERNAL_SERVICE_ROLE = "ROLE_INTERNAL_SERVICE";

    @Value("${internal.api-key}")
    private String internalApiKey;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String requestedKey = request.getHeader(API_KEY_HEADER);

        // Chỉ xử lý nếu có header key VÀ chưa được authenticate bởi filter khác
        if (requestedKey != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            if (internalApiKey.equals(requestedKey)) {
                // Key hợp lệ → tạo authentication cho Flask service
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        "flask-ai-service", // principal
                        null, // credentials (không cần)
                        List.of(new SimpleGrantedAuthority(INTERNAL_SERVICE_ROLE)));
                SecurityContextHolder.getContext().setAuthentication(auth);
                log.debug("Flask AI service authenticated via Internal API Key from {}",
                        request.getRemoteAddr());
            } else {
                // Key sai → log cảnh báo bảo mật
                log.warn("⚠️  Invalid Internal API Key attempt from {}. Header: X-Internal-API-Key",
                        request.getRemoteAddr());
            }
        }

        filterChain.doFilter(request, response);
    }
}
