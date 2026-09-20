<template>
  <PageWrapper dense contentFullHeight fixedHeight>
    <BasicTable @register="registerTable" :rowSelection="rowSelection">
      <template #toolbar>
        <Button type="primary" @click="handleCreate">Thêm từ vựng</Button>
        <Button @click="openImportModal(true, {})">Nhập từ Excel</Button>
        <Button @click="openAiJobModal(true, {})">Sinh mẫu AI</Button>
        <Button :disabled="!selectedRowKeys.length" @click="handleBulkPublish(true)">
          Xuất bản ({{ selectedRowKeys.length }})
        </Button>
        <Button :disabled="!selectedRowKeys.length" @click="handleBulkPublish(false)">
          Gỡ xuất bản
        </Button>
        <Button
          type="primary"
          ghost
          :disabled="!selectedRowKeys.length"
          @click="openAssignModal(true, { signIds: selectedRowKeys })"
        >
          Gán chủ đề
        </Button>
      </template>

      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'action'">
          <TableAction
            :actions="[
              {
                icon: 'clarity:note-edit-line',
                tooltip: 'Sửa từ vựng',
                onClick: handleEdit.bind(null, record),
              },
              {
                icon: 'ant-design:video-camera-outlined',
                tooltip: 'Quản lý video',
                onClick: handleManageVideo.bind(null, record),
              },
              {
                icon: 'ant-design:delete-outlined',
                color: 'error',
                tooltip: 'Xoá từ vựng',
                popConfirm: {
                  title: `Xoá từ ${record.wordVi}? Video và exemplar đi kèm cũng bị xoá.`,
                  placement: 'left',
                  confirm: handleDelete.bind(null, record),
                },
              },
            ]"
          />
        </template>
      </template>
    </BasicTable>

    <SignDrawer @register="registerDrawer" @success="handleSuccess" />
    <SignImportModal @register="registerImportModal" @success="handleSuccess" />
    <AssignTopicsModal @register="registerAssignModal" @success="handleSuccess" />
    <AiExemplarJobModal @register="registerAiJobModal" @success="handleSuccess" />
  </PageWrapper>
</template>

<script lang="ts" setup>
  import { Button } from '@/components/Button';
  import { ref, computed, unref } from 'vue';
  import { BasicTable, useTable, TableAction } from '@/components/Table';
  import { PageWrapper } from '@/components/Page';
  import { useDrawer } from '@/components/Drawer';
  import { useModal } from '@/components/Modal';
  import { useMessage } from '@/hooks/web/useMessage';

  import SignDrawer from './SignDrawer.vue';
  import SignImportModal from './SignImportModal.vue';
  import AssignTopicsModal from './AssignTopicsModal.vue';
  import AiExemplarJobModal from './AiExemplarJobModal.vue';
  import { columns, searchFormSchema } from './sign.data';
  import { signSearchApi, signDeleteApi, signPublishApi } from '@/api/content/sign';
  import type { SignModel } from '@/api/content/model/contentModel';

  defineOptions({ name: 'SignManagement' });

  const { createMessage } = useMessage();
  const [registerDrawer, { openDrawer }] = useDrawer();
  const [registerImportModal, { openModal: openImportModal }] = useModal();
  const [registerAssignModal, { openModal: openAssignModal }] = useModal();
  const [registerAiJobModal, { openModal: openAiJobModal }] = useModal();

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
   * Cầu nối giữa vben và backend. Hai việc quy đổi ở đây:
   *
   * 1. vben đánh trang từ 1, Spring Data đánh từ 0 — lệch một đơn vị
   *    sẽ làm mất nguyên trang đầu tiên mà không báo lỗi gì.
   * 2. Ant Design Vue không nhận option value kiểu boolean, nên bộ lọc
   *    gửi lên chuỗi 'true'/'false'; đổi lại thành boolean thật cho backend.
   */
  const loadSigns = async (params: Recordable) => {
    const { page = 1, size = 20, isPublished, missingExemplar, ...rest } = params;
    return signSearchApi({
      ...rest,
      isPublished: toBool(isPublished),
      missingExemplar: toBool(missingExemplar),
      page: page - 1,
      size,
    });
  };

  function toBool(value: unknown): boolean | undefined {
    if (value === undefined || value === null || value === '') return undefined;
    return value === true || value === 'true';
  }

  const [registerTable, { reload }] = useTable({
    title: 'Từ vựng VSL',
    api: loadSigns,
    rowKey: 'id',
    columns,
    formConfig: {
      labelWidth: 110,
      schemas: searchFormSchema,
      autoSubmitOnEnter: true,
    },
    // Backend nhận `size`, vben mặc định gửi `pageSize`
    fetchSetting: { sizeField: 'size' },
    useSearchForm: true,
    showTableSetting: true,
    bordered: true,
    striped: false,
    actionColumn: {
      width: 120,
      title: 'Thao tác',
      dataIndex: 'action',
      fixed: 'right',
    },
  });

  function handleCreate() {
    openDrawer(true, { isUpdate: false });
  }

  // Slot bodyCell của vben trả record dạng Recordable, không phải SignModel,
  // nên ép kiểu ngay ở biên thay vì rải ép kiểu khắp nơi bên dưới.
  function handleEdit(record: Recordable) {
    openDrawer(true, { record: record as SignModel, isUpdate: true, activeTab: 'info' });
  }

  function handleManageVideo(record: Recordable) {
    openDrawer(true, { record: record as SignModel, isUpdate: true, activeTab: 'video' });
  }

  async function handleDelete(record: Recordable) {
    const sign = record as SignModel;
    await signDeleteApi(sign.id);
    createMessage.success(`Đã xoá từ ${sign.wordVi}`);
    reload();
  }

  async function handleBulkPublish(published: boolean) {
    const ids = unref(selectedRowKeys);
    if (!ids.length) return;
    const result = await signPublishApi(ids, published);
    createMessage.success(
      `${published ? 'Đã xuất bản' : 'Đã gỡ xuất bản'} ${result.affected} từ`,
    );
    selectedRowKeys.value = [];
    reload();
  }

  function handleSuccess() {
    reload();
  }
</script>
