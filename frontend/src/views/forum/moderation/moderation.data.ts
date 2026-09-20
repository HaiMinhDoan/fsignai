import type { BasicColumn, FormSchema } from '@/components/Table';
import { Tag } from 'ant-design-vue';
import { h } from 'vue';
import { forumCategoryListApi } from '@/api/forum';

export const STATUS_OPTIONS = [
  { label: 'Chờ duyệt', value: 'PENDING_REVIEW' },
  { label: 'Đã đăng', value: 'PUBLISHED' },
  { label: 'Đã ẩn', value: 'HIDDEN' },
  { label: 'Đã gỡ', value: 'REMOVED' },
];

const STATUS_COLOR: Record<string, string> = {
  DRAFT: 'default',
  PENDING_REVIEW: 'orange',
  PUBLISHED: 'success',
  HIDDEN: 'warning',
  REMOVED: 'error',
};
const STATUS_LABEL: Record<string, string> = {
  DRAFT: 'Nháp',
  PENDING_REVIEW: 'Chờ duyệt',
  PUBLISHED: 'Đã đăng',
  HIDDEN: 'Đã ẩn',
  REMOVED: 'Đã gỡ',
};

export const columns: BasicColumn[] = [
  {
    title: 'Tiêu đề',
    dataIndex: 'titleVi',
    width: 260,
    customRender: ({ record }) =>
      h('span', {}, [record.isPinned ? h(Tag, { color: 'gold' }, () => 'Ghim') : null, ' ', record.titleVi]),
  },
  {
    title: 'Chuyên mục',
    dataIndex: 'categoryNameVi',
    width: 160,
  },
  {
    title: 'Tác giả',
    dataIndex: 'authorName',
    width: 150,
  },
  {
    title: 'Lượt xem',
    dataIndex: 'viewCount',
    width: 90,
  },
  {
    title: 'Bình luận',
    dataIndex: 'commentCount',
    width: 90,
  },
  {
    title: 'Thích',
    dataIndex: 'reactionCount',
    width: 80,
  },
  {
    title: 'Khoá BL',
    dataIndex: 'isLocked',
    width: 80,
    customRender: ({ record }) => (record.isLocked ? '🔒' : ''),
  },
  {
    title: 'Trạng thái',
    dataIndex: 'status',
    width: 110,
    customRender: ({ record }) =>
      h(Tag, { color: STATUS_COLOR[record.status] ?? 'default' }, () => STATUS_LABEL[record.status] ?? record.status),
  },
  {
    title: 'Hoạt động gần nhất',
    dataIndex: 'lastActivityAt',
    width: 160,
    customRender: ({ record }) => new Date(record.lastActivityAt).toLocaleString('vi-VN'),
  },
];

export const searchFormSchema: FormSchema[] = [
  {
    field: 'status',
    label: 'Trạng thái',
    component: 'Select',
    componentProps: { options: STATUS_OPTIONS, allowClear: true, placeholder: 'Tất cả' },
    colProps: { span: 6 },
  },
  {
    field: 'categoryId',
    label: 'Chuyên mục',
    component: 'ApiSelect',
    componentProps: {
      api: () => forumCategoryListApi(),
      labelField: 'nameVi',
      valueField: 'id',
      allowClear: true,
      placeholder: 'Mọi chuyên mục',
    },
    colProps: { span: 6 },
  },
];
