<template>
  <BasicModal
    v-bind="$attrs"
    @register="registerModal"
    title="Xử lý báo cáo"
    width="480px"
    @ok="handleSubmit"
  >
    <div class="mb-3 text-gray-600">
      Nội dung bị báo cáo: <strong>{{ preview || '(không có xem trước)' }}</strong>
    </div>
    <Radio.Group v-model:value="status" class="mb-3">
      <Radio value="RESOLVED">Đã xử lý (gỡ/ẩn nội dung hoặc xác nhận vi phạm)</Radio>
      <Radio value="DISMISSED">Bỏ qua (không vi phạm)</Radio>
    </Radio.Group>
    <Input.TextArea v-model:value="handlerNote" :rows="3" placeholder="Ghi chú xử lý (không bắt buộc)" />
  </BasicModal>
</template>

<script lang="ts" setup>
  import { ref } from 'vue';
  import { Radio, Input } from 'ant-design-vue';
  import { BasicModal, useModalInner } from '@/components/Modal';
  import { forumReportHandleApi } from '@/api/forum';
  import type { ForumReportModel } from '@/api/forum/model/forumModel';

  defineOptions({ name: 'ResolveReportModal' });

  const emit = defineEmits(['success', 'register']);

  const reportId = ref('');
  const preview = ref('');
  const status = ref<'RESOLVED' | 'DISMISSED'>('RESOLVED');
  const handlerNote = ref('');

  const [registerModal, { setModalProps, closeModal }] = useModalInner((data) => {
    const record = data?.record as ForumReportModel;
    reportId.value = record.id;
    preview.value = record.targetPreview ?? '';
    status.value = 'RESOLVED';
    handlerNote.value = '';
    setModalProps({ confirmLoading: false });
  });

  async function handleSubmit() {
    try {
      setModalProps({ confirmLoading: true });
      await forumReportHandleApi(reportId.value, status.value, handlerNote.value || undefined);
      closeModal();
      emit('success');
    } finally {
      setModalProps({ confirmLoading: false });
    }
  }
</script>
