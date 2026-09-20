import type { BasicColumn, FormSchema } from '@/components/Table';
import { h } from 'vue';
import { Tag } from 'ant-design-vue';
import { topicOptionsApi } from '@/api/content/topic';

export const QUIZ_TYPE_OPTIONS = [
  { label: 'Đề của bài học', value: 'LESSON_QUIZ' },
  { label: 'Đề kiểm tra chủ đề', value: 'TOPIC_TEST' },
  { label: 'Luyện tập tự do', value: 'PRACTICE' },
];

export const QUESTION_TYPE_OPTIONS = [
  { label: 'Xem video, chọn từ', value: 'VIDEO_TO_WORD' },
  { label: 'Đọc từ, chọn video', value: 'WORD_TO_VIDEO' },
  { label: 'Trắc nghiệm', value: 'MULTIPLE_CHOICE' },
  { label: 'Ghép đôi', value: 'MATCHING' },
  { label: 'Tự thực hiện ký hiệu', value: 'AI_PERFORM' },
];

export const DISTRACTOR_OPTIONS = [
  { label: 'Ưu tiên từ dễ nhầm', value: 'EASILY_CONFUSED' },
  { label: 'Cùng chủ đề', value: 'SAME_TOPIC' },
  { label: 'Trộn cả hai', value: 'MIXED' },
];

export const questionTypeLabel = (v?: string) =>
  QUESTION_TYPE_OPTIONS.find((o) => o.value === v)?.label ?? v ?? '';

const quizTypeLabel = (v?: string) => QUIZ_TYPE_OPTIONS.find((o) => o.value === v)?.label ?? v ?? '';

export const columns: BasicColumn[] = [
  {
    title: 'Tên đề',
    dataIndex: 'titleVi',
    width: 260,
    fixed: 'left',
  },
  {
    title: 'Loại',
    dataIndex: 'quizType',
    width: 170,
    customRender: ({ record }) => quizTypeLabel(record.quizType),
  },
  {
    title: 'Thuộc bài học',
    dataIndex: 'lessonTitleVi',
    width: 200,
    customRender: ({ record }) =>
      record.lessonTitleVi ?? h('span', { style: 'color:#9AB0BC' }, '— không gắn —'),
  },
  {
    title: 'Chủ đề',
    dataIndex: 'topicNameVi',
    width: 160,
    customRender: ({ record }) =>
      record.topicNameVi ?? h('span', { style: 'color:#9AB0BC' }, '— chưa gán —'),
  },
  {
    title: 'Số câu',
    dataIndex: 'questionCount',
    width: 110,
    customRender: ({ record }) => {
      const count = record.questionCount ?? 0;
      // Đề rỗng không xuất bản được — cho thấy ngay thay vì để phát hiện lúc bấm nút
      return count === 0
        ? h(Tag, { color: 'orange' }, () => 'Chưa có câu')
        : h('span', {}, String(count));
    },
  },
  {
    title: 'Điểm đạt',
    dataIndex: 'passScore',
    width: 100,
    customRender: ({ record }) => `${record.passScore}%`,
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
    componentProps: { placeholder: 'Tên đề' },
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
    field: 'quizType',
    label: 'Loại đề',
    component: 'Select',
    componentProps: { options: QUIZ_TYPE_OPTIONS, allowClear: true, placeholder: 'Mọi loại' },
    colProps: { span: 6 },
  },
  {
    field: 'isPublished',
    label: 'Trạng thái',
    component: 'Select',
    componentProps: {
      // Ant Design Vue không nhận boolean làm value của Select nên dùng chuỗi
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

export const quizFormSchema: FormSchema[] = [
  {
    field: 'titleVi',
    label: 'Tên đề',
    component: 'Input',
    required: true,
    componentProps: { placeholder: 'Ví dụ: Kiểm tra chủ đề Gia đình' },
  },
  {
    field: 'descriptionVi',
    label: 'Mô tả',
    component: 'InputTextArea',
    componentProps: { rows: 3 },
  },
  {
    field: 'quizType',
    label: 'Loại đề',
    component: 'Select',
    defaultValue: 'TOPIC_TEST',
    componentProps: { options: QUIZ_TYPE_OPTIONS },
  },
  {
    field: 'topicId',
    label: 'Chủ đề',
    component: 'ApiSelect',
    helpMessage: 'Dùng làm nguồn từ vựng mặc định khi sinh câu hỏi tự động',
    componentProps: {
      api: topicOptionsApi,
      labelField: 'nameVi',
      valueField: 'id',
      placeholder: 'Chọn chủ đề',
      allowClear: true,
    },
  },
  {
    field: 'passScore',
    label: 'Điểm đạt (%)',
    component: 'InputNumber',
    defaultValue: 70,
    componentProps: { min: 0, max: 100 },
  },
  {
    field: 'timeLimitSeconds',
    label: 'Giới hạn thời gian (giây)',
    component: 'InputNumber',
    helpMessage: 'Để trống nghĩa là không giới hạn',
    componentProps: { min: 1 },
  },
  {
    field: 'isPublished',
    label: 'Xuất bản',
    component: 'Switch',
    defaultValue: false,
    helpMessage: 'Chỉ xuất bản được khi đề đã có ít nhất một câu hỏi',
  },
];
