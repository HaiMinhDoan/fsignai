package com.sunmoon.backend.service;

import com.sunmoon.backend.entity.progress.UserStreak;

import java.util.UUID;

/**
 * Chuỗi ngày học liên tiếp.
 *
 * "Hôm nay" tính theo múi giờ NGƯỜI HỌC (xem UserTimeZoneResolver), không
 * phải UTC — học lúc 23h30 giờ Việt Nam mà tính theo UTC sẽ rơi sang ngày
 * hôm sau và mất chuỗi oan.
 */
public interface StreakService {

    /**
     * Ghi nhận "hôm nay người học có hoạt động". Gọi nhiều lần trong cùng một
     * ngày chỉ tính một lần (thao tác này là idempotent theo ngày).
     *
     * Quy tắc chuỗi:
     * - Hôm nay = hôm qua (theo bản ghi trước đó) + 1 ngày -> chuỗi +1.
     * - Đã ghi nhận hôm nay rồi -> không đổi gì, trả về trạng thái hiện tại.
     * - Cách xa hơn 1 ngày: nếu số ngày đứt quãng nhỏ hơn hoặc bằng số lượt
     *   "khiên bảo vệ" (freeze) còn lại thì dùng khiên che hết khoảng đứt,
     *   chuỗi coi như không đứt; nếu không đủ khiên, chuỗi reset về 1.
     * - Lần đầu tiên có hoạt động: chuỗi bắt đầu ở 1.
     */
    UserStreak recordStudyDay(UUID userId);

    /** Trạng thái chuỗi hiện tại, tạo bản ghi mặc định nếu người dùng chưa từng có hoạt động */
    UserStreak getOrCreate(UUID userId);
}
