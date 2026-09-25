package com.sunmoon.backend.service.impl.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sunmoon.backend.constant.ConstantVariables;
import com.sunmoon.backend.dto.AuthInfo;
import com.sunmoon.backend.service.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.util.*;

@Service
public class JwtServiceImpl implements JwtService {

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public String buildScope(Set<String> roles) {
        StringJoiner scopeJoiner = new StringJoiner(" ");
        roles.forEach(scopeJoiner::add);
        return scopeJoiner.toString();
    }

    @Override
    public String generateToken(AuthInfo authInfo, String userAgent) {
        Set<String> roles = authInfo.getRoles();
        try {
            JWSHeader jwtHeader = new JWSHeader(JWSAlgorithm.HS256);
            String rolesJson = objectMapper.writeValueAsString(authInfo.getRoles());
            JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
                    .subject(authInfo.getEmail())
                    .issuer("UnitelFms")
                    .issueTime(new Date())
                    .expirationTime(new Date(new Date().getTime() + 24 * 60 * 60 * 1000))
                    .claim("id", authInfo.getId())
                    .claim("email", authInfo.getEmail())
                    .claim("username", authInfo.getUsername())
                    .claim("phone", authInfo.getPhone())
                    .claim("scope", buildScope(roles))
                    .claim("roles", rolesJson)
                    .claim("userAgent", userAgent)
                    .build();

            Payload jwtPayload = new Payload(jwtClaimsSet.toJSONObject());
            JWSObject jwsObject = new JWSObject(jwtHeader, jwtPayload);
            jwsObject.sign(new MACSigner(ConstantVariables.SIGNER_KEY.getBytes()));
            return jwsObject.serialize();
        } catch (JOSEException | JsonProcessingException e) {
            throw new RuntimeException("Error generating token: " + e.getMessage(), e);
        }

    }

    @Override
    public UUID getUserId(String token) {
        String idStr = (String) getClaimsFromToken(token).getClaims().get("id");
        return idStr != null ? UUID.fromString(idStr) : null;
    }

    @Override
    public JWTClaimsSet getClaimsFromToken(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            return signedJWT.getJWTClaimsSet();
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getTokenFromAuthHeader(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        return authHeader.substring(7);
    }

    @Override
    public AuthInfo getAuthInfoFromToken(String token) {
        try {
            JWSObject jwsObject = JWSObject.parse(token);
            JWSVerifier verifier = new MACVerifier(ConstantVariables.SIGNER_KEY.getBytes());

            if (!jwsObject.verify(verifier)) {
                throw new RuntimeException("Invalid token signature");
            }

            JWTClaimsSet claims = JWTClaimsSet.parse(jwsObject.getPayload().toJSONObject());

            String rolesJson = claims.getStringClaim("roles");
            Set<String> roles = objectMapper.readValue(
                    rolesJson,
                    new TypeReference<>() {
                    }
            );
            return AuthInfo.builder()
                    .id(getUserId(token))
                    .email(claims.getStringClaim("email"))
                    .username(claims.getStringClaim("username"))
                    .phone(claims.getStringClaim("phone"))
                    .roles(roles)
                    .build();

        } catch (JOSEException | ParseException | JsonProcessingException e) {
            // ignore
        }
        return null;
    }

    @Override
    public String generateRefreshToken(AuthInfo authInfo, String userAgent) {
        try {
            JWSHeader jwtHeader = new JWSHeader(JWSAlgorithm.HS256);
            JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
                    .subject(authInfo.getEmail())
                    .issuer("FSIGN")
                    .issueTime(new Date())
                    .expirationTime(new Date(new Date().getTime() + 7L * 24 * 60 * 60 * 1000)) // 7 days
                    .claim("id", authInfo.getId())
                    .claim("type", "REFRESH")
                    .claim("userAgent", userAgent)
                    // Mã ngẫu nhiên cho từng token. Thiếu nó thì hai lần đăng nhập trong cùng
                    // một giây, cùng máy, cùng tài khoản sẽ sinh ra chuỗi JWT GIỐNG HỆT NHAU
                    // (claim thời gian chỉ tính theo giây), bản ghi thứ hai đâm vào unique
                    // refresh_tokens.token_hash và người dùng nhận 409. Bấm đăng nhập hai
                    // lần, hoặc đăng ký xong vào thẳng, là dính ngay.
                    .jwtID(UUID.randomUUID().toString())
                    .build();

            Payload jwtPayload = new Payload(jwtClaimsSet.toJSONObject());
            JWSObject jwsObject = new JWSObject(jwtHeader, jwtPayload);
            jwsObject.sign(new MACSigner(ConstantVariables.SIGNER_KEY.getBytes()));
            return jwsObject.serialize();
        } catch (JOSEException e) {
            throw new RuntimeException("Error generating refresh token: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isTokenExpired(String token) {
        try {
            JWTClaimsSet claims = getClaimsFromToken(token);
            Date expirationTime = claims.getExpirationTime();
            return expirationTime != null && expirationTime.before(new Date());
        } catch (Exception e) {
            return true; // Treat invalid tokens as expired
        }
    }

}
