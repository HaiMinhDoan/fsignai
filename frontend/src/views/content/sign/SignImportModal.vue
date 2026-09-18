<template>
  <BasicModal
    v-bind="$attrs"
    @register="registerModal"
    title="Nhập từ vựng hàng loạt"
    width="960px"
    :okText="step === 'preview' ? 'Ghi vào hệ thống' : 'Đóng'"
    :okButtonProps="{ disabled: step !== 'preview' || !rows.length }"
    @ok="handleConfirmImport"
  >
    <Steps :current="stepIndex" size="small" class="mb-6">
      <Step title="Chọn file" />
      <Step title="Xem trước" />
      <Step title="Kết quả" />
    </Steps>

    <!-- Bước 1: chọn file -->
    <div v-if="step === 'upload'">
      <Alert
        type="info"
        show-icon
        class="mb-4"
        message="Định dạng file Excel"
        description="Mỗi dòng là MỘT VIDEO, không phải một từ. Các dòng cùng gloss sẽ được gộp thành một từ với nhiều video. Cột phân loại để trống vẫn nhập được, bổ sung sau."
      />

      <Table
        :columns="templateColumns"
        :data-source="templateSample"
        :pagination="false"
        size="small"
        bordered
        class="mb-4"
      />

      <div class="flex gap-3">
        <Button @click="downloadTemplate">Tải file mẫu</Button>
        <Upload :before-upload="handleParseFile" :show-upload-list="false" accept=".xlsx,.xls">
          <Button type="primary">Chọn file Excel</Button>
        </Upload>
      </div>
    </div>

    <!-- Bước 2: xem trước -->
    <div v-else-if="step === 'preview'">
      <Alert
        v-if="preview"
        :type="preview.failedCount > 0 ? 'warning' : 'success'"
        show-icon
        class="mb-4"
        :message="`Đọc được ${preview.totalRows} dòng`"
      >
        <template #description>
          Sẽ thêm mới <b>{{ preview.createdCount }}</b>, cập nhật
          <b>{{ preview.updatedCount }}</b>, bỏ qua <b>{{ preview.skippedCount }}</b>, lỗi
          <b :class="preview.failedCount ? 'text-red-500' : ''">{{ preview.failedCount }}</b>.
          <div v-if="preview.failedCount" class="mt-1">
            Dòng lỗi sẽ bị bỏ qua khi ghi, các dòng còn lại vẫn được nhập.
          </div>
        </template>
      </Alert>

      <div class="mb-3 flex items-center gap-3">
        <span class="text-sm">Xử lý từ trùng gloss:</span>
        <RadioGroup v-model:value="duplicateStrategy" size="small" button-style="solid">
          <RadioButton value="SKIP">Bỏ qua</RadioButton>
          <RadioButton value="OVERWRITE">Ghi đè</RadioButton>
          <RadioButton value="CREATE_DRAFT">Tạo bản nháp</RadioButton>
        </RadioGroup>
        <Button size="small" @click="runPreview">Xem lại</Button>
      </div>

      <Table
        :columns="previewColumns"
        :data-source="previewRows"
        :pagination="{ pageSize: 10 }"
        size="small"
        bordered
        :row-class-name="(record) => (record.__error ? 'import-row-error' : '')"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <Tag v-if="record.__error" color="red">{{ record.__error }}</Tag>
            <Tag v-else color="green">Hợp lệ</Tag>
          </template>
        </template>
      </Table>
    </div>

    <!-- Bước 3: kết quả -->
    <div v-else>
      <Result
        :status="result?.failedCount ? 'warning' : 'success'"
        :title="`Đã nhập ${(result?.createdCount ?? 0) + (result?.updatedCount ?? 0)} từ vựng`"
      >
        <template #subTitle>
          Thêm mới {{ result?.createdCount }} · Cập nhật {{ result?.updatedCount }} · Bỏ qua
          {{ result?.skippedCount }} · Lỗi {{ result?.failedCount }}
        </template>
      </Result>

      <Table
        v-if="result?.errors?.length"
        :columns="errorColumns"
        :data-source="result.errors"
        :pagination="{ pageSize: 8 }"
        size="small"
        bordered
      />
    </div>
  </BasicModal>
</template>

<script lang="ts" setup>
  import { ref, computed } from 'vue';
  import * as XLSX from 'xlsx';
  // Xem ghi chú ở SignDrawer.vue: không có auto-import, phải khai báo từng cái
  import { Alert, Button, Radio, Result, Steps, Table, Tag, Upload } from 'ant-design-vue';
  import { BasicModal, useModalInner } from '@/components/Modal';
  import { useMessage } from '@/hooks/web/useMessage';
  import { signImportApi } from '@/api/content/sign';
  import type {
    DuplicateStrategy,
    SignImportResult,
    SignImportRow,
  } from '@/api/content/model/contentModel';

  const RadioGroup = Radio.Group;
  const RadioButton = Radio.Button;
  const Step = Steps.Step;

  defineOptions({ name: 'SignImportModal' });

  const emit = defineEmits(['success', 'register']);
  const { createMessage } = useMessage();

  const step = ref<'upload' | 'preview' | 'done'>('upload');
  const stepIndex = computed(() => ({ upload: 0, preview: 1, done: 2 })[step.value]);

  const rows = ref<SignImportRow[]>([]);
  const preview = ref<SignImportResult>();
  const result = ref<SignImportResult>();
  const duplicateStrategy = ref<DuplicateStrategy>('SKIP');

  const templateColumns = [
    { title: 'word_vi', dataIndex: 'wordVi', width: 100 },
    { title: 'gloss', dataIndex: 'gloss', width: 100 },
    { title: 'video_id', dataIndex: 'videoId', width: 100 },
    { title: 'region', dataIndex: 'region', width: 90 },
    { title: 'unit_type', dataIndex: 'unitType', width: 90 },
    { title: 'word_type', dataIndex: 'wordType', width: 90 },
    { title: 'level', dataIndex: 'level', width: 100 },
    { title: 'topics', dataIndex: 'topics' },
  ];

  // Ví dụ lấy từ cấu trúc thật của dữ liệu Bộ GD&ĐT:
  // cùng một từ có ba bản B/T/N -> ba dòng, cùng gloss
  const templateSample = [
    {
      key: 1,
      wordVi: 'địa chỉ',
      gloss: 'DIA_CHI',
      videoId: 'D0001B',
      region: '(tự suy)',
      unitType: 'WORD',
      wordType: 'DANH_TU',
      level: 'BEGINNER',
      topics: 'co-ban',
    },
    {
      key: 2,
      wordVi: 'địa chỉ',
      gloss: 'DIA_CHI',
      videoId: 'D0001N',
      region: '(tự suy)',
      unitType: 'WORD',
      wordType: 'DANH_TU',
      level: 'BEGINNER',
      topics: 'co-ban',
    },
  ];

  const previewColumns = [
    { title: 'Dòng', dataIndex: 'rowNumber', width: 70 },
    { title: 'Từ tiếng Việt', dataIndex: 'wordVi', width: 150 },
    { title: 'Gloss', dataIndex: 'gloss', width: 130 },
    { title: 'Mã video', dataIndex: 'videoId', width: 110 },
    { title: 'Chủ đề', dataIndex: 'topics', width: 140 },
    { title: 'Trạng thái', key: 'status', width: 200 },
  ];

  const errorColumns = [
    { title: 'Dòng', dataIndex: 'rowNumber', width: 80 },
    { title: 'Từ', dataIndex: 'wordVi', width: 160 },
    { title: 'Lý do', dataIndex: 'message' },
  ];

  /** Gộp lỗi từ server vào từng dòng để tô đỏ đúng chỗ */
  const previewRows = computed(() => {
    const errorMap = new Map<number, string>();
    preview.value?.errors?.forEach((e) => {
      if (e.rowNumber != null) errorMap.set(e.rowNumber, e.message);
    });
    return rows.value.map((r) => ({
      ...r,
      key: r.rowNumber,
      __error: r.rowNumber != null ? errorMap.get(r.rowNumber) : undefined,
    }));
  });

  const [registerModal, { changeOkLoading, closeModal }] = useModalInner(() => {
    step.value = 'upload';
    rows.value = [];
    preview.value = undefined;
    result.value = undefined;
    duplicateStrategy.value = 'SKIP';
  });

  /**
   * Phân tích .xlsx ngay trên trình duyệt bằng thư viện xlsx mà vben đã cài sẵn.
   * Nhờ vậy backend không cần Apache POI, và người dùng xem trước được
   * trước khi bất kỳ dòng nào được ghi vào CSDL.
   */
  function handleParseFile(file: File) {
    const reader = new FileReader();
    reader.onload = async (e) => {
      try {
        const workbook = XLSX.read(e.target?.result, { type: 'binary' });
        const sheet = workbook.Sheets[workbook.SheetNames[0]];
        const raw = XLSX.utils.sheet_to_json<Recordable>(sheet, { defval: '' });

        if (!raw.length) {
          createMessage.error('File không có dòng dữ liệu nào');
          return;
        }

        rows.value = raw.map((r, index) => ({
          // +2 vì dòng 1 là tiêu đề, và Excel đánh số từ 1
          rowNumber: index + 2,
          wordVi: pick(r, 'word_vi', 'wordVi', 'Từ tiếng Việt'),
          gloss: pick(r, 'gloss', 'Gloss'),
          wordEn: pick(r, 'word_en', 'wordEn'),
          unitType: pick(r, 'unit_type', 'unitType'),
          wordType: pick(r, 'word_type', 'wordType'),
          wordSubtype: pick(r, 'word_subtype', 'wordSubtype'),
          domain: pick(r, 'domain'),
          level: pick(r, 'level'),
          descriptionVi: pick(r, 'description_vi', 'descriptionVi'),
          topics: pick(r, 'topics'),
          videoId: pick(r, 'video_id', 'videoId'),
          region: pick(r, 'region'),
        }));

        await runPreview();
        step.value = 'preview';
      } catch (err: any) {
        createMessage.error(`Không đọc được file: ${err?.message ?? err}`);
      }
    };
    reader.readAsBinaryString(file);
    return false;
  }

  function pick(row: Recordable, ...keys: string[]): string {
    for (const k of keys) {
      const v = row[k];
      if (v !== undefined && v !== null && String(v).trim() !== '') {
        return String(v).trim();
      }
    }
    return '';
  }

  /** dryRun=true: backend kiểm tra và đếm kết quả dự kiến nhưng KHÔNG ghi gì */
  async function runPreview() {
    preview.value = await signImportApi({
      rows: rows.value,
      duplicateStrategy: duplicateStrategy.value,
      dryRun: true,
    });
  }

  async function handleConfirmImport() {
    if (step.value !== 'preview') {
      closeModal();
      return;
    }
    changeOkLoading(true);
    try {
      result.value = await signImportApi({
        rows: rows.value,
        duplicateStrategy: duplicateStrategy.value,
        dryRun: false,
      });
      step.value = 'done';
      emit('success');
    } finally {
      changeOkLoading(false);
    }
  }

  function downloadTemplate() {
    const header = [
      'word_vi', 'gloss', 'word_en', 'unit_type', 'word_type',
      'word_subtype', 'domain', 'level', 'description_vi', 'topics',
      'video_id', 'region',
    ];
    const sample = [
      ['địa chỉ', 'DIA_CHI', 'address', 'WORD', 'DANH_TU', 'CHUNG', '', 'BEGINNER', '', 'co-ban', 'D0001B', ''],
      ['địa chỉ', 'DIA_CHI', 'address', 'WORD', 'DANH_TU', 'CHUNG', '', 'BEGINNER', '', 'co-ban', 'D0001N', ''],
      ['mẹ', '', 'mother', 'WORD', 'DANH_TU', 'CHUNG', '', 'BEGINNER', '', 'gia-dinh', '', 'COMMON'],
    ];
    const worksheet = XLSX.utils.aoa_to_sheet([header, ...sample]);
    const workbook = XLSX.utils.book_new();
    XLSX.utils.book_append_sheet(workbook, worksheet, 'TuVung');
    XLSX.writeFile(workbook, 'mau-nhap-tu-vung.xlsx');
  }
</script>

<style lang="less">
  .import-row-error td {
    background-color: #fff1f0 !important;
  }
</style>
