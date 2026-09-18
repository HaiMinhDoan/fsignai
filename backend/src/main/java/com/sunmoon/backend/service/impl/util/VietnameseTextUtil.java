package com.sunmoon.backend.service.impl.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Bo dau tieng Viet o tang Java, doi xung voi ham f_unaccent() cua PostgreSQL.
 *
 * Vi sao can: cot signs.word_vi_unaccent duoc DB sinh san bang f_unaccent().
 * Khi nguoi dung go "dia chi" de tim "dia chi" co dau, ta phai bo dau tu khoa
 * o Java truoc khi dua vao filter LIKE, neu khong se so chuoi co dau voi cot
 * da bo dau va khong bao gio khop.
 *
 * Dung java.text.Normalizer co san trong JDK - khong them thu vien nao.
 */
public final class VietnameseTextUtil {

    private static final Pattern DIACRITICS =
            Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

    private static final Pattern NON_SLUG_CHARS = Pattern.compile("[^a-z0-9\\s-]");
    private static final Pattern NON_GLOSS_CHARS = Pattern.compile("[^a-z0-9\\s_]");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final Pattern REPEATED_DASH = Pattern.compile("-{2,}");
    private static final Pattern REPEATED_UNDERSCORE = Pattern.compile("_{2,}");
    private static final Pattern EDGE_DASH = Pattern.compile("^-|-$");
    private static final Pattern EDGE_UNDERSCORE = Pattern.compile("^_|_$");

    private VietnameseTextUtil() {
    }

    /**
     * Bo dau va chuyen ve chu thuong. "Dia Chi" -> "dia chi".
     * Chu D gach ngang khong phai dau to hop nen Normalizer khong tach duoc,
     * phai thay thu cong bang ma Unicode U+0111 / U+0110.
     */
    public static String unaccent(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        String stripped = DIACRITICS.matcher(normalized).replaceAll("");
        return stripped.replace('đ', 'd')
                       .replace('Đ', 'D')
                       .toLowerCase(Locale.ROOT)
                       .trim();
    }

    /**
     * Sinh slug tu chuoi tieng Viet.
     * "Giao tiep hang ngay" -> "giao-tiep-hang-ngay"
     */
    public static String toSlug(String input) {
        String base = unaccent(input);
        if (base.isEmpty()) {
            return "";
        }
        String slug = NON_SLUG_CHARS.matcher(base).replaceAll("");
        slug = WHITESPACE.matcher(slug).replaceAll("-");
        slug = REPEATED_DASH.matcher(slug).replaceAll("-");
        return EDGE_DASH.matcher(slug).replaceAll("");
    }

    /**
     * Sinh gloss tu tu tieng Viet. Gloss la ma dinh danh ky hieu,
     * viet hoa va dung gach duoi: "gia dinh" -> "GIA_DINH".
     */
    public static String toGloss(String input) {
        String base = unaccent(input);
        if (base.isEmpty()) {
            return "";
        }
        String gloss = NON_GLOSS_CHARS.matcher(base).replaceAll("");
        gloss = WHITESPACE.matcher(gloss).replaceAll("_");
        gloss = REPEATED_UNDERSCORE.matcher(gloss).replaceAll("_");
        gloss = EDGE_UNDERSCORE.matcher(gloss).replaceAll("");
        return gloss.toUpperCase(Locale.ROOT);
    }
}
