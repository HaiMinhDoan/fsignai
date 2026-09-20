<template>
  <BasicDrawer
    v-bind="$attrs"
    @register="registerDrawer"
    :title="isUpdate ? 'Sửa chuyên mục' : 'Thêm chuyên mục mới'"
    width="480px"
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

  import { categoryFormSchema } from './category.data';
  import { forumCategoryCreateApi, forumCategoryUpdateApi } from '@/api/forum';
  import type { ForumCategoryModel } from '@/api/forum/model/forumModel';

  defineOptions({ name: 'ForumCategoryDrawer' });

  const emit = defineEmits(['success', 'register']);

  const isUpdate = ref(false);
  const currentId = ref<string>();

  const [registerForm, { resetFields, setFieldsValue, validate }] = useForm({
    labelWidth: 150,
    schemas: categoryFormSchema,
    showActionButtonGroup: false,
    baseColProps: { span: 24 },
  });

  const [registerDrawer, { setDrawerProps, closeDrawer }] = useDrawerInner(async (data) => {
    resetFields();
    setDrawerProps({ confirmLoading: false });

    isUpdate.value = !!data?.isUpdate;
    currentId.value = undefined;

    if (isUpdate.value && data?.record) {
      const record = data.record as ForumCategoryModel;
      currentId.value = record.id;
      setFieldsValue(record);
    }
  });

  async function handleSubmit() {
    try {
      const values = await validate();
      setDrawerProps({ confirmLoading: true });

      if (isUpdate.value && currentId.value) {
        await forumCategoryUpdateApi(currentId.value, values as any);
      } else {
        await forumCategoryCreateApi(values as any);
      }

      closeDrawer();
      emit('success');
    } finally {
      setDrawerProps({ confirmLoading: false });
    }
  }
</script>
