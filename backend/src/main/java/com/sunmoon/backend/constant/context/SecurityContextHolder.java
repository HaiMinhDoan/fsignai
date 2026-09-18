package com.sunmoon.backend.constant.context;


import com.sunmoon.backend.dto.AuthInfo;

public class SecurityContextHolder {
    private static final ThreadLocal<String> path = new ThreadLocal<>();
    private static final ThreadLocal<String> lang = new ThreadLocal<>();
    private static final ThreadLocal<AuthInfo> authInfo = new ThreadLocal<>();

    public static void setPath(String path) {
        SecurityContextHolder.path.set(path);
    }

    public static void setLang(String lang) {
        SecurityContextHolder.lang.set(lang);
    }

    public static void setAuthInfo(AuthInfo authInfo) {
        SecurityContextHolder.authInfo.set(authInfo);
    }

    public static String getPath() {
        return path.get();
    }

    public static String getLang() {
        return lang.get();
    }

    public static AuthInfo getAuthInfo() {
        return authInfo.get();
    }

    public static void clear() {
        path.remove();
        lang.remove();
        authInfo.remove();
    }
}
