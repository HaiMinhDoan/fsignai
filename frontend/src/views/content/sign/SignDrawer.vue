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

      <TabPane key="ai" tab="Chấm điểm AI" :disabled="!isUpdate">
        <Descriptions bordered :column="1" size="small">
          <DescriptionsItem label="Tình trạng exemplar">
            <Badge
              :status="currentSign?.aiReady ? 'success' : 'default'"
              :text="currentSign?.aiReady ? 'Đã có mẫu, chấm điểm được' : 'Chưa có mẫu'"
            />
          </DescriptionsItem>
          <DescriptionsItem label="Số tay">
            {{ currentSign?.handCount === 2 ? '2 tay' : '1 tay' }}
          </DescriptionsItem>
          <DescriptionsItem label="Đơn vị ngôn ngữ">
            {{ unitTypeLabel(currentSign?.unitType) }}
          </DescriptionsItem>
        </Descriptions>

        <Alert
          class="mt-4"
          type="info"
          show-icon
          message="Exemplar được sinh tự động"
          description="Hệ thống chạy MediaPipe trên chính video đã tải lên để tạo mẫu chuẩn — không cần quay thêm clip riêng. Ngưỡng chấm điểm lấy theo nhóm (đơn vị ngôn ngữ × số tay) cho tới khi có đủ dữ liệu hiệu chỉnh riêng."
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
    Table,
    Tabs,
    Tag,
    Upload,
  } from 'ant-design-vue';
  import { BasicDrawer, useDrawerInner } from '@/components/Drawer';
  import { BasicForm, useForm } from '@/components/Form';
  import { useMessage } from '@/hooks/web/useMessage';

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
  } from '@/api/content/sign';
  import type { SignModel, SignVideoModel } from '@/api/content/model/contentModel';

  defineOptions({ name: 'SignDrawer' });

  const emit = defineEmits(['success', 'register']);
  const { createMessage } = useMessage();

  const isUpdate = ref(false);
  const activeTab = ref('info');
  const currentSign = ref<SignModel>();
  const videos = ref<SignVideoModel[]>([]);
  const uploading = ref(false);

  const uploadMeta = ref({
    region: 'COMMON',
    viewAngle: 'FRONT',
    captionVi: '',
  });

  const videoColumns = [
    { title: 'Xem trước', key: 'preview', width: 180 },
    { title: 'Vùng miền', key: 'region', dataIndex: 'region', width: 110 },
    { title: 'Phụ đề', dataIndex: 'captionVi', ellipsis: true },
    { title: 'Video chính', key: 'isPrimary', width: 130 },
    { title: '', key: 'action', width: 80 },
  ];

  const regionLabel = (v?: string) => REGION_OPTIONS.find((o) => o.value === v)?.label ?? v;
  const unitTypeLabel = (v?: string) => UNIT_TYPE_OPTIONS.find((o) => o.value === v)?.label ?? v;

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
</script>
