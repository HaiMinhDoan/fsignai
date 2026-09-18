<template>
  <template v-if="getShow">
    <LoginFormTitle class="enter-x" />
    <Form class="p-4 enter-x" :model="formData" :rules="getFormRules" ref="formRef">
      <template v-if="!sent">
        <div class="mb-4 text-sm text-gray-500 enter-x">
          Nhập email đã đăng ký. Chúng tôi sẽ gửi liên kết đặt lại mật khẩu.
        </div>

        <FormItem name="account" class="enter-x">
          <Input size="large" v-model:value="formData.account" placeholder="Email" />
        </FormItem>

        <FormItem class="enter-x">
          <Button type="primary" size="large" block @click="handleSubmit" :loading="loading">
            Gửi liên kết đặt lại
          </Button>
          <Button size="large" block class="mt-4" @click="handleBackLogin">
            {{ t('sys.login.backSignIn') }}
          </Button>
        </FormItem>
      </template>

      <!--
        Thông báo cố tình KHÔNG nói email có tồn tại hay không.
        Nói "email này chưa đăng ký" là lộ cho người khác biết tài khoản nào có thật.
      -->
      <template v-else>
        <Result
          status="success"
          title="Đã gửi yêu cầu"
          sub-title="Nếu email này đã đăng ký, bạn sẽ nhận được liên kết đặt lại mật khẩu trong vài phút. Liên kết có hiệu lực 30 phút."
        >
          <template #extra>
            <Button type="primary" @click="handleBackLogin">Quay lại đăng nhập</Button>
          </template>
        </Result>
      </template>
    </Form>
  </template>
</template>

<script lang="ts" setup>
  import { reactive, ref, computed, unref } from 'vue';
  import LoginFormTitle from './LoginFormTitle.vue';
  import { Form, Input, Button, Result } from 'ant-design-vue';
  import { useI18n } from '@/hooks/web/useI18n';
  import { useMessage } from '@/hooks/web/useMessage';
  import { useLoginState, useFormRules, useFormValid, LoginStateEnum } from './useLogin';
  import { forgotPasswordApi } from '@/api/auth';

  const FormItem = Form.Item;
  const { t } = useI18n();
  const { createErrorModal } = useMessage();
  const { handleBackLogin, getLoginState } = useLoginState();
  const { getFormRules } = useFormRules();

  const formRef = ref();
  const loading = ref(false);
  const sent = ref(false);

  const formData = reactive({
    account: '',
  });

  const { validForm } = useFormValid(formRef);
  const getShow = computed(() => unref(getLoginState) === LoginStateEnum.RESET_PASSWORD);

  async function handleSubmit() {
    const data = await validForm();
    if (!data) return;

    try {
      loading.value = true;
      await forgotPasswordApi({ email: formData.account });
      // Backend luôn trả thành công dù email có tồn tại hay không
      sent.value = true;
    } catch (error) {
      createErrorModal({
        title: t('sys.api.errorTip'),
        content: (error as unknown as Error).message || t('sys.api.networkExceptionMsg'),
      });
    } finally {
      loading.value = false;
    }
  }
</script>
