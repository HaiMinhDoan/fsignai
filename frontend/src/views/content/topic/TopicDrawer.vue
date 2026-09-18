<template>
  <BasicDrawer
    v-bind="$attrs"
    @register="registerDrawer"
    :title="isUpdate ? 'Sửa chủ đề' : 'Thêm chủ đề mới'"
    width="520px"
    showFooter
    @ok="handleSubmit"
  >
    <BasicForm @register="registerForm" />
  </BasicDrawer>
</template>

<script lang="ts" setup>
  import { ref } from 'vue';
  import { BasicDrawer, useDrawerInner } from '@/components/Drawer';
  import { BasicForm, useForm } from '@/components/Form';

  import { topicFormSchema } from './topic.data';
  import { topicCreateApi, topicUpdateApi } from '@/api/content/topic';
  import type { TopicModel } from '@/api/content/model/contentModel';

  defineOptions({ name: 'TopicDrawer' });

  const emit = defineEmits(['success', 'register']);

  const isUpdate = ref(false);
  const currentId = ref<string>();

  const [registerForm, { resetFields, setFieldsValue, validate }] = useForm({
    labelWidth: 130,
    schemas: topicFormSchema,
    showActionButtonGroup: false,
    baseColProps: { span: 24 },
  });

  const [registerDrawer, { setDrawerProps, closeDrawer }] = useDrawerInner(async (data) => {
    resetFields();
    setDrawerProps({ confirmLoading: false });

    isUpdate.value = !!data?.isUpdate;
    currentId.value = undefined;

    if (isUpdate.value && data?.record) {
      const record = data.record as TopicModel;
      currentId.value = record.id;
      setFieldsValue(record);
    }
  });

  async function handleSubmit() {
    try {
      const values = await validate();
      setDrawerProps({ confirmLoading: true });

      if (isUpdate.value && currentId.value) {
        await topicUpdateApi(currentId.value, values as any);
      } else {
        await topicCreateApi(values as any);
      }

      closeDrawer();
      emit('success');
    } finally {
      setDrawerProps({ confirmLoading: false });
    }
  }
</script>
