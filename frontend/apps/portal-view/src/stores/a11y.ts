import { defineStore } from 'pinia';
import { ref, watch } from 'vue';

/**
 * Cài đặt trợ năng của thanh trên cùng (Figma: Top Accessibility Quick Toolbar).
 *
 * Để ở store chứ không ở từng màn hình vì hai lựa chọn này phải theo người học đi
 * khắp nơi: bật phụ đề to ở trang chủ thì vào phòng luyện vẫn phải to, chỉnh
 * tốc độ 0.5x thì mọi video sau đó đều chạy 0.5x. Bắt người học chỉnh lại ở từng
 * trang là đánh mất ý nghĩa của trợ năng.
 */
const KEY = 'signai_a11y';

interface Saved {
  bigCaption: boolean;
  playbackRate: number;
}

function load(): Saved {
  try {
    const raw = localStorage.getItem(KEY);
    if (raw) {
      const parsed = JSON.parse(raw) as Partial<Saved>;
      return {
        bigCaption: parsed.bigCaption === true,
        // Chỉ nhận ba mức hợp lệ — dữ liệu cũ hoặc bị sửa tay không được
        // làm video chạy với tốc độ vô lý
        playbackRate: [0.5, 0.75, 1].includes(parsed.playbackRate as number)
          ? (parsed.playbackRate as number)
          : 1,
      };
    }
  } catch {
    // Trình duyệt chặn localStorage — dùng mặc định, không làm hỏng trang
  }
  return { bigCaption: false, playbackRate: 1 };
}

export const useA11yStore = defineStore('a11y', () => {
  const saved = load();
  const bigCaption = ref(saved.bigCaption);
  const playbackRate = ref(saved.playbackRate);

  function toggleBigCaption() {
    bigCaption.value = !bigCaption.value;
  }

  function setRate(rate: number) {
    playbackRate.value = rate;
  }

  watch([bigCaption, playbackRate], () => {
    try {
      localStorage.setItem(
        KEY,
        JSON.stringify({ bigCaption: bigCaption.value, playbackRate: playbackRate.value }),
      );
    } catch {
      // Không lưu được thì cài đặt chỉ sống trong phiên này — vẫn dùng được
    }
    // Gắn lên thẻ <html> để CSS toàn cục phóng to phụ đề mà không component
    // nào phải tự xử lý
    document.documentElement.dataset.bigCaption = bigCaption.value ? 'on' : 'off';
  });

  document.documentElement.dataset.bigCaption = bigCaption.value ? 'on' : 'off';

  return { bigCaption, playbackRate, toggleBigCaption, setRate };
});
