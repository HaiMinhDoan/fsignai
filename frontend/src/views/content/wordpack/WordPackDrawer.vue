<template>
  <BasicDrawer
    v-bind="$attrs"
    @register="registerDrawer"
    :title="isUpdate ? `Sửa gói: ${currentPack?.titleVi}` : 'Thêm gói từ mới'"
    width="70%"
    showFooter
    @ok="handleSubmit"
  >
    <Tabs v-model:activeKey="activeTab">
      <TabPane key="info" tab="Thông tin">
        <BasicForm @register="registerForm" />
      </TabPane>

      <TabPane key="items" tab="Từ vựng trong gói" :disabled="!isUpdate">
        <div class="mb-4 flex items-end gap-3 flex-wrap">
          <div class="flex-1" style="min-width: 240px">
            <div class="mb-1 text-xs text-gray-500">Tìm từ vựng để thêm</div>
            <Input
              v-model:value="keyword"
              placeholder="Gõ không dấu cũng tìm được: dia chi"
              allow-clear
              @press-enter="searchSigns"
            />
          </div>
          <div style="width: 200px">
            <div class="mb-1 text-xs text-gray-500">Lọc theo chủ đề</div>
            <Select
              v-model:value="topicId"
              style="width: 100%"
              placeholder="Mọi chủ đề"
              allow-clear
              :options="topicOptions"
            />
          </div>
          <Button type="primary" :loading="searching" @click="searchSigns">Tìm</Button>
          <Button
            type="primary"
            ghost
            :disabled="!pickedSignIds.length"
            :loading="adding"
            @click="addPicked"
          >
            Thêm {{ pickedSignIds.length }} từ đã chọn
          </Button>
        </div>

        <Table
          v-if="searchResults.length"
          class="mb-6"
          :columns="pickerColumns"
          :data-source="searchResults"
          :pagination="false"
          :row-selection="pickerSelection"
          row-key="id"
          size="small"
          :scroll="{ y: 220 }"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'inPack'">
              <Tag v-if="existingSignIds.has(record.id)" color="green">Đã có trong gói</Tag>
              <span v-else class="text-gray-400">—</span>
            </template>
          </template>
        </Table>

        <Divider>Từ trong gói ({{ items.length }})</Divider>

        <Alert
          v-if="!items.length"
          type="info"
          show-icon
          message="Gói chưa có từ nào"
          description="Tìm từ vựng ở ô trên, tích chọn rồi bấm Thêm. Gói chưa có từ sẽ không xuất bản được."
        />

        <Table
          v-else
          :columns="itemColumns"
          :data-source="items"
          :pagination="false"
          row-key="id"
          size="small"
        >
          <template #bodyCell="{ column, record, index }">
            <template v-if="column.key === 'order'">
              <Button size="small" :disabled="index === 0 || reordering" @click="moveItem(index, -1)">
                ↑
              </Button>
              <Button
                size="small"
                class="ml-1"
                :disabled="index === items.length - 1 || reordering"
                @click="moveItem(index, 1)"
              >
                ↓
              </Button>
            </template>

            <template v-if="column.key === 'preview'">
              <video
                v-if="record.signPrimaryVideoUrl"
                :src="record.signPrimaryVideoUrl"
                :poster="record.signThumbnailUrl"
                controls
                preload="metadata"
                style="width: 150px; border-radius: 4px; background: #eaf0f4"
              />
              <Tag v-else color="orange">Chưa có video</Tag>
            </template>

            <template v-if="column.key === 'action'">
              <Popconfirm title="Bỏ từ này khỏi gói?" @confirm="removeItem(record)">
                <Button size="small" danger>Bỏ ra</Button>
              </Popconfirm>
            </template>
          </template>
        </Table>
      </TabPane>
    </Tabs>
  </BasicDrawer>
</template>

<script lang="ts" setup>
  import { computed, ref } from 'vue';
  import {
    Alert,
    Button,
    Divider,
    Input,
    Popconfirm,
    Select,
    Table,
    Tabs,
    Tag,
  } from 'ant-design-vue';
  import { BasicDrawer, useDrawerInner } from '@/components/Drawer';
  import { BasicForm, useForm } from '@/components/Form';
  import { useMessage } from '@/hooks/web/useMessage';

  import { signSearchApi } from '@/api/content/sign';
  import { topicOptionsApi } from '@/api/content/topic';
  import {
    wordPackCreateApi,
    wordPackUpdateApi,
    wordPackDetailApi,
    wordPackAddSignsApi,
    wordPackRemoveItemApi,
    wordPackReorderItemsApi,
  } from '@/api/catalog/wordPack';
  import type { WordPackItemModel, WordPackModel } from '@/api/catalog/model/catalogModel';
  import { wordPackFormSchema } from './wordPack.data';

  const TabPane = Tabs.TabPane;

  defineOptions({ name: 'WordPackDrawer' });

  const emit = defineEmits(['success', 'register']);
  const { createMessage } = useMessage();

  const isUpdate = ref(false);
  const activeTab = ref('info');
  const currentPack = ref<WordPackModel>();
  const items = ref<WordPackItemModel[]>([]);

  const keyword = ref('');
  const topicId = ref<string>();
  const topicOptions = ref<{ label: string; value: string }[]>([]);
  const searchResults = ref<Recordable[]>([]);
  const pickedSignIds = ref<string[]>([]);
  const searching = ref(false);
  const adding = ref(false);
  const reordering = ref(false);

  /** Từ đã nằm trong gói — dùng để đánh dấu trong bảng tìm kiếm */
  const existingSignIds = computed(
    () => new Set(items.value.map((i) => i.signId).filter(Boolean)),
  );

  const pickerSelection = computed(
    () =>
      ({
        type: 'checkbox',
        selectedRowKeys: pickedSignIds.value,
        onChange: (keys: (string | number)[]) => {
          pickedSignIds.value = keys as string[];
        },
      }) as any,
  );

  const pickerColumns = [
    { title: 'Từ', dataIndex: 'wordVi', width: 180 },
    { title: 'Gloss', dataIndex: 'gloss', width: 160 },
    { title: 'Chủ đề chính', dataIndex: 'primaryTopicNameVi', ellipsis: true },
    { title: '', key: 'inPack', width: 140 },
  ];

  const itemColumns = [
    { title: 'Thứ tự', key: 'order', width: 110 },
    { title: 'Xem trước', key: 'preview', width: 170 },
    { title: 'Từ vựng', dataIndex: 'signWordVi', ellipsis: true },
    { title: 'Gloss', dataIndex: 'signGloss', width: 160 },
    { title: '', key: 'action', width: 100 },
  ];

  const [registerForm, { resetFields, setFieldsValue, validate, updateSchema }] = useForm({
    labelWidth: 170,
    schemas: wordPackFormSchema(),
    showActionButtonGroup: false,
    baseColProps: { span: 24 },
  });

  const [registerDrawer, { setDrawerProps, closeDrawer }] = useDrawerInner(
    async (data) => {
      resetFields();
      setDrawerProps({ confirmLoading: false });

      isUpdate.value = !!data?.isUpdate;
      activeTab.value = data?.activeTab ?? 'info';
      items.value = [];
      searchResults.value = [];
      pickedSignIds.value = [];
      keyword.value = '';
      topicId.value = undefined;
      currentPack.value = undefined;

      if (!topicOptions.value.length) {
        const topics = await topicOptionsApi();
        topicOptions.value = topics.map((t) => ({ label: t.nameVi, value: t.id }));
      }

      // Loại chính gói đang sửa khỏi lựa chọn "chỉ mở sau khi xong" — phải
      // biết id trước, nên chỉ dựng lại field này lúc mở drawer, không phải
      // lúc build schema tĩnh ban đầu.
      updateSchema(wordPackFormSchema(data?.record?.id));

      if (isUpdate.value && data?.record?.id) {
        // Luôn lấy bản đầy đủ từ server: dòng trên bảng không chứa danh sách từ
        const detail = await wordPackDetailApi(data.record.id);
        currentPack.value = detail;
        items.value = detail.items ?? [];
        setFieldsValue(detail);
      }
    },
  );

  async function handleSubmit() {
    try {
      const values = await validate();
      setDrawerProps({ confirmLoading: true });

      if (isUpdate.value && currentPack.value) {
        await wordPackUpdateApi(currentPack.value.id, values as any);
      } else {
        await wordPackCreateApi(values as any);
      }

      closeDrawer();
      emit('success');
    } catch (error) {
      // Backend chặn xuất bản gói rỗng hoặc tự khoá chính nó — hiện nguyên văn lý do
      const message = (error as Error)?.message;
      if (message) createMessage.error(message);
    } finally {
      setDrawerProps({ confirmLoading: false });
    }
  }

  async function searchSigns() {
    searching.value = true;
    try {
      const result = await signSearchApi({
        keyword: keyword.value || undefined,
        topicId: topicId.value,
        page: 0,
        size: 50,
      } as any);
      searchResults.value = result.items ?? [];
      pickedSignIds.value = [];
      if (!searchResults.value.length) {
        createMessage.info('Không tìm thấy từ nào khớp');
      }
    } finally {
      searching.value = false;
    }
  }

  async function addPicked() {
    if (!currentPack.value || !pickedSignIds.value.length) return;
    adding.value = true;
    try {
      const wanted = pickedSignIds.value.length;
      const added = await wordPackAddSignsApi(currentPack.value.id, pickedSignIds.value);
      const skipped = wanted - added.length;
      // Backend bỏ qua từ đã có sẵn; nói rõ để người dùng không tưởng là lỗi
      if (skipped > 0) {
        createMessage.success(`Đã thêm ${added.length} từ, bỏ qua ${skipped} từ đã có sẵn`);
      } else {
        createMessage.success(`Đã thêm ${added.length} từ`);
      }
      pickedSignIds.value = [];
      await refresh();
      emit('success');
    } finally {
      adding.value = false;
    }
  }

  async function removeItem(record: Recordable<any>) {
    if (!currentPack.value) return;
    await wordPackRemoveItemApi(currentPack.value.id, record.id);
    await refresh();
    emit('success');
  }

  async function moveItem(index: number, delta: number) {
    if (!currentPack.value) return;
    const target = index + delta;
    if (target < 0 || target >= items.value.length) return;

    const next = [...items.value];
    [next[index], next[target]] = [next[target], next[index]];
    items.value = next;

    reordering.value = true;
    try {
      await wordPackReorderItemsApi(currentPack.value.id, { orderedIds: next.map((i) => i.id) });
    } catch (error) {
      createMessage.error((error as Error).message || 'Không sắp xếp được, đang tải lại');
      await refresh();
    } finally {
      reordering.value = false;
    }
  }

  async function refresh() {
    if (!currentPack.value) return;
    const detail = await wordPackDetailApi(currentPack.value.id);
    currentPack.value = detail;
    items.value = detail.items ?? [];
  }
</script>
