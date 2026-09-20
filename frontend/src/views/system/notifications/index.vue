<template>
  <PageWrapper dense contentFullHeight fixedHeight>
    <BasicTable @register="registerTable" />
  </PageWrapper>
</template>

<script lang="ts" setup>
  import { BasicTable, useTable } from '@/components/Table';
  import { PageWrapper } from '@/components/Page';

  import { columns, searchFormSchema } from './notifications.data';
  import { notificationAdminFilterApi } from '@/api/system/notifications';

  defineOptions({ name: 'NotificationAdminViewer' });

  const loadNotifications = async (params: Recordable) => {
    const { page = 1, size = 20, channel, status } = params;
    return notificationAdminFilterApi({ channel, status, page: page - 1, size });
  };

  const [registerTable] = useTable({
    title: 'Lịch sử thông báo đã gửi',
    api: loadNotifications,
    rowKey: 'id',
    columns,
    formConfig: { labelWidth: 80, schemas: searchFormSchema },
    useSearchForm: true,
    fetchSetting: { sizeField: 'size' },
    showTableSetting: true,
    bordered: true,
  });
</script>
