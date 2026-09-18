import type { BasicColumn, FormSchema } from '@/components/Table';
import { h } from 'vue';
import { Tag } from 'ant-design-vue';
import { topicOptionsApi } from '@/api/content/topic';

export const CATEGORY_OPTIONS = [
  { label: 'Ký hiệu đơn giản', value: 'SIMPLE_SIGN' },
  { label: 'Ký hiệu phức tạp', value: 'COMPLEX_SIGN' },
  { label: 'Tình huống giao tiếp', value: 'SITUATION' },
];

const categoryLabel = (v?: string) => CATEGORY_OPTIONS.find((o) => o.value === v)?.label ?? v ?? '';

export const columns: BasicColumn[] = [
  {
    title: 'Tên chủ đề',
    dataIndex: 'nameVi',
    width: 220,
    fixed: 'left',
  },
  {
    title: 'Slug',
    dataIndex: 'slug',
    width: 180,
    customRender: ({ record }) => h('code', { style: 'font-size:12px;color:#5A7683' }, record.slug),
  },
  {
    title: 'Chủ đề cha',
    dataIndex: 'parentNameVi',
    width: 160,
    customRender: ({ record }) =>
      record.parentNameVi ?? h('span', { style: 'color:#9AB0BC' }, '— gốc —'),
  },
  {
    title: 'Phân loại',
    dataIndex: 'category',
    width: 170,
    customRender: ({ record }) => categoryLabel(record.category),
  },
  {
    title: 'Số từ vựng',
    dataIndex: 'signCount',
    width: 110,
    customRender: ({ record }) => {
      const count = record.signCount ?? 0;
      // Chủ đề rỗng là chủ đề chưa dùng được — làm nổi để admin thấy ngay
      return count === 0
        ? h(Tag, { color: 'orange' }, () => 'Chưa có từ')
        : h('span', {}, String(count));
    },
  },
  {
    title: 'Thứ tự',
    dataIndex: 'displayOrder',
    width: 90,
  },
  {
    title: 'Trạng thái',
    dataIndex: 'isPublished',
    width: 130,
    customRender: ({ record }) =>
      record.isPublished
        ? h(Tag, { color: 'green' }, () => 'Đã xuất bản')
        : h(Tag, {}, () => 'Bản nháp'),
  },
];

export const topicFormSchema: FormSchema[] = [
  {
    field: 'nameVi',
    label: 'Tên chủ đề',
    component: 'Input',
    required: true,
    componentProps: { placeholder: 'Ví dụ: Giao tiếp hằng ngày' },
  },
  {
    field: 'slug',
    label: 'Slug',
    component: 'Input',
    componentProps: { placeholder: 'Bỏ trống để tự sinh: "Gia đình" → gia-dinh' },
    helpMessage: 'Dùng trong URL. Để trống thì hệ thống tự sinh từ tên chủ đề.',
  },
  {
    field: 'parentId',
    label: 'Chủ đề cha',
    component: 'ApiSelect',
    componentProps: {
      api: topicOptionsApi,
      labelField: 'nameVi',
      valueField: 'id',
      allowClear: true,
      placeholder: 'Để trống nếu là chủ đề gốc',
    },
  },
  {
    field: 'category',
    label: 'Phân loại',
    component: 'Select',
    required: true,
    componentProps: { options: CATEGORY_OPTIONS },
    defaultValue: 'SIMPLE_SIGN',
  },
  {
    field: 'iconName',
    label: 'Tên icon',
    component: 'Input',
    componentProps: { placeholder: 'family, food, school...' },
    helpMessage: 'Tên icon logic. Đổi bộ icon sau này không phải sửa dữ liệu.',
  },
  {
    field: 'displayOrder',
    label: 'Thứ tự hiển thị',
    component: 'InputNumber',
    defaultValue: 0,
    componentProps: { min: 0, style: { width: '100%' } },
  },
  {
    field: 'descriptionVi',
    label: 'Mô tả',
    component: 'InputTextArea',
    componentProps: { rows: 3 },
  },
  {
    field: 'isPublished',
    label: 'Xuất bản',
    component: 'Switch',
    defaultValue: false,
  },
];
