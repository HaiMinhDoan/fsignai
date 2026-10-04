<template>
  <section class="pricing-page">
    <header class="page-head">
      <h1>Gói dịch vụ</h1>
      <p class="page-sub">Bắt đầu miễn phí, nâng cấp khi bạn muốn kiểm tra kiến thức và luyện cùng AI.</p>
    </header>

    <!-- Bị chuyển tới đây từ một trang Premium: nói rõ vì sao -->
    <p v-if="tinhNang && !laPremium" class="notice">
      <SiIcon name="lock" :size="18" />
      <span><strong>{{ tinhNang }}</strong> thuộc gói Premium. Nâng cấp để mở khoá nhé.</span>
    </p>

    <p v-if="laPremium && sub" class="notice notice--ok">
      <SiIcon name="check" :size="18" />
      <span v-if="sub.staff">Tài khoản nội bộ — bạn đã dùng được mọi tính năng.</span>
      <span v-else>Bạn đang dùng gói Premium đến hết ngày <strong>{{ ngay(sub.premiumUntil) }}</strong>.</span>
    </p>

    <div class="plan-grid">
      <article v-for="p in plans" :key="p.code" class="plan-card" :class="{ 'plan-card--premium': p.code === 'PREMIUM' }">
        <p class="plan-tag">{{ p.taglineVi }}</p>
        <h2>{{ p.nameVi }}</h2>
        <p class="plan-price">
          <template v-if="p.price === 0">Miễn phí</template>
          <template v-else>
            {{ tien(p.price) }}
            <small v-if="p.durationDays">/ {{ p.durationDays }} ngày</small>
          </template>
        </p>
        <p class="plan-desc">{{ p.descriptionVi }}</p>

        <p v-if="p.includesFree" class="plan-includes">Bao gồm toàn bộ quyền lợi của gói Free, cùng với:</p>
        <p v-else class="plan-includes">Quyền lợi bao gồm:</p>
        <ul class="benefits">
          <li v-for="b in p.benefits" :key="b"><SiIcon name="check" :size="16" /> {{ b }}</li>
        </ul>

        <template v-if="p.code === 'PREMIUM'">
          <button
            v-if="!sub?.staff"
            type="button"
            class="btn-buy"
            :disabled="dangTao"
            @click="muaPremium"
          >
            <SiIcon name="sparkles" :size="18" />
            <span>{{ dangTao ? 'Đang tạo mã…' : laPremium ? 'Gia hạn Premium' : 'Nâng cấp Premium' }}</span>
          </button>
        </template>
        <p v-else class="plan-current">{{ laPremium ? 'Đã gồm trong gói của bạn' : 'Gói hiện tại của bạn' }}</p>
      </article>
    </div>

    <p v-if="loi" class="error-text">{{ loi }}</p>

    <!-- Khung thanh toán: QR + thông tin chuyển khoản, tự hỏi trạng thái mỗi 3 giây -->
    <section v-if="don" class="checkout" aria-live="polite">
      <template v-if="don.status === 'PAID'">
        <div class="paid">
          <SiIcon name="check" :size="40" />
          <h2>Thanh toán thành công!</h2>
          <p>Gói Premium của bạn có hiệu lực đến hết ngày <strong>{{ ngay(don.premiumUntil) }}</strong>.</p>
          <RouterLink to="/kiem-tra" class="btn-buy">Vào làm bài kiểm tra</RouterLink>
        </div>
      </template>

      <template v-else-if="don.status === 'EXPIRED'">
        <p class="notice">Mã thanh toán đã hết hạn. Nếu bạn ĐÃ chuyển khoản, đừng lo — tiền đến đúng mã vẫn được ghi nhận.</p>
        <button type="button" class="btn-buy" @click="muaPremium">Tạo mã mới</button>
      </template>

      <template v-else>
        <h2>Quét mã để thanh toán</h2>
        <div class="checkout-body">
          <img :src="don.qrUrl" alt="Mã QR chuyển khoản gói Premium" class="qr" />
          <dl class="bank-info">
            <dt>Ngân hàng</dt>
            <dd>{{ don.bankCode }}</dd>
            <dt>Số tài khoản</dt>
            <dd>
              {{ don.accountNumber }}
              <button type="button" class="copy" @click="chep(don.accountNumber)">Chép</button>
            </dd>
            <template v-if="don.accountName">
              <dt>Chủ tài khoản</dt>
              <dd>{{ don.accountName }}</dd>
            </template>
            <dt>Số tiền</dt>
            <dd class="strong">{{ tien(don.amount) }}</dd>
            <dt>Nội dung</dt>
            <dd class="strong">
              {{ don.paymentCode }}
              <button type="button" class="copy" @click="chep(don.paymentCode)">Chép</button>
            </dd>
          </dl>
        </div>
        <p class="checkout-hint">
          <SiIcon name="sparkles" :size="16" />
          Ghi <strong>đúng nội dung {{ don.paymentCode }}</strong> để hệ thống tự kích hoạt. Trang này tự cập nhật
          khi nhận được tiền — mã còn hiệu lực {{ conLai }}.
        </p>
      </template>
    </section>
  </section>
</template>

<script lang="ts" setup>
  import { computed, onBeforeUnmount, onMounted, ref } from 'vue';
  import { useRoute, useRouter } from 'vue-router';
  import SiIcon from '@/components/SiIcon.vue';
  import { useAuthStore } from '@/stores/auth';
  import {
    createOrderApi,
    mySubscriptionApi,
    orderStatusApi,
    plansApi,
    type PaymentOrder,
    type Plan,
    type Subscription,
  } from '@/api/payment';

  defineOptions({ name: 'PricingView' });

  const route = useRoute();
  const router = useRouter();
  const auth = useAuthStore();

  const plans = ref<Plan[]>([]);
  const sub = ref<Subscription>();
  const don = ref<PaymentOrder>();
  const dangTao = ref(false);
  const loi = ref('');
  const bayGio = ref(Date.now());

  let hoi: number | undefined;
  let dongHo: number | undefined;

  /** Tên tính năng Premium vừa bị chặn (router gửi qua ?tinh-nang=) */
  const tinhNang = computed(() => (route.query['tinh-nang'] as string) || '');
  const laPremium = computed(() => !!sub.value?.premium);

  const conLai = computed(() => {
    if (!don.value) return '';
    const giay = Math.max(0, Math.floor((new Date(don.value.expiresAt).getTime() - bayGio.value) / 1000));
    return `${Math.floor(giay / 60)} phút ${String(giay % 60).padStart(2, '0')} giây`;
  });

  const tien = (n: number) => n.toLocaleString('vi-VN') + 'đ';
  const ngay = (iso?: string) => (iso ? new Date(iso).toLocaleDateString('vi-VN') : '');

  async function muaPremium() {
    loi.value = '';
    if (!auth.isLoggedIn()) {
      router.push({ name: 'login', query: { redirect: '/goi-dich-vu' } });
      return;
    }
    dangTao.value = true;
    try {
      don.value = await createOrderApi('PREMIUM');
      batDauHoi();
    } catch (e) {
      loi.value = (e as Error).message;
    } finally {
      dangTao.value = false;
    }
  }

  /** Hỏi trạng thái đơn mỗi 3 giây — SePay báo tiền về qua webhook, trang không tự biết được */
  function batDauHoi() {
    window.clearInterval(hoi);
    hoi = window.setInterval(async () => {
      if (!don.value) return;
      try {
        don.value = await orderStatusApi(don.value.id);
      } catch {
        return;
      }
      if (don.value.status !== 'PENDING') {
        window.clearInterval(hoi);
        if (don.value.status === 'PAID') {
          // Nạp lại hồ sơ để mọi trang Premium mở khoá ngay, không cần đăng nhập lại
          await auth.refreshProfile();
          sub.value = await mySubscriptionApi();
        }
      }
    }, 3000);
  }

  async function chep(text: string) {
    try {
      await navigator.clipboard.writeText(text);
    } catch {
      // Trình duyệt chặn clipboard: người dùng vẫn đọc được chữ ngay bên cạnh
    }
  }

  onMounted(async () => {
    dongHo = window.setInterval(() => (bayGio.value = Date.now()), 1000);
    try {
      plans.value = await plansApi();
      if (auth.isLoggedIn()) sub.value = await mySubscriptionApi();
    } catch (e) {
      loi.value = (e as Error).message;
    }
  });

  onBeforeUnmount(() => {
    window.clearInterval(hoi);
    window.clearInterval(dongHo);
  });
</script>

<style scoped>
  .pricing-page {
    display: flex;
    flex-direction: column;
    gap: 20px;
  }
  .page-head h1 {
    margin: 0;
  }
  .page-sub {
    margin: 4px 0 0;
    color: var(--sk-brown);
  }

  .notice {
    display: flex;
    align-items: center;
    gap: 8px;
    margin: 0;
    padding: 12px 16px;
    border-radius: var(--sk-r-card);
    background: var(--sk-peach);
    color: var(--sk-brown-dark);
  }
  .notice--ok {
    background: var(--sk-mint);
    color: var(--sk-green-dark);
  }

  .plan-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
    gap: 20px;
  }
  .plan-card {
    display: flex;
    flex-direction: column;
    gap: 8px;
    padding: 26px;
    border-radius: var(--sk-r-block);
    background: var(--sk-surface);
    box-shadow: var(--sk-shadow-card);
    border: 3px solid transparent;
  }
  .plan-card--premium {
    border-color: var(--sk-amber);
  }
  .plan-tag {
    margin: 0;
    font-family: var(--sk-font-head);
    font-weight: 700;
    color: var(--sk-amber-ink);
    text-transform: uppercase;
    font-size: 13px;
  }
  .plan-card h2 {
    margin: 0;
    font-size: 26px;
  }
  .plan-price {
    margin: 0;
    font-family: var(--sk-font-head);
    font-size: 32px;
    font-weight: 700;
    color: var(--sk-blue-ink);
  }
  .plan-price small {
    font-size: 15px;
    color: var(--sk-brown);
  }
  .plan-desc,
  .plan-includes {
    margin: 0;
    color: var(--sk-brown);
  }
  .plan-includes {
    font-weight: 700;
  }
  .benefits {
    list-style: none;
    margin: 0 0 8px;
    padding: 0;
    display: flex;
    flex-direction: column;
    gap: 6px;
  }
  .benefits li {
    display: flex;
    align-items: center;
    gap: 8px;
  }
  .benefits :deep(svg) {
    color: var(--sk-green-ink);
  }
  .plan-current {
    margin: auto 0 0;
    font-weight: 700;
    color: var(--sk-green-ink);
  }

  .btn-buy {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    margin-top: auto;
    min-height: var(--sk-tap);
    padding: 0 24px;
    border: none;
    border-radius: var(--sk-r-pill);
    background: var(--sk-amber);
    color: #2a1700; /* chữ tối cố định: nền cam giữ nguyên ở chế độ tối */
    font-family: var(--sk-font-head);
    font-weight: 700;
    font-size: 16px;
    text-decoration: none;
    cursor: pointer;
  }
  .btn-buy:disabled {
    opacity: 0.6;
    cursor: wait;
  }

  .checkout {
    padding: 26px;
    border-radius: var(--sk-r-block);
    background: var(--sk-surface);
    box-shadow: var(--sk-shadow-card);
  }
  .checkout h2 {
    margin: 0 0 14px;
  }
  .checkout-body {
    display: flex;
    gap: 28px;
    flex-wrap: wrap;
    align-items: center;
  }
  .qr {
    width: 260px;
    max-width: 100%;
    border-radius: 16px;
    border: 2px solid var(--sk-blue-150);
  }
  .bank-info {
    display: grid;
    grid-template-columns: auto 1fr;
    gap: 8px 16px;
    margin: 0;
  }
  .bank-info dt {
    color: var(--sk-brown);
  }
  .bank-info dd {
    margin: 0;
    font-weight: 600;
  }
  .bank-info .strong {
    font-size: 18px;
    color: var(--sk-blue-ink);
  }
  .copy {
    margin-left: 8px;
    padding: 2px 10px;
    border-radius: var(--sk-r-pill);
    border: 1px solid var(--sk-blue-150);
    background: var(--sk-lavender);
    cursor: pointer;
    font-size: 12px;
  }
  .checkout-hint {
    display: flex;
    align-items: center;
    gap: 6px;
    margin: 16px 0 0;
    color: var(--sk-brown);
  }
  .paid {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 8px;
    text-align: center;
    color: var(--sk-green-ink);
  }
  .paid p {
    color: var(--sk-ink);
  }
  .error-text {
    color: #c4503f;
    font-weight: 600;
  }
</style>
