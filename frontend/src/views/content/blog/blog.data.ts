import type { BasicColumn, FormSchema } from '@/components/Table';
import { Tag } from 'ant-design-vue';
import { h } from 'vue';

export const CATEGORY_OPTIONS = [
  { label: 'Blog', value: 'BLOG' },
  { label: 'Sứ mệnh (Our Mission)', value: 'MISSION' },
  { label: 'Hướng dẫn', value: 'GUIDE' },
  { label: 'Tin tức', value: 'NEWS' },
];

export const columns: BasicColumn[] = [
  {
    title: 'Tiêu đề',
    dataIndex: 'titleVi',
    width: 260,
  },
  {
    title: 'Mã',
    dataIndex: 'slug',
    width: 200,
    customRender: ({ record }) => h('code', { style: 'font-size:12px;color:#5A7683' }, record.slug),
  },
  {
    title: 'Chuyên mục',
    dataIndex: 'category',
    width: 150,
    customRender: ({ record }) => CATEGORY_OPTIONS.find((o) => o.value === record.category)?.label ?? record.category,
  },
  {
    title: 'Tác giả',
    dataIndex: 'authorName',
    width: 140,
  },
  {
    title: 'Lượt xem',
    dataIndex: 'viewCount',
    width: 90,
  },
  {
    title: 'Trạng thái',
    dataIndex: 'isPublished',
    width: 130,
    customRender: ({ record }) =>
      record.isPublished
        ? h(Tag, { color: 'success' }, () => 'Đã xuất bản')
        : h(Tag, { color: 'default' }, () => 'Bản nháp'),
  },
];

export const searchFormSchema: FormSchema[] = [
  {
    field: 'category',
    label: 'Chuyên mục',
    component: 'Select',
    componentProps: { options: CATEGORY_OPTIONS, allowClear: true, placeholder: 'Tất cả' },
    colProps: { span: 6 },
  },
  {
    field: 'isPublished',
    label: 'Trạng thái',
    component: 'Select',
    componentProps: {
      options: [
        { label: 'Đã xuất bản', value: 'true' },
        { label: 'Bản nháp', value: 'false' },
      ],
      allowClear: true,
      placeholder: 'Tất cả',
    },
    colProps: { span: 6 },
  },
];

export const blogFormSchema: FormSchema[] = [
  {
    field: 'titleVi',
    label: 'Tiêu đề',
    component: 'Input',
    required: true,
  },
  {
    field: 'slug',
    label: 'Mã',
    component: 'Input',
    helpMessage: 'Để trống thì hệ thống tự sinh từ tiêu đề',
  },
  {
    field: 'category',
    label: 'Chuyên mục',
    component: 'Select',
    defaultValue: 'BLOG',
    componentProps: { options: CATEGORY_OPTIONS },
  },
  {
    field: 'excerptVi',
    label: 'Tóm tắt',
    component: 'InputTextArea',
    componentProps: { rows: 2 },
  },
  {
    field: 'contentMd',
    label: 'Nội dung (Markdown)',
    component: 'InputTextArea',
    required: true,
    componentProps: { rows: 12 },
  },
  {
    field: 'isPublished',
    label: 'Xuất bản',
    component: 'Switch',
    defaultValue: false,
  },
];
