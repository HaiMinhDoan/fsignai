<template>
  <PageWrapper dense contentFullHeight fixedHeight>
    <BasicTable @register="registerTable">
      <template #toolbar>
        <Button type="primary" @click="handleCreate">Thêm chủ đề</Button>
      </template>

      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'action'">
          <TableAction
            :actions="[
              {
                icon: 'clarity:note-edit-line',
                tooltip: 'Sửa chủ đề',
                onClick: handleEdit.bind(null, record),
              },
              {
                icon: 'ant-design:delete-outlined',
                color: 'error',
                tooltip: 'Xoá chủ đề',
                popConfirm: {
                  title: `Xoá chủ đề ${record.nameVi}?`,
                  placement: 'left',
                  confirm: handleDelete.bind(null, record),
                },
              },
            ]"
          />
        </template>
      </template>
    </BasicTable>

    <TopicDrawer @register="registerDrawer" @success="handleSuccess" />
  </PageWrapper>
</template>

<script lang="ts" setup>
  import { Button } from '@/components/Button';
  import { BasicTable, useTable, TableAction } from '@/components/Table';
  import { PageWrapper } from '@/components/Page';
  import { useDrawer } from '@/components/Drawer';
  import { useMessage } from '@/hooks/web/useMessage';

  import TopicDrawer from './TopicDrawer.vue';
  import { columns } from './topic.data';
  import { topicFilterApi, topicDeleteApi } from '@/api/content/topic';
  import type { TopicModel } from '@/api/content/model/contentModel';

  defineOptions({ name: 'TopicManagement' });

  const { createMessage } = useMessage();
  const [registerDrawer, { openDrawer }] = useDrawer();

  /**
   * Dùng thẳng BaseFilterRequest của backend — không cần endpoint riêng.
   * vben đánh trang từ 1, Spring Data từ 0 nên phải trừ đi 1.
   */
  const loadTopics = async (params: Recordable) => {
    const { page = 1, size = 20 } = params;
    return topicFilterApi({
      filters: [],
      sorts: [{ fieldName: 'displayOrder', direction: 'ASC' }],
      page: page - 1,
      size,
    });
  };

  const [registerTable, { reload }] = useTable({
    title: 'Chủ đề từ vựng',
    api: loadTopics,
    rowKey: 'id',
    columns,
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

  function handleEdit(record: Recordable) {
    openDrawer(true, { record: record as TopicModel, isUpdate: true });
  }

  async function handleDelete(record: Recordable) {
    // Backend chặn xoá nếu chủ đề còn chủ đề con hoặc còn từ vựng,
    // và trả về thông điệp cụ thể — không cần kiểm tra lại ở đây.
    await topicDeleteApi((record as TopicModel).id);
    createMessage.success(`Đã xoá chủ đề ${(record as TopicModel).nameVi}`);
    reload();
  }

  function handleSuccess() {
    reload();
  }
</script>
