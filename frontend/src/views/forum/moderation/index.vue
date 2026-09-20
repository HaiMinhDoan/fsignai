<template>
  <PageWrapper dense contentFullHeight fixedHeight>
    <BasicTable @register="registerTable">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'action'">
          <TableAction
            :actions="[
              {
                label: 'Duyệt',
                color: 'success',
                ifShow: record.status === 'PENDING_REVIEW',
                onClick: handleModerate.bind(null, record, 'PUBLISHED'),
              },
              {
                label: 'Ẩn',
                ifShow: record.status === 'PUBLISHED',
                onClick: handleModerate.bind(null, record, 'HIDDEN'),
              },
              {
                label: 'Khôi phục',
                color: 'success',
                ifShow: record.status === 'HIDDEN' || record.status === 'REMOVED',
                onClick: handleModerate.bind(null, record, 'PUBLISHED'),
              },
              {
                label: 'Gỡ vĩnh viễn',
                color: 'error',
                ifShow: record.status !== 'REMOVED',
                popConfirm: {
                  title: `Gỡ vĩnh viễn bài '${record.titleVi}'?`,
                  placement: 'left',
                  confirm: handleModerate.bind(null, record, 'REMOVED'),
                },
              },
              {
                label: record.isPinned ? 'Bỏ ghim' : 'Ghim',
                onClick: handlePin.bind(null, record, !record.isPinned),
              },
              {
                label: record.isLocked ? 'Mở bình luận' : 'Khoá bình luận',
                onClick: handleLock.bind(null, record, !record.isLocked),
              },
            ]"
          />
        </template>
      </template>
    </BasicTable>
  </PageWrapper>
</template>

<script lang="ts" setup>
  import { BasicTable, useTable, TableAction } from '@/components/Table';
  import { PageWrapper } from '@/components/Page';

  import { columns, searchFormSchema } from './moderation.data';
  import { forumPostFilterApi, forumPostModerateApi, forumPostSetPinnedApi, forumPostSetLockedApi } from '@/api/forum';
  import type { ForumPostStatus } from '@/api/forum/model/forumModel';

  defineOptions({ name: 'ForumModeration' });

  const loadPosts = async (params: Recordable) => {
    const { page = 1, size = 20, status, categoryId } = params;
    return forumPostFilterApi({ status, categoryId, page: page - 1, size });
  };

  const [registerTable, { reload }] = useTable({
    title: 'Kiểm duyệt bài viết',
    api: loadPosts,
    rowKey: 'id',
    columns,
    formConfig: { labelWidth: 100, schemas: searchFormSchema },
    useSearchForm: true,
    fetchSetting: { sizeField: 'size' },
    showTableSetting: true,
    bordered: true,
    actionColumn: {
      width: 340,
      title: 'Thao tác',
      dataIndex: 'action',
      fixed: 'right',
    },
  });

  async function handleModerate(record: Recordable<any>, status: ForumPostStatus) {
    await forumPostModerateApi(record.id, status);
    reload();
  }

  async function handlePin(record: Recordable<any>, pinned: boolean) {
    await forumPostSetPinnedApi(record.id, pinned);
    reload();
  }

  async function handleLock(record: Recordable<any>, locked: boolean) {
    await forumPostSetLockedApi(record.id, locked);
    reload();
  }
</script>
