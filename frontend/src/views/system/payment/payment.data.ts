import type { BasicColumn, FormSchema } from '@/components/Table';
import { Tag } from 'ant-design-vue';
import { h } from 'vue';

export const PAYMENT_STATUS_OPTIONS = [
  { label: 'Đã thanh toán', value: 'PAID' },
  { label: 'Chờ chuyển khoản', value: 'PENDING' },
  { label: 'Hết hạn', value: 'EXPIRED' },
];

const STATUS_COLOR: Record<string, string> = {
  PAID: 'success',
  PENDING: 'processing',
  EXPIRED: 'default',
  CANCELLED: 'default',
};
const STATUS_LABEL: Record<string, string> = {
  PAID: 'Đã thanh toán',
  PENDING: 'Chờ chuyển khoản',
  EXPIRED: 'Hết hạn',
  CANCELLED: 'Đã huỷ',
};

export const money = (v?: number | null) => (v == null ? '—' : `${v.toLocaleString('vi-VN')}đ`);
export const dateTime = (iso?: string | null) => (iso ? new Date(iso).toLocaleString('vi-VN') : '—');

export const paymentStatusTag = (status: string) =>
  h(Tag, { color: STATUS_COLOR[status] ?? 'default' }, () => STATUS_LABEL[status] ?? status);

/**
 * Khoảng ngày của bộ lọc ('YYYY-MM-DD') → mốc ISO theo giờ Việt Nam cho backend.
 * `to` là 0 giờ của NGÀY HÔM SAU để chọn "đến 04/10" thì vẫn tính cả ngày 04/10.
 */
export function vnRange(from?: string, to?: string) {
  const next = (d: string) => {
    const t = new Date(`${d}T00:00:00+07:00`);
    t.setUTCDate(t.getUTCDate() + 1);
    return t.toISOString();
  };
  return {
    from: from ? `${from}T00:00:00+07:00` : undefined,
    to: to ? next(to) : undefined,
  };
}

export const orderColumns: BasicColumn[] = [
  { title: 'Ngày tạo', dataIndex: 'createdAt', width: 160, customRender: ({ record }) => dateTime(record.createdAt) },
  {
    title: 'Người dùng',
    dataIndex: 'userName',
    width: 220,
    customRender: ({ record }) =>
      h('div', {}, [h('div', {}, record.userName), h('small', { style: 'color:#8b949e' }, record.userEmail)]),
  },
  { title: 'Mã thanh toán', dataIndex: 'paymentCode', width: 170 },
  {
    title: 'Gói',
    dataIndex: 'planCode',
    width: 130,
    customRender: ({ record }) => `Premium${record.durationDays ? ` ${record.durationDays} ngày` : ''}`,
  },
  {
    title: 'Số tiền',
    dataIndex: 'amount',
    width: 120,
    align: 'right',
    customRender: ({ record }) => money(record.paidAmount ?? record.amount),
  },
  { title: 'Trạng thái', dataIndex: 'status', width: 140, customRender: ({ record }) => paymentStatusTag(record.status) },
  { title: 'Thanh toán lúc', dataIndex: 'paidAt', width: 160, customRender: ({ record }) => dateTime(record.paidAt) },
  { title: 'Mã GD SePay', dataIndex: 'sepayTransactionId', width: 120, customRender: ({ record }) => record.sepayTransactionId ?? '—' },
];

export const orderSearchSchema: FormSchema[] = [
  {
    field: 'keyword',
    label: 'Tìm kiếm',
    component: 'Input',
    componentProps: { placeholder: 'Tên, email hoặc mã thanh toán' },
    colProps: { span: 7 },
  },
  {
    field: 'status',
    label: 'Trạng thái',
    component: 'Select',
    componentProps: { options: PAYMENT_STATUS_OPTIONS, allowClear: true, placeholder: 'Tất cả' },
    colProps: { span: 5 },
  },
  {
    field: 'range',
    label: 'Ngày tạo',
    component: 'RangePicker',
    componentProps: { format: 'DD/MM/YYYY', placeholder: ['Từ ngày', 'Đến ngày'] },
    colProps: { span: 8 },
  },
];

export const transactionColumns: BasicColumn[] = [
  { title: 'Nhận lúc', dataIndex: 'createdAt', width: 160, customRender: ({ record }) => dateTime(record.createdAt) },
  { title: 'Ngân hàng', dataIndex: 'gateway', width: 110 },
  {
    title: 'Loại',
    dataIndex: 'transferType',
    width: 90,
    customRender: ({ record }) =>
      h(Tag, { color: record.transferType === 'in' ? 'green' : 'default' }, () =>
        record.transferType === 'in' ? 'Tiền vào' : 'Tiền ra',
      ),
  },
  { title: 'Số tiền', dataIndex: 'transferAmount', width: 120, align: 'right', customRender: ({ record }) => money(record.transferAmount) },
  { title: 'Nội dung chuyển khoản', dataIndex: 'content', width: 260, ellipsis: true },
  {
    title: 'Khớp đơn',
    dataIndex: 'matchedPaymentCode',
    width: 200,
    customRender: ({ record }) =>
      record.matchedOrderId
        ? h('div', {}, [h('div', {}, record.matchedPaymentCode), h('small', { style: 'color:#8b949e' }, record.matchedUserName)])
        : h(Tag, { color: 'warning' }, () => 'Chưa khớp'),
  },
  { title: 'Ghi chú hệ thống', dataIndex: 'note', width: 280, ellipsis: true },
  { title: 'Mã tham chiếu', dataIndex: 'referenceCode', width: 140 },
];

export const transactionSearchSchema: FormSchema[] = [
  {
    field: 'keyword',
    label: 'Tìm kiếm',
    component: 'Input',
    componentProps: { placeholder: 'Nội dung, mã, mã tham chiếu' },
    colProps: { span: 7 },
  },
  {
    field: 'matched',
    label: 'Khớp đơn',
    component: 'Select',
    componentProps: {
      allowClear: true,
      placeholder: 'Tất cả',
      options: [
        { label: 'Đã khớp đơn', value: 'true' },
        { label: 'Chưa khớp (cần xử lý tay)', value: 'false' },
      ],
    },
    colProps: { span: 5 },
  },
  {
    field: 'range',
    label: 'Ngày nhận',
    component: 'RangePicker',
    componentProps: { format: 'DD/MM/YYYY', placeholder: ['Từ ngày', 'Đến ngày'] },
    colProps: { span: 8 },
  },
];
