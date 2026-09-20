package com.sunmoon.backend.service.support;

import java.util.Map;

/**
 * Mã gợi ý → lời khuyên tiếng Việt.
 *
 * DB lưu MÃ, không lưu câu: đổi cách diễn đạt (hoặc thêm ngôn ngữ) chỉ sửa ở đây, không phải deploy lại
 * ai-service và không phải sửa dữ liệu cũ. Giọng văn dành cho trẻ nhỏ: ngắn, ấm, nói cách sửa chứ không phán "sai".
 */
public final class AiHintCatalog {

    private AiHintCatalog() {}

    // Mã do ai-service sinh (hình học) — không phụ thuộc ngưỡng từng từ
    public static final String HAND_MISSING = "HAND_MISSING";
    public static final String EXTRA_HAND = "EXTRA_HAND";
    public static final String LOCATION_TOO_LOW = "LOCATION_TOO_LOW";
    public static final String LOCATION_TOO_HIGH = "LOCATION_TOO_HIGH";
    public static final String MOVEMENT_TOO_SMALL = "MOVEMENT_TOO_SMALL";
    public static final String MOVEMENT_TOO_LARGE = "MOVEMENT_TOO_LARGE";

    // Mã do Spring sinh từ điểm từng phần so với ngưỡng của từ đó
    public static final String HANDSHAPE_OK = "HANDSHAPE_OK";
    public static final String HANDSHAPE_OFF = "HANDSHAPE_OFF";
    public static final String LOCATION_OK = "LOCATION_OK";
    public static final String LOCATION_OFF = "LOCATION_OFF";
    public static final String MOVEMENT_OK = "MOVEMENT_OK";
    public static final String MOVEMENT_OFF = "MOVEMENT_OFF";
    public static final String TRACKING_LOW = "TRACKING_LOW";

    private static final Map<String, String> TEXT = Map.ofEntries(
            Map.entry(HAND_MISSING, "Ký hiệu này dùng cả hai tay — bé giơ cả hai tay lên nhé."),
            Map.entry(EXTRA_HAND, "Ký hiệu này chỉ dùng một tay — bé hạ tay còn lại xuống nhé."),
            Map.entry(LOCATION_TOO_LOW, "Thử đưa tay lên cao hơn một chút."),
            Map.entry(LOCATION_TOO_HIGH, "Thử hạ tay xuống thấp hơn một chút."),
            Map.entry(MOVEMENT_TOO_SMALL, "Làm động tác rộng hơn một chút, giống video mẫu."),
            Map.entry(MOVEMENT_TOO_LARGE, "Làm động tác nhỏ lại một chút, giống video mẫu."),
            Map.entry(HANDSHAPE_OK, "Hình tay chính xác."),
            Map.entry(HANDSHAPE_OFF, "Hình bàn tay chưa giống mẫu — xem lại cách xoè hoặc nắm các ngón."),
            Map.entry(LOCATION_OK, "Vị trí đặt tay đúng."),
            Map.entry(LOCATION_OFF, "Vị trí đặt tay chưa giống video mẫu."),
            Map.entry(MOVEMENT_OK, "Chuyển động đúng nhịp."),
            Map.entry(MOVEMENT_OFF, "Chuyển động chưa giống mẫu — xem lại hướng và nhịp."),
            Map.entry(TRACKING_LOW, "Camera thấy tay chưa rõ — bé thử ngồi chỗ sáng hơn và giơ tay giữa khung hình nhé."));

    /** Lời khuyên cho một mã; mã lạ (dữ liệu cũ, phiên bản khác) trả null để bỏ qua thay vì hiện mã thô */
    public static String textOf(String code) {
        return TEXT.get(code);
    }

    /** Lời nhắn khi ai-service không thấy được người/ký hiệu trong clip */
    public static String messageForError(String code) {
        return switch (code) {
            case "NO_BODY" -> "Mình chưa thấy rõ hai vai của bé. Bé ngồi lùi ra một chút để camera thấy cả vai và hai tay nhé.";
            case "NO_SIGN" -> "Mình chưa thấy bé giơ tay ký hiệu. Bé thử lại, giơ tay lên rõ ràng hơn nhé.";
            case "NO_FRAMES" -> "Chưa ghi được hình nào. Bé kiểm tra camera rồi thử lại nhé.";
            default -> null;
        };
    }
}
