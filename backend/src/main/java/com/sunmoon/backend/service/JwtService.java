package com.sunmoon.backend.service;

import com.nimbusds.jwt.JWTClaimsSet;
import com.sunmoon.backend.dto.AuthInfo;

import java.util.Set;
import java.util.UUID;

public interface JwtService {
    String buildScope(Set<String> roles);

    String generateToken(AuthInfo authInfo, String userAgent);

    UUID getUserId(String token);

    JWTClaimsSet getClaimsFromToken(String token);

    String getTokenFromAuthHeader(String authHeader);

    AuthInfo getAuthInfoFromToken(String token);

    String generateRefreshToken(AuthInfo authInfo, String userAgent);

    boolean isTokenExpired(String token);
}
