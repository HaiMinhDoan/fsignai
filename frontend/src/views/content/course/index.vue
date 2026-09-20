<template>
  <PageWrapper dense contentFullHeight fixedHeight>
    <BasicTable @register="registerTable" :rowSelection="rowSelection">
      <template #toolbar>
        <Button type="primary" @click="handleCreate">Thêm khoá học</Button>
        <Button @click="openGenerateModal(true, {})">Sinh tự động từ chủ đề</Button>
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
                tooltip: 'Quản lý bài học',
                onClick: handleManageLessons.bind(null, record),
              },
              {
                icon: 'clarity:note-edit-line',
                tooltip: 'Sửa thông tin khoá',
                onClick: handleEdit.bind(null, record),
              },
              {
                icon: 'ant-design:delete-outlined',
                color: 'error',
                tooltip: 'Xoá khoá học',
                popConfirm: {
                  title: `Xoá khoá ${record.titleVi}? Toàn bộ bài học bên trong cũng bị xoá.`,
                  placement: 'left',
                  confirm: handleDelete.bind(null, record),
                },
              },
            ]"
          />
        </template>
      </template>
    </BasicTable>

    <CourseDrawer @register="registerDrawer" @success="handleSuccess" />
    <GenerateCoursesModal @register="registerGenerateModal" @success="handleSuccess" />
  </PageWrapper>
</template>

<script lang="ts" setup>
  import { ref, computed, unref } from 'vue';
  import { Button } from 'ant-design-vue';
  import { BasicTable, useTable, TableAction } from '@/components/Table';
  import { PageWrapper } from '@/components/Page';
  import { useDrawer } from '@/components/Drawer';
  import { useModal } from '@/components/Modal';
  import { useMessage } from '@/hooks/web/useMessage';

  import CourseDrawer from './CourseDrawer.vue';
  import GenerateCoursesModal from './GenerateCoursesModal.vue';
  import { columns, searchFormSchema } from './course.data';
  import { courseFilterApi, courseDeleteApi, coursePublishApi } from '@/api/catalog/course';
  import type { CourseModel } from '@/api/catalog/model/catalogModel';

  defineOptions({ name: 'CourseManagement' });

  const { createMessage } = useMessage();
  const [registerDrawer, { openDrawer }] = useDrawer();
  const [registerGenerateModal, { openModal: openGenerateModal }] = useModal();

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
   *
   * Khác trang Từ vựng: endpoint /courses/filter nhận bộ lọc dạng mảng
   * FilterCriteria chứ không phải các trường phẳng, nên phải dựng tay ở đây.
   * Đổi lại, thêm điều kiện lọc mới về sau không cần sửa backend.
   */
  const loadCourses = async (params: Recordable) => {
    const { page = 1, size = 20, keyword, topicId, level, isPublished } = params;

    const filters: Recordable[] = [];
    if (keyword?.trim()) {
      // ILIKE để không phân biệt hoa thường; tên khoá do người soạn gõ tay
      filters.push({ fieldName: 'titleVi', operation: 'ILIKE', value: keyword.trim() });
    }
    if (topicId) filters.push({ fieldName: 'topic.id', operation: 'EQUALS', value: topicId });
    if (level) filters.push({ fieldName: 'level', operation: 'EQUALS', value: level });
    const published = toBool(isPublished);
    if (published !== undefined) {
      filters.push({ fieldName: 'isPublished', operation: 'EQUALS', value: published });
    }

    return courseFilterApi({
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
    title: 'Khoá học',
    api: loadCourses,
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
      width: 120,
      title: 'Thao tác',
      dataIndex: 'action',
      fixed: 'right',
    },
  });

  function handleCreate() {
    openDrawer(true, { isUpdate: false });
  }

  // Slot bodyCell trả Recordable chứ không phải CourseModel — ép kiểu ngay ở biên
  function handleEdit(record: Recordable) {
    openDrawer(true, { record: record as CourseModel, isUpdate: true, activeTab: 'info' });
  }

  function handleManageLessons(record: Recordable) {
    openDrawer(true, { record: record as CourseModel, isUpdate: true, activeTab: 'lessons' });
  }

  async function handleDelete(record: Recordable) {
    await courseDeleteApi(record.id);
    handleSuccess();
  }

  async function handleBulkPublish(published: boolean) {
    const ids = unref(selectedRowKeys);
    if (!ids.length) return;
    try {
      const result = await coursePublishApi(ids, published);
      createMessage.success(
        `${published ? 'Đã xuất bản' : 'Đã gỡ xuất bản'} ${result.affected} khoá học`,
      );
      selectedRowKeys.value = [];
      reload();
    } catch (error) {
      // Backend chặn xuất bản khoá chưa có bài học và nói rõ khoá nào.
      // Thông điệp đó hữu ích hơn hẳn câu báo lỗi chung chung nên hiện nguyên văn.
      createMessage.error((error as Error).message || 'Không thực hiện được');
    }
  }

  function handleSuccess() {
    selectedRowKeys.value = [];
    reload();
  }
</script>
