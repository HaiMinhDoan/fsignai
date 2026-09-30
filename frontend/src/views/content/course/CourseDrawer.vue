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
        <div class="cover-row">
          <span class="cover-label">Ảnh đại diện</span>
          <ImageUploadCard
            :value="coverUrl"
            alt="Ảnh đại diện khoá học"
            hint="Hiện ở thẻ khoá học ngoài trang người dùng. Tối đa 1 ảnh, định dạng: JPG, PNG, GIF"
            confirm-text="Gỡ ảnh đại diện của khoá học này?"
            :upload="handleCoverUpload"
            :remove="handleCoverRemove"
          />
        </div>
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
          <Button v-if="lessons.length" :loading="publishingAll" @click="handlePublishAll(true)">
            Xuất bản tất cả
          </Button>
          <Button
            v-if="lessons.some((l) => l.isPublished)"
            :loading="publishingAll"
            @click="handlePublishAll(false)"
          >
            Gỡ tất cả
          </Button>
          <span class="text-xs text-gray-500">
            Dùng mũi tên để đổi thứ tự — thứ tự này là thứ tự người học nhìn thấy.
          </span>
        </div>

        <!-- Khoá đã xuất bản mà bài còn nháp thì học viên mở khoá ra thấy trống trơn -->
        <Alert
          v-if="lessons.length && !lessons.some((l) => l.isPublished)"
          type="warning"
          show-icon
          class="mb-4"
          message="Chưa bài nào được xuất bản"
          description="Người học chỉ thấy bài đã xuất bản. Bật công tắc ở cột Trạng thái, hoặc bấm nút Xuất bản tất cả."
        />

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
              <!-- Bài trống không xuất bản được: khoá công tắc và nói rõ lý do,
                   thay vì để bấm rồi mới nhận lỗi từ máy chủ -->
              <Tooltip :title="baiTrong(record) ? 'Soạn nội dung trước rồi mới xuất bản được' : ''">
                <Switch
                  :checked="record.isPublished"
                  :loading="togglingId === record.id"
                  :disabled="baiTrong(record)"
                  checked-children="Đã xuất bản"
                  un-checked-children="Bản nháp"
                  @change="(v) => handleTogglePublish(record, !!v)"
                />
              </Tooltip>
            </template>

            <template v-if="column.key === 'action'">
              <Button size="small" @click="handleEditLesson(record)">Sửa</Button>
              <Button size="small" type="primary" ghost class="ml-2" @click="handleEditItems(record)">
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

    <Modal
      :open="!!editingLesson"
      title="Sửa bài học"
      :confirm-loading="savingLesson"
      ok-text="Lưu"
      cancel-text="Huỷ"
      @ok="handleSaveLesson"
      @cancel="editingLesson = undefined"
    >
      <BasicForm @register="registerLessonForm" />
    </Modal>
  </BasicDrawer>
</template>

<script lang="ts" setup>
  import { ref } from 'vue';
  import { Alert, Button, Input, Modal, Popconfirm, Switch, Table, Tabs, Tag, Tooltip } from 'ant-design-vue';
  import { ImageUploadCard } from '@/components/ImageUploadCard';
  import { BasicDrawer, useDrawer, useDrawerInner } from '@/components/Drawer';
  import { BasicForm, useForm } from '@/components/Form';
  import { useMessage } from '@/hooks/web/useMessage';

  import LessonEditorDrawer from './LessonEditorDrawer.vue';
  import { courseFormSchema, lessonFormSchema } from './course.data';
  import { courseCreateApi, courseUpdateApi, courseDetailApi } from '@/api/catalog/course';
  import {
    imageUploadApi,
    lessonCreateApi,
    lessonDeleteApi,
    lessonPublishAllApi,
    lessonReorderApi,
    lessonUpdateApi,
  } from '@/api/catalog/lesson';
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
  const togglingId = ref<string>();
  const publishingAll = ref(false);
  const editingLesson = ref<LessonModel>();
  const savingLesson = ref(false);

  // Ảnh đại diện đi đường riêng: tải tệp lên trước để lấy id, rồi gửi id đó kèm form khi bấm Đồng ý
  const coverUrl = ref('');
  const coverFileId = ref<string>();
  const removeCover = ref(false);

  const baiTrong = (record: Recordable) => !record.itemCount && !record.isPublished;

  const [registerLessonDrawer, { openDrawer: openLessonDrawer }] = useDrawer();

  const lessonColumns = [
    { title: 'Thứ tự', key: 'order', width: 110 },
    { title: 'Tên bài học', dataIndex: 'titleVi', ellipsis: true },
    { title: 'Thời lượng', dataIndex: 'estimatedMinutes', width: 100 },
    { title: 'Nội dung', key: 'itemCount', width: 140 },
    { title: 'Trạng thái', key: 'isPublished', width: 150 },
    { title: '', key: 'action', width: 240 },
  ];

  const [registerForm, { resetFields, setFieldsValue, validate }] = useForm({
    labelWidth: 140,
    schemas: courseFormSchema,
    showActionButtonGroup: false,
    baseColProps: { span: 24 },
  });

  const [registerLessonForm, lessonForm] = useForm({
    labelWidth: 130,
    schemas: lessonFormSchema,
    showActionButtonGroup: false,
    baseColProps: { span: 24 },
  });

  const [registerDrawer, { setDrawerProps, closeDrawer }] = useDrawerInner(async (data) => {
    resetFields();
    coverUrl.value = '';
    coverFileId.value = undefined;
    removeCover.value = false;
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
      coverUrl.value = detail.coverUrl ?? '';
      setFieldsValue(detail);
    }
  });

  async function handleCoverUpload(file: File) {
    const res = await imageUploadApi(file, 'courses');
    coverFileId.value = res.id;
    coverUrl.value = res.url;
    removeCover.value = false;
  }

  async function handleCoverRemove() {
    coverUrl.value = '';
    coverFileId.value = undefined;
    removeCover.value = true;
  }

  async function handleSubmit() {
    try {
      const values = await validate();
      setDrawerProps({ confirmLoading: true });

      const payload = { ...values, coverFileId: coverFileId.value, removeCover: removeCover.value };
      if (isUpdate.value && currentCourse.value) {
        await courseUpdateApi(currentCourse.value.id, payload as any);
      } else {
        await courseCreateApi(payload as any);
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

  /**
   * API sửa bài nhận NGUYÊN bộ trường: gửi thiếu isPublished là bài bị đưa về nháp,
   * gửi thiếu tên là bị từ chối. Nên luôn gửi lại đủ những gì bài đang có.
   */
  function duLieuBai(l: LessonModel, doi: Partial<LessonModel> = {}) {
    return {
      titleVi: l.titleVi,
      descriptionVi: l.descriptionVi,
      estimatedMinutes: l.estimatedMinutes,
      displayOrder: l.displayOrder,
      isPublished: l.isPublished,
      ...doi,
    };
  }

  async function handleTogglePublish(record: Recordable, published: boolean) {
    togglingId.value = record.id;
    try {
      await lessonUpdateApi(record.id, duLieuBai(record as LessonModel, { isPublished: published }));
      await refreshLessons();
      emit('success');
    } catch (error) {
      createMessage.error((error as Error).message || 'Không đổi được trạng thái bài học');
    } finally {
      togglingId.value = undefined;
    }
  }

  async function handlePublishAll(published: boolean) {
    if (!currentCourse.value) return;
    publishingAll.value = true;
    try {
      const res = await lessonPublishAllApi(currentCourse.value.id, published);
      createMessage.success(
        (published ? `Đã xuất bản ${res.affected} bài` : `Đã đưa ${res.affected} bài về bản nháp`) +
          (res.skipped ? `, bỏ qua ${res.skipped} bài chưa có nội dung` : ''),
      );
      await refreshLessons();
      emit('success');
    } finally {
      publishingAll.value = false;
    }
  }

  function handleEditLesson(record: Recordable) {
    editingLesson.value = record as LessonModel;
    // Form nằm trong Modal, chỉ dựng xong sau khi modal mở
    setTimeout(() => {
      lessonForm.resetFields();
      lessonForm.setFieldsValue(record);
    }, 0);
  }

  async function handleSaveLesson() {
    if (!editingLesson.value) return;
    try {
      const values = await lessonForm.validate();
      savingLesson.value = true;
      await lessonUpdateApi(editingLesson.value.id, duLieuBai(editingLesson.value, values));
      editingLesson.value = undefined;
      await refreshLessons();
      emit('success');
    } catch (error) {
      const message = (error as Error)?.message;
      if (message) createMessage.error(message);
    } finally {
      savingLesson.value = false;
    }
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

<style lang="less" scoped>
  .cover-row {
    display: flex;
    gap: 12px;
    margin-bottom: 8px;
  }

  .cover-label {
    flex: 0 0 128px;
    padding-top: 6px;
    text-align: right;
  }
</style>
