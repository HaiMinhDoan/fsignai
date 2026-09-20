import { defineStore } from 'pinia';
import { ref } from 'vue';
import { learnerStatsApi, type LearnerStats } from '@/api/catalog';

/**
 * Chuỗi ngày, sao và cấp độ — hiện ở header và ở khối "Cấp Độ" trang chủ.
 *
 * Để ở store vì hai chỗ đó dùng chung đúng một bộ số; gọi API riêng ở từng
 * nơi sẽ ra hai con số lệch nhau trên cùng một màn hình.
 */
const EMPTY: LearnerStats = {
  streakDays: 0,
  longestStreakDays: 0,
  stars: 0,
  weeklyStars: 0,
  level: 1,
  levelPoints: 0,
  levelTarget: 200,
  levelPercent: 0,
};

export const useLearnerStatsStore = defineStore('learnerStats', () => {
  const stats = ref<LearnerStats>({ ...EMPTY });
  const loaded = ref(false);

  async function load(force = false) {
    if (loaded.value && !force) return;
    try {
      stats.value = await learnerStatsApi();
      loaded.value = true;
    } catch {
      // Chưa đăng nhập hoặc API lỗi: giữ số 0. Thanh đầu trang vẫn hiện đủ ô
      // để bé biết chỗ đó rồi sẽ có gì, thay vì mất hẳn.
      stats.value = { ...EMPTY };
    }
  }

  function reset() {
    stats.value = { ...EMPTY };
    loaded.value = false;
  }

  return { stats, loaded, load, reset };
});
