<template>
  <BasicDrawer
    v-bind="$attrs"
    @register="registerDrawer"
    :title="isUpdate ? 'Sửa bài blog' : 'Viết bài blog mới'"
    width="640px"
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

  import { blogFormSchema } from './blog.data';
  import { blogCreateApi, blogUpdateApi } from '@/api/notification/blog';
  import type { BlogPostModel } from '@/api/notification/model/blogModel';

  defineOptions({ name: 'BlogDrawer' });

  const emit = defineEmits(['success', 'register']);

  const isUpdate = ref(false);
  const currentId = ref<string>();

  const [registerForm, { resetFields, setFieldsValue, validate }] = useForm({
    labelWidth: 130,
    schemas: blogFormSchema,
    showActionButtonGroup: false,
    baseColProps: { span: 24 },
  });

  const [registerDrawer, { setDrawerProps, closeDrawer }] = useDrawerInner(async (data) => {
    resetFields();
    setDrawerProps({ confirmLoading: false });

    isUpdate.value = !!data?.isUpdate;
    currentId.value = undefined;

    if (isUpdate.value && data?.record) {
      const record = data.record as BlogPostModel;
      currentId.value = record.id;
      setFieldsValue(record);
    }
  });

  async function handleSubmit() {
    try {
      const values = await validate();
      setDrawerProps({ confirmLoading: true });

      if (isUpdate.value && currentId.value) {
        await blogUpdateApi(currentId.value, values as any);
      } else {
        await blogCreateApi(values as any);
      }

      closeDrawer();
      emit('success');
    } finally {
      setDrawerProps({ confirmLoading: false });
    }
  }
</script>
