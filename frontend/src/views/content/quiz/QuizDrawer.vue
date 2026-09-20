<template>
  <BasicDrawer
    v-bind="$attrs"
    @register="registerDrawer"
    :title="isUpdate ? `Sửa đề: ${currentQuiz?.titleVi}` : 'Thêm đề mới'"
    width="72%"
    showFooter
    @ok="handleSubmit"
  >
    <Tabs v-model:activeKey="activeTab">
      <TabPane key="info" tab="Thông tin">
        <BasicForm @register="registerForm" />
      </TabPane>

      <TabPane key="questions" tab="Câu hỏi" :disabled="!isUpdate">
        <div class="mb-4 flex items-center gap-3 flex-wrap">
          <Button type="primary" @click="openGenerateModal(true, { quizId: currentQuiz?.id })">
            Sinh câu hỏi tự động
          </Button>
          <span class="text-xs text-gray-500">
            Máy sinh câu từ kho từ vựng theo chủ đề. Bạn xem lại rồi sửa câu nào chưa ổn.
          </span>
        </div>

        <Alert
          v-if="!questions.length"
          type="info"
          show-icon
          message="Đề này chưa có câu hỏi"
          description="Đề chưa có câu hỏi sẽ không xuất bản được. Bấm Sinh câu hỏi tự động để bắt đầu."
        />

        <Table
          v-else
          :columns="questionColumns"
          :data-source="questions"
          :pagination="false"
          row-key="id"
          size="small"
        >
          <template #bodyCell="{ column, record, index }">
            <template v-if="column.key === 'order'">
              <Button
                size="small"
                :disabled="index === 0 || reordering"
                @click="moveQuestion(index, -1)"
              >
                ↑
              </Button>
              <Button
                size="small"
                class="ml-1"
                :disabled="index === questions.length - 1 || reordering"
                @click="moveQuestion(index, 1)"
              >
                ↓
              </Button>
            </template>

            <template v-if="column.key === 'questionType'">
              <Tag>{{ questionTypeLabel(record.questionType) }}</Tag>
            </template>

            <template v-if="column.key === 'preview'">
              <video
                v-if="record.signVideoUrl"
                :src="record.signVideoUrl"
                :poster="record.signThumbnailUrl"
                controls
                preload="metadata"
                style="width: 140px; border-radius: 4px; background: #eaf0f4"
              />
              <Tag v-else color="orange">Chưa có video</Tag>
            </template>

            <template v-if="column.key === 'options'">
              <div v-if="record.questionType === 'AI_PERFORM'" class="text-xs text-gray-500">
                Người học tự thực hiện trước webcam — không có lựa chọn
              </div>
              <div v-else class="flex flex-wrap gap-1">
                <!-- Đánh dấu đáp án đúng để người soạn soát được bằng mắt -->
                <Tag
                  v-for="(opt, i) in record.options"
                  :key="i"
                  :color="i === record.correctOptionIndex ? 'success' : 'default'"
                >
                  {{ opt.label }}
                </Tag>
              </div>
            </template>

            <template v-if="column.key === 'action'">
              <Popconfirm title="Xoá câu hỏi này?" @confirm="handleDeleteQuestion(record)">
                <Button size="small" danger>Xoá</Button>
              </Popconfirm>
            </template>
          </template>
        </Table>
      </TabPane>
    </Tabs>

    <GenerateQuestionsModal @register="registerGenerateModal" @success="refreshQuestions" />
  </BasicDrawer>
</template>

<script lang="ts" setup>
  import { ref } from 'vue';
  import { Alert, Button, Popconfirm, Table, Tabs, Tag } from 'ant-design-vue';
  import { BasicDrawer, useDrawerInner } from '@/components/Drawer';
  import { BasicForm, useForm } from '@/components/Form';
  import { useModal } from '@/components/Modal';
  import { useMessage } from '@/hooks/web/useMessage';

  import GenerateQuestionsModal from './GenerateQuestionsModal.vue';
  import { quizFormSchema, questionTypeLabel } from './quiz.data';
  import {
    quizCreateApi,
    quizUpdateApi,
    quizDetailApi,
    quizDeleteQuestionApi,
    quizReorderQuestionsApi,
  } from '@/api/practice/quiz';
  import type { QuizModel, QuizQuestionModel } from '@/api/practice/model/practiceModel';

  const TabPane = Tabs.TabPane;

  defineOptions({ name: 'QuizDrawer' });

  const emit = defineEmits(['success', 'register']);
  const { createMessage } = useMessage();

  const isUpdate = ref(false);
  const activeTab = ref('info');
  const currentQuiz = ref<QuizModel>();
  const questions = ref<QuizQuestionModel[]>([]);
  const reordering = ref(false);

  const [registerGenerateModal, { openModal: openGenerateModal }] = useModal();

  const questionColumns = [
    { title: 'Thứ tự', key: 'order', width: 110 },
    { title: 'Dạng câu', key: 'questionType', width: 160 },
    { title: 'Xem trước', key: 'preview', width: 160 },
    { title: 'Đáp án đúng', dataIndex: 'signWordVi', width: 150 },
    { title: 'Lựa chọn', key: 'options' },
    { title: '', key: 'action', width: 80 },
  ];

  const [registerForm, { resetFields, setFieldsValue, validate }] = useForm({
    labelWidth: 170,
    schemas: quizFormSchema,
    showActionButtonGroup: false,
    baseColProps: { span: 24 },
  });

  const [registerDrawer, { setDrawerProps, closeDrawer }] = useDrawerInner(async (data) => {
    resetFields();
    setDrawerProps({ confirmLoading: false });

    isUpdate.value = !!data?.isUpdate;
    activeTab.value = data?.activeTab ?? 'info';
    questions.value = [];
    currentQuiz.value = undefined;

    if (isUpdate.value && data?.record?.id) {
      // Luôn lấy bản đầy đủ từ server: dòng trên bảng không chứa câu hỏi
      const detail = await quizDetailApi(data.record.id);
      currentQuiz.value = detail;
      questions.value = detail.questions ?? [];
      setFieldsValue(detail);
    }
  });

  async function handleSubmit() {
    try {
      const values = await validate();
      setDrawerProps({ confirmLoading: true });

      if (isUpdate.value && currentQuiz.value) {
        await quizUpdateApi(currentQuiz.value.id, values as any);
      } else {
        await quizCreateApi(values as any);
      }

      closeDrawer();
      emit('success');
    } catch (error) {
      // Backend chặn xuất bản đề rỗng — hiện nguyên văn lý do
      const message = (error as Error)?.message;
      if (message) createMessage.error(message);
    } finally {
      setDrawerProps({ confirmLoading: false });
    }
  }

  async function handleDeleteQuestion(record: Recordable) {
    if (!currentQuiz.value) return;
    await quizDeleteQuestionApi(currentQuiz.value.id, record.id);
    await refreshQuestions();
    emit('success');
  }

  /**
   * Đổi chỗ hai câu kề nhau rồi gửi toàn bộ danh sách id theo thứ tự mới.
   * Backend từ chối nếu số lượng không khớp — khi đó tải lại thay vì ghi đè.
   */
  async function moveQuestion(index: number, delta: number) {
    if (!currentQuiz.value) return;
    const target = index + delta;
    if (target < 0 || target >= questions.value.length) return;

    const next = [...questions.value];
    [next[index], next[target]] = [next[target], next[index]];
    questions.value = next;

    reordering.value = true;
    try {
      await quizReorderQuestionsApi(currentQuiz.value.id, {
        orderedIds: next.map((q) => q.id),
      });
    } catch (error) {
      createMessage.error((error as Error).message || 'Không sắp xếp được, đang tải lại');
      await refreshQuestions();
    } finally {
      reordering.value = false;
    }
  }

  async function refreshQuestions() {
    if (!currentQuiz.value) return;
    const detail = await quizDetailApi(currentQuiz.value.id);
    currentQuiz.value = detail;
    questions.value = detail.questions ?? [];
    emit('success');
  }
</script>
