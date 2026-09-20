<template>
  <BasicDrawer
    v-bind="$attrs"
    @register="registerDrawer"
    :title="isUpdate ? `Sửa khoá: ${currentCourse?.titleVi}` : 'Thêm khoá học mới'"
    width="60%"
    showFooter
    @ok="handleSubmit"
  >
    <Tabs v-model:activeKey="activeTab">
      <TabPane key="info" tab="Thông tin">
        <BasicForm @register="registerForm" />
      </TabPane>

      <TabPane key="lessons" tab="Bài học" :disabled="!isUpdate">
        <div class="mb-4 flex items-center gap-3 flex-wrap">
          <Input
            v-model:value="newLessonTitle"
            placeholder="Tên bài học mới"
            style="width: 260px"
            @press-enter="handleAddLesson"
          />
          <Button type="primary" :loading="addingLesson" @click="handleAddLesson">
            Thêm bài học
          </Button>
          <span class="text-xs text-gray-500">
            Dùng mũi tên để đổi thứ tự — thứ tự này là thứ tự người học nhìn thấy.
          </span>
        </div>

        <Alert
          v-if="!lessons.length"
          type="info"
          show-icon
          message="Khoá này chưa có bài học"
          description="Khoá chưa có bài học sẽ không xuất bản được. Thêm bài học, rồi bấm biểu tượng danh sách để đưa từ vựng vào bài."
        />

        <Table
          v-else
          :columns="lessonColumns"
          :data-source="lessons"
          :pagination="false"
          row-key="id"
          size="small"
        >
          <template #bodyCell="{ column, record, index }">
            <template v-if="column.key === 'order'">
              <Button
                size="small"
                :disabled="index === 0 || reordering"
                @click="moveLesson(index, -1)"
              >
                ↑
              </Button>
              <Button
                size="small"
                class="ml-1"
                :disabled="index === lessons.length - 1 || reordering"
                @click="moveLesson(index, 1)"
              >
                ↓
              </Button>
            </template>

            <template v-if="column.key === 'itemCount'">
              <Tag v-if="!record.itemCount" color="orange">Chưa có nội dung</Tag>
              <span v-else>{{ record.itemCount }} nội dung</span>
            </template>

            <template v-if="column.key === 'isPublished'">
              <Tag v-if="record.isPublished" color="success">Đã xuất bản</Tag>
              <Tag v-else color="default">Bản nháp</Tag>
            </template>

            <template v-if="column.key === 'action'">
              <Button size="small" type="primary" ghost @click="handleEditItems(record)">
                Soạn nội dung
              </Button>
              <Popconfirm
                :title="`Xoá bài ${record.titleVi}?`"
                @confirm="handleDeleteLesson(record)"
              >
                <Button size="small" danger class="ml-2">Xoá</Button>
              </Popconfirm>
            </template>
          </template>
        </Table>
      </TabPane>
    </Tabs>

    <LessonEditorDrawer @register="registerLessonDrawer" @success="refreshLessons" />
  </BasicDrawer>
</template>

<script lang="ts" setup>
  import { ref } from 'vue';
  import { Alert, Button, Input, Popconfirm, Table, Tabs, Tag } from 'ant-design-vue';
  import { BasicDrawer, useDrawer, useDrawerInner } from '@/components/Drawer';
  import { BasicForm, useForm } from '@/components/Form';
  import { useMessage } from '@/hooks/web/useMessage';

  import LessonEditorDrawer from './LessonEditorDrawer.vue';
  import { courseFormSchema } from './course.data';
  import { courseCreateApi, courseUpdateApi, courseDetailApi } from '@/api/catalog/course';
  import { lessonCreateApi, lessonDeleteApi, lessonReorderApi } from '@/api/catalog/lesson';
  import type { CourseModel, LessonModel } from '@/api/catalog/model/catalogModel';

  const TabPane = Tabs.TabPane;

  defineOptions({ name: 'CourseDrawer' });

  const emit = defineEmits(['success', 'register']);
  const { createMessage } = useMessage();

  const isUpdate = ref(false);
  const activeTab = ref('info');
  const currentCourse = ref<CourseModel>();
  const lessons = ref<LessonModel[]>([]);
  const newLessonTitle = ref('');
  const addingLesson = ref(false);
  const reordering = ref(false);

  const [registerLessonDrawer, { openDrawer: openLessonDrawer }] = useDrawer();

  const lessonColumns = [
    { title: 'Thứ tự', key: 'order', width: 110 },
    { title: 'Tên bài học', dataIndex: 'titleVi', ellipsis: true },
    { title: 'Thời lượng', dataIndex: 'estimatedMinutes', width: 100 },
    { title: 'Nội dung', key: 'itemCount', width: 140 },
    { title: 'Trạng thái', key: 'isPublished', width: 120 },
    { title: '', key: 'action', width: 180 },
  ];

  const [registerForm, { resetFields, setFieldsValue, validate }] = useForm({
    labelWidth: 140,
    schemas: courseFormSchema,
    showActionButtonGroup: false,
    baseColProps: { span: 24 },
  });

  const [registerDrawer, { setDrawerProps, closeDrawer }] = useDrawerInner(async (data) => {
    resetFields();
    setDrawerProps({ confirmLoading: false });

    isUpdate.value = !!data?.isUpdate;
    activeTab.value = data?.activeTab ?? 'info';
    lessons.value = [];
    currentCourse.value = undefined;
    newLessonTitle.value = '';

    if (isUpdate.value && data?.record?.id) {
      // Luôn lấy bản đầy đủ từ server: dòng trên bảng không chứa danh sách bài học
      const detail = await courseDetailApi(data.record.id);
      currentCourse.value = detail;
      lessons.value = detail.lessons ?? [];
      setFieldsValue(detail);
    }
  });

  async function handleSubmit() {
    try {
      const values = await validate();
      setDrawerProps({ confirmLoading: true });

      if (isUpdate.value && currentCourse.value) {
        await courseUpdateApi(currentCourse.value.id, values as any);
      } else {
        await courseCreateApi(values as any);
      }

      closeDrawer();
      emit('success');
    } catch (error) {
      // Backend chặn xuất bản khoá rỗng — hiện nguyên văn lý do
      const message = (error as Error)?.message;
      if (message) createMessage.error(message);
    } finally {
      setDrawerProps({ confirmLoading: false });
    }
  }

  async function handleAddLesson() {
    const title = newLessonTitle.value.trim();
    if (!title) {
      createMessage.warning('Nhập tên bài học trước đã');
      return;
    }
    if (!currentCourse.value) return;

    addingLesson.value = true;
    try {
      await lessonCreateApi(currentCourse.value.id, { titleVi: title });
      newLessonTitle.value = '';
      await refreshLessons();
      emit('success');
    } finally {
      addingLesson.value = false;
    }
  }

  async function handleDeleteLesson(record: Recordable) {
    await lessonDeleteApi(record.id);
    await refreshLessons();
    emit('success');
  }

  function handleEditItems(record: Recordable) {
    openLessonDrawer(true, { lessonId: record.id, lessonTitle: record.titleVi });
  }

  /**
   * Đổi chỗ hai bài kề nhau rồi gửi TOÀN BỘ danh sách id theo thứ tự mới.
   *
   * Backend từ chối nếu danh sách không khớp số bài hiện có — đó là lúc ai đó
   * vừa thêm hoặc xoá bài ở tab khác. Khi ấy tải lại danh sách thay vì ghi đè
   * lên trạng thái đã cũ.
   */
  async function moveLesson(index: number, delta: number) {
    if (!currentCourse.value) return;
    const target = index + delta;
    if (target < 0 || target >= lessons.value.length) return;

    const next = [...lessons.value];
    [next[index], next[target]] = [next[target], next[index]];
    lessons.value = next;

    reordering.value = true;
    try {
      await lessonReorderApi(currentCourse.value.id, { orderedIds: next.map((l) => l.id) });
    } catch (error) {
      createMessage.error((error as Error).message || 'Không sắp xếp được, đang tải lại');
      await refreshLessons();
    } finally {
      reordering.value = false;
    }
  }

  async function refreshLessons() {
    if (!currentCourse.value) return;
    const detail = await courseDetailApi(currentCourse.value.id);
    currentCourse.value = detail;
    lessons.value = detail.lessons ?? [];
  }
</script>
