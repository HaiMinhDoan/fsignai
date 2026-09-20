import type { BasicColumn, FormSchema } from '@/components/Table';
import { h } from 'vue';
import { Tag } from 'ant-design-vue';
import { topicOptionsApi } from '@/api/content/topic';

export const LEVEL_OPTIONS = [
  { label: 'Nhập môn', value: 'BEGINNER' },
  { label: 'Cơ bản', value: 'BASIC' },
  { label: 'Trung cấp', value: 'INTERMEDIATE' },
  { label: 'Nâng cao', value: 'ADVANCED' },
];

const levelLabel = (v?: string) => LEVEL_OPTIONS.find((o) => o.value === v)?.label ?? v ?? '';

const LEVEL_COLOR: Record<string, string> = {
  BEGINNER: 'green',
  BASIC: 'cyan',
  INTERMEDIATE: 'blue',
  ADVANCED: 'purple',
};

export const columns: BasicColumn[] = [
  {
    title: 'Tên khoá học',
    dataIndex: 'titleVi',
    width: 260,
    fixed: 'left',
    customRender: ({ record }) => {
      const nodes = [h('span', {}, record.titleVi)];
      // Đánh dấu khoá do máy sinh: nội dung là bản nháp, cần người xem lại
      if (record.generated) {
        nodes.push(
          h(Tag, { color: 'geekblue', style: 'margin-left:6px' }, () => 'Sinh tự động'),
        );
      }
      return h('div', {}, nodes);
    },
  },
  {
    title: 'Slug',
    dataIndex: 'slug',
    width: 180,
    customRender: ({ record }) => h('code', { style: 'font-size:12px;color:#5A7683' }, record.slug),
  },
  {
    title: 'Chủ đề',
    dataIndex: 'topicNameVi',
    width: 160,
    customRender: ({ record }) =>
      record.topicNameVi ?? h('span', { style: 'color:#9AB0BC' }, '— chưa gán —'),
  },
  {
    title: 'Cấp độ',
    dataIndex: 'level',
    width: 120,
    customRender: ({ record }) =>
      h(Tag, { color: LEVEL_COLOR[record.level] ?? 'default' }, () => levelLabel(record.level)),
  },
  {
    title: 'Số bài học',
    dataIndex: 'lessonCount',
    width: 120,
    customRender: ({ record }) => {
      const count = record.lessonCount ?? 0;
      // Khoá rỗng không xuất bản được — cho thấy ngay thay vì để phát hiện lúc bấm nút
      return count === 0
        ? h(Tag, { color: 'orange' }, () => 'Chưa có bài')
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
        ? h(Tag, { color: 'success' }, () => 'Đã xuất bản')
        : h(Tag, { color: 'default' }, () => 'Bản nháp'),
  },
];

export const searchFormSchema: FormSchema[] = [
  {
    field: 'keyword',
    label: 'Tìm kiếm',
    component: 'Input',
    componentProps: { placeholder: 'Tên khoá học' },
    colProps: { span: 6 },
  },
  {
    field: 'topicId',
    label: 'Chủ đề',
    component: 'ApiSelect',
    componentProps: {
      api: topicOptionsApi,
      labelField: 'nameVi',
      valueField: 'id',
      placeholder: 'Mọi chủ đề',
      allowClear: true,
    },
    colProps: { span: 6 },
  },
  {
    field: 'level',
    label: 'Cấp độ',
    component: 'Select',
    componentProps: { options: LEVEL_OPTIONS, allowClear: true, placeholder: 'Mọi cấp độ' },
    colProps: { span: 5 },
  },
  {
    field: 'isPublished',
    label: 'Trạng thái',
    component: 'Select',
    componentProps: {
      // Ant Design Vue không nhận boolean làm value của Select nên dùng chuỗi,
      // rồi đổi lại thành boolean khi dựng bộ lọc gửi lên backend.
      options: [
        { label: 'Đã xuất bản', value: 'true' },
        { label: 'Bản nháp', value: 'false' },
      ],
      allowClear: true,
      placeholder: 'Tất cả',
    },
    colProps: { span: 5 },
  },
];

export const courseFormSchema: FormSchema[] = [
  {
    field: 'titleVi',
    label: 'Tên khoá học',
    component: 'Input',
    required: true,
    componentProps: { placeholder: 'Ví dụ: Giao tiếp hằng ngày' },
  },
  {
    field: 'slug',
    label: 'Slug',
    component: 'Input',
    helpMessage: 'Để trống thì hệ thống tự sinh từ tên khoá, đã bỏ dấu tiếng Việt',
    componentProps: { placeholder: 'giao-tiep-hang-ngay' },
  },
  {
    field: 'descriptionVi',
    label: 'Mô tả',
    component: 'InputTextArea',
    componentProps: { rows: 3, placeholder: 'Khoá này dạy gì, dành cho ai' },
  },
  {
    field: 'topicId',
    label: 'Chủ đề',
    component: 'ApiSelect',
    componentProps: {
      api: topicOptionsApi,
      labelField: 'nameVi',
      valueField: 'id',
      placeholder: 'Chọn chủ đề nguồn từ vựng',
      allowClear: true,
    },
  },
  {
    field: 'level',
    label: 'Cấp độ',
    component: 'Select',
    defaultValue: 'BEGINNER',
    componentProps: { options: LEVEL_OPTIONS },
  },
  {
    field: 'displayOrder',
    label: 'Thứ tự hiển thị',
    component: 'InputNumber',
    defaultValue: 0,
    componentProps: { min: 0 },
  },
  {
    field: 'isPublished',
    label: 'Xuất bản',
    component: 'Switch',
    defaultValue: false,
    helpMessage: 'Chỉ xuất bản được khi khoá đã có ít nhất một bài học',
  },
];

export const lessonFormSchema: FormSchema[] = [
  {
    field: 'titleVi',
    label: 'Tên bài học',
    component: 'Input',
    required: true,
    componentProps: { placeholder: 'Ví dụ: Chào hỏi cơ bản' },
  },
  {
    field: 'descriptionVi',
    label: 'Mô tả',
    component: 'InputTextArea',
    componentProps: { rows: 2 },
  },
  {
    field: 'estimatedMinutes',
    label: 'Thời lượng (phút)',
    component: 'InputNumber',
    defaultValue: 5,
    componentProps: { min: 1, max: 600 },
  },
  {
    field: 'isPublished',
    label: 'Xuất bản',
    component: 'Switch',
    defaultValue: false,
    helpMessage: 'Chỉ xuất bản được khi bài đã có nội dung',
  },
];
