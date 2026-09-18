package com.sunmoon.backend.constant.enums;

import lombok.Getter;

@Getter
public enum FileType {
    IMAGE   ("image/jpeg", "image/png", "image/webp"),
    VIDEO   ("video/mp4", "video/quicktime"),
    DOCUMENT("application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
    HTML    ("text/html"),
    TEXT    ("text/plain"),
    PDF     ("application/pdf");

    private final String[] mimeTypes;

    FileType(String... mimeTypes) {
        this.mimeTypes = mimeTypes;
    }

    public boolean matches(String mimeType) {
        for (String m : mimeTypes) {
            if (m.equals(mimeType)) return true;
        }
        return false;
    }

    public static FileType fromMimeType(String mimeType) {
        for (FileType type : values()) {
            if (type.matches(mimeType)) return type;
        }
        throw new IllegalArgumentException("Unsupported mime type: " + mimeType);
    }

    public String getPrimaryMimeType() {
        return mimeTypes[0];
    }
}