package com.sunmoon.backend.configs;


import com.nimbusds.jose.JWSAlgorithm;
import com.sunmoon.backend.constant.ConstantVariables;
import com.sunmoon.backend.midleware.AuthInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.crypto.spec.SecretKeySpec;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig implements WebMvcConfigurer {

    @Autowired
    private AuthInterceptor authInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Chỉ loại trừ những endpoint THỰC SỰ công khai.
        // Trước đây loại trừ cả "/api/v1/auth/**" nên /auth/me, /auth/logout,
        // /auth/change-password không bao giờ có AuthInfo và @RequireAuth luôn chặn.
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/v1/auth/login",
                        "/api/v1/auth/register",
                        "/api/v1/auth/refresh",
                        "/api/v1/auth/forgot-password",
                        "/api/v1/auth/reset-password");
    }

    //Security Config==================================================================================================>
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                        .requestMatchers("/**").permitAll()
//                        .requestMatchers("/private/**").hasAnyAuthority("SCOPE_ROLE_ADMIN", "SCOPE_ROLE_CUSTOMER")
                        .anyRequest().authenticated())
                .csrf().disable();
        http.oauth2ResourceServer(oauth2 -> {
            oauth2.jwt(jwtConfigurer -> {
                jwtConfigurer.decoder(jwtDecoder());
            });
        });
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        SecretKeySpec secretKeySpec = new SecretKeySpec(ConstantVariables.SIGNER_KEY.getBytes(), JWSAlgorithm.HS256.toString());
        return NimbusJwtDecoder.withSecretKey(secretKeySpec)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*")); // Cho phép mọi origin
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(List.of("*")); // Cho phép mọi header
        config.setAllowCredentials(false); // QUAN TRỌNG: phải false khi dùng "*"
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }


    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                // allow all origins, methods, and headers
                .allowedOrigins("*")
                // PATCH phải có mặt ở ĐÂY, không chỉ ở corsConfigurationSource() phía trên.
                // Trình duyệt gửi kèm Origin cho mọi phương thức khác GET/HEAD, kể cả khi đi qua proxy
                // cùng origin của Vite; phương thức không nằm trong danh sách này bị CORS của Spring MVC
                // chặn thẳng bằng 403 trước khi vào controller — gọi bằng curl thì vẫn chạy, nên rất khó đoán.
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }
}
