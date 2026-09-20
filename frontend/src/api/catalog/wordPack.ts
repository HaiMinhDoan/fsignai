import { defHttp } from '@/utils/http/axios';
import type { PageResult } from '@/api/content/model/contentModel';
import type {
  ReorderParams,
  WordPackFilterParams,
  WordPackItemModel,
  WordPackModel,
  WordPackSaveParams,
} from './model/catalogModel';

enum Api {
  BASE = '/api/v1/admin/word-packs',
  FILTER = '/api/v1/admin/word-packs/filter',
  PUBLISH = '/api/v1/admin/word-packs/publish',
  REORDER = '/api/v1/admin/word-packs/reorder',
}

export const wordPackFilterApi = (params: WordPackFilterParams) =>
  defHttp.post<PageResult<WordPackModel>>({ url: Api.FILTER, data: params });

/**
 * Danh sách phẳng cho dropdown "chỉ mở sau khi xong".
 * Không có endpoint /options riêng ở backend (bộ gói từ nhỏ, khác với hàng
 * nghìn từ vựng) — xin nguyên trang đầu cỡ lớn từ /filter là đủ.
 */
export const wordPackOptionsApi = () =>
  wordPackFilterApi({ page: 0, size: 200, sorts: [{ fieldName: 'displayOrder', direction: 'ASC' }] })
    .then((res) => res.items);

/** Kèm danh sách từ đã sắp thứ tự */
export const wordPackDetailApi = (id: string) =>
  defHttp.get<WordPackModel>({ url: `${Api.BASE}/${id}` });

export const wordPackCreateApi = (params: WordPackSaveParams) =>
  defHttp.post<WordPackModel>({ url: Api.BASE, data: params }, { successMessageMode: 'message' });

export const wordPackUpdateApi = (id: string, params: WordPackSaveParams) =>
  defHttp.put<WordPackModel>(
    { url: `${Api.BASE}/${id}`, data: params },
    { successMessageMode: 'message' },
  );

export const wordPackDeleteApi = (id: string) =>
  defHttp.delete<void>({ url: `${Api.BASE}/${id}` }, { successMessageMode: 'message' });

export const wordPackPublishApi = (ids: string[], published: boolean) =>
  defHttp.post<{ affected: number }>(
    { url: Api.PUBLISH, data: { ids, published } },
    { successMessageMode: 'message' },
  );

export const wordPackReorderApi = (params: ReorderParams) =>
  defHttp.post<void>({ url: Api.REORDER, data: params }, { errorMessageMode: 'message' });

// ==================== Từ trong gói ====================

export const wordPackAddSignsApi = (packId: string, signIds: string[]) =>
  defHttp.post<WordPackItemModel[]>(
    { url: `${Api.BASE}/${packId}/items`, data: { signIds } },
    { successMessageMode: 'message' },
  );

export const wordPackRemoveItemApi = (packId: string, itemId: string) =>
  defHttp.delete<void>(
    { url: `${Api.BASE}/${packId}/items/${itemId}` },
    { successMessageMode: 'message' },
  );

export const wordPackReorderItemsApi = (packId: string, params: ReorderParams) =>
  defHttp.post<void>(
    { url: `${Api.BASE}/${packId}/items/reorder`, data: params },
    { errorMessageMode: 'message' },
  );
