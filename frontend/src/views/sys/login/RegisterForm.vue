<template>
  <div v-if="getShow">
    <LoginFormTitle class="enter-x" />
    <Form class="p-4 enter-x" :model="formData" :rules="getFormRules" ref="formRef">
      <FormItem name="email" class="enter-x">
        <Input
          class="fix-auto-fill"
          size="large"
          v-model:value="formData.email"
          placeholder="Email"
        />
      </FormItem>

      <FormItem name="fullName" class="enter-x">
        <Input
          class="fix-auto-fill"
          size="large"
          v-model:value="formData.fullName"
          placeholder="Họ và tên"
        />
      </FormItem>

      <FormItem name="password" class="enter-x">
        <StrengthMeter size="large" v-model:value="formData.password" placeholder="Mật khẩu" />
      </FormItem>

      <FormItem name="confirmPassword" class="enter-x">
        <InputPassword
          size="large"
          visibilityToggle
          v-model:value="formData.confirmPassword"
          placeholder="Nhập lại mật khẩu"
        />
      </FormItem>

      <!--
        Ba câu hỏi dưới đây quyết định trải nghiệm học, không phải thủ tục:
        userType định hình nội dung gợi ý, region quyết định video ký hiệu nào
        được phục vụ (học ở Hà Nội mà được dạy ký hiệu miền Nam thì không
        giao tiếp được với người điếc quanh mình).
      -->
      <FormItem name="userType" class="enter-x">
        <Select
          size="large"
          v-model:value="formData.userType"
          placeholder="Bạn là ai?"
          :options="USER_TYPE_OPTIONS"
        />
      </FormItem>

      <FormItem name="region" class="enter-x">
        <Select
          size="large"
          v-model:value="formData.region"
          placeholder="Bạn ở miền nào?"
          :options="REGION_OPTIONS"
        />
      </FormItem>

      <FormItem name="vslRole" class="enter-x">
        <Select
          size="large"
          v-model:value="formData.vslRole"
          placeholder="Vai trò trong cộng đồng ngôn ngữ ký hiệu"
          :options="VSL_ROLE_OPTIONS"
        />
      </FormItem>

      <!-- Chỉ hiện khi khai vai trò chuyên môn, để form đăng ký không dài vô ích -->
      <FormItem v-if="needsEvidence" name="vslRoleEvidence" class="enter-x">
        <Input
          class="fix-auto-fill"
          size="large"
          v-model:value="formData.vslRoleEvidence"
          placeholder="Nơi công tác hoặc số chứng chỉ"
        />
        <div class="mt-1 text-xs text-gray-500">
          Chúng tôi sẽ xác minh trước khi kích hoạt quyền góp ý chuyên môn.
        </div>
      </FormItem>

      <FormItem class="enter-x" name="policy">
        <Checkbox v-model:checked="formData.policy" size="small">
          Tôi đồng ý với điều khoản sử dụng
        </Checkbox>
      </FormItem>

      <Button
        type="primary"
        class="enter-x"
        size="large"
        block
        @click="handleRegister"
        :loading="loading"
      >
        Đăng ký
      </Button>

      <Button size="large" block class="mt-4 enter-x" @click="handleBackLogin">
        Quay lại đăng nhập
      </Button>
    </Form>
  </div>
</template>

<script lang="ts" setup>
  import { reactive, ref, computed, unref } from 'vue';
  import LoginFormTitle from './LoginFormTitle.vue';
  import { Form, Input, Button, Select, Checkbox } from 'ant-design-vue';
  import { StrengthMeter } from '@/components/StrengthMeter';
  import { useI18n } from '@/hooks/web/useI18n';
  import { useMessage } from '@/hooks/web/useMessage';
  import { LoginStateEnum, useLoginState, useFormRules, useFormValid } from './useLogin';
  import { useUserStore } from '@/store/modules/user';
  import { registerApi } from '@/api/auth';
  import type { Region, UserType, VslRole } from '@/api/auth/model/authModel';

  const FormItem = Form.Item;
  const InputPassword = Input.Password;
  const { t } = useI18n();
  const { notification, createErrorModal } = useMessage();
  const { handleBackLogin, getLoginState } = useLoginState();
  const { getFormRules } = useFormRules();
  const userStore = useUserStore();

  const formRef = ref();
  const loading = ref(false);

  const USER_TYPE_OPTIONS = [
    { label: 'Người khiếm thính', value: 'DEAF_HOH' },
    { label: 'Người thân trong gia đình', value: 'FAMILY' },
    { label: 'Bạn bè', value: 'FRIEND' },
    { label: 'Người chăm sóc', value: 'CAREGIVER' },
    { label: 'Khác', value: 'OTHER' },
  ];

  const REGION_OPTIONS = [
    { label: 'Miền Bắc', value: 'NORTH' },
    { label: 'Miền Trung', value: 'CENTRAL' },
    { label: 'Miền Nam', value: 'SOUTH' },
  ];

  /**
   * DEAF_NATIVE đứng ngay sau LEARNER, không xếp dưới TEACHER.
   * Về tính đúng đắn của một ký hiệu, người điếc dùng VSL hằng ngày là người
   * bản ngữ — đáng tin ngang, thường hơn cả giáo viên nghe được.
   */
  const VSL_ROLE_OPTIONS = [
    { label: 'Người học', value: 'LEARNER' },
    { label: 'Người điếc sử dụng VSL hằng ngày', value: 'DEAF_NATIVE' },
    { label: 'Giáo viên VSL / dạy trẻ điếc', value: 'TEACHER' },
    { label: 'Phiên dịch viên ngôn ngữ ký hiệu', value: 'INTERPRETER' },
  ];

  const formData = reactive({
    email: '',
    fullName: '',
    password: '',
    confirmPassword: '',
    userType: 'DEAF_HOH' as UserType,
    region: 'NORTH' as Region,
    vslRole: 'LEARNER' as VslRole,
    vslRoleEvidence: '',
    policy: false,
  });

  const needsEvidence = computed(() => formData.vslRole !== 'LEARNER');

  const { validForm } = useFormValid(formRef);
  const getShow = computed(() => unref(getLoginState) === LoginStateEnum.REGISTER);

  async function handleRegister() {
    const data = await validForm();
    if (!data) return;

    if (!formData.policy) {
      createErrorModal({
        title: 'Chưa đồng ý điều khoản',
        content: 'Bạn cần đồng ý với điều khoản sử dụng trước khi đăng ký.',
      });
      return;
    }

    try {
      loading.value = true;
      const result = await registerApi(
        {
          email: formData.email,
          password: formData.password,
          fullName: formData.fullName,
          userType: formData.userType,
          region: formData.region,
          vslRole: formData.vslRole,
          vslRoleEvidence: needsEvidence.value ? formData.vslRoleEvidence : undefined,
        },
        'none',
      );

      // Backend trả token luôn sau khi đăng ký, nên đăng nhập thẳng
      // thay vì bắt người dùng nhập lại email/mật khẩu vừa gõ xong.
      userStore.setToken(result.token);
      userStore.setRefreshToken(result.refreshToken);
      await userStore.afterLoginAction(true);

      notification.success({
        message: 'Đăng ký thành công',
        description: `Chào mừng ${result.user.realName}!`,
        duration: 3,
      });
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
