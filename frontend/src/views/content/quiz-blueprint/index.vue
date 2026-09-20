<template>
  <PageWrapper dense contentFullHeight fixedHeight>
    <BasicTable @register="registerTable" :rowSelection="rowSelection">
      <template #toolbar>
        <Button type="primary" @click="handleCreate">Thêm cấu hình</Button>
        <Button :disabled="!selectedRowKeys.length" @click="handleBulkActive(true)">
          Bật ({{ selectedRowKeys.length }})
        </Button>
        <Button :disabled="!selectedRowKeys.length" @click="handleBulkActive(false)">Tắt</Button>
      </template>

      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'action'">
          <TableAction
            :actions="[
              {
                icon: 'ant-design:play-circle-outlined',
                tooltip: 'Rút thử',
                onClick: handlePreview.bind(null, record),
              },
              {
                icon: 'clarity:note-edit-line',
                tooltip: 'Sửa cấu hình',
                onClick: handleEdit.bind(null, record),
              },
              {
                icon: 'ant-design:delete-outlined',
                color: 'error',
                tooltip: 'Xoá cấu hình',
                popConfirm: {
                  title: `Xoá cấu hình ${record.titleVi}?`,
                  placement: 'left',
                  confirm: handleDelete.bind(null, record),
                },
              },
            ]"
          />
        </template>
      </template>
    </BasicTable>

    <BlueprintDrawer @register="registerDrawer" @success="handleSuccess" />
  </PageWrapper>
</template>

<script lang="ts" setup>
  import { ref, computed, unref } from 'vue';
  import { Button } from 'ant-design-vue';
  import { BasicTable, useTable, TableAction } from '@/components/Table';
  import { PageWrapper } from '@/components/Page';
  import { useDrawer } from '@/components/Drawer';
  import { useMessage } from '@/hooks/web/useMessage';

  import BlueprintDrawer from './BlueprintDrawer.vue';
  import { columns, searchFormSchema } from './blueprint.data';
  import {
    quizBlueprintFilterApi,
    quizBlueprintDeleteApi,
    quizBlueprintSetActiveApi,
  } from '@/api/practice/quizBlueprint';
  import type { QuizBlueprintModel } from '@/api/practice/model/practiceModel';

  defineOptions({ name: 'QuizBlueprintManagement' });

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

  /** Dịch form tìm kiếm sang BaseFilterRequest của backend */
  const loadBlueprints = async (params: Recordable) => {
    const { page = 1, size = 20, keyword, isActive } = params;

    const filters: Recordable[] = [];
    if (keyword?.trim()) {
      filters.push({ fieldName: 'titleVi', operation: 'ILIKE', value: keyword.trim() });
    }
    const active = toBool(isActive);
    if (active !== undefined) {
      filters.push({ fieldName: 'isActive', operation: 'EQUALS', value: active });
    }

    return quizBlueprintFilterApi({
      filters: filters as any,
      // vben đánh trang từ 1, Spring Data đánh từ 0
      page: page - 1,
      size,
    });
  };

  function toBool(value: unknown): boolean | undefined {
    if (value === undefined || value === null || value === '') return undefined;
    return value === true || value === 'true';
  }

  const [registerTable, { reload }] = useTable({
    title: 'Cấu hình đề trộn',
    api: loadBlueprints,
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

  // Slot bodyCell trả Recordable chứ không phải QuizBlueprintModel — ép kiểu ngay ở biên
  function handleEdit(record: Recordable) {
    openDrawer(true, { record: record as QuizBlueprintModel, isUpdate: true });
  }

  function handlePreview(record: Recordable) {
    // Mở luôn tab "Rút thử" của drawer thay vì bắt admin bấm thêm một bước
    openDrawer(true, { record: record as QuizBlueprintModel, isUpdate: true, activeTab: 'preview' });
  }

  async function handleDelete(record: Recordable) {
    await quizBlueprintDeleteApi(record.id);
    handleSuccess();
  }

  async function handleBulkActive(active: boolean) {
    const ids = unref(selectedRowKeys);
    if (!ids.length) return;
    try {
      const result = await quizBlueprintSetActiveApi(ids, active);
      createMessage.success(`${active ? 'Đã bật' : 'Đã tắt'} ${result.affected} cấu hình`);
      selectedRowKeys.value = [];
      reload();
    } catch (error) {
      createMessage.error((error as Error).message || 'Không thực hiện được');
    }
  }

  function handleSuccess() {
    selectedRowKeys.value = [];
    reload();
  }
</script>
