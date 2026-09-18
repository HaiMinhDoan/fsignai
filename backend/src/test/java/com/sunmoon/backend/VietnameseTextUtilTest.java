package com.sunmoon.backend;

import com.sunmoon.backend.service.impl.util.VietnameseTextUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

// Test thuan, khong can Spring context - chay trong vai mili giay.
// Quan trong vi ham nay phai doi xung voi f_unaccent() cua PostgreSQL:
// lech mot ky tu la tinh nang tim kiem khong dau hong am tham.
class VietnameseTextUtilTest {

    @Test
    @DisplayName("Bo dau tieng Viet, ke ca chu D gach ngang")
    void unaccent() {
        assertEquals("dia chi", VietnameseTextUtil.unaccent("Địa Chỉ"));
        assertEquals("me", VietnameseTextUtil.unaccent("Mẹ"));
        assertEquals("do", VietnameseTextUtil.unaccent("Đỗ"));
        assertEquals("nguoi khiem thinh", VietnameseTextUtil.unaccent("Người khiếm thính"));
        assertEquals("", VietnameseTextUtil.unaccent(null));
    }

    @Test
    @DisplayName("Sinh slug cho chu de")
    void toSlug() {
        assertEquals("giao-tiep-hang-ngay", VietnameseTextUtil.toSlug("Giao tiếp hằng ngày"));
        assertEquals("gia-dinh", VietnameseTextUtil.toSlug("Gia đình"));
        assertEquals("bang-chu-cai", VietnameseTextUtil.toSlug("Bảng chữ cái"));
    }

    @Test
    @DisplayName("Sinh gloss cho tu vung")
    void toGloss() {
        assertEquals("GIA_DINH", VietnameseTextUtil.toGloss("gia đình"));
        assertEquals("ME", VietnameseTextUtil.toGloss("mẹ"));
        assertEquals("BA_CHAN_BON_CANG", VietnameseTextUtil.toGloss("Ba chân bốn cẳng"));
    }
}
