package com.chatsever.auth.service;

import com.chatsever.auth.dto.AuthRequest;
import com.chatsever.auth.dto.AuthResponse;
import com.chatsever.auth.model.User;
import com.chatsever.auth.repository.UserRepository;
import com.chatsever.auth.dto.RefreshTokenRequest;
import com.chatsever.auth.dto.ValidateRequest;
import com.chatsever.auth.dto.ValidateResponse;
import com.chatsever.common.util.SecurityUtil;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    @Value("${jwt.refresh-expiration}")
    private long jwtRefreshExpiration;

    public String register(AuthRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Tên đăng nhập đã tồn tại!");
        }
        User user = User.builder()
                .username(request.getUsername())
                .passwordHash(SecurityUtil.hashPassword(request.getPassword()))
                .displayName(request.getUsername())
                .avatarUrl("https://default-avatar.com/user.png")
                .role("MEMBER")
                .build();
        userRepository.save(user);
        return "Đăng ký thành công!";
    }

    public AuthResponse login(AuthRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        if (!SecurityUtil.checkPassword(request.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Mật khẩu không chính xác");
        }

        if ("BANNED".equals(user.getRole())) {
            throw new RuntimeException("Tài khoản của bạn đã bị khóa (BANNED)!");
        }

        String accessToken = Jwts.builder()
                .subject(user.getUsername())
                .claim("userId", user.getId())
                .claim("role", user.getRole())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(Keys.hmacShaKeyFor(jwtSecret.getBytes()))
                .compact();

        String refreshToken = Jwts.builder()
                .subject(user.getUsername())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtRefreshExpiration))
                .signWith(Keys.hmacShaKeyFor(jwtSecret.getBytes()))
                .compact();

        return new AuthResponse(user.getId(), accessToken, refreshToken, user.getUsername());
    }

    public AuthResponse refreshToken(RefreshTokenRequest request) {
        try {
            // Giải mã Refresh Token để lấy username
            String username = Jwts.parser()
                    .verifyWith(Keys.hmacShaKeyFor(jwtSecret.getBytes()))
                    .build()
                    .parseSignedClaims(request.getToken())
                    .getPayload()
                    .getSubject();

            // Tìm user trong DB
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

            // Cấp lại Access Token mới (sống 2 tiếng)
            String newAccessToken = Jwts.builder()
                    .subject(user.getUsername())
                    .claim("userId", user.getId())
                    .issuedAt(new Date())
                    .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
                    .signWith(Keys.hmacShaKeyFor(jwtSecret.getBytes()))
                    .compact();

            // Trả về vé mới (giữ nguyên refresh token cũ)
            return new AuthResponse(user.getId(), newAccessToken, request.getToken(), user.getUsername());

        } catch (Exception e) {
            throw new RuntimeException("Refresh Token không hợp lệ hoặc đã hết hạn!");
        }
    }

    public ValidateResponse validateToken(ValidateRequest request) {
        try {
            String username = Jwts.parser()
                    .verifyWith(Keys.hmacShaKeyFor(jwtSecret.getBytes()))
                    .build()
                    .parseSignedClaims(request.getToken())
                    .getPayload()
                    .getSubject();

            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

            if ("BANNED".equals(user.getRole())) {
                return new ValidateResponse(false, null);
            }

            return new ValidateResponse(true, username);
        } catch (Exception e) {
            return new ValidateResponse(false, null);
        }
    }
}
