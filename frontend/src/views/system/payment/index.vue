<template>
  <PageWrapper dense contentFullHeight>
    <div class="pay-summary">
      <div v-for="s in summaryItems" :key="s.label" class="pay-summary-item" :class="{ 'is-warn': s.warn }">
        <span>{{ s.label }}</span>
        <b>{{ s.value }}</b>
      </div>
    </div>

    <Tabs v-model:activeKey="tab" class="pay-tabs">
      <TabPane key="orders" tab="Đơn thanh toán">
        <BasicTable @register="registerOrders" />
      </TabPane>
      <TabPane key="transactions" tab="Giao dịch SePay (đối soát)">
        <Alert
          type="info"
          show-icon
          class="!mb-3"
          message="Mọi giao dịch SePay báo về đều được lưu, kể cả giao dịch không khớp đơn nào. Giao dịch 'Chưa khớp' là tiền đã về tài khoản nhưng hệ thống chưa cộng Premium cho ai — đọc cột Ghi chú để biết vì sao rồi xử lý tay."
        />
        <BasicTable @register="registerTransactions" />
      </TabPane>
    </Tabs>
  </PageWrapper>
</template>

<script lang="ts" setup>
  import { computed, onMounted, ref } from 'vue';
  import { useRoute } from 'vue-router';
  import { Alert, Tabs } from 'ant-design-vue';
  import { BasicTable, useTable } from '@/components/Table';
  import { PageWrapper } from '@/components/Page';
  import { paymentOrdersApi, paymentSummaryApi, sepayTransactionsApi, type PaymentSummaryModel } from '@/api/system/payment';
  import {
    money,
    orderColumns,
    orderSearchSchema,
    transactionColumns,
    transactionSearchSchema,
    vnRange,
  } from './payment.data';

  defineOptions({ name: 'PaymentManagement' });

  const TabPane = Tabs.TabPane;

  const route = useRoute();
  const tab = ref(route.query.tab === 'transactions' ? 'transactions' : 'orders');
  const summary = ref<PaymentSummaryModel>();

  // Tổng hợp 30 ngày gần nhất — số đầu trang không chạy theo bộ lọc của từng bảng
  const summaryItems = computed(() => {
    const s = summary.value;
    return [
      { label: 'Doanh thu 30 ngày', value: s ? money(s.revenue) : '…' },
      { label: 'Đơn đã thanh toán', value: s?.paidOrders.toLocaleString('vi-VN') ?? '…' },
      { label: 'Người trả tiền', value: s?.payingUsers.toLocaleString('vi-VN') ?? '…' },
      { label: 'Đơn đang chờ', value: s?.pendingOrders.toLocaleString('vi-VN') ?? '…' },
      { label: 'Đơn hết hạn', value: s?.expiredOrders.toLocaleString('vi-VN') ?? '…' },
      {
        label: 'Giao dịch chưa khớp',
        value: s?.unmatchedTransactions.toLocaleString('vi-VN') ?? '…',
        warn: (s?.unmatchedTransactions ?? 0) > 0,
      },
    ];
  });

  const [registerOrders] = useTable({
    title: 'Đơn thanh toán của người dùng',
    api: async (p: Recordable) => {
      const { page = 1, size = 20, keyword, status, from, to } = p;
      return paymentOrdersApi({ keyword, status, ...vnRange(from, to), page: page - 1, size });
    },
    rowKey: 'id',
    columns: orderColumns,
    formConfig: {
      labelWidth: 90,
      schemas: orderSearchSchema,
      fieldMapToTime: [['range', ['from', 'to'], 'YYYY-MM-DD']],
    },
    useSearchForm: true,
    fetchSetting: { sizeField: 'size' },
    showTableSetting: true,
    bordered: true,
    scroll: { x: 1300 },
  });

  const [registerTransactions] = useTable({
    title: 'Giao dịch ngân hàng SePay báo về',
    api: async (p: Recordable) => {
      const { page = 1, size = 20, keyword, matched, from, to } = p;
      return sepayTransactionsApi({
        keyword,
        matched: matched === undefined || matched === null || matched === '' ? undefined : matched === 'true',
        ...vnRange(from, to),
        page: page - 1,
        size,
      });
    },
    rowKey: 'id',
    columns: transactionColumns,
    formConfig: {
      labelWidth: 90,
      schemas: transactionSearchSchema,
      fieldMapToTime: [['range', ['from', 'to'], 'YYYY-MM-DD']],
    },
    useSearchForm: true,
    fetchSetting: { sizeField: 'size' },
    showTableSetting: true,
    bordered: true,
    scroll: { x: 1500 },
  });

  onMounted(async () => {
    const today = new Date();
    const start = new Date(today);
    start.setDate(today.getDate() - 29);
    const ymd = (d: Date) =>
      `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
    summary.value = await paymentSummaryApi(vnRange(ymd(start), ymd(today)));
  });
</script>

<style lang="less" scoped>
  .pay-summary {
    display: grid;
    grid-template-columns: repeat(6, minmax(0, 1fr));
    gap: 12px;
    margin: 12px 12px 12px;
  }

  .pay-summary-item {
    display: flex;
    flex-direction: column;
    gap: 4px;
    padding: 12px 16px;
    border: 1px solid @border-color-base;
    border-radius: 8px;
    background: @component-background;

    span {
      color: @text-color-secondary;
      font-size: 13px;
    }

    b {
      font-size: 20px;
      font-variant-numeric: tabular-nums;
    }

    &.is-warn b {
      color: #b0610a;
    }
  }

  .pay-tabs {
    margin: 0 12px 12px;
    padding: 0 12px 12px;
    border-radius: 8px;
    background: @component-background;
  }

  @media (max-width: 1100px) {
    .pay-summary {
      grid-template-columns: repeat(3, minmax(0, 1fr));
    }
  }
</style>
