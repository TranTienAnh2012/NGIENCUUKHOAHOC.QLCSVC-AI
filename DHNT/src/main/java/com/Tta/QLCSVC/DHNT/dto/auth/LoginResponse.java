package com.Tta.QLCSVC.DHNT.dto.auth;

import com.Tta.QLCSVC.DHNT.entity.NguoiDung;
import com.fasterxml.jackson.annotation.JsonIgnore;

public class LoginResponse {
    private String token;
    private String refreshToken;
    private String tokenType = "Bearer";

    @JsonIgnore
    private NguoiDung user;

    public LoginResponse() {
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public NguoiDung getUser() {
        return user;
    }

    public void setUser(NguoiDung user) {
        this.user = user;
    }

    // Expose only safe user info
    public Long getUserId() {
        return user != null ? user.getId() : null;
    }

    public String getUserName() {
        return user != null ? user.getHoTen() : null;
    }

    public String getUserEmail() {
        return user != null ? user.getEmail() : null;
    }

    public String getUserRole() {
        return user != null ? user.getVaiTro().name() : null;
    }

    public LoginResponse(String token, String refreshToken, NguoiDung user) {
        this.token = token;
        this.refreshToken = refreshToken;
        this.user = user;
    }
}
