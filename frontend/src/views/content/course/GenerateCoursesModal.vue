<template>
  <BasicModal
    v-bind="$attrs"
    @register="registerModal"
    title="Sinh khoá học tự động từ chủ đề"
    width="860px"
    :showOkBtn="false"
    :cancelText="result ? 'Đóng' : 'Huỷ'"
  >
    <Alert
      class="mb-4"
      type="info"
      show-icon
      message="Máy chia bài, người duyệt"
      description="Hệ thống gom từ vựng theo chủ đề rồi cắt thành các bài đều nhau. Khoá sinh ra luôn ở trạng thái bản nháp — bạn xem lại rồi mới xuất bản. Chủ đề nào đã có khoá kèm bài học sẽ được bỏ qua, không ghi đè."
    />

    <div class="flex items-end gap-4 flex-wrap mb-4">
      <div style="min-width: 300px; flex: 1">
        <div class="mb-1 text-xs text-gray-500">Chủ đề (để trống = mọi chủ đề đang có từ vựng)</div>
        <Select
          v-model:value="topicIds"
          mode="multiple"
          style="width: 100%"
          placeholder="Chọn chủ đề"
          allow-clear
          :options="topicOptions"
          :max-tag-count="3"
        />
      </div>
      <div style="width: 170px">
        <div class="mb-1 text-xs text-gray-500">Số từ mỗi bài</div>
        <InputNumber v-model:value="signsPerLesson" :min="3" :max="30" style="width: 100%" />
      </div>
      <div>
        <Checkbox v-model:checked="onlyPublishedSigns">Chỉ lấy từ đã xuất bản</Checkbox>
      </div>
    </div>

    <div class="flex gap-3 mb-4">
      <Button type="primary" :loading="loading" @click="run(true)">Xem trước</Button>
      <Button
        type="primary"
        danger
        :disabled="!canCommit"
        :loading="loading"
        @click="run(false)"
      >
        Sinh thật
      </Button>
      <span v-if="!canCommit" class="text-xs text-gray-500 self-center">
        Phải xem trước và có ít nhất một chủ đề tạo được khoá thì mới sinh thật được.
      </span>
    </div>

    <template v-if="result">
      <Descriptions bordered size="small" :column="4" class="mb-4">
        <DescriptionsItem label="Khoá mới">{{ result.coursesCreated }}</DescriptionsItem>
        <DescriptionsItem label="Bài học">{{ result.lessonsCreated }}</DescriptionsItem>
        <DescriptionsItem label="Nội dung">{{ result.itemsCreated }}</DescriptionsItem>
        <DescriptionsItem label="Chủ đề bỏ qua">{{ result.topicsSkipped }}</DescriptionsItem>
      </Descriptions>

      <Table
        :columns="planColumns"
        :data-source="result.plans"
        :pagination="false"
        row-key="topicNameVi"
        size="small"
        :scroll="{ y: 260 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <Tag v-if="record.skippedReason" color="orange">{{ record.skippedReason }}</Tag>
            <Tag v-else color="green">
              {{ record.signCount }} từ → {{ record.lessonCount }} bài
            </Tag>
          </template>
        </template>
      </Table>
    </template>
  </BasicModal>
</template>

<script lang="ts" setup>
  import { computed, ref } from 'vue';
  import {
    Alert,
    Button,
    Checkbox,
    Descriptions,
    InputNumber,
    Select,
    Table,
    Tag,
  } from 'ant-design-vue';
  import { BasicModal, useModalInner } from '@/components/Modal';
  import { useMessage } from '@/hooks/web/useMessage';

  import { topicOptionsApi } from '@/api/content/topic';
  import { courseGenerateApi } from '@/api/catalog/course';
  import type { GenerateCoursesResult } from '@/api/catalog/model/catalogModel';

  const DescriptionsItem = Descriptions.Item;

  defineOptions({ name: 'GenerateCoursesModal' });

  const emit = defineEmits(['success', 'register']);
  const { createMessage } = useMessage();

  const topicIds = ref<string[]>([]);
  const signsPerLesson = ref(8);
  const onlyPublishedSigns = ref(false);
  const loading = ref(false);
  const result = ref<GenerateCoursesResult>();
  /** Chỉ cho sinh thật sau khi đã xem trước và thấy có việc để làm */
  const previewed = ref(false);

  const canCommit = computed(
    () => previewed.value && !!result.value && result.value.lessonsCreated > 0,
  );

  const topicOptions = ref<{ label: string; value: string }[]>([]);

  const planColumns = [
    { title: 'Chủ đề', dataIndex: 'topicNameVi', width: 260 },
    { title: 'Số từ', dataIndex: 'signCount', width: 100 },
    { title: 'Số bài', dataIndex: 'lessonCount', width: 100 },
    { title: 'Kết quả', key: 'status' },
  ];

  const [registerModal, { setModalProps }] = useModalInner(async () => {
    setModalProps({ confirmLoading: false });
    result.value = undefined;
    previewed.value = false;
    topicIds.value = [];

    if (!topicOptions.value.length) {
      const topics = await topicOptionsApi();
      topicOptions.value = topics.map((t) => ({ label: t.nameVi, value: t.id }));
    }
  });

  async function run(dryRun: boolean) {
    loading.value = true;
    try {
      const data = await courseGenerateApi({
        topicIds: topicIds.value.length ? topicIds.value : undefined,
        signsPerLesson: signsPerLesson.value,
        onlyPublishedSigns: onlyPublishedSigns.value,
        dryRun,
      });
      result.value = data;

      if (dryRun) {
        previewed.value = true;
        if (data.lessonsCreated === 0) {
          // Trường hợp hay gặp nhất: từ vựng chưa được gán chủ đề nào.
          // Nói thẳng nguyên nhân thay vì để người dùng nhìn bảng rỗng và đoán.
          createMessage.warning(
            'Không có gì để sinh. Thường là do từ vựng chưa được gán chủ đề — ' +
              'hãy vào trang Từ vựng, chọn nhiều từ rồi dùng nút "Gán chủ đề".',
          );
        }
      } else {
        createMessage.success(
          `Đã tạo ${data.coursesCreated} khoá, ${data.lessonsCreated} bài, ${data.itemsCreated} nội dung`,
        );
        previewed.value = false;
        emit('success');
      }
    } finally {
      loading.value = false;
    }
  }
</script>
