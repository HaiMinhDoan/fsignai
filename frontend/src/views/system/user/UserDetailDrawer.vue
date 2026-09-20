<template>
  <BasicDrawer v-bind="$attrs" @register="registerDrawer" title="Chi tiết người dùng" width="520px">
    <template v-if="user">
      <Descriptions :column="1" bordered size="small" class="mb-4">
        <DescriptionsItem label="Họ tên">{{ user.fullName }}</DescriptionsItem>
        <DescriptionsItem label="Email">
          {{ user.email }} <Tag v-if="!user.emailVerified" color="warning">Chưa xác minh email</Tag>
        </DescriptionsItem>
        <DescriptionsItem label="Loại tài khoản">
          {{ ACCOUNT_KIND_OPTIONS.find((o) => o.value === user!.accountKind)?.label }}
        </DescriptionsItem>
        <DescriptionsItem label="Đăng nhập gần nhất">
          {{ user.lastLoginAt ? new Date(user.lastLoginAt).toLocaleString('vi-VN') : '—' }}
        </DescriptionsItem>
        <DescriptionsItem label="Ngày tạo">
          {{ new Date(user.createdAt).toLocaleString('vi-VN') }}
        </DescriptionsItem>
      </Descriptions>

      <Divider>Vai trò hệ thống</Divider>
      <CheckboxGroup v-model:value="selectedRoles" :options="roleOptionItems" class="mb-3" />
      <div>
        <Button type="primary" size="small" :loading="savingRoles" @click="saveRoles">
          Lưu vai trò
        </Button>
      </div>

      <Divider>Hồ sơ chuyên môn VSL</Divider>
      <p>
        Tự khai: <strong>{{ user.vslRole }}</strong> —
        <Tag :color="VSL_STATUS_COLOR[user.vslRoleStatus]">{{ VSL_STATUS_LABEL[user.vslRoleStatus] }}</Tag>
      </p>
      <p v-if="user.vslRoleEvidence" class="text-gray-500">Minh chứng: {{ user.vslRoleEvidence }}</p>
      <p v-if="user.vslRoleVerifiedByName" class="text-gray-500">
        Duyệt bởi {{ user.vslRoleVerifiedByName }} lúc
        {{ new Date(user.vslRoleVerifiedAt!).toLocaleString('vi-VN') }}
      </p>
      <Space v-if="user.vslRoleStatus === 'PENDING'">
        <Button type="primary" :loading="decidingVsl" @click="decideVsl('VERIFIED')">Duyệt</Button>
        <Button danger :loading="decidingVsl" @click="decideVsl('REJECTED')">Từ chối</Button>
      </Space>

      <Divider>Trạng thái tài khoản</Divider>
      <RadioGroup v-model:value="statusDraft" class="mb-3">
        <Radio v-for="o in STATUS_OPTIONS" :key="o.value" :value="o.value">{{ o.label }}</Radio>
      </RadioGroup>
      <Input
        v-if="statusDraft === 'BANNED'"
        v-model:value="banReasonDraft"
        placeholder="Lý do khoá tài khoản"
        class="mb-3"
      />
      <div>
        <Button type="primary" size="small" :loading="savingStatus" @click="saveStatus">
          Lưu trạng thái
        </Button>
      </div>
    </template>
  </BasicDrawer>
</template>

<script lang="ts" setup>
  import { ref } from 'vue';
  // Dự án KHÔNG có unplugin-vue-components, và registerGlobComp.ts chỉ đăng ký Input, Button,
  // Layout. Mọi component Ant Design khác phải import ngay trong file dùng nó — thiếu import thì
  // Vue coi <a-descriptions>, <a-radio-group>... là thẻ HTML lạ: không báo lỗi biên dịch, chỉ đổ
  // hết nhãn và giá trị ra thành một khối chữ dính liền nhau.
  import { Button, Checkbox, Descriptions, Divider, Input, Radio, Space, Tag } from 'ant-design-vue';
  import { BasicDrawer, useDrawerInner } from '@/components/Drawer';

  const DescriptionsItem = Descriptions.Item;
  const CheckboxGroup = Checkbox.Group;
  const RadioGroup = Radio.Group;

  import { useMessage } from '@/hooks/web/useMessage';
  import {
    userDetailApi,
    userRoleOptionsApi,
    userSetRolesApi,
    userSetStatusApi,
    userDecideVslRoleApi,
  } from '@/api/system/user';
  import { ACCOUNT_KIND_OPTIONS, STATUS_OPTIONS } from './user.data';
  import type { UserAdminModel, RoleOption } from '@/api/system/model/systemModel';

  defineOptions({ name: 'UserDetailDrawer' });

  const emit = defineEmits(['success', 'register']);
  const { createMessage } = useMessage();

  const VSL_STATUS_COLOR: Record<string, string> = {
    SELF_DECLARED: 'default',
    PENDING: 'orange',
    VERIFIED: 'success',
    REJECTED: 'error',
  };
  const VSL_STATUS_LABEL: Record<string, string> = {
    SELF_DECLARED: 'Tự khai',
    PENDING: 'Chờ duyệt',
    VERIFIED: 'Đã xác minh',
    REJECTED: 'Bị từ chối',
  };

  const user = ref<UserAdminModel>();
  const roleOptions = ref<RoleOption[]>([]);
  const roleOptionItems = ref<{ label: string; value: string }[]>([]);
  const selectedRoles = ref<string[]>([]);
  const statusDraft = ref('ACTIVE');
  const banReasonDraft = ref('');
  const savingRoles = ref(false);
  const savingStatus = ref(false);
  const decidingVsl = ref(false);

  const [registerDrawer] = useDrawerInner(async (data) => {
    const id = data?.id as string;
    if (!roleOptions.value.length) {
      roleOptions.value = await userRoleOptionsApi();
      roleOptionItems.value = roleOptions.value.map((r) => ({ label: r.nameVi, value: r.code }));
    }
    user.value = await userDetailApi(id);
    selectedRoles.value = [...user.value.roleCodes];
    statusDraft.value = user.value.status;
    banReasonDraft.value = user.value.banReason ?? '';
  });

  async function saveRoles() {
    if (!user.value) return;
    savingRoles.value = true;
    try {
      user.value = await userSetRolesApi(user.value.id, selectedRoles.value);
      emit('success');
    } finally {
      savingRoles.value = false;
    }
  }

  async function saveStatus() {
    if (!user.value) return;
    savingStatus.value = true;
    try {
      user.value = await userSetStatusApi(
        user.value.id,
        statusDraft.value,
        statusDraft.value === 'BANNED' ? banReasonDraft.value : undefined,
      );
      emit('success');
    } finally {
      savingStatus.value = false;
    }
  }

  async function decideVsl(decision: 'VERIFIED' | 'REJECTED') {
    if (!user.value) return;
    decidingVsl.value = true;
    try {
      user.value = await userDecideVslRoleApi(user.value.id, decision);
      createMessage.success(decision === 'VERIFIED' ? 'Đã duyệt hồ sơ VSL' : 'Đã từ chối hồ sơ VSL');
      emit('success');
    } finally {
      decidingVsl.value = false;
    }
  }
</script>
