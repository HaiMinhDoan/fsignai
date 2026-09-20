import type { BasicColumn, FormSchema } from '@/components/Table';
import { h } from 'vue';
import { Tag } from 'ant-design-vue';
import { topicOptionsApi } from '@/api/content/topic';
import { LEVEL_OPTIONS, UNIT_TYPE_OPTIONS, WORD_TYPE_OPTIONS } from '../sign/sign.data';
import { DISTRACTOR_OPTIONS } from '../quiz/quiz.data';

// Chỉ 4 dạng câu này có "lựa chọn" để trộn — AI_PERFORM là người học tự thực
// hiện trước webcam, không có gì để rút ngẫu nhiên theo tỉ lệ ở đây cả.
export const MIX_QUESTION_TYPES = [
  { value: 'VIDEO_TO_WORD', label: 'Xem video, chọn từ' },
  { value: 'WORD_TO_VIDEO', label: 'Đọc từ, chọn video' },
  { value: 'MULTIPLE_CHOICE', label: 'Trắc nghiệm' },
  { value: 'MATCHING', label: 'Ghép đôi' },
] as const;

export const columns: BasicColumn[] = [
  {
    title: 'Tên cấu hình',
    dataIndex: 'titleVi',
    width: 240,
    fixed: 'left',
  },
  {
    title: 'Mã',
    dataIndex: 'code',
    width: 160,
  },
  {
    title: 'Chủ đề',
    dataIndex: 'topicNames',
    width: 220,
    customRender: ({ record }) => {
      const names: string[] = record.topicNames ?? [];
      return names.length
        ? names.join(', ')
        : h('span', { style: 'color:#9AB0BC' }, 'Mọi chủ đề');
    },
  },
  {
    title: 'Số câu / Số từ khớp',
    dataIndex: 'matchingSignCount',
    width: 160,
    customRender: ({ record }) => {
      const need = record.questionCount ?? 0;
      const has = record.matchingSignCount ?? 0;
      // Đủ từ để rút thì hiện màu thường; thiếu thì cảnh báo ngay ở bảng danh
      // sách — admin không phải bấm vào từng cấu hình mới biết đề sẽ bị thiếu câu.
      return has < need
        ? h(Tag, { color: 'warning' }, () => `${has} / ${need} từ`)
        : h('span', {}, `${has} / ${need} từ`);
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
    dataIndex: 'isActive',
    width: 120,
    customRender: ({ record }) =>
      record.isActive
        ? h(Tag, { color: 'success' }, () => 'Đang dùng')
        : h(Tag, { color: 'default' }, () => 'Đã tắt'),
  },
];

export const searchFormSchema: FormSchema[] = [
  {
    field: 'keyword',
    label: 'Tìm kiếm',
    component: 'Input',
    componentProps: { placeholder: 'Tên cấu hình' },
    colProps: { span: 8 },
  },
  {
    field: 'isActive',
    label: 'Trạng thái',
    component: 'Select',
    componentProps: {
      // Ant Design Vue không nhận boolean làm value của Select nên dùng chuỗi
      options: [
        { label: 'Đang dùng', value: 'true' },
        { label: 'Đã tắt', value: 'false' },
      ],
      allowClear: true,
      placeholder: 'Tất cả',
    },
    colProps: { span: 8 },
  },
];

/** Form thông tin chính — riêng tỉ lệ dạng câu (questionTypeMix) do là một Map nên soạn thủ công trong Drawer */
export const blueprintFormSchema: FormSchema[] = [
  {
    field: 'titleVi',
    label: 'Tên cấu hình',
    component: 'Input',
    required: true,
    componentProps: { placeholder: 'Ví dụ: Ôn tập chủ đề Gia đình' },
  },
  {
    field: 'code',
    label: 'Mã',
    component: 'Input',
    helpMessage: 'Để trống thì hệ thống tự đặt theo tên. Dùng để gọi từ màn hình học.',
    componentProps: { placeholder: 'vi-du: on-tap-gia-dinh' },
  },
  {
    field: 'descriptionVi',
    label: 'Mô tả',
    component: 'InputTextArea',
    componentProps: { rows: 2 },
  },
  {
    field: 'topicIds',
    label: 'Chủ đề',
    component: 'ApiSelect',
    helpMessage: 'Để trống = lấy từ mọi chủ đề',
    componentProps: {
      api: topicOptionsApi,
      labelField: 'nameVi',
      valueField: 'id',
      mode: 'multiple',
      allowClear: true,
      placeholder: 'Mọi chủ đề',
    },
  },
  {
    field: 'levels',
    label: 'Cấp độ',
    component: 'Select',
    helpMessage: 'Để trống = mọi cấp độ',
    componentProps: { options: LEVEL_OPTIONS, mode: 'multiple', allowClear: true },
  },
  {
    field: 'unitTypes',
    label: 'Đơn vị ngôn ngữ',
    component: 'Select',
    helpMessage: 'Để trống = mọi đơn vị (từ, cụm từ, câu...)',
    componentProps: { options: UNIT_TYPE_OPTIONS, mode: 'multiple', allowClear: true },
  },
  {
    field: 'wordTypes',
    label: 'Từ loại',
    component: 'Select',
    helpMessage: 'Để trống = không giới hạn từ loại',
    componentProps: { options: WORD_TYPE_OPTIONS, mode: 'multiple', allowClear: true },
  },
  {
    field: 'questionCount',
    label: 'Số câu mỗi lần thi',
    component: 'InputNumber',
    defaultValue: 10,
    componentProps: { min: 1, max: 100, style: { width: '100%' } },
  },
  {
    field: 'optionCount',
    label: 'Số lựa chọn mỗi câu',
    component: 'InputNumber',
    defaultValue: 4,
    componentProps: { min: 2, max: 8, style: { width: '100%' } },
  },
  {
    field: 'passScore',
    label: 'Điểm đạt (%)',
    component: 'InputNumber',
    defaultValue: 70,
    componentProps: { min: 0, max: 100, style: { width: '100%' } },
  },
  {
    field: 'timeLimitSeconds',
    label: 'Giới hạn thời gian (giây)',
    component: 'InputNumber',
    helpMessage: 'Để trống nghĩa là không giới hạn',
    componentProps: { min: 1, style: { width: '100%' } },
  },
  {
    field: 'distractorStrategy',
    label: 'Đáp án nhiễu',
    component: 'Select',
    defaultValue: 'EASILY_CONFUSED',
    componentProps: { options: DISTRACTOR_OPTIONS },
  },
  {
    field: 'avoidRecentDays',
    label: 'Tránh lặp từ trong (ngày)',
    component: 'InputNumber',
    defaultValue: 30,
    helpMessage: 'Không hỏi lại từ người học đã gặp trong khoảng thời gian này. 0 = không tránh',
    componentProps: { min: 0, style: { width: '100%' } },
  },
  {
    field: 'isActive',
    label: 'Đang dùng',
    component: 'Switch',
    defaultValue: true,
    helpMessage: 'Tắt để tạm ẩn khỏi màn hình chọn đề của người học mà không phải xoá',
  },
];
