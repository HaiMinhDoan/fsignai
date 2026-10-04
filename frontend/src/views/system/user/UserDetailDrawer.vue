<template>
  <BasicDrawer v-bind="$attrs" @register="registerDrawer" title="Chi tiết người dùng" width="640px">
    <template v-if="user">
      <div class="ud-head">
        <!-- Ảnh đại diện video: phát lặp tối đa 5 giây như ở web học -->
        <video
          v-if="user.avatarVideoUrl"
          class="ud-avatar"
          :src="user.avatarVideoUrl"
          :poster="user.avatarUrl"
          muted
          loop
          autoplay
          playsinline
          @timeupdate="giuTrong5Giay"
        ></video>
        <img v-else-if="user.avatarUrl" class="ud-avatar" :src="user.avatarUrl" alt="" />
        <span v-else class="ud-avatar ud-avatar--empty">{{ user.fullName.trim().split(/\s+/).pop()?.[0]?.toUpperCase() }}</span>
        <div class="ud-head-body">
          <strong>{{ user.fullName }}</strong>
          <p v-if="user.bio" class="ud-bio">{{ user.bio }}</p>
          <p v-else class="ud-bio ud-bio--empty">Chưa viết giới thiệu</p>
        </div>
      </div>

      <Descriptions :column="1" bordered size="small" class="mb-4">
        <DescriptionsItem label="Họ tên">{{ user.fullName }}</DescriptionsItem>
        <DescriptionsItem label="Email">
          {{ user.email }} <Tag v-if="!user.emailVerified" color="warning">Chưa xác minh email</Tag>
        </DescriptionsItem>
        <DescriptionsItem label="Loại tài khoản">
          {{ ACCOUNT_KIND_OPTIONS.find((o) => o.value === user!.accountKind)?.label }}
        </DescriptionsItem>
        <DescriptionsItem label="Gói dịch vụ">
          <Tag v-if="isPremium" color="gold">Premium đến {{ new Date(user.premiumUntil!).toLocaleDateString('vi-VN') }}</Tag>
          <template v-else-if="user.premiumUntil">
            Free <span class="text-gray-500">(Premium hết hạn {{ new Date(user.premiumUntil).toLocaleDateString('vi-VN') }})</span>
          </template>
          <template v-else>Free</template>
        </DescriptionsItem>
        <DescriptionsItem label="Đăng nhập gần nhất">
          {{ user.lastLoginAt ? new Date(user.lastLoginAt).toLocaleString('vi-VN') : '—' }}
        </DescriptionsItem>
        <DescriptionsItem label="Ngày tạo">
          {{ new Date(user.createdAt).toLocaleString('vi-VN') }}
        </DescriptionsItem>
      </Descriptions>

      <Divider>Lịch sử giao dịch</Divider>
      <Table
        :columns="orderColumns"
        :data-source="orders"
        :loading="ordersLoading"
        :pagination="ordersPagination"
        row-key="id"
        size="small"
        class="mb-2"
        :locale="{ emptyText: 'Người dùng này chưa có giao dịch nào' }"
        @change="onOrdersChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'createdAt'">{{ dateTime(record.createdAt) }}</template>
          <template v-else-if="column.key === 'amount'">{{ money(record.paidAmount ?? record.amount) }}</template>
          <template v-else-if="column.key === 'status'">
            <component :is="paymentStatusTag(record.status)" />
            <div v-if="record.paidAt" class="text-gray-500 text-xs">{{ dateTime(record.paidAt) }}</div>
          </template>
        </template>
      </Table>
      <p class="text-gray-500 text-xs mb-4">Tổng đã thanh toán: <b>{{ money(totalPaid) }}</b> (trên các đơn đang hiện)</p>

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
  import { computed, ref } from 'vue';
  // Dự án KHÔNG có unplugin-vue-components, và registerGlobComp.ts chỉ đăng ký Input, Button,
  // Layout. Mọi component Ant Design khác phải import ngay trong file dùng nó — thiếu import thì
  // Vue coi <a-descriptions>, <a-radio-group>... là thẻ HTML lạ: không báo lỗi biên dịch, chỉ đổ
  // hết nhãn và giá trị ra thành một khối chữ dính liền nhau.
  import { Button, Checkbox, Descriptions, Divider, Input, Radio, Space, Table, Tag } from 'ant-design-vue';
  import type { TablePaginationConfig } from 'ant-design-vue';
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
  import { paymentOrdersApi, type PaymentOrderModel } from '@/api/system/payment';
  import { dateTime, money, paymentStatusTag } from '../payment/payment.data';
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

  // ---------------------------------------------------------------- giao dịch của người này
  const ORDERS_PAGE_SIZE = 5;
  const orderColumns = [
    { title: 'Ngày tạo', key: 'createdAt', width: 150 },
    { title: 'Mã thanh toán', dataIndex: 'paymentCode', key: 'paymentCode' },
    { title: 'Số tiền', key: 'amount', align: 'right' as const, width: 110 },
    { title: 'Trạng thái', key: 'status', width: 150 },
  ];
  const orders = ref<PaymentOrderModel[]>([]);
  const ordersLoading = ref(false);
  const ordersPagination = ref<TablePaginationConfig>({ current: 1, pageSize: ORDERS_PAGE_SIZE, total: 0, size: 'small' });
  const totalPaid = computed(() =>
    orders.value.filter((o) => o.status === 'PAID').reduce((sum, o) => sum + (o.paidAmount ?? o.amount), 0),
  );
  const isPremium = computed(() => !!user.value?.premiumUntil && new Date(user.value.premiumUntil) > new Date());

  async function loadOrders(page = 1) {
    if (!user.value) return;
    ordersLoading.value = true;
    try {
      const res = await paymentOrdersApi({ userId: user.value.id, page: page - 1, size: ORDERS_PAGE_SIZE });
      orders.value = res.items;
      ordersPagination.value = { ...ordersPagination.value, current: page, total: res.total };
    } finally {
      ordersLoading.value = false;
    }
  }

  function onOrdersChange(p: TablePaginationConfig) {
    loadOrders(p.current ?? 1);
  }

  function giuTrong5Giay(e: Event) {
    const v = e.target as HTMLVideoElement;
    if (v.currentTime >= 5) v.currentTime = 0;
  }

  const [registerDrawer] = useDrawerInner(async (data) => {
    const id = data?.id as string;
    orders.value = [];
    if (!roleOptions.value.length) {
      roleOptions.value = await userRoleOptionsApi();
      roleOptionItems.value = roleOptions.value.map((r) => ({ label: r.nameVi, value: r.code }));
    }
    user.value = await userDetailApi(id);
    selectedRoles.value = [...user.value.roleCodes];
    statusDraft.value = user.value.status;
    banReasonDraft.value = user.value.banReason ?? '';
    loadOrders(1);
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

<style lang="less" scoped>
  .ud-head {
    display: flex;
    align-items: center;
    gap: 16px;
    margin-bottom: 16px;
  }

  .ud-avatar {
    flex-shrink: 0;
    width: 72px;
    height: 72px;
    border-radius: 50%;
    object-fit: cover;
    background: #855300;
  }

  .ud-avatar--empty {
    display: grid;
    place-items: center;
    color: #fff;
    font-size: 28px;
    font-weight: 700;
  }

  .ud-head-body {
    min-width: 0;

    strong {
      font-size: 16px;
    }
  }

  .ud-bio {
    margin: 4px 0 0;
    white-space: pre-line;
    overflow-wrap: anywhere;
  }

  .ud-bio--empty {
    color: @text-color-secondary;
    font-style: italic;
  }
</style>
