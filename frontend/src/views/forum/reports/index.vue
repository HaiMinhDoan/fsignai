<template>
  <PageWrapper dense contentFullHeight fixedHeight>
    <BasicTable @register="registerTable">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'action'">
          <TableAction
            :actions="[
              {
                label: 'Xử lý',
                type: 'primary',
                ifShow: record.status === 'OPEN' || record.status === 'REVIEWING',
                onClick: handleResolve.bind(null, record),
              },
            ]"
          />
        </template>
      </template>
    </BasicTable>

    <ResolveReportModal @register="registerModal" @success="reload" />
  </PageWrapper>
</template>

<script lang="ts" setup>
  import { BasicTable, useTable, TableAction } from '@/components/Table';
  import { PageWrapper } from '@/components/Page';
  import { useModal } from '@/components/Modal';

  import ResolveReportModal from './ResolveReportModal.vue';
  import { columns, searchFormSchema } from './reports.data';
  import { forumReportFilterApi } from '@/api/forum';

  defineOptions({ name: 'ForumReports' });

  const [registerModal, { openModal }] = useModal();

  const loadReports = async (params: Recordable) => {
    const { page = 1, size = 20, openOnly = true } = params;
    return forumReportFilterApi({ openOnly, page: page - 1, size });
  };

  const [registerTable, { reload }] = useTable({
    title: 'Báo cáo vi phạm Diễn đàn',
    api: loadReports,
    rowKey: 'id',
    columns,
    formConfig: { labelWidth: 140, schemas: searchFormSchema },
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

  function handleResolve(record: Recordable<any>) {
    openModal(true, { record });
  }
</script>
