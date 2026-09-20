<template>
  <PageWrapper dense contentFullHeight fixedHeight>
    <BasicTable @register="registerTable">
      <template #toolbar>
        <Button type="primary" @click="handleCreate">Thêm chuyên mục</Button>
      </template>

      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'action'">
          <TableAction
            :actions="[
              {
                icon: 'clarity:note-edit-line',
                tooltip: 'Sửa chuyên mục',
                onClick: handleEdit.bind(null, record),
              },
              {
                icon: 'ant-design:delete-outlined',
                color: 'error',
                tooltip: 'Xoá chuyên mục',
                popConfirm: {
                  title: `Xoá chuyên mục ${record.nameVi}?`,
                  placement: 'left',
                  confirm: handleDelete.bind(null, record),
                },
              },
            ]"
          />
        </template>
      </template>
    </BasicTable>

    <CategoryDrawer @register="registerDrawer" @success="handleSuccess" />
  </PageWrapper>
</template>

<script lang="ts" setup>
  import { Button } from '@/components/Button';
  import { BasicTable, useTable, TableAction } from '@/components/Table';
  import { PageWrapper } from '@/components/Page';
  import { useDrawer } from '@/components/Drawer';
  import { useMessage } from '@/hooks/web/useMessage';

  import CategoryDrawer from './CategoryDrawer.vue';
  import { columns } from './category.data';
  import { forumCategoryListApi, forumCategoryDeleteApi } from '@/api/forum';
  import type { ForumCategoryModel } from '@/api/forum/model/forumModel';

  defineOptions({ name: 'ForumCategoryManagement' });

  const { createMessage } = useMessage();
  const [registerDrawer, { openDrawer }] = useDrawer();

  const loadCategories = async () => {
    const items = await forumCategoryListApi();
    return { items, total: items.length };
  };

  const [registerTable, { reload }] = useTable({
    title: 'Chuyên mục Diễn đàn',
    api: loadCategories,
    rowKey: 'id',
    columns,
    pagination: false,
    bordered: true,
    actionColumn: {
      width: 100,
      title: 'Thao tác',
      dataIndex: 'action',
      fixed: 'right',
    },
  });

  function handleCreate() {
    openDrawer(true, { isUpdate: false });
  }

  function handleEdit(record: Recordable) {
    openDrawer(true, { record: record as ForumCategoryModel, isUpdate: true });
  }

  async function handleDelete(record: Recordable) {
    await forumCategoryDeleteApi((record as ForumCategoryModel).id);
    createMessage.success(`Đã xoá chuyên mục ${(record as ForumCategoryModel).nameVi}`);
    reload();
  }

  function handleSuccess() {
    reload();
  }
</script>
