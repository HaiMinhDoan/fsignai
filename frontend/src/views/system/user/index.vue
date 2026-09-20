<template>
  <PageWrapper dense contentFullHeight fixedHeight>
    <BasicTable @register="registerTable">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'action'">
          <TableAction
            :actions="[
              {
                label: 'Chi tiết',
                onClick: handleDetail.bind(null, record),
              },
            ]"
          />
        </template>
      </template>
    </BasicTable>

    <UserDetailDrawer @register="registerDrawer" @success="reload" />
  </PageWrapper>
</template>

<script lang="ts" setup>
  import { BasicTable, useTable, TableAction } from '@/components/Table';
  import { PageWrapper } from '@/components/Page';
  import { useDrawer } from '@/components/Drawer';

  import UserDetailDrawer from './UserDetailDrawer.vue';
  import { columns, searchFormSchema } from './user.data';
  import { userFilterApi } from '@/api/system/user';

  defineOptions({ name: 'UserManagement' });

  const [registerDrawer, { openDrawer }] = useDrawer();

  const loadUsers = async (params: Recordable) => {
    const { page = 1, size = 20, keyword, status, accountKind, vslRoleStatus } = params;
    return userFilterApi({ keyword, status, accountKind, vslRoleStatus, page: page - 1, size });
  };

  const [registerTable, { reload }] = useTable({
    title: 'Quản lý người dùng',
    api: loadUsers,
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

  function handleDetail(record: Recordable<any>) {
    openDrawer(true, { id: record.id });
  }
</script>
