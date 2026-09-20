<template>
  <BasicDrawer
    v-bind="$attrs"
    @register="registerDrawer"
    :title="`Soạn nội dung: ${lessonTitle}`"
    width="70%"
  >
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
        <template v-if="column.key === 'inLesson'">
          <Tag v-if="existingSignIds.has(record.id)" color="green">Đã có trong bài</Tag>
          <span v-else class="text-gray-400">—</span>
        </template>
      </template>
    </Table>

    <Divider>Nội dung bài học ({{ items.length }})</Divider>

    <Alert
      v-if="!items.length"
      type="info"
      show-icon
      message="Bài học chưa có nội dung"
      description="Tìm từ vựng ở ô trên, tích chọn rồi bấm Thêm. Bài chưa có nội dung sẽ không xuất bản được."
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

        <template v-if="column.key === 'itemType'">
          <Tag>{{ ITEM_TYPE_LABEL[record.itemType] ?? record.itemType }}</Tag>
        </template>

        <template v-if="column.key === 'action'">
          <Popconfirm title="Bỏ nội dung này khỏi bài?" @confirm="removeItem(record)">
            <Button size="small" danger>Bỏ ra</Button>
          </Popconfirm>
        </template>
      </template>
    </Table>
  </BasicDrawer>
</template>

<script lang="ts" setup>
  import { computed, ref } from 'vue';
  import { Alert, Button, Divider, Input, Popconfirm, Select, Table, Tag } from 'ant-design-vue';
  import { BasicDrawer, useDrawerInner } from '@/components/Drawer';
  import { useMessage } from '@/hooks/web/useMessage';

  import { signSearchApi } from '@/api/content/sign';
  import { topicOptionsApi } from '@/api/content/topic';
  import {
    lessonDetailApi,
    lessonAddSignsApi,
    lessonRemoveItemApi,
    lessonReorderItemsApi,
  } from '@/api/catalog/lesson';
  import type { LessonItemModel } from '@/api/catalog/model/catalogModel';

  defineOptions({ name: 'LessonEditorDrawer' });

  const emit = defineEmits(['success', 'register']);
  const { createMessage } = useMessage();

  const ITEM_TYPE_LABEL: Record<string, string> = {
    SIGN: 'Từ vựng',
    VIDEO: 'Video',
    TEXT: 'Văn bản',
    PRACTICE: 'Luyện tập',
    QUIZ: 'Câu hỏi',
  };

  const lessonId = ref<string>();
  const lessonTitle = ref('');
  const items = ref<LessonItemModel[]>([]);

  const keyword = ref('');
  const topicId = ref<string>();
  const topicOptions = ref<{ label: string; value: string }[]>([]);
  const searchResults = ref<Recordable[]>([]);
  const pickedSignIds = ref<string[]>([]);
  const searching = ref(false);
  const adding = ref(false);
  const reordering = ref(false);

  /** Từ đã nằm trong bài — dùng để đánh dấu trong bảng tìm kiếm */
  const existingSignIds = computed(
    () => new Set(items.value.map((i) => i.signId).filter(Boolean) as string[]),
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
    { title: '', key: 'inLesson', width: 140 },
  ];

  const itemColumns = [
    { title: 'Thứ tự', key: 'order', width: 110 },
    { title: 'Xem trước', key: 'preview', width: 170 },
    { title: 'Loại', key: 'itemType', width: 110 },
    { title: 'Từ vựng', dataIndex: 'signWordVi', ellipsis: true },
    { title: 'Gloss', dataIndex: 'signGloss', width: 160 },
    { title: '', key: 'action', width: 100 },
  ];

  const [registerDrawer] = useDrawerInner(async (data) => {
    lessonId.value = data?.lessonId;
    lessonTitle.value = data?.lessonTitle ?? '';
    items.value = [];
    searchResults.value = [];
    pickedSignIds.value = [];
    keyword.value = '';
    topicId.value = undefined;

    if (!topicOptions.value.length) {
      const topics = await topicOptionsApi();
      topicOptions.value = topics.map((t) => ({ label: t.nameVi, value: t.id }));
    }
    await refresh();
  });

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
    if (!lessonId.value || !pickedSignIds.value.length) return;
    adding.value = true;
    try {
      const result = await lessonAddSignsApi(lessonId.value, pickedSignIds.value);
      // Backend bỏ qua từ đã có sẵn; nói rõ để người dùng không tưởng là lỗi
      if (result.skipped > 0) {
        createMessage.success(`Đã thêm ${result.added} từ, bỏ qua ${result.skipped} từ đã có sẵn`);
      } else {
        createMessage.success(`Đã thêm ${result.added} từ`);
      }
      pickedSignIds.value = [];
      await refresh();
      emit('success');
    } finally {
      adding.value = false;
    }
  }

  async function removeItem(record: Recordable) {
    if (!lessonId.value) return;
    await lessonRemoveItemApi(lessonId.value, record.id);
    await refresh();
    emit('success');
  }

  async function moveItem(index: number, delta: number) {
    if (!lessonId.value) return;
    const target = index + delta;
    if (target < 0 || target >= items.value.length) return;

    const next = [...items.value];
    [next[index], next[target]] = [next[target], next[index]];
    items.value = next;

    reordering.value = true;
    try {
      await lessonReorderItemsApi(lessonId.value, { orderedIds: next.map((i) => i.id) });
    } catch (error) {
      createMessage.error((error as Error).message || 'Không sắp xếp được, đang tải lại');
      await refresh();
    } finally {
      reordering.value = false;
    }
  }

  async function refresh() {
    if (!lessonId.value) return;
    const detail = await lessonDetailApi(lessonId.value);
    items.value = detail.items ?? [];
  }
</script>
