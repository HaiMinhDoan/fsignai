<template>
  <BasicModal
    v-bind="$attrs"
    @register="registerModal"
    title="Gán chủ đề cho từ vựng đã chọn"
    width="640px"
    :okText="`Gán cho ${signIds.length} từ`"
    :okButtonProps="{ disabled: !topicIds.length }"
    @ok="handleSubmit"
  >
    <Alert
      class="mb-4"
      type="info"
      show-icon
      message="Vì sao cần gán chủ đề"
      description="Từ điển Bộ GD&ĐT không kèm chủ đề. Chưa gán thì không sinh được khoá học, không trộn được đề theo chủ đề, và người học cũng không duyệt từ vựng theo chủ đề được."
    />

    <div class="mb-4">
      <div class="mb-1 text-sm">Chủ đề</div>
      <Select
        v-model:value="topicIds"
        mode="multiple"
        style="width: 100%"
        placeholder="Chọn một hoặc nhiều chủ đề"
        :options="topicOptions"
      />
    </div>

    <div class="mb-3">
      <RadioGroup v-model:value="mode">
        <Radio value="add">Thêm vào chủ đề đã có</Radio>
        <Radio value="replace">Thay thế toàn bộ chủ đề cũ</Radio>
      </RadioGroup>
      <div class="mt-1 text-xs text-gray-500">
        Mặc định là thêm. Thay thế sẽ xoá hết chủ đề cũ của các từ này và không hoàn tác được.
      </div>
    </div>

    <!--
      Chủ đề chính chỉ đặt được khi chọn đúng một chủ đề: có nhiều chủ đề thì
      không có căn cứ nào để máy tự chọn cái nào là chính.
    -->
    <Checkbox v-model:checked="setPrimary" :disabled="topicIds.length !== 1">
      Đặt luôn làm chủ đề chính
    </Checkbox>
    <div v-if="topicIds.length !== 1" class="mt-1 text-xs text-gray-500">
      Chỉ dùng được khi chọn đúng một chủ đề.
    </div>
  </BasicModal>
</template>

<script lang="ts" setup>
  import { ref, watch } from 'vue';
  import { Alert, Checkbox, Radio, Select } from 'ant-design-vue';
  import { BasicModal, useModalInner } from '@/components/Modal';
  import { useMessage } from '@/hooks/web/useMessage';

  import { topicOptionsApi } from '@/api/content/topic';
  import { signAssignTopicsApi } from '@/api/content/sign';

  const RadioGroup = Radio.Group;

  defineOptions({ name: 'AssignTopicsModal' });

  const emit = defineEmits(['success', 'register']);
  const { createMessage } = useMessage();

  const signIds = ref<string[]>([]);
  const topicIds = ref<string[]>([]);
  const mode = ref<'add' | 'replace'>('add');
  const setPrimary = ref(false);
  const topicOptions = ref<{ label: string; value: string }[]>([]);

  // Bỏ tích khi người dùng chọn thêm chủ đề thứ hai, để trạng thái trên màn hình
  // không mâu thuẫn với thứ thực sự được gửi lên
  watch(topicIds, (value) => {
    if (value.length !== 1) setPrimary.value = false;
  });

  const [registerModal, { setModalProps, closeModal }] = useModalInner(async (data) => {
    setModalProps({ confirmLoading: false });
    signIds.value = data?.signIds ?? [];
    topicIds.value = [];
    mode.value = 'add';
    setPrimary.value = false;

    if (!topicOptions.value.length) {
      const topics = await topicOptionsApi();
      topicOptions.value = topics.map((t) => ({ label: t.nameVi, value: t.id }));
    }
  });

  async function handleSubmit() {
    if (!topicIds.value.length || !signIds.value.length) return;
    setModalProps({ confirmLoading: true });
    try {
      const result = await signAssignTopicsApi({
        signIds: signIds.value,
        topicIds: topicIds.value,
        replace: mode.value === 'replace',
        setPrimary: setPrimary.value,
      });
      createMessage.success(`Đã gán chủ đề cho ${result.affected} từ`);
      closeModal();
      emit('success');
    } finally {
      setModalProps({ confirmLoading: false });
    }
  }
</script>
