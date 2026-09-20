import type { BasicColumn, FormSchema } from '@/components/Table';
import { Tag } from 'ant-design-vue';
import { h } from 'vue';

export const STATUS_OPTIONS = [
  { label: 'Đang hoạt động', value: 'ACTIVE' },
  { label: 'Chờ xác minh', value: 'PENDING_VERIFICATION' },
  { label: 'Vô hiệu hoá', value: 'DISABLED' },
  { label: 'Đã khoá', value: 'BANNED' },
];

export const ACCOUNT_KIND_OPTIONS = [
  { label: 'Học sinh (trẻ em)', value: 'CHILD' },
  { label: 'Người lớn', value: 'ADULT' },
  { label: 'Phụ huynh', value: 'PARENT' },
  { label: 'Giáo viên', value: 'TEACHER' },
];

export const VSL_ROLE_STATUS_OPTIONS = [
  { label: 'Tự khai (không cần duyệt)', value: 'SELF_DECLARED' },
  { label: 'Chờ duyệt', value: 'PENDING' },
  { label: 'Đã xác minh', value: 'VERIFIED' },
  { label: 'Bị từ chối', value: 'REJECTED' },
];

const STATUS_COLOR: Record<string, string> = {
  ACTIVE: 'success',
  PENDING_VERIFICATION: 'orange',
  DISABLED: 'default',
  BANNED: 'error',
};
const STATUS_LABEL: Record<string, string> = {
  ACTIVE: 'Đang hoạt động',
  PENDING_VERIFICATION: 'Chờ xác minh',
  DISABLED: 'Vô hiệu hoá',
  BANNED: 'Đã khoá',
};

const VSL_STATUS_COLOR: Record<string, string> = {
  SELF_DECLARED: 'default',
  PENDING: 'orange',
  VERIFIED: 'success',
  REJECTED: 'error',
};
const VSL_STATUS_LABEL: Record<string, string> = {
  SELF_DECLARED: 'Tự khai',
  PENDING: 'Chờ duyệt',
  VERIFIED: 'Đã xác minh',
  REJECTED: 'Bị từ chối',
};

export const columns: BasicColumn[] = [
  {
    title: 'Họ tên',
    dataIndex: 'fullName',
    width: 180,
  },
  {
    title: 'Email',
    dataIndex: 'email',
    width: 220,
  },
  {
    title: 'Loại tài khoản',
    dataIndex: 'accountKind',
    width: 130,
    customRender: ({ record }) =>
      ACCOUNT_KIND_OPTIONS.find((o) => o.value === record.accountKind)?.label ?? record.accountKind,
  },
  {
    title: 'Vai trò hệ thống',
    dataIndex: 'roleCodes',
    width: 180,
    customRender: ({ record }) =>
      h(
        'span',
        {},
        (record.roleCodes as string[]).map((c) => h(Tag, { key: c }, () => c)),
      ),
  },
  {
    title: 'VSL',
    dataIndex: 'vslRoleStatus',
    width: 120,
    customRender: ({ record }) =>
      h(Tag, { color: VSL_STATUS_COLOR[record.vslRoleStatus] ?? 'default' }, () =>
        VSL_STATUS_LABEL[record.vslRoleStatus] ?? record.vslRoleStatus,
      ),
  },
  {
    title: 'Trạng thái',
    dataIndex: 'status',
    width: 130,
    customRender: ({ record }) =>
      h(Tag, { color: STATUS_COLOR[record.status] ?? 'default' }, () => STATUS_LABEL[record.status] ?? record.status),
  },
  {
    title: 'Đăng nhập gần nhất',
    dataIndex: 'lastLoginAt',
    width: 160,
    customRender: ({ record }) =>
      record.lastLoginAt ? new Date(record.lastLoginAt).toLocaleString('vi-VN') : '—',
  },
];

export const searchFormSchema: FormSchema[] = [
  {
    field: 'keyword',
    label: 'Tìm kiếm',
    component: 'Input',
    componentProps: { placeholder: 'Tên hoặc email' },
    colProps: { span: 6 },
  },
  {
    field: 'accountKind',
    label: 'Loại tài khoản',
    component: 'Select',
    componentProps: { options: ACCOUNT_KIND_OPTIONS, allowClear: true, placeholder: 'Tất cả' },
    colProps: { span: 5 },
  },
  {
    field: 'vslRoleStatus',
    label: 'Duyệt VSL',
    component: 'Select',
    componentProps: { options: VSL_ROLE_STATUS_OPTIONS, allowClear: true, placeholder: 'Tất cả' },
    colProps: { span: 5 },
  },
  {
    field: 'status',
    label: 'Trạng thái',
    component: 'Select',
    componentProps: { options: STATUS_OPTIONS, allowClear: true, placeholder: 'Tất cả' },
    colProps: { span: 5 },
  },
];
