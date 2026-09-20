import type { BasicColumn, FormSchema } from '@/components/Table';
import { Tag } from 'ant-design-vue';
import { h } from 'vue';

const REASON_LABEL: Record<string, string> = {
  SPAM: 'Spam / quảng cáo',
  ABUSE: 'Ngôn từ xúc phạm',
  WRONG_SIGN: 'Ký hiệu sai',
  OFF_TOPIC: 'Lạc chủ đề',
  SENSITIVE: 'Nội dung nhạy cảm',
  OTHER: 'Khác',
};

const STATUS_COLOR: Record<string, string> = {
  OPEN: 'orange',
  REVIEWING: 'blue',
  RESOLVED: 'success',
  DISMISSED: 'default',
};
const STATUS_LABEL: Record<string, string> = {
  OPEN: 'Mới báo cáo',
  REVIEWING: 'Đang xem xét',
  RESOLVED: 'Đã xử lý',
  DISMISSED: 'Bỏ qua',
};

export const columns: BasicColumn[] = [
  {
    title: 'Nội dung bị báo cáo',
    dataIndex: 'targetPreview',
    width: 260,
    customRender: ({ record }) =>
      record.targetStillExists
        ? (record.targetPreview ?? '(không có xem trước)')
        : h('span', { style: 'color:#C4503F' }, 'Nội dung đã bị gỡ trước đó'),
  },
  {
    title: 'Loại',
    dataIndex: 'targetType',
    width: 100,
  },
  {
    title: 'Người báo cáo',
    dataIndex: 'reporterName',
    width: 150,
  },
  {
    title: 'Lý do',
    dataIndex: 'reason',
    width: 150,
    customRender: ({ record }) => REASON_LABEL[record.reason] ?? record.reason,
  },
  {
    title: 'Ghi chú',
    dataIndex: 'note',
    width: 200,
  },
  {
    title: 'Trạng thái',
    dataIndex: 'status',
    width: 120,
    customRender: ({ record }) =>
      h(Tag, { color: STATUS_COLOR[record.status] ?? 'default' }, () => STATUS_LABEL[record.status] ?? record.status),
  },
  {
    title: 'Thời gian',
    dataIndex: 'createdAt',
    width: 160,
    customRender: ({ record }) => new Date(record.createdAt).toLocaleString('vi-VN'),
  },
];

export const searchFormSchema: FormSchema[] = [
  {
    field: 'openOnly',
    label: 'Chỉ hiện chưa xử lý',
    component: 'Switch',
    defaultValue: true,
    colProps: { span: 6 },
  },
];
