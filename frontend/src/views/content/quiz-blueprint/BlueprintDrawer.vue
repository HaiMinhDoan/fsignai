<template>
  <BasicDrawer
    v-bind="$attrs"
    @register="registerDrawer"
    :title="isUpdate ? `Sửa cấu hình: ${currentBlueprint?.titleVi}` : 'Thêm cấu hình đề trộn'"
    width="70%"
    showFooter
    @ok="handleSubmit"
  >
    <Tabs v-model:activeKey="activeTab">
      <TabPane key="info" tab="Thông tin">
        <Alert
          v-if="isUpdate && currentBlueprint"
          class="mb-4"
          :type="matchEnough ? 'success' : 'warning'"
          show-icon
          :message="`${currentBlueprint.matchingSignCount} / ${currentBlueprint.questionCount} từ khớp bộ lọc hiện tại`"
          :description="
            matchEnough
              ? 'Đủ từ vựng để rút đề theo cấu hình này.'
              : 'Kho chỉ khớp ít hơn số câu yêu cầu — đề rút ra sẽ bị thiếu câu. Hãy mở rộng bộ lọc hoặc gán thêm chủ đề cho từ vựng.'
          "
        />

        <BasicForm @register="registerForm" />

        <div class="mb-2 text-sm font-medium">Tỉ lệ các dạng câu</div>
        <div class="mb-1 text-xs text-gray-500">
          Số lượng tương đối giữa các dạng câu trong một đề. Ví dụ 5/3/0/2 nghĩa là cứ 10 câu thì
          khoảng 5 câu xem video chọn từ, 3 câu đọc từ chọn video, 2 câu ghép đôi.
        </div>
        <div class="flex flex-wrap gap-4 mb-4">
          <div v-for="type in MIX_QUESTION_TYPES" :key="type.value" style="width: 180px">
            <div class="mb-1 text-xs text-gray-500">{{ type.label }}</div>
            <InputNumber v-model:value="mix[type.value]" :min="0" :max="20" style="width: 100%" />
          </div>
        </div>
      </TabPane>

      <TabPane key="preview" tab="Rút thử" :disabled="!isUpdate">
        <div class="mb-4 flex items-center gap-3 flex-wrap">
          <Button type="primary" :loading="previewLoading" @click="runPreview">Rút thử đề</Button>
          <span class="text-xs text-gray-500">
            Dùng hạt cố định theo cấu hình này nên rút thử nhiều lần ra cùng một đề. Không ghi vào
            đâu cả — chỉ để soát lại luật.
          </span>
        </div>

        <template v-if="previewResult">
          <div v-if="previewResult.warnings.length" class="mb-3">
            <Alert
              v-for="(w, i) in previewResult.warnings"
              :key="i"
              class="mb-1"
              type="warning"
              show-icon
              :message="w"
            />
          </div>

          <Table
            v-if="previewResult.questions.length"
            :columns="previewColumns"
            :data-source="previewResult.questions"
            :pagination="false"
            :row-key="(_, i) => String(i)"
            size="small"
            :scroll="{ y: 360 }"
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
      </TabPane>
    </Tabs>
  </BasicDrawer>
</template>

<script lang="ts" setup>
  import { computed, reactive, ref } from 'vue';
  import { Alert, Button, InputNumber, Table, Tabs, Tag } from 'ant-design-vue';
  import { BasicDrawer, useDrawerInner } from '@/components/Drawer';
  import { BasicForm, useForm } from '@/components/Form';
  import { useMessage } from '@/hooks/web/useMessage';

  import { MIX_QUESTION_TYPES, blueprintFormSchema } from './blueprint.data';
  import { questionTypeLabel } from '../quiz/quiz.data';
  import {
    quizBlueprintCreateApi,
    quizBlueprintUpdateApi,
    quizBlueprintDetailApi,
    quizBlueprintPreviewApi,
  } from '@/api/practice/quizBlueprint';
  import type {
    GenerateQuestionsResult,
    QuestionType,
    QuizBlueprintModel,
  } from '@/api/practice/model/practiceModel';

  const TabPane = Tabs.TabPane;

  defineOptions({ name: 'BlueprintDrawer' });

  const emit = defineEmits(['success', 'register']);
  const { createMessage } = useMessage();

  const isUpdate = ref(false);
  const activeTab = ref('info');
  const currentBlueprint = ref<QuizBlueprintModel>();

  const previewLoading = ref(false);
  const previewResult = ref<GenerateQuestionsResult>();

  // Map<QuestionType, number> không có widget dựng sẵn nên soạn thủ công 4 ô số
  const mix = reactive<Record<string, number>>({
    VIDEO_TO_WORD: 5,
    WORD_TO_VIDEO: 3,
    MULTIPLE_CHOICE: 0,
    MATCHING: 2,
  });

  const matchEnough = computed(
    () => (currentBlueprint.value?.matchingSignCount ?? 0) >= (currentBlueprint.value?.questionCount ?? 0),
  );

  const previewColumns = [
    { title: 'Dạng câu', key: 'questionType', width: 150 },
    { title: 'Câu dẫn', dataIndex: 'promptVi', width: 260, ellipsis: true },
    { title: 'Đáp án đúng', dataIndex: 'signWordVi', width: 140 },
    { title: 'Lựa chọn', key: 'options' },
  ];

  const [registerForm, { resetFields, setFieldsValue, validate }] = useForm({
    labelWidth: 190,
    schemas: blueprintFormSchema,
    showActionButtonGroup: false,
    baseColProps: { span: 24 },
  });

  const [registerDrawer, { setDrawerProps, closeDrawer }] = useDrawerInner(async (data) => {
    resetFields();
    setDrawerProps({ confirmLoading: false });

    isUpdate.value = !!data?.isUpdate;
    activeTab.value = data?.activeTab ?? 'info';
    currentBlueprint.value = undefined;
    previewResult.value = undefined;

    Object.assign(mix, {
      VIDEO_TO_WORD: 5,
      WORD_TO_VIDEO: 3,
      MULTIPLE_CHOICE: 0,
      MATCHING: 2,
    });

    if (isUpdate.value && data?.record?.id) {
      const detail = await quizBlueprintDetailApi(data.record.id);
      applyDetail(detail);
      // Mở thẳng vào tab Rút thử (từ nút "Rút thử" ở bảng danh sách) thì chạy luôn,
      // đỡ bắt admin bấm thêm một lần nữa
      if (activeTab.value === 'preview') runPreview();
    }
  });

  function applyDetail(detail: QuizBlueprintModel) {
    currentBlueprint.value = detail;
    setFieldsValue(detail);
    Object.keys(mix).forEach((key) => {
      mix[key] = detail.questionTypeMix[key as QuestionType] ?? 0;
    });
  }

  async function handleSubmit() {
    try {
      const values = await validate();
      setDrawerProps({ confirmLoading: true });

      const questionTypeMix: Partial<Record<QuestionType, number>> = {};
      Object.entries(mix).forEach(([key, value]) => {
        if (value > 0) questionTypeMix[key as QuestionType] = value;
      });
      if (!Object.keys(questionTypeMix).length) {
        createMessage.error('Phải có ít nhất một dạng câu với số lượng lớn hơn 0');
        setDrawerProps({ confirmLoading: false });
        return;
      }

      const payload = { ...values, questionTypeMix } as any;

      if (isUpdate.value && currentBlueprint.value) {
        await quizBlueprintUpdateApi(currentBlueprint.value.id, payload);
      } else {
        await quizBlueprintCreateApi(payload);
      }

      closeDrawer();
      emit('success');
    } catch (error) {
      const message = (error as Error)?.message;
      if (message) createMessage.error(message);
    } finally {
      setDrawerProps({ confirmLoading: false });
    }
  }

  async function runPreview() {
    if (!currentBlueprint.value) return;
    previewLoading.value = true;
    try {
      previewResult.value = await quizBlueprintPreviewApi(currentBlueprint.value.id);
      if (previewResult.value.created === 0) {
        createMessage.warning(
          'Không rút được câu nào. Thường là do bộ lọc chưa khớp từ vựng nào có video — ' +
            'kiểm tra lại chủ đề/cấp độ, hoặc gán chủ đề cho từ vựng trước.',
        );
      }
    } finally {
      previewLoading.value = false;
    }
  }
</script>
