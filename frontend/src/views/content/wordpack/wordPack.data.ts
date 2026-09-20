import type { BasicColumn, FormSchema } from '@/components/Table';
import { h } from 'vue';
import { Tag } from 'ant-design-vue';
import { topicOptionsApi } from '@/api/content/topic';
import { wordPackOptionsApi } from '@/api/catalog/wordPack';

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

/** Icon dùng cho đảo trên bản đồ — cùng bộ tên với SiIcon.vue bên portal-view */
export const ICON_OPTIONS = [
  { label: 'Bàn tay', value: 'hand' },
  { label: 'Sách', value: 'book' },
  { label: 'Ngôi sao', value: 'star' },
  { label: 'Lấp lánh', value: 'sparkles' },
  { label: 'Lưới ô', value: 'grid' },
  { label: 'Bản đồ', value: 'map' },
  { label: 'Ngọn lửa', value: 'flame' },
];

export const columns: BasicColumn[] = [
  {
    title: 'Tên gói từ',
    dataIndex: 'titleVi',
    width: 240,
    fixed: 'left',
  },
  {
    title: 'Mã',
    dataIndex: 'code',
    width: 160,
    customRender: ({ record }) => h('code', { style: 'font-size:12px;color:#5A7683' }, record.code),
  },
  {
    title: 'Chủ đề',
    dataIndex: 'topicNameVi',
    width: 150,
    customRender: ({ record }) =>
      record.topicNameVi ?? h('span', { style: 'color:#9AB0BC' }, '— chưa gán —'),
  },
  {
    title: 'Cấp độ',
    dataIndex: 'level',
    width: 110,
    customRender: ({ record }) =>
      h(Tag, { color: LEVEL_COLOR[record.level] ?? 'default' }, () => levelLabel(record.level)),
  },
  {
    title: 'Số từ',
    dataIndex: 'itemCount',
    width: 100,
    customRender: ({ record }) => {
      const count = record.itemCount ?? 0;
      // Gói rỗng không xuất bản được — cho thấy ngay thay vì để phát hiện lúc bấm nút
      return count === 0
        ? h(Tag, { color: 'orange' }, () => 'Chưa có từ')
        : h('span', {}, String(count));
    },
  },
  {
    title: 'Mở sau gói',
    dataIndex: 'unlockAfterPackTitleVi',
    width: 180,
    customRender: ({ record }) =>
      record.unlockAfterPackTitleVi ??
      h('span', { style: 'color:#9AB0BC' }, 'Mở sẵn từ đầu'),
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
    componentProps: { placeholder: 'Tên gói từ' },
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

export function wordPackFormSchema(currentId?: string): FormSchema[] {
  return [
    {
      field: 'titleVi',
      label: 'Tên gói từ',
      component: 'Input',
      required: true,
      componentProps: { placeholder: 'Ví dụ: Chào hỏi cơ bản' },
    },
    {
      field: 'code',
      label: 'Mã',
      component: 'Input',
      helpMessage: 'Để trống thì hệ thống tự sinh từ tên gói, đã bỏ dấu tiếng Việt',
      componentProps: { placeholder: 'chao-hoi-co-ban' },
    },
    {
      field: 'descriptionVi',
      label: 'Mô tả',
      component: 'InputTextArea',
      componentProps: { rows: 3, placeholder: 'Gói này dạy gì, dành cho ai' },
    },
    {
      field: 'topicId',
      label: 'Chủ đề',
      component: 'ApiSelect',
      componentProps: {
        api: topicOptionsApi,
        labelField: 'nameVi',
        valueField: 'id',
        placeholder: 'Không bắt buộc',
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
      field: 'iconName',
      label: 'Icon trên đảo',
      component: 'Select',
      componentProps: { options: ICON_OPTIONS, allowClear: true },
    },
    {
      field: 'islandColor',
      label: 'Màu đảo',
      component: 'ColorPicker',
      helpMessage: 'Chọn từ bảng màu của portal, hoặc bấm ô màu để tự chọn — để trống thì portal tự chọn màu theo thứ tự',
    },
    {
      field: 'unlockAfterPackId',
      label: 'Chỉ mở sau khi xong',
      component: 'ApiSelect',
      helpMessage: 'Để trống = mở sẵn ngay từ đầu, không cần hoàn thành gói nào trước',
      componentProps: () => ({
        // Loại chính gói đang sửa khỏi danh sách chọn — một gói không thể tự khoá chính nó
        api: () => wordPackOptionsApi().then((list) => list.filter((p) => p.id !== currentId)),
        labelField: 'titleVi',
        valueField: 'id',
        placeholder: 'Không bắt buộc',
        allowClear: true,
      }),
    },
    {
      field: 'passScore',
      label: 'Điểm đạt tối thiểu (%)',
      component: 'InputNumber',
      defaultValue: 80,
      componentProps: { min: 0, max: 100 },
    },
    {
      field: 'displayOrder',
      label: 'Thứ tự trên bản đồ',
      component: 'InputNumber',
      defaultValue: 0,
      componentProps: { min: 0 },
    },
    {
      field: 'isPublished',
      label: 'Xuất bản',
      component: 'Switch',
      defaultValue: false,
      helpMessage: 'Chỉ xuất bản được khi gói đã có ít nhất một từ',
    },
  ];
}
