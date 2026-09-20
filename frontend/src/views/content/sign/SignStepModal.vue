<template>
  <BasicModal
    v-bind="$attrs"
    @register="registerModal"
    :title="isUpdate ? `Sửa bước ${step?.stepOrder}` : 'Thêm bước mới'"
    width="640px"
    @ok="handleSubmit"
  >
    <div class="flex flex-col gap-4">
      <div class="flex gap-4">
        <div style="width: 160px">
          <div class="mb-1 text-xs text-gray-500">Vị trí (thứ tự)</div>
          <InputNumber v-model:value="stepOrder" :min="1" :max="maxPosition" style="width: 100%" />
        </div>
        <div class="flex-1">
          <div class="mb-1 text-xs text-gray-500">Tiêu đề bước (không bắt buộc)</div>
          <Input v-model:value="titleVi" placeholder="Ví dụ: Khép bốn ngón tay" />
        </div>
      </div>

      <div>
        <div class="mb-1 text-xs text-gray-500">
          Mô tả bước <span class="text-red-500">*</span>
        </div>
        <Input.TextArea
          v-model:value="descriptionVi"
          :rows="4"
          placeholder="Mô tả bằng chữ cách thực hiện bước này — bắt buộc, vì ảnh không được là kênh thông tin duy nhất"
        />
      </div>

      <div class="flex gap-4">
        <div class="flex-1">
          <div class="mb-1 text-xs text-gray-500">Bộ phận cần chú ý</div>
          <Select
            v-model:value="bodyFocus"
            style="width: 100%"
            allow-clear
            placeholder="Không chỉ định"
            :options="BODY_FOCUS_OPTIONS"
          />
        </div>
        <div style="width: 200px">
          <div class="mb-1 text-xs text-gray-500">Giữ nguyên trong (giây)</div>
          <InputNumber
            v-model:value="holdSeconds"
            :min="0.1"
            :step="0.5"
            style="width: 100%"
            placeholder="Không bắt buộc"
          />
        </div>
      </div>
    </div>
  </BasicModal>
</template>

<script lang="ts" setup>
  import { ref } from 'vue';
  import { Input, InputNumber, Select } from 'ant-design-vue';
  import { BasicModal, useModalInner } from '@/components/Modal';
  import { useMessage } from '@/hooks/web/useMessage';

  import { signStepCreateApi, signStepUpdateApi } from '@/api/content/sign';
  import type { BodyFocus, SignStepModel } from '@/api/content/model/contentModel';

  defineOptions({ name: 'SignStepModal' });

  const emit = defineEmits(['success', 'register']);
  const { createMessage } = useMessage();

  const BODY_FOCUS_OPTIONS: { label: string; value: BodyFocus }[] = [
    { label: 'Tay trái', value: 'LEFT_HAND' },
    { label: 'Tay phải', value: 'RIGHT_HAND' },
    { label: 'Cả hai tay', value: 'BOTH_HANDS' },
    { label: 'Nét mặt', value: 'FACE' },
    { label: 'Khẩu hình miệng', value: 'MOUTH' },
    { label: 'Vai', value: 'SHOULDER' },
    { label: 'Trước ngực', value: 'CHEST' },
  ];

  const signId = ref<string>();
  const step = ref<SignStepModel>();
  const isUpdate = ref(false);
  /** Chèn ở cuối tối đa là (số bước hiện có + 1) — quá số đó vô nghĩa */
  const maxPosition = ref(1);

  const stepOrder = ref(1);
  const titleVi = ref('');
  const descriptionVi = ref('');
  const bodyFocus = ref<BodyFocus>();
  const holdSeconds = ref<number>();

  const [registerModal, { setModalProps, closeModal }] = useModalInner((data) => {
    setModalProps({ confirmLoading: false });
    signId.value = data?.signId;
    step.value = data?.step;
    isUpdate.value = !!data?.step;
    maxPosition.value = data?.maxPosition ?? 1;

    stepOrder.value = data?.step?.stepOrder ?? maxPosition.value;
    titleVi.value = data?.step?.titleVi ?? '';
    descriptionVi.value = data?.step?.descriptionVi ?? '';
    bodyFocus.value = data?.step?.bodyFocus;
    holdSeconds.value = data?.step?.holdSeconds;
  });

  async function handleSubmit() {
    if (!signId.value) return;
    if (!descriptionVi.value.trim()) {
      createMessage.error('Cần nhập mô tả cho bước này');
      return;
    }

    setModalProps({ confirmLoading: true });
    try {
      const params = {
        stepOrder: stepOrder.value,
        titleVi: titleVi.value || undefined,
        descriptionVi: descriptionVi.value.trim(),
        bodyFocus: bodyFocus.value,
        holdSeconds: holdSeconds.value,
      };

      if (isUpdate.value && step.value) {
        await signStepUpdateApi(signId.value, step.value.id, params);
      } else {
        await signStepCreateApi(signId.value, params);
      }

      closeModal();
      emit('success');
    } finally {
      setModalProps({ confirmLoading: false });
    }
  }
</script>
