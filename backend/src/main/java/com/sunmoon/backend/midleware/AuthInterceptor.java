package com.sunmoon.backend.midleware;

import com.sunmoon.backend.constant.context.SecurityContextHolder;
import com.sunmoon.backend.dto.AuthInfo;
import com.sunmoon.backend.service.JwtService;
import com.sunmoon.backend.service.RedisService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtService jwtService;

    @Autowired
    private RedisService redisService;

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
            @NonNull Object handler) throws Exception {
        SecurityContextHolder.setPath(request.getRequestURI());

        String authHeader = request.getHeader("Authorization");
        String lang = request.getHeader("lang");
        if (lang != null) {
            SecurityContextHolder.setLang(lang);
        } else {
            SecurityContextHolder.setLang("vi");
        }
        if (authHeader != null) {
            String token = jwtService.getTokenFromAuthHeader(authHeader);

            // Header có nhưng sai định dạng (thiếu tiền tố "Bearer ").
            // Trước đây vẫn chạy tiếp và gọi getAuthInfoFromToken(null), khiến
            // Nimbus JWSObject.parse(null) ném NullPointerException -> HTTP 500.
            // Client gửi header sai phải nhận 401, không phải lỗi máy chủ.
            if (token == null) {
                writeUnauthorized(response, "Authorization header phải có dạng: Bearer <token>");
                return false;
            }

            String blacklistKey = redisService.buildKey("token-blacklist", token);
            if (redisService.exists(blacklistKey)) {
                writeUnauthorized(response, "Token has been revoked");
                return false;
            }

            AuthInfo authInfo = jwtService.getAuthInfoFromToken(token);
            SecurityContextHolder.setAuthInfo(authInfo);
        } else {
            SecurityContextHolder.setAuthInfo(null);
        }
        return true;
    }

    /** Đặt Content-Type TRƯỚC khi ghi, nếu không charset sẽ không được áp dụng */
    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(
                "{\"status\": 401, \"message\": " + quote(message) + "}");
    }

    private String quote(String raw) {
        return "\"" + raw.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
            @NonNull Object handler, Exception ex) {
        SecurityContextHolder.clear();
    }
}
