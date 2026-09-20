<template>
  <PageWrapper dense contentFullHeight fixedHeight>
    <BasicTable @register="registerTable" :rowSelection="rowSelection">
      <template #toolbar>
        <Button type="primary" @click="handleCreate">Thêm gói từ</Button>
        <Button :disabled="!selectedRowKeys.length" @click="handleBulkPublish(true)">
          Xuất bản ({{ selectedRowKeys.length }})
        </Button>
        <Button :disabled="!selectedRowKeys.length" @click="handleBulkPublish(false)">
          Gỡ xuất bản
        </Button>
      </template>

      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'action'">
          <TableAction
            :actions="[
              {
                icon: 'ant-design:unordered-list-outlined',
                tooltip: 'Quản lý từ trong gói',
                onClick: handleManageItems.bind(null, record),
              },
              {
                icon: 'clarity:note-edit-line',
                tooltip: 'Sửa thông tin gói',
                onClick: handleEdit.bind(null, record),
              },
              {
                icon: 'ant-design:delete-outlined',
                color: 'error',
                tooltip: 'Xoá gói từ',
                popConfirm: {
                  title: `Xoá gói ${record.titleVi}? Toàn bộ liên kết từ vựng bên trong cũng bị xoá.`,
                  placement: 'left',
                  confirm: handleDelete.bind(null, record),
                },
              },
            ]"
          />
        </template>
      </template>
    </BasicTable>

    <WordPackDrawer @register="registerDrawer" @success="handleSuccess" />
  </PageWrapper>
</template>

<script lang="ts" setup>
  import { ref, computed, unref } from 'vue';
  import { Button } from 'ant-design-vue';
  import { BasicTable, useTable, TableAction } from '@/components/Table';
  import { PageWrapper } from '@/components/Page';
  import { useDrawer } from '@/components/Drawer';
  import { useMessage } from '@/hooks/web/useMessage';

  import WordPackDrawer from './WordPackDrawer.vue';
  import { columns, searchFormSchema } from './wordPack.data';
  import { wordPackFilterApi, wordPackDeleteApi, wordPackPublishApi } from '@/api/catalog/wordPack';
  import type { WordPackModel } from '@/api/catalog/model/catalogModel';

  defineOptions({ name: 'WordPackManagement' });

  const { createMessage } = useMessage();
  const [registerDrawer, { openDrawer }] = useDrawer();

  const selectedRowKeys = ref<string[]>([]);
  const rowSelection = computed(
    () =>
      ({
        type: 'checkbox',
        selectedRowKeys: unref(selectedRowKeys),
        onChange: (keys: (string | number)[]) => {
          selectedRowKeys.value = keys as string[];
        },
      }) as any,
  );

  /**
   * Dịch form tìm kiếm của vben sang BaseFilterRequest của backend.
   * Cùng cách trang Khoá học đang làm — xem course/index.vue.
   */
  const loadPacks = async (params: Recordable) => {
    const { page = 1, size = 20, keyword, topicId, level, isPublished } = params;

    const filters: Recordable[] = [];
    if (keyword?.trim()) {
      filters.push({ fieldName: 'titleVi', operation: 'ILIKE', value: keyword.trim() });
    }
    if (topicId) filters.push({ fieldName: 'topic.id', operation: 'EQUALS', value: topicId });
    if (level) filters.push({ fieldName: 'level', operation: 'EQUALS', value: level });
    const published = toBool(isPublished);
    if (published !== undefined) {
      filters.push({ fieldName: 'isPublished', operation: 'EQUALS', value: published });
    }

    return wordPackFilterApi({
      filters: filters as any,
      // vben đánh trang từ 1, Spring Data đánh từ 0
      page: page - 1,
      size,
      sorts: [{ fieldName: 'displayOrder', direction: 'ASC' }],
    });
  };

  // Ant Design Vue không nhận boolean làm value của Select nên form gửi chuỗi
  function toBool(value: unknown): boolean | undefined {
    if (value === undefined || value === null || value === '') return undefined;
    return value === true || value === 'true';
  }

  const [registerTable, { reload }] = useTable({
    title: 'Gói từ',
    api: loadPacks,
    rowKey: 'id',
    columns,
    formConfig: {
      labelWidth: 100,
      schemas: searchFormSchema,
      autoSubmitOnEnter: true,
    },
    fetchSetting: { sizeField: 'size' },
    useSearchForm: true,
    showTableSetting: true,
    bordered: true,
    actionColumn: {
      width: 130,
      title: 'Thao tác',
      dataIndex: 'action',
      fixed: 'right',
    },
  });

  function handleCreate() {
    openDrawer(true, { isUpdate: false });
  }

  // Slot bodyCell trả Recordable chứ không phải WordPackModel — ép kiểu ngay ở biên
  function handleEdit(record: Recordable) {
    openDrawer(true, { record: record as WordPackModel, isUpdate: true, activeTab: 'info' });
  }

  function handleManageItems(record: Recordable) {
    openDrawer(true, { record: record as WordPackModel, isUpdate: true, activeTab: 'items' });
  }

  async function handleDelete(record: Recordable) {
    await wordPackDeleteApi(record.id);
    handleSuccess();
  }

  async function handleBulkPublish(published: boolean) {
    const ids = unref(selectedRowKeys);
    if (!ids.length) return;
    try {
      const result = await wordPackPublishApi(ids, published);
      createMessage.success(
        `${published ? 'Đã xuất bản' : 'Đã gỡ xuất bản'} ${result.affected} gói từ`,
      );
      selectedRowKeys.value = [];
      reload();
    } catch (error) {
      // Backend chặn xuất bản gói chưa có từ và nói rõ gói nào
      createMessage.error((error as Error).message || 'Không thực hiện được');
    }
  }

  function handleSuccess() {
    selectedRowKeys.value = [];
    reload();
  }
</script>
