<template>
  <PageWrapper dense contentFullHeight fixedHeight>
    <BasicTable @register="registerTable">
      <template #toolbar>
        <Button type="primary" @click="handleCreate">Thêm cấu hình</Button>
      </template>

      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'value'">
          <code style="font-size: 12px">{{ JSON.stringify(record.value) }}</code>
        </template>
        <template v-if="column.key === 'action'">
          <TableAction
            :actions="[
              { icon: 'clarity:note-edit-line', tooltip: 'Sửa', onClick: handleEdit.bind(null, record) },
              {
                icon: 'ant-design:delete-outlined',
                color: 'error',
                tooltip: 'Xoá',
                popConfirm: {
                  title: `Xoá cấu hình ${record.key}?`,
                  placement: 'left',
                  confirm: handleDelete.bind(null, record),
                },
              },
            ]"
          />
        </template>
      </template>
    </BasicTable>

    <SettingDrawer @register="registerDrawer" @success="reload" />
  </PageWrapper>
</template>

<script lang="ts" setup>
  import { Button } from '@/components/Button';
  import { BasicTable, useTable, TableAction, type BasicColumn } from '@/components/Table';
  import { PageWrapper } from '@/components/Page';
  import { useDrawer } from '@/components/Drawer';
  import { useMessage } from '@/hooks/web/useMessage';

  import SettingDrawer from './SettingDrawer.vue';
  import { settingListApi, settingDeleteApi } from '@/api/system/settings';
  import type { SystemSettingModel } from '@/api/system/model/systemSettingModel';

  defineOptions({ name: 'SystemSettingManagement' });

  const { createMessage } = useMessage();
  const [registerDrawer, { openDrawer }] = useDrawer();

  const columns: BasicColumn[] = [
    { title: 'Khoá', dataIndex: 'key', width: 220 },
    { title: 'Giá trị', dataIndex: 'value', key: 'value', width: 260 },
    { title: 'Mô tả', dataIndex: 'description' },
    { title: 'Cập nhật lần cuối', dataIndex: 'updatedByName', width: 150 },
  ];

  const loadSettings = async () => {
    const items = await settingListApi();
    return { items, total: items.length };
  };

  const [registerTable, { reload }] = useTable({
    title: 'Cài đặt hệ thống',
    api: loadSettings,
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

  function handleEdit(record: Recordable<any>) {
    openDrawer(true, { record: record as SystemSettingModel, isUpdate: true });
  }

  async function handleDelete(record: Recordable<any>) {
    await settingDeleteApi(record.id);
    createMessage.success(`Đã xoá cấu hình ${record.key}`);
    reload();
  }
</script>
