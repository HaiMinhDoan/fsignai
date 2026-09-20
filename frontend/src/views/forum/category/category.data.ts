import type { BasicColumn, FormSchema } from '@/components/Table';
import { Tag } from 'ant-design-vue';
import { h } from 'vue';

export const columns: BasicColumn[] = [
  {
    title: 'Tên chuyên mục',
    dataIndex: 'nameVi',
    width: 220,
  },
  {
    title: 'Mã',
    dataIndex: 'slug',
    width: 180,
    customRender: ({ record }) => h('code', { style: 'font-size:12px;color:#5A7683' }, record.slug),
  },
  {
    title: 'Số bài (còn hiệu lực)',
    dataIndex: 'postCount',
    width: 150,
  },
  {
    title: 'Thứ tự',
    dataIndex: 'displayOrder',
    width: 90,
  },
  {
    title: 'Khoá đăng bài',
    dataIndex: 'isLocked',
    width: 120,
    customRender: ({ record }) =>
      record.isLocked
        ? h(Tag, { color: 'orange' }, () => 'Đã khoá')
        : h('span', { style: 'color:#9AB0BC' }, 'Mở'),
  },
  {
    title: 'Trạng thái',
    dataIndex: 'isPublished',
    width: 130,
    customRender: ({ record }) =>
      record.isPublished
        ? h(Tag, { color: 'success' }, () => 'Đang hiện')
        : h(Tag, { color: 'default' }, () => 'Đã ẩn'),
  },
];

export const categoryFormSchema: FormSchema[] = [
  {
    field: 'nameVi',
    label: 'Tên chuyên mục',
    component: 'Input',
    required: true,
    componentProps: { placeholder: 'Ví dụ: Hỏi đáp ký hiệu' },
  },
  {
    field: 'slug',
    label: 'Mã',
    component: 'Input',
    helpMessage: 'Để trống thì hệ thống tự sinh từ tên, đã bỏ dấu tiếng Việt',
    componentProps: { placeholder: 'hoi-dap-ky-hieu' },
  },
  {
    field: 'descriptionVi',
    label: 'Mô tả',
    component: 'InputTextArea',
    componentProps: { rows: 3 },
  },
  {
    field: 'displayOrder',
    label: 'Thứ tự hiển thị',
    component: 'InputNumber',
    defaultValue: 0,
    componentProps: { min: 0 },
  },
  {
    field: 'isLocked',
    label: 'Khoá không cho đăng bài mới',
    component: 'Switch',
    defaultValue: false,
  },
  {
    field: 'isPublished',
    label: 'Hiện với người dùng',
    component: 'Switch',
    defaultValue: true,
  },
];
