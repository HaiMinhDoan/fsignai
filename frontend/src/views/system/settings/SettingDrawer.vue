<template>
  <BasicDrawer
    v-bind="$attrs"
    @register="registerDrawer"
    :title="isUpdate ? 'Sửa cấu hình' : 'Thêm cấu hình mới'"
    width="520px"
    showFooter
    @ok="handleSubmit"
  >
    <Form layout="vertical">
      <FormItem label="Khoá cấu hình">
        <Input v-model:value="keyDraft" :disabled="isUpdate" placeholder="ví dụ: feature.forum_enabled" />
      </FormItem>
      <FormItem label="Giá trị (JSON)">
        <Textarea v-model:value="valueDraft" :rows="6" placeholder='ví dụ: true, 10, "text", {"a":1}' />
      </FormItem>
      <FormItem label="Mô tả">
        <Textarea v-model:value="descriptionDraft" :rows="2" />
      </FormItem>
      <p v-if="error" class="text-red-500">{{ error }}</p>
    </Form>
  </BasicDrawer>
</template>

<script lang="ts" setup>
  import { ref } from 'vue';
  // Phải import: dự án không có unplugin-vue-components, chỉ Input/Button/Layout được đăng ký
  // toàn cục. Thiếu <a-form>/<a-form-item> thì nhãn "Khoá cấu hình"... rơi ra ngoài ô nhập.
  import { Form, Input } from 'ant-design-vue';
  import { BasicDrawer, useDrawerInner } from '@/components/Drawer';
  import { settingCreateApi, settingUpdateApi } from '@/api/system/settings';

  const FormItem = Form.Item;
  const Textarea = Input.TextArea;
  import type { SystemSettingModel } from '@/api/system/model/systemSettingModel';

  defineOptions({ name: 'SettingDrawer' });

  const emit = defineEmits(['success', 'register']);

  const isUpdate = ref(false);
  const currentId = ref<string>();
  const keyDraft = ref('');
  const valueDraft = ref('');
  const descriptionDraft = ref('');
  const error = ref('');

  const [registerDrawer, { setDrawerProps, closeDrawer }] = useDrawerInner((data) => {
    error.value = '';
    setDrawerProps({ confirmLoading: false });
    isUpdate.value = !!data?.isUpdate;

    if (isUpdate.value && data?.record) {
      const record = data.record as SystemSettingModel;
      currentId.value = record.id;
      keyDraft.value = record.key;
      valueDraft.value = JSON.stringify(record.value, null, 2);
      descriptionDraft.value = record.description ?? '';
    } else {
      currentId.value = undefined;
      keyDraft.value = '';
      valueDraft.value = '';
      descriptionDraft.value = '';
    }
  });

  async function handleSubmit() {
    error.value = '';
    let value: unknown;
    try {
      value = JSON.parse(valueDraft.value);
    } catch {
      error.value = 'Giá trị phải là JSON hợp lệ - ví dụ: true, 10, "chữ", hoặc {"a":1}';
      return;
    }
    if (!keyDraft.value.trim()) {
      error.value = 'Khoá cấu hình không được để trống';
      return;
    }

    try {
      setDrawerProps({ confirmLoading: true });
      const params = { key: keyDraft.value.trim(), value, description: descriptionDraft.value || undefined };
      if (isUpdate.value && currentId.value) {
        await settingUpdateApi(currentId.value, params);
      } else {
        await settingCreateApi(params);
      }
      closeDrawer();
      emit('success');
    } finally {
      setDrawerProps({ confirmLoading: false });
    }
  }
</script>
