import type { BasicColumn, FormSchema } from '@/components/Table';
import { Tag } from 'ant-design-vue';
import { h } from 'vue';

export const CHANNEL_OPTIONS = [
  { label: 'Trong app', value: 'IN_APP' },
  { label: 'Email', value: 'EMAIL' },
  { label: 'Push', value: 'PUSH' },
];

export const STATUS_OPTIONS = [
  { label: 'Đang chờ', value: 'PENDING' },
  { label: 'Đã gửi', value: 'SENT' },
  { label: 'Thất bại', value: 'FAILED' },
  { label: 'Bỏ qua', value: 'SKIPPED' },
];

const STATUS_COLOR: Record<string, string> = {
  PENDING: 'orange',
  SENT: 'success',
  FAILED: 'error',
  SKIPPED: 'default',
};

export const columns: BasicColumn[] = [
  { title: 'Người nhận', dataIndex: 'userName', width: 160 },
  { title: 'Loại', dataIndex: 'type', width: 160 },
  {
    title: 'Kênh',
    dataIndex: 'channel',
    width: 100,
    customRender: ({ record }) => CHANNEL_OPTIONS.find((o) => o.value === record.channel)?.label ?? record.channel,
  },
  { title: 'Tiêu đề', dataIndex: 'titleVi', width: 220 },
  {
    title: 'Trạng thái',
    dataIndex: 'status',
    width: 110,
    customRender: ({ record }) =>
      h(Tag, { color: STATUS_COLOR[record.status] ?? 'default' }, () =>
        STATUS_OPTIONS.find((o) => o.value === record.status)?.label ?? record.status,
      ),
  },
  {
    title: 'Lỗi',
    dataIndex: 'errorMessage',
    width: 200,
    customRender: ({ record }) => record.errorMessage ?? '—',
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
    field: 'channel',
    label: 'Kênh',
    component: 'Select',
    componentProps: { options: CHANNEL_OPTIONS, allowClear: true, placeholder: 'Tất cả' },
    colProps: { span: 6 },
  },
  {
    field: 'status',
    label: 'Trạng thái',
    component: 'Select',
    componentProps: { options: STATUS_OPTIONS, allowClear: true, placeholder: 'Tất cả' },
    colProps: { span: 6 },
  },
];
