import type { BasicColumn, FormSchema } from '@/components/Table';
import { h } from 'vue';
import { Tag, Badge } from 'ant-design-vue';
import { topicOptionsApi } from '@/api/content/topic';
import type { Region } from '@/api/content/model/contentModel';

/**
 * Nhãn hiển thị tiếng Việt cho các enum của backend.
 * Tách riêng khỏi type để đổi chữ hiển thị không phải đụng vào tầng API.
 */
export const LEVEL_OPTIONS = [
  { label: 'Người mới', value: 'BEGINNER' },
  { label: 'Cơ bản', value: 'BASIC' },
  { label: 'Trung cấp', value: 'INTERMEDIATE' },
  { label: 'Nâng cao', value: 'ADVANCED' },
];

export const REGION_OPTIONS = [
  { label: 'Bắc', value: 'NORTH' },
  { label: 'Trung', value: 'CENTRAL' },
  { label: 'Nam', value: 'SOUTH' },
  { label: 'Dùng chung', value: 'COMMON' },
];

export const UNIT_TYPE_OPTIONS = [
  { label: 'Chữ cái', value: 'LETTER' },
  { label: 'Chữ số', value: 'NUMBER' },
  { label: 'Từ', value: 'WORD' },
  { label: 'Cụm từ', value: 'PHRASE' },
  { label: 'Câu', value: 'SENTENCE' },
];

/** Từ loại theo ngữ pháp tiếng Việt phổ thông */
export const WORD_TYPE_OPTIONS = [
  { label: 'Danh từ', value: 'DANH_TU' },
  { label: 'Động từ', value: 'DONG_TU' },
  { label: 'Tính từ', value: 'TINH_TU' },
  { label: 'Số từ', value: 'SO_TU' },
  { label: 'Đại từ', value: 'DAI_TU' },
  { label: 'Phó từ', value: 'PHO_TU' },
  { label: 'Quan hệ từ', value: 'QUAN_HE_TU' },
  { label: 'Lượng từ', value: 'LUONG_TU' },
  { label: 'Chỉ từ', value: 'CHI_TU' },
  { label: 'Trợ từ', value: 'TRO_TU' },
  { label: 'Tình thái từ', value: 'TINH_THAI_TU' },
  { label: 'Thán từ', value: 'THAN_TU' },
  { label: 'Không xác định', value: 'KHONG_XAC_DINH' },
];

export const WORD_SUBTYPE_OPTIONS = [
  { label: 'Danh từ chung', value: 'CHUNG' },
  { label: 'Danh từ riêng', value: 'RIENG' },
  { label: 'Danh từ chỉ đơn vị', value: 'DON_VI' },
];

export const DOMAIN_OPTIONS = [
  { label: 'Toán học', value: 'MATH' },
  { label: 'Địa danh', value: 'GEOGRAPHY' },
  { label: 'Quốc gia', value: 'COUNTRY' },
  { label: 'Y tế', value: 'MEDICAL' },
  { label: 'Pháp luật', value: 'LEGAL' },
  { label: 'Trường học', value: 'SCHOOL' },
  { label: 'Công nghệ', value: 'IT' },
  { label: 'Thể thao', value: 'SPORT' },
  { label: 'Tôn giáo', value: 'RELIGION' },
];

export const SOURCE_OPTIONS = [
  { label: 'Từ điển Bộ GD&ĐT', value: 'MOET_QIPEDC' },
  { label: 'Tự quay', value: 'SELF_RECORDED' },
  { label: 'Nhập khẩu', value: 'IMPORTED' },
  { label: 'Nhập tay', value: 'MANUAL' },
];

export const REVIEW_STATUS_OPTIONS = [
  { label: 'Chưa duyệt', value: 'UNREVIEWED' },
  { label: 'Đã duyệt', value: 'APPROVED' },
  { label: 'Cần sửa', value: 'NEEDS_FIX' },
];

export const VIEW_ANGLE_OPTIONS = [
  { label: 'Chính diện', value: 'FRONT' },
  { label: 'Bên trái', value: 'LEFT' },
  { label: 'Bên phải', value: 'RIGHT' },
];

const labelOf = (options: Array<{ label: string; value: string }>, value?: string) =>
  options.find((o) => o.value === value)?.label ?? value ?? '';

/** Ba chấm tròn B · T · N: tô đậm vùng miền nào đã có video */
const renderRegionDots = (available: Region[] = []) => {
  const dots: Array<{ key: Region; text: string }> = [
    { key: 'NORTH', text: 'B' },
    { key: 'CENTRAL', text: 'T' },
    { key: 'SOUTH', text: 'N' },
  ];
  return h(
    'div',
    { style: 'display:flex;gap:4px' },
    dots.map((d) => {
      const has = available.includes(d.key) || available.includes('COMMON');
      return h(
        'span',
        {
          title: has ? `Đã có video miền ${d.text}` : `Chưa có video miền ${d.text}`,
          style: [
            'display:inline-flex;align-items:center;justify-content:center',
            'width:20px;height:20px;border-radius:50%;font-size:11px;font-weight:600',
            has ? 'background:#1B6CA8;color:#fff' : 'background:#EAF0F4;color:#9AB0BC',
          ].join(';'),
        },
        d.text,
      );
    }),
  );
};

/**
 * Ảnh đại diện lấy từ video chính của từ (sign_videos.thumbnail_file_id).
 *
 * Khung ảnh giữ nguyên kích thước kể cả khi chưa có ảnh — nếu để trống hẳn,
 * mỗi dòng trong bảng sẽ cao thấp khác nhau và mắt rất khó dò theo hàng.
 */
const renderThumbnail = (url?: string, wordVi?: string) =>
  h(
    'div',
    {
      style: [
        'width:64px;height:48px;border-radius:4px;overflow:hidden',
        'background:#EAF0F4;display:flex;align-items:center;justify-content:center',
      ].join(';'),
    },
    url
      ? [
          h('img', {
            src: url,
            alt: `Ảnh ký hiệu từ ${wordVi ?? ''}`,
            loading: 'lazy',
            style: 'width:100%;height:100%;object-fit:cover',
          }),
        ]
      : [h('span', { style: 'font-size:10px;color:#9AB0BC' }, 'Chưa có')],
  );

export const columns: BasicColumn[] = [
  {
    title: 'Ảnh',
    dataIndex: 'thumbnailUrl',
    width: 80,
    fixed: 'left',
    customRender: ({ record }) => renderThumbnail(record.thumbnailUrl, record.wordVi),
  },
  {
    title: 'Từ tiếng Việt',
    dataIndex: 'wordVi',
    width: 180,
    fixed: 'left',
  },
  {
    title: 'Gloss',
    dataIndex: 'gloss',
    width: 140,
    customRender: ({ record }) =>
      h('code', { style: 'font-size:12px;color:#5A7683' }, record.gloss),
  },
  {
    title: 'Đơn vị',
    dataIndex: 'unitType',
    width: 100,
    customRender: ({ record }) => labelOf(UNIT_TYPE_OPTIONS, record.unitType),
  },
  {
    title: 'Từ loại',
    dataIndex: 'wordType',
    width: 130,
    customRender: ({ record }) => labelOf(WORD_TYPE_OPTIONS, record.wordType),
  },
  {
    title: 'Chủ đề',
    dataIndex: 'topics',
    width: 220,
    customRender: ({ record }) => {
      const topics = record.topics ?? [];
      if (!topics.length) return h('span', { style: 'color:#9AB0BC' }, '—');
      return h(
        'div',
        { style: 'display:flex;flex-wrap:wrap;gap:4px' },
        topics.map((t: any) => h(Tag, { color: 'blue' }, () => t.nameVi)),
      );
    },
  },
  {
    title: 'Cấp độ',
    dataIndex: 'level',
    width: 110,
    customRender: ({ record }) => labelOf(LEVEL_OPTIONS, record.level),
  },
  {
    title: 'Vùng miền',
    dataIndex: 'availableRegions',
    width: 110,
    customRender: ({ record }) => renderRegionDots(record.availableRegions),
  },
  {
    title: 'AI',
    dataIndex: 'aiReady',
    width: 80,
    customRender: ({ record }) =>
      record.aiReady
        ? h(Badge, { status: 'success', text: 'Sẵn sàng' })
        : h(Badge, { status: 'default', text: 'Chưa' }),
  },
  {
    title: 'Trạng thái',
    dataIndex: 'isPublished',
    width: 120,
    customRender: ({ record }) =>
      record.isPublished
        ? h(Tag, { color: 'green' }, () => 'Đã xuất bản')
        : h(Tag, {}, () => 'Bản nháp'),
  },
];

/**
 * Bộ lọc phía trên bảng.
 * Hai bộ lọc quan trọng nhất là "vùng miền còn thiếu video" và "chưa có exemplar" —
 * chúng trả lời câu hỏi "còn phải làm gì nữa", nên đặt ngay hàng đầu.
 */
export const searchFormSchema: FormSchema[] = [
  {
    field: 'keyword',
    label: 'Tìm kiếm',
    component: 'Input',
    colProps: { span: 6 },
    componentProps: {
      placeholder: 'Gõ có dấu hoặc không dấu: "me" ra "mẹ"',
      allowClear: true,
    },
  },
  {
    field: 'topicId',
    label: 'Chủ đề',
    component: 'ApiSelect',
    colProps: { span: 6 },
    componentProps: {
      api: topicOptionsApi,
      labelField: 'nameVi',
      valueField: 'id',
      allowClear: true,
      placeholder: 'Tất cả chủ đề',
    },
  },
  {
    field: 'level',
    label: 'Cấp độ',
    component: 'Select',
    colProps: { span: 6 },
    componentProps: { options: LEVEL_OPTIONS, allowClear: true, placeholder: 'Tất cả' },
  },
  {
    field: 'unitType',
    label: 'Đơn vị',
    component: 'Select',
    colProps: { span: 6 },
    componentProps: { options: UNIT_TYPE_OPTIONS, allowClear: true, placeholder: 'Tất cả' },
  },
  {
    field: 'wordType',
    label: 'Từ loại',
    component: 'Select',
    colProps: { span: 6 },
    componentProps: { options: WORD_TYPE_OPTIONS, allowClear: true, placeholder: 'Tất cả' },
  },
  {
    field: 'isPublished',
    label: 'Trạng thái',
    component: 'Select',
    colProps: { span: 6 },
    componentProps: {
      options: [
        { label: 'Đã xuất bản', value: 'true' },
        { label: 'Bản nháp', value: 'false' },
      ],
      allowClear: true,
      placeholder: 'Tất cả',
    },
  },
  {
    field: 'missingVideoRegion',
    label: 'Thiếu video',
    component: 'Select',
    colProps: { span: 6 },
    componentProps: {
      options: REGION_OPTIONS.filter((r) => r.value !== 'COMMON'),
      allowClear: true,
      placeholder: 'Vùng miền chưa có video',
    },
  },
  {
    field: 'missingExemplar',
    label: 'Chưa có AI',
    component: 'Select',
    colProps: { span: 6 },
    componentProps: {
      options: [{ label: 'Chỉ từ chưa chấm được', value: 'true' }],
      allowClear: true,
      placeholder: 'Tất cả',
    },
  },
];

/** Form thêm/sửa từ vựng — tab Thông tin */
export const signFormSchema: FormSchema[] = [
  {
    field: 'wordVi',
    label: 'Từ tiếng Việt',
    component: 'Input',
    required: true,
    colProps: { span: 12 },
    componentProps: { placeholder: 'Ví dụ: mẹ' },
  },
  {
    field: 'gloss',
    label: 'Gloss',
    component: 'Input',
    colProps: { span: 12 },
    componentProps: { placeholder: 'Bỏ trống để tự sinh: "gia đình" → GIA_DINH' },
    helpMessage: 'Mã định danh ký hiệu. Để trống thì hệ thống tự sinh từ từ tiếng Việt.',
  },
  {
    field: 'wordEn',
    label: 'Tiếng Anh',
    component: 'Input',
    colProps: { span: 12 },
    componentProps: { placeholder: 'mother' },
  },
  {
    field: 'unitType',
    label: 'Đơn vị',
    component: 'Select',
    required: true,
    colProps: { span: 12 },
    componentProps: { options: UNIT_TYPE_OPTIONS },
    defaultValue: 'WORD',
  },
  {
    field: 'wordType',
    label: 'Từ loại',
    component: 'Select',
    required: true,
    colProps: { span: 12 },
    componentProps: { options: WORD_TYPE_OPTIONS },
    defaultValue: 'KHONG_XAC_DINH',
  },
  {
    field: 'wordSubtype',
    label: 'Phân loại con',
    component: 'Select',
    colProps: { span: 12 },
    componentProps: { options: WORD_SUBTYPE_OPTIONS, allowClear: true },
    // Chỉ hiện khi từ loại là danh từ
    ifShow: ({ values }) => values.wordType === 'DANH_TU',
  },
  {
    field: 'level',
    label: 'Cấp độ',
    component: 'Select',
    required: true,
    colProps: { span: 12 },
    componentProps: { options: LEVEL_OPTIONS },
    defaultValue: 'BEGINNER',
  },
  {
    field: 'domain',
    label: 'Lĩnh vực',
    component: 'Select',
    colProps: { span: 12 },
    componentProps: { options: DOMAIN_OPTIONS, allowClear: true, placeholder: 'Không thuộc lĩnh vực nào' },
  },
  {
    field: 'handCount',
    label: 'Số tay',
    component: 'Select',
    required: true,
    colProps: { span: 12 },
    componentProps: {
      options: [
        { label: '1 tay', value: 1 },
        { label: '2 tay', value: 2 },
      ],
    },
    defaultValue: 1,
    helpMessage: 'Dùng để chọn ngưỡng chấm điểm DTW mặc định. Ký hiệu 2 tay có ngưỡng rộng hơn.',
  },
  {
    field: 'primaryTopicId',
    label: 'Chủ đề chính',
    component: 'ApiSelect',
    colProps: { span: 12 },
    componentProps: {
      api: topicOptionsApi,
      labelField: 'nameVi',
      valueField: 'id',
      allowClear: true,
    },
  },
  {
    field: 'topicIds',
    label: 'Chủ đề khác',
    component: 'ApiSelect',
    colProps: { span: 24 },
    componentProps: {
      api: topicOptionsApi,
      labelField: 'nameVi',
      valueField: 'id',
      mode: 'multiple',
      allowClear: true,
      placeholder: 'Một từ có thể thuộc nhiều chủ đề',
    },
  },
  {
    field: 'descriptionVi',
    label: 'Mô tả cách làm',
    component: 'InputTextArea',
    colProps: { span: 24 },
    componentProps: {
      rows: 3,
      placeholder: 'Ví dụ: Bàn tay phải mở, đầu ngón trỏ chạm cằm rồi hạ xuống.',
    },
    helpMessage:
      'Bắt buộc cho người khiếm thị và cho tìm kiếm. Có thể bổ sung sau nếu nhập hàng loạt.',
  },
  {
    field: 'source',
    label: 'Nguồn',
    component: 'Select',
    colProps: { span: 12 },
    componentProps: { options: SOURCE_OPTIONS },
    defaultValue: 'MANUAL',
  },
  {
    field: 'isPublished',
    label: 'Xuất bản',
    component: 'Switch',
    colProps: { span: 12 },
    defaultValue: false,
  },
];
