import type { BasicColumn, FormSchema } from '@/components/Table';

export const columns: BasicColumn[] = [
  {
    title: 'Thời gian',
    dataIndex: 'createdAt',
    width: 160,
    customRender: ({ record }) => new Date(record.createdAt).toLocaleString('vi-VN'),
  },
  {
    title: 'Người thực hiện',
    dataIndex: 'actorName',
    width: 160,
    customRender: ({ record }) => record.actorName ?? '(hệ thống)',
  },
  {
    title: 'Hành động',
    dataIndex: 'action',
    width: 200,
  },
  {
    title: 'Đối tượng',
    dataIndex: 'entityType',
    width: 140,
  },
  {
    title: 'Mã đối tượng',
    dataIndex: 'entityId',
    width: 280,
    customRender: ({ record }) => (record.entityId ? String(record.entityId) : '—'),
  },
  {
    title: 'Địa chỉ IP',
    dataIndex: 'ipAddress',
    width: 130,
    customRender: ({ record }) => record.ipAddress ?? '—',
  },
];

export const searchFormSchema: FormSchema[] = [
  {
    field: 'action',
    label: 'Hành động',
    component: 'Input',
    componentProps: { placeholder: 'Ví dụ: user.roles.update' },
    colProps: { span: 8 },
  },
  {
    field: 'entityType',
    label: 'Đối tượng',
    component: 'Input',
    componentProps: { placeholder: 'Ví dụ: forum_posts' },
    colProps: { span: 8 },
  },
];
