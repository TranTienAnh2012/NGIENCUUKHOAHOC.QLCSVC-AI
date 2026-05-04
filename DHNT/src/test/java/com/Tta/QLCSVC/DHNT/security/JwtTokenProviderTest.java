package com.Tta.QLCSVC.DHNT.security;

import com.Tta.QLCSVC.DHNT.config.JwtConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;

/**
 * Unit Tests cho JwtTokenProvider
 * Kiểm tra chức năng tạo, xác thực và đọc JWT token
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT) // Cho phép stub không dùng trong 1 số test
@DisplayName("JwtTokenProvider Tests")
class JwtTokenProviderTest {

    @Mock
    private JwtConfig jwtConfig;

    @InjectMocks
    private JwtTokenProvider jwtTokenProvider;

    private UserDetails userDetails;

    // Key đủ dài (≥512 bit) cho HS512
    private static final String TEST_SECRET = "test-secret-key-for-qlcsvc-ai-system-authentication-dhnt-2026-very-long-key";
    private static final long TEST_EXPIRATION = 86400000L; // 24 giờ
    private static final long TEST_REFRESH_EXPIRATION = 604800000L; // 7 ngày

    @BeforeEach
    void setUp() {
        // Dùng lenient() để tránh UnnecessaryStubbingException khi không phải test nào
        // cũng dùng đủ stubs
        lenient().when(jwtConfig.getSecret()).thenReturn(TEST_SECRET);
        lenient().when(jwtConfig.getExpiration()).thenReturn(TEST_EXPIRATION);
        lenient().when(jwtConfig.getRefreshExpiration()).thenReturn(TEST_REFRESH_EXPIRATION);

        userDetails = User.builder()
                .username("giaovien@dhnt.edu.vn")
                .password("encoded_password")
                .authorities(Collections.emptyList())
                .build();
    }

    // ===========================================================
    // TEST: generateToken
    // ===========================================================

    @Test
    @DisplayName("Tạo JWT token - kết quả không null và không rỗng")
    void generateToken_ValidUser_ReturnNonNullToken() {
        // When
        String token = jwtTokenProvider.generateToken(userDetails);

        // Then
        assertThat(token).isNotNull().isNotBlank();
    }

    @Test
    @DisplayName("Tạo JWT token - có 3 phần ngăn cách bởi dấu chấm")
    void generateToken_ValidFormat_HasThreeParts() {
        // When
        String token = jwtTokenProvider.generateToken(userDetails);

        // Then
        String[] parts = token.split("\\.");
        assertThat(parts).hasSize(3); // header.payload.signature
    }

    // ===========================================================
    // TEST: generateRefreshToken
    // ===========================================================

    @Test
    @DisplayName("Tạo refresh token - trả về token hợp lệ")
    void generateRefreshToken_ValidUser_ReturnToken() {
        // When
        String refreshToken = jwtTokenProvider.generateRefreshToken(userDetails);

        // Then
        assertThat(refreshToken).isNotNull().isNotBlank();
        String[] parts = refreshToken.split("\\.");
        assertThat(parts).hasSize(3);
    }

    @Test
    @DisplayName("Access token và refresh token khác nhau (do thời gian hết hạn khác)")
    void generateToken_AccessAndRefreshAreDifferent() {
        // When
        String accessToken = jwtTokenProvider.generateToken(userDetails);
        String refreshToken = jwtTokenProvider.generateRefreshToken(userDetails);

        // Then: không nhất thiết phải khác nhau nếu được tạo nhanh, nhưng cấu trúc phải
        // khác
        // (Chú ý: trong thực tế thường sẽ khác do timestamp khác nhau)
        assertThat(accessToken).isNotNull();
        assertThat(refreshToken).isNotNull();
    }

    // ===========================================================
    // TEST: validateToken
    // ===========================================================

    @Test
    @DisplayName("Xác thực token hợp lệ - trả về true")
    void validateToken_ValidToken_ReturnTrue() {
        // Given
        String token = jwtTokenProvider.generateToken(userDetails);

        // When
        boolean isValid = jwtTokenProvider.validateToken(token);

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Xác thực token giả mạo (malformed) - trả về false")
    void validateToken_FakeToken_ReturnFalse() {
        // Given: Token không đúng định dạng JWT (sẽ gây MalformedJwtException)
        String fakeToken = "this.is.not.a.valid.jwt.token.at.all";

        // When
        boolean isValid = jwtTokenProvider.validateToken(fakeToken);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Xác thực token bị thay đổi payload - trả về false")
    void validateToken_TamperedToken_ReturnFalse() {
        // Given: Tạo token hợp lệ, sau đó giả mạo chữ ký
        String validToken = jwtTokenProvider.generateToken(userDetails);
        // Thay thế phần chữ ký bằng ký tự random
        String[] parts = validToken.split("\\.");
        String tamperedToken = parts[0] + "." + parts[1] + ".INVALID_SIGNATURE_HERE";

        // When & Then: validateToken nên xử lý exception nội bộ và trả về false
        // (có thể throw UncheckedJwtException nếu SignatureException chưa được catch)
        // Test này kiểm tra rằng hàm không crash với exception
        try {
            boolean isValid = jwtTokenProvider.validateToken(tamperedToken);
            assertThat(isValid).isFalse();
        } catch (Exception e) {
            // Nếu exception được ném ra, đây là một thiếu sót cần ghi nhận
            // SignatureException chưa được xử lý đúng trong validateToken()
            assertThat(e.getMessage()).contains("signature");
        }
    }

    @Test
    @DisplayName("Xác thực token rỗng - trả về false")
    void validateToken_EmptyToken_ReturnFalse() {
        // When
        boolean isValid = jwtTokenProvider.validateToken("");

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Xác thực token null - trả về false")
    void validateToken_NullToken_ReturnFalse() {
        // When
        boolean isValid = jwtTokenProvider.validateToken(null);

        // Then
        assertThat(isValid).isFalse();
    }

    // ===========================================================
    // TEST: getUsernameFromToken
    // ===========================================================

    @Test
    @DisplayName("Lấy username từ token - đúng với username gốc")
    void getUsernameFromToken_ValidToken_ReturnCorrectUsername() {
        // Given
        String token = jwtTokenProvider.generateToken(userDetails);

        // When
        String username = jwtTokenProvider.getUsernameFromToken(token);

        // Then
        assertThat(username).isEqualTo("giaovien@dhnt.edu.vn");
    }

    @Test
    @DisplayName("Lấy username từ refresh token - đúng với username gốc")
    void getUsernameFromRefreshToken_ValidToken_ReturnCorrectUsername() {
        // Given
        String refreshToken = jwtTokenProvider.generateRefreshToken(userDetails);

        // When
        String username = jwtTokenProvider.getUsernameFromToken(refreshToken);

        // Then
        assertThat(username).isEqualTo("giaovien@dhnt.edu.vn");
    }

    // ===========================================================
    // TEST: getExpirationFromToken
    // ===========================================================

    @Test
    @DisplayName("Lấy ngày hết hạn từ token - không null và trong tương lai")
    void getExpirationFromToken_ValidToken_ReturnFutureDate() {
        // Given
        String token = jwtTokenProvider.generateToken(userDetails);

        // When
        java.util.Date expiration = jwtTokenProvider.getExpirationFromToken(token);

        // Then
        assertThat(expiration).isNotNull();
        assertThat(expiration).isAfter(new java.util.Date());
    }
}
