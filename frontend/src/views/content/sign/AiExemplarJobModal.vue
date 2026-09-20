<template>
  <BasicModal
    v-bind="$attrs"
    @register="registerModal"
    title="Sinh mẫu chấm điểm AI hàng loạt"
    width="640px"
    :showOkBtn="false"
    cancelText="Đóng"
    @visible-change="handleVisibleChange"
  >
    <div class="flex flex-col gap-4">
      <Alert
        type="info"
        show-icon
        message="Mẫu được sinh tự động từ video đã có"
        description="Hệ thống chạy MediaPipe trên từng video từ điển để tạo mẫu chuẩn — không cần quay thêm clip. Mỗi video mất vài giây, nên chạy nền theo lô; bạn có thể đóng cửa sổ này và quay lại xem tiến độ sau."
      />

      <div v-if="status" class="grid grid-cols-2 gap-3">
        <Statistic title="Video đã có mẫu" :value="status.readyExemplars" :suffix="`/ ${status.totalVideos}`" />
        <Statistic title="Từ chấm điểm được" :value="status.signsReady" :suffix="`/ ${status.totalSigns}`" />
      </div>
      <Progress
        v-if="status"
        :percent="coverage"
        :status="status.running ? 'active' : 'normal'"
        :format="(p?: number) => `${p ?? 0}% video`"
      />
      <p v-if="status && status.failedExemplars > 0" class="m-0 text-sm text-orange-600">
        {{ status.failedExemplars }} video dựng mẫu lỗi (thường do người hoặc bàn tay bị che/ngoài khung). Xem lý do trong tab
        "Chấm điểm AI" của từng từ.
      </p>

      <div class="rounded border border-gray-200 p-3">
        <div class="mb-2 font-medium">Lượt chạy</div>
        <template v-if="status?.running">
          <Progress :percent="jobPercent" status="active" />
          <p class="m-0 mt-1 text-sm text-gray-500">
            Đã xử lý {{ status.done }} / {{ status.total }} video ({{ status.failed }} lỗi)
          </p>
        </template>
        <template v-else>
          <div class="flex items-center gap-3">
            <span class="text-sm text-gray-500">Số video mỗi lượt</span>
            <InputNumber v-model:value="limit" :min="1" :max="6000" style="width: 120px" />
            <Checkbox v-model:checked="retryFailed">Thử lại cả video từng lỗi</Checkbox>
          </div>
          <div class="mt-3 flex items-center gap-3">
            <Button type="primary" :loading="starting" :disabled="remaining === 0" @click="handleStart">
              Bắt đầu sinh mẫu
            </Button>
            <span class="text-sm text-gray-500">
              {{ remaining === 0 ? 'Mọi video đều đã có mẫu.' : `Còn ${remaining} video chưa có mẫu, ước tính ~${estimateMinutes(Math.min(limit, remaining))} phút.` }}
            </span>
          </div>
          <p v-if="status?.lastMessage" class="m-0 mt-2 text-sm text-gray-500">
            Lượt gần nhất: {{ status.lastMessage }}
          </p>
        </template>
      </div>
    </div>
  </BasicModal>
</template>

<script lang="ts" setup>
  import { computed, onBeforeUnmount, ref } from 'vue';
  import { Alert, Button, Checkbox, InputNumber, Progress, Statistic } from 'ant-design-vue';
  import { BasicModal, useModalInner } from '@/components/Modal';
  import { useMessage } from '@/hooks/web/useMessage';
  import { exemplarJobStartApi, exemplarJobStatusApi } from '@/api/content/ai';
  import type { ExemplarJobStatus } from '@/api/content/model/contentModel';

  defineOptions({ name: 'AiExemplarJobModal' });

  const emit = defineEmits(['success', 'register']);
  const { createMessage } = useMessage();

  const status = ref<ExemplarJobStatus>();
  const limit = ref(200);
  const retryFailed = ref(false);
  const starting = ref(false);
  let poller: ReturnType<typeof setInterval> | undefined;

  /** Mỗi video ~2-3 giây trên CPU thường (MediaPipe + chuẩn hoá + tải lên MinIO) */
  const SECONDS_PER_VIDEO = 3;

  const [registerModal] = useModalInner(async () => {
    await refresh();
  });

  const coverage = computed(() =>
    status.value && status.value.totalVideos > 0
      ? Math.floor((status.value.readyExemplars / status.value.totalVideos) * 100)
      : 0,
  );
  const jobPercent = computed(() =>
    status.value && status.value.total > 0 ? Math.floor((status.value.done / status.value.total) * 100) : 0,
  );
  /** Video từng lỗi chỉ được thử lại khi tick "Thử lại", nên không tính vào số còn lại nếu không tick */
  const remaining = computed(() =>
    status.value
      ? Math.max(
          0,
          status.value.totalVideos -
            status.value.readyExemplars -
            (retryFailed.value ? 0 : status.value.failedExemplars),
        )
      : 0,
  );
  const estimateMinutes = (videos: number) => Math.max(1, Math.round((videos * SECONDS_PER_VIDEO) / 60));

  async function refresh() {
    const wasRunning = status.value?.running;
    status.value = await exemplarJobStatusApi();
    // Job vừa chạy xong → báo danh sách bên ngoài làm mới cột "exemplar"
    if (wasRunning && !status.value.running) emit('success');
    // Đang chạy thì tự cập nhật; xong thì dừng để khỏi gọi API thừa
    if (status.value.running) startPolling();
    else stopPolling();
  }

  function startPolling() {
    if (poller) return;
    poller = setInterval(() => void refresh(), 3000);
  }

  function stopPolling() {
    if (poller) clearInterval(poller);
    poller = undefined;
  }

  function handleVisibleChange(visible: boolean) {
    if (!visible) stopPolling();
  }

  async function handleStart() {
    starting.value = true;
    try {
      status.value = await exemplarJobStartApi(limit.value, retryFailed.value);
      if (status.value.running) {
        createMessage.success(`Đã bắt đầu sinh mẫu cho ${status.value.total} video`);
        startPolling();
      } else {
        createMessage.info(status.value.lastMessage ?? 'Không có video nào cần sinh mẫu');
      }
    } finally {
      starting.value = false;
    }
  }

  onBeforeUnmount(stopPolling);
</script>
