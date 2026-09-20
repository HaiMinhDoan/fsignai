<template>
  <BasicModal
    v-bind="$attrs"
    @register="registerModal"
    title="Sinh câu hỏi tự động"
    width="900px"
    :showOkBtn="false"
    :cancelText="result ? 'Đóng' : 'Huỷ'"
  >
    <Alert
      class="mb-4"
      type="info"
      show-icon
      message="Xem trước đúng là thứ sẽ được ghi"
      description="Bản chạy thử và bản ghi thật dùng chung một hạt ngẫu nhiên lấy từ id của đề, nên câu hỏi bạn xem trước chính là câu sẽ được lưu. Từ đã được hỏi trong đề sẽ không bị hỏi lại."
    />

    <div class="flex items-end gap-4 flex-wrap mb-4">
      <div style="min-width: 260px; flex: 1">
        <div class="mb-1 text-xs text-gray-500">Chủ đề nguồn (để trống = chủ đề của đề)</div>
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
      <div style="width: 120px">
        <div class="mb-1 text-xs text-gray-500">Số câu</div>
        <InputNumber v-model:value="questionCount" :min="1" :max="100" style="width: 100%" />
      </div>
      <div style="width: 130px">
        <div class="mb-1 text-xs text-gray-500">Lựa chọn/câu</div>
        <InputNumber v-model:value="optionCount" :min="2" :max="8" style="width: 100%" />
      </div>
      <div style="width: 200px">
        <div class="mb-1 text-xs text-gray-500">Đáp án nhiễu</div>
        <Select
          v-model:value="distractorStrategy"
          style="width: 100%"
          :options="DISTRACTOR_OPTIONS"
        />
      </div>
    </div>

    <div class="flex gap-3 mb-4">
      <Button type="primary" :loading="loading" @click="run(true)">Xem trước</Button>
      <Button type="primary" danger :disabled="!canCommit" :loading="loading" @click="run(false)">
        Ghi thật
      </Button>
      <span v-if="!canCommit" class="text-xs text-gray-500 self-center">
        Phải xem trước và có ít nhất một câu sinh được thì mới ghi thật được.
      </span>
    </div>

    <template v-if="result">
      <div v-if="result.warnings.length" class="mb-3">
        <Alert
          v-for="(w, i) in result.warnings"
          :key="i"
          class="mb-1"
          type="warning"
          show-icon
          :message="w"
        />
      </div>

      <Table
        v-if="result.questions.length"
        :columns="previewColumns"
        :data-source="result.questions"
        :pagination="false"
        :row-key="(_, i) => String(i)"
        size="small"
        :scroll="{ y: 300 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'questionType'">
            <Tag>{{ questionTypeLabel(record.questionType) }}</Tag>
          </template>
          <template v-if="column.key === 'options'">
            <div class="flex flex-wrap gap-1">
              <Tag
                v-for="(opt, i) in record.options"
                :key="i"
                :color="i === record.correctOptionIndex ? 'success' : 'default'"
              >
                {{ opt.label }}
              </Tag>
            </div>
          </template>
        </template>
      </Table>
    </template>
  </BasicModal>
</template>

<script lang="ts" setup>
  import { computed, ref } from 'vue';
  import { Alert, Button, InputNumber, Select, Table, Tag } from 'ant-design-vue';
  import { BasicModal, useModalInner } from '@/components/Modal';
  import { useMessage } from '@/hooks/web/useMessage';

  import { DISTRACTOR_OPTIONS, questionTypeLabel } from './quiz.data';
  import { topicOptionsApi } from '@/api/content/topic';
  import { quizGenerateQuestionsApi } from '@/api/practice/quiz';
  import type {
    DistractorStrategy,
    GenerateQuestionsResult,
  } from '@/api/practice/model/practiceModel';

  defineOptions({ name: 'GenerateQuestionsModal' });

  const emit = defineEmits(['success', 'register']);
  const { createMessage } = useMessage();

  const quizId = ref<string>();
  const topicIds = ref<string[]>([]);
  const questionCount = ref(10);
  const optionCount = ref(4);
  const distractorStrategy = ref<DistractorStrategy>('EASILY_CONFUSED');
  const loading = ref(false);
  const result = ref<GenerateQuestionsResult>();
  /** Chỉ cho ghi thật sau khi đã xem trước và thấy có câu sinh được */
  const previewed = ref(false);

  const topicOptions = ref<{ label: string; value: string }[]>([]);

  const canCommit = computed(() => previewed.value && (result.value?.created ?? 0) > 0);

  const previewColumns = [
    { title: 'Dạng câu', key: 'questionType', width: 150 },
    { title: 'Câu dẫn', dataIndex: 'promptVi', width: 260, ellipsis: true },
    { title: 'Đáp án đúng', dataIndex: 'signWordVi', width: 140 },
    { title: 'Lựa chọn', key: 'options' },
  ];

  const [registerModal, { setModalProps }] = useModalInner(async (data) => {
    setModalProps({ confirmLoading: false });
    quizId.value = data?.quizId;
    result.value = undefined;
    previewed.value = false;
    topicIds.value = [];

    if (!topicOptions.value.length) {
      const topics = await topicOptionsApi();
      topicOptions.value = topics.map((t) => ({ label: t.nameVi, value: t.id }));
    }
  });

  async function run(dryRun: boolean) {
    if (!quizId.value) return;
    loading.value = true;
    try {
      const data = await quizGenerateQuestionsApi(quizId.value, {
        topicIds: topicIds.value.length ? topicIds.value : undefined,
        questionCount: questionCount.value,
        optionCount: optionCount.value,
        distractorStrategy: distractorStrategy.value,
        requireVideo: true,
        dryRun,
      });
      result.value = data;

      if (dryRun) {
        previewed.value = true;
        if (data.created === 0) {
          // Nguyên nhân hay gặp nhất: từ vựng chưa được gán chủ đề
          createMessage.warning(
            'Không sinh được câu nào. Thường là do từ vựng chưa được gán chủ đề — ' +
              'hãy vào trang Từ vựng, chọn nhiều từ rồi dùng nút "Gán chủ đề".',
          );
        }
      } else {
        createMessage.success(`Đã thêm ${data.created} câu hỏi vào đề`);
        previewed.value = false;
        emit('success');
      }
    } finally {
      loading.value = false;
    }
  }
</script>
