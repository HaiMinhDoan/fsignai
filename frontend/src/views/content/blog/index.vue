<template>
  <PageWrapper dense contentFullHeight fixedHeight>
    <BasicTable @register="registerTable">
      <template #toolbar>
        <Button type="primary" @click="handleCreate">Viết bài mới</Button>
      </template>

      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'action'">
          <TableAction
            :actions="[
              { icon: 'clarity:note-edit-line', tooltip: 'Sửa', onClick: handleEdit.bind(null, record) },
              {
                icon: 'ant-design:delete-outlined',
                color: 'error',
                tooltip: 'Xoá',
                popConfirm: {
                  title: `Xoá bài '${record.titleVi}'?`,
                  placement: 'left',
                  confirm: handleDelete.bind(null, record),
                },
              },
            ]"
          />
        </template>
      </template>
    </BasicTable>

    <BlogDrawer @register="registerDrawer" @success="reload" />
  </PageWrapper>
</template>

<script lang="ts" setup>
  import { Button } from '@/components/Button';
  import { BasicTable, useTable, TableAction } from '@/components/Table';
  import { PageWrapper } from '@/components/Page';
  import { useDrawer } from '@/components/Drawer';
  import { useMessage } from '@/hooks/web/useMessage';

  import BlogDrawer from './BlogDrawer.vue';
  import { columns, searchFormSchema } from './blog.data';
  import { blogFilterApi, blogDeleteApi } from '@/api/notification/blog';
  import type { BlogPostModel } from '@/api/notification/model/blogModel';

  defineOptions({ name: 'BlogManagement' });

  const { createMessage } = useMessage();
  const [registerDrawer, { openDrawer }] = useDrawer();

  const loadBlogs = async (params: Recordable) => {
    const { page = 1, size = 20, category, isPublished } = params;
    return blogFilterApi({
      category,
      isPublished: isPublished === undefined ? undefined : isPublished === 'true',
      page: page - 1,
      size,
    });
  };

  const [registerTable, { reload }] = useTable({
    title: 'Blog',
    api: loadBlogs,
    rowKey: 'id',
    columns,
    formConfig: { labelWidth: 100, schemas: searchFormSchema },
    useSearchForm: true,
    fetchSetting: { sizeField: 'size' },
    showTableSetting: true,
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

  function handleEdit(record: Recordable<any>) {
    openDrawer(true, { record: record as BlogPostModel, isUpdate: true });
  }

  async function handleDelete(record: Recordable<any>) {
    await blogDeleteApi(record.id);
    createMessage.success(`Đã xoá bài ${record.titleVi}`);
    reload();
  }
</script>
