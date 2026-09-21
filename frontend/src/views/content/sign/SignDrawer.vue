<template>
  <BasicDrawer
    v-bind="$attrs"
    @register="registerDrawer"
    :title="isUpdate ? `Sửa từ: ${currentSign?.wordVi}` : 'Thêm từ vựng mới'"
    width="60%"
    showFooter
    @ok="handleSubmit"
  >
    <Tabs v-model:activeKey="activeTab">
      <TabPane key="info" tab="Thông tin">
        <BasicForm @register="registerForm" />
      </TabPane>

      <TabPane key="video" tab="Video ký hiệu" :disabled="!isUpdate">
        <div class="mb-4 flex items-end gap-3 flex-wrap">
          <div>
            <div class="mb-1 text-xs text-gray-500">Vùng miền</div>
            <Select v-model:value="uploadMeta.region" style="width: 140px" :options="REGION_OPTIONS" />
          </div>
          <div>
            <div class="mb-1 text-xs text-gray-500">Góc quay</div>
            <Select
              v-model:value="uploadMeta.viewAngle"
              style="width: 140px"
              :options="VIEW_ANGLE_OPTIONS"
            />
          </div>
          <div class="flex-1" style="min-width: 200px">
            <div class="mb-1 text-xs text-gray-500">Phụ đề / mô tả</div>
            <Input v-model:value="uploadMeta.captionVi" placeholder="Mô tả ngắn cho video" />
          </div>
          <Upload :before-upload="handleUpload" :show-upload-list="false" accept="video/*">
            <Button type="primary" :loading="uploading">Tải video lên</Button>
          </Upload>
        </div>

        <p class="mb-4 text-xs text-gray-500">
          Ảnh đại diện hiện ở thẻ từ vựng, flashcard, trò chơi và bài học. Video tải từ nguồn về
          đã có sẵn ảnh; video tự tải lên thì phải đặt ảnh ở cột <b>Ảnh đại diện</b>, nếu không
          những chỗ đó để trống.
        </p>

        <Alert
          v-if="!videos.length"
          type="info"
          show-icon
          message="Chưa có video nào"
          description="Từ chưa có video sẽ không hiển thị được cho người học và không chấm điểm AI được."
          class="mb-4"
        />

        <Table
          v-else
          :columns="videoColumns"
          :data-source="videos"
          :pagination="false"
          row-key="id"
          size="small"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'preview'">
              <video
                v-if="record.videoUrl"
                :src="record.videoUrl"
                :poster="record.thumbnailUrl"
                controls
                preload="metadata"
                style="width: 160px; border-radius: 4px; background: #eaf0f4"
              />
              <Tag v-else-if="record.ingestStatus === 'PENDING'" color="orange">
                Chờ nạp từ nguồn
              </Tag>
              <Tag v-else-if="record.ingestStatus === 'FAILED'" color="red">Nạp lỗi</Tag>
              <span v-else class="text-gray-400">—</span>
            </template>

            <template v-if="column.key === 'region'">
              {{ regionLabel(record.region) }}
            </template>

            <template v-if="column.key === 'thumbnail'">
              <!-- Ảnh thiếu KHÔNG phải lỗi video, nhưng cũng không vô hại: thẻ từ vựng,
                   flashcard, trò chơi và bài học đều lấy ảnh này. Video crawl về đã có
                   sẵn ảnh; video tự tải lên thì phải đặt ảnh ở đây. -->
              <ImageUploadCard
                :value="record.thumbnailUrl"
                :alt="`Ảnh đại diện video miền ${regionLabel(record.region)}`"
                text="Tải ảnh"
                hint=""
                confirm-text="Xoá ảnh đại diện của video này?"
                :upload="(file: File) => handleThumbnailUpload(record, file)"
                :remove="() => handleThumbnailRemove(record)"
              />
            </template>

            <template v-if="column.key === 'isPrimary'">
              <Tag v-if="record.isPrimary" color="green">Video chính</Tag>
              <Button v-else size="small" @click="handleSetPrimary(record)">Đặt làm chính</Button>
            </template>

            <template v-if="column.key === 'action'">
              <Popconfirm title="Xoá video này?" @confirm="handleDeleteVideo(record)">
                <Button size="small" danger>Xoá</Button>
              </Popconfirm>
            </template>
          </template>
        </Table>
      </TabPane>

      <TabPane key="steps" tab="Hướng dẫn từng bước" :disabled="!isUpdate">
        <div class="mb-4 flex items-center gap-3 flex-wrap">
          <Button type="primary" @click="openAddStep">Thêm bước</Button>
          <span class="text-xs text-gray-500">
            Ảnh không bắt buộc, nhưng mô tả bằng chữ thì bắt buộc — ảnh không được là
            kênh thông tin duy nhất.
          </span>
        </div>

        <Alert
          v-if="!steps.length"
          type="info"
          show-icon
          message="Từ này chưa có hướng dẫn từng bước"
          description="Người học sẽ chỉ thấy video mẫu và mô tả chung của từ cho tới khi có ít nhất một bước."
        />

        <Table
          v-else
          :columns="stepColumns"
          :data-source="steps"
          :pagination="false"
          row-key="id"
          size="small"
        >
          <template #bodyCell="{ column, record, index }">
            <template v-if="column.key === 'order'">
              <Button size="small" :disabled="index === 0 || reorderingSteps" @click="moveStep(index, -1)">
                ↑
              </Button>
              <Button
                size="small"
                class="ml-1"
                :disabled="index === steps.length - 1 || reorderingSteps"
                @click="moveStep(index, 1)"
              >
                ↓
              </Button>
            </template>

            <template v-if="column.key === 'image'">
              <ImageUploadCard
                :value="record.imageUrl"
                :alt="`Ảnh bước ${record.stepOrder}`"
                text="Tải ảnh"
                hint=""
                confirm-text="Xoá ảnh của bước này?"
                :upload="(file: File) => handleStepImageUpload(record, file)"
                :remove="() => handleDeleteStepImage(record)"
              />
            </template>

            <template v-if="column.key === 'bodyFocus'">
              <Tag v-if="record.bodyFocus">{{ bodyFocusLabel(record.bodyFocus) }}</Tag>
              <span v-else class="text-gray-400">—</span>
            </template>

            <template v-if="column.key === 'action'">
              <Button size="small" @click="openEditStep(record)">Sửa</Button>
              <Popconfirm title="Xoá bước này?" @confirm="handleDeleteStep(record)">
                <Button size="small" danger class="ml-1">Xoá</Button>
              </Popconfirm>
            </template>
          </template>
        </Table>

        <SignStepModal @register="registerStepModal" @success="refreshSteps" />
      </TabPane>

      <TabPane key="ai" tab="Chấm điểm AI" :disabled="!isUpdate">
        <Descriptions bordered :column="1" size="small">
          <DescriptionsItem label="Tình trạng exemplar">
            <Badge
              :status="currentSign?.aiReady ? 'success' : 'default'"
              :text="currentSign?.aiReady ? 'Đã có mẫu, chấm điểm được' : 'Chưa có mẫu'"
            />
          </DescriptionsItem>
          <DescriptionsItem label="Số tay khai báo">
            {{ currentSign?.handCount === 2 ? '2 tay' : '1 tay' }}
            <span class="text-gray-400">
              — chỉ để tham khảo: khi chấm, hệ thống tự đo số tay từ mẫu (cột này đang là 1 cho hầu hết từ).
            </span>
          </DescriptionsItem>
          <DescriptionsItem label="Đơn vị ngôn ngữ">
            {{ unitTypeLabel(currentSign?.unitType) }}
          </DescriptionsItem>
        </Descriptions>

        <div class="mb-2 mt-4 flex items-center justify-between">
          <span class="font-medium">Mẫu chấm điểm ({{ exemplars.length }})</span>
          <Button type="primary" :loading="rebuilding" @click="handleRebuildExemplars">
            Sinh lại mẫu từ video
          </Button>
        </div>
        <Table
          :columns="exemplarColumns"
          :data-source="exemplars"
          :pagination="false"
          size="small"
          row-key="id"
          :locale="{ emptyText: 'Chưa có mẫu. Bấm nút Sinh lại mẫu từ video để tạo từ các video của từ này.' }"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'region'">{{ regionLabel(record.region) }}</template>
            <template v-else-if="column.key === 'status'">
              <Tooltip :title="record.buildError">
                <Badge
                  :status="EXEMPLAR_STATUS[record.buildStatus]?.badge ?? 'default'"
                  :text="EXEMPLAR_STATUS[record.buildStatus]?.label ?? record.buildStatus"
                />
              </Tooltip>
            </template>
            <template v-else-if="column.key === 'quality'">
              {{ record.qualityScore != null ? `${Math.round(record.qualityScore * 100)}%` : '—' }}
            </template>
            <template v-else-if="column.key === 'version'">
              <Tag v-if="record.current" color="green">Hiện hành</Tag>
              <Tag v-else color="orange">Cũ — cần sinh lại</Tag>
            </template>
            <template v-else-if="column.key === 'active'">
              <Switch
                :checked="record.isActive"
                :disabled="record.buildStatus !== 'READY'"
                size="small"
                @change="(checked: any) => handleToggleExemplar(record, !!checked)"
              />
            </template>
          </template>
        </Table>

        <Alert
          class="mt-4"
          type="info"
          show-icon
          message="Mẫu được sinh tự động"
          description="Hệ thống chạy MediaPipe trên chính video đã tải lên để tạo mẫu chuẩn — không cần quay thêm clip riêng. Tắt một mẫu nếu video đó quay hỏng hoặc ký sai: mẫu bị tắt không dùng để chấm nữa nhưng vẫn giữ lại. Ngưỡng chấm điểm lấy theo nhóm (đơn vị ngôn ngữ × số tay) cho tới khi có đủ dữ liệu hiệu chỉnh riêng."
        />
      </TabPane>
    </Tabs>
  </BasicDrawer>
</template>

<script lang="ts" setup>
  import { ref } from 'vue';
  // Dự án này KHÔNG có unplugin-vue-components. registerGlobComp.ts chỉ đăng ký
  // Input, Button và Layout, nên mọi component Ant Design khác phải import ngay
  // trong file dùng nó — đúng cách các view sẵn có (TabsForm.vue) đang làm.
  // Thiếu import thì Vue coi <Tabs> là thẻ HTML lạ: không báo lỗi biên dịch,
  // chỉ lặng lẽ đổ hết nội dung mọi tab ra chung một chỗ.
  import {
    Alert,
    Badge,
    Button,
    Descriptions,
    Input,
    Popconfirm,
    Select,
    Switch,
    Table,
    Tabs,
    Tag,
    Tooltip,
    Upload,
  } from 'ant-design-vue';
  import { BasicDrawer, useDrawerInner } from '@/components/Drawer';
  import { ImageUploadCard } from '@/components/ImageUploadCard';
  import { BasicForm, useForm } from '@/components/Form';
  import { useModal } from '@/components/Modal';
  import { useMessage } from '@/hooks/web/useMessage';

  import SignStepModal from './SignStepModal.vue';

  const TabPane = Tabs.TabPane;
  const DescriptionsItem = Descriptions.Item;

  import {
    signFormSchema,
    REGION_OPTIONS,
    VIEW_ANGLE_OPTIONS,
    UNIT_TYPE_OPTIONS,
  } from './sign.data';
  import {
    signCreateApi,
    signUpdateApi,
    signDetailApi,
    signVideoUploadApi,
    signVideoSetPrimaryApi,
    signVideoDeleteApi,
    signVideoThumbnailUploadApi,
    signVideoThumbnailDeleteApi,
    signStepListApi,
    signStepDeleteApi,
    signStepReorderApi,
    signStepImageUploadApi,
    signStepImageDeleteApi,
  } from '@/api/content/sign';
  import { exemplarListApi, exemplarRebuildApi, exemplarSetActiveApi } from '@/api/content/ai';
  import type {
    BodyFocus,
    ExemplarModel,
    SignModel,
    SignStepModel,
    SignVideoModel,
  } from '@/api/content/model/contentModel';

  defineOptions({ name: 'SignDrawer' });

  const emit = defineEmits(['success', 'register']);
  const { createMessage } = useMessage();

  const isUpdate = ref(false);
  const activeTab = ref('info');
  const currentSign = ref<SignModel>();
  const videos = ref<SignVideoModel[]>([]);
  const uploading = ref(false);

  const steps = ref<SignStepModel[]>([]);
  const reorderingSteps = ref(false);

  const exemplars = ref<ExemplarModel[]>([]);
  const rebuilding = ref(false);
  const [registerStepModal, { openModal: openStepModal }] = useModal();

  const uploadMeta = ref({
    region: 'COMMON',
    viewAngle: 'FRONT',
    captionVi: '',
  });

  const videoColumns = [
    { title: 'Xem trước', key: 'preview', width: 180 },
    { title: 'Ảnh đại diện', key: 'thumbnail', width: 110 },
    { title: 'Vùng miền', key: 'region', dataIndex: 'region', width: 110 },
    { title: 'Phụ đề', dataIndex: 'captionVi', ellipsis: true },
    { title: 'Video chính', key: 'isPrimary', width: 130 },
    { title: '', key: 'action', width: 80 },
  ];

  const regionLabel = (v?: string) => REGION_OPTIONS.find((o) => o.value === v)?.label ?? v;
  const unitTypeLabel = (v?: string) => UNIT_TYPE_OPTIONS.find((o) => o.value === v)?.label ?? v;

  const BODY_FOCUS_LABELS: Record<BodyFocus, string> = {
    LEFT_HAND: 'Tay trái',
    RIGHT_HAND: 'Tay phải',
    BOTH_HANDS: 'Cả hai tay',
    FACE: 'Nét mặt',
    MOUTH: 'Khẩu hình miệng',
    SHOULDER: 'Vai',
    CHEST: 'Trước ngực',
  };
  const bodyFocusLabel = (v?: BodyFocus) => (v ? (BODY_FOCUS_LABELS[v] ?? v) : v);

  const stepColumns = [
    { title: 'Thứ tự', key: 'order', width: 100 },
    { title: 'Ảnh', key: 'image', width: 140 },
    { title: 'Tiêu đề', dataIndex: 'titleVi', width: 180, ellipsis: true },
    { title: 'Mô tả', dataIndex: 'descriptionVi', ellipsis: true },
    { title: 'Chú ý', key: 'bodyFocus', width: 120 },
    { title: '', key: 'action', width: 130 },
  ];

  const [registerForm, { resetFields, setFieldsValue, validate }] = useForm({
    labelWidth: 130,
    schemas: signFormSchema,
    showActionButtonGroup: false,
    baseColProps: { span: 24 },
  });

  const [registerDrawer, { setDrawerProps, closeDrawer }] = useDrawerInner(async (data) => {
    resetFields();
    setDrawerProps({ confirmLoading: false });

    isUpdate.value = !!data?.isUpdate;
    activeTab.value = data?.activeTab ?? 'info';
    videos.value = [];
    steps.value = [];
    exemplars.value = [];
    currentSign.value = undefined;

    if (isUpdate.value && data?.record?.id) {
      // Luôn lấy bản đầy đủ từ server thay vì dùng dòng trên bảng:
      // dòng bảng không chứa danh sách video và quan hệ từ.
      const detail = await signDetailApi(data.record.id);
      currentSign.value = detail;
      videos.value = detail.videos ?? [];

      setFieldsValue({
        ...detail,
        topicIds: (detail.topics ?? []).map((t) => t.id),
      });

      steps.value = await signStepListApi(detail.id);
      exemplars.value = await exemplarListApi(detail.id);
    }
  });

  async function handleSubmit() {
    try {
      const values = await validate();
      setDrawerProps({ confirmLoading: true });

      if (isUpdate.value && currentSign.value) {
        await signUpdateApi(currentSign.value.id, values as any);
      } else {
        await signCreateApi(values as any);
      }

      closeDrawer();
      emit('success');
    } finally {
      setDrawerProps({ confirmLoading: false });
    }
  }

  async function handleUpload(file: File) {
    if (!currentSign.value) return false;
    uploading.value = true;
    try {
      await signVideoUploadApi(currentSign.value.id, file, {
        region: uploadMeta.value.region as any,
        viewAngle: uploadMeta.value.viewAngle as any,
        captionVi: uploadMeta.value.captionVi,
      });
      await refreshVideos();
      uploadMeta.value.captionVi = '';
      emit('success');
    } finally {
      uploading.value = false;
    }
    // Trả false để antd không tự tải lên — ta đã tự gọi API
    return false;
  }

  // Slot #bodyCell của antd trả về Recordable chứ không phải SignVideoModel —
  // khai đúng kiểu ở đây thay vì ép kiểu trong template.
  async function handleSetPrimary(record: Recordable<any>) {
    if (!currentSign.value) return;
    await signVideoSetPrimaryApi(currentSign.value.id, record.id);
    await refreshVideos();
    emit('success');
  }

  async function handleDeleteVideo(record: Recordable<any>) {
    if (!currentSign.value) return;
    await signVideoDeleteApi(currentSign.value.id, record.id);
    createMessage.success('Đã xoá video');
    await refreshVideos();
    emit('success');
  }

  async function refreshVideos() {
    if (!currentSign.value) return;
    const detail = await signDetailApi(currentSign.value.id);
    currentSign.value = detail;
    videos.value = detail.videos ?? [];
  }

  // ==================== Chấm điểm AI ====================

  const EXEMPLAR_STATUS: Record<string, { label: string; badge: 'success' | 'error' | 'processing' | 'default' }> = {
    READY: { label: 'Sẵn sàng', badge: 'success' },
    FAILED: { label: 'Lỗi', badge: 'error' },
    PROCESSING: { label: 'Đang dựng', badge: 'processing' },
    PENDING: { label: 'Chờ dựng', badge: 'default' },
  };

  const exemplarColumns = [
    { title: 'Vùng miền', key: 'region', width: 110 },
    { title: 'Trạng thái', key: 'status', width: 120 },
    { title: 'Chất lượng', key: 'quality', width: 100 },
    { title: 'Phiên bản', key: 'version', width: 150 },
    { title: 'Dùng để chấm', key: 'active', width: 120 },
  ];

  async function handleRebuildExemplars() {
    if (!currentSign.value) return;
    rebuilding.value = true;
    try {
      exemplars.value = await exemplarRebuildApi(currentSign.value.id);
      const ready = exemplars.value.filter((e) => e.buildStatus === 'READY').length;
      if (ready > 0) createMessage.success(`Đã sinh ${ready}/${exemplars.value.length} mẫu`);
      else createMessage.warning('Chưa dựng được mẫu nào — xem lý do ở cột Trạng thái');
      await refreshVideos(); // cập nhật cờ aiReady ở phần đầu tab
      emit('success');
    } finally {
      rebuilding.value = false;
    }
  }

  async function handleToggleExemplar(record: Recordable<any>, active: boolean) {
    await exemplarSetActiveApi(record.id, active);
    if (currentSign.value) exemplars.value = await exemplarListApi(currentSign.value.id);
    await refreshVideos();
    emit('success');
  }

  // ==================== Hướng dẫn từng bước ====================

  function openAddStep() {
    if (!currentSign.value) return;
    openStepModal(true, { signId: currentSign.value.id, maxPosition: steps.value.length + 1 });
  }

  function openEditStep(record: Recordable<any>) {
    if (!currentSign.value) return;
    openStepModal(true, { signId: currentSign.value.id, step: record, maxPosition: steps.value.length });
  }

  async function handleDeleteStep(record: Recordable<any>) {
    if (!currentSign.value) return;
    await signStepDeleteApi(currentSign.value.id, record.id);
    await refreshSteps();
    emit('success');
  }

  /**
   * Đổi chỗ hai bước kề nhau rồi gửi toàn bộ danh sách id theo thứ tự mới —
   * cùng cách QuizDrawer đang sắp xếp câu hỏi.
   */
  async function moveStep(index: number, delta: number) {
    if (!currentSign.value) return;
    const target = index + delta;
    if (target < 0 || target >= steps.value.length) return;

    const next = [...steps.value];
    [next[index], next[target]] = [next[target], next[index]];
    steps.value = next;

    reorderingSteps.value = true;
    try {
      await signStepReorderApi(
        currentSign.value.id,
        next.map((s) => s.id),
      );
    } catch (error) {
      createMessage.error((error as Error).message || 'Không sắp xếp được, đang tải lại');
      await refreshSteps();
    } finally {
      reorderingSteps.value = false;
    }
  }

  async function handleStepImageUpload(record: Recordable<any>, file: File) {
    if (!currentSign.value) return;
    await signStepImageUploadApi(currentSign.value.id, record.id, file);
    await refreshSteps();
    emit('success');
  }

  async function handleThumbnailUpload(record: Recordable<any>, file: File) {
    if (!currentSign.value) return;
    await signVideoThumbnailUploadApi(currentSign.value.id, record.id, file);
    await refreshVideos();
    // Báo cho danh sách ngoài: ảnh đại diện của TỪ lấy từ video chính, đổi ảnh
    // ở đây là cột ảnh ngoài bảng cũng phải đổi theo
    emit('success');
  }

  async function handleThumbnailRemove(record: Recordable<any>) {
    if (!currentSign.value) return;
    await signVideoThumbnailDeleteApi(currentSign.value.id, record.id);
    await refreshVideos();
    emit('success');
  }

  async function handleDeleteStepImage(record: Recordable<any>) {
    if (!currentSign.value) return;
    await signStepImageDeleteApi(currentSign.value.id, record.id);
    await refreshSteps();
    emit('success');
  }

  async function refreshSteps() {
    if (!currentSign.value) return;
    steps.value = await signStepListApi(currentSign.value.id);
  }
</script>
