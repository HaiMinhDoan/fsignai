import { defHttp } from '@/utils/http/axios';
import type {
  PageResult,
  Region,
  SignImportParams,
  SignImportResult,
  SignModel,
  SignSaveParams,
  SignSearchParams,
  SignStepModel,
  SignStepSaveParams,
  SignVideoModel,
  ViewAngle,
} from './model/contentModel';

enum Api {
  BASE = '/api/v1/admin/signs',
  SEARCH = '/api/v1/admin/signs/search',
  PUBLISH = '/api/v1/admin/signs/publish',
  IMPORT = '/api/v1/admin/signs/import',
}

/**
 * Tìm kiếm từ vựng.
 * Backend tự bỏ dấu tiếng Việt trước khi so với cột word_vi_unaccent,
 * nên gõ "dia chi" vẫn ra "địa chỉ" — frontend không cần xử lý gì thêm.
 */
export const signSearchApi = (params: SignSearchParams) =>
  defHttp.post<PageResult<SignModel>>({ url: Api.SEARCH, data: params });

export const signDetailApi = (id: string) => defHttp.get<SignModel>({ url: `${Api.BASE}/${id}` });

export const signCreateApi = (params: SignSaveParams) =>
  defHttp.post<SignModel>({ url: Api.BASE, data: params }, { successMessageMode: 'message' });

export const signUpdateApi = (id: string, params: SignSaveParams) =>
  defHttp.put<SignModel>({ url: `${Api.BASE}/${id}`, data: params }, { successMessageMode: 'message' });

export const signDeleteApi = (id: string) =>
  defHttp.delete<void>({ url: `${Api.BASE}/${id}` }, { successMessageMode: 'message' });

/** Xuất bản / gỡ xuất bản hàng loạt từ thanh công cụ của bảng */
export const signPublishApi = (ids: string[], published: boolean) =>
  defHttp.post<{ affected: number }>(
    { url: Api.PUBLISH, data: { ids, published } },
    { successMessageMode: 'message' },
  );

/**
 * Gán chủ đề cho nhiều từ cùng lúc.
 *
 * Từ điển Bộ GD&ĐT nạp về không kèm chủ đề, nên đây là đường duy nhất khả thi
 * để phân loại hơn 3.300 từ. Chưa gán chủ đề thì không sinh được khoá học,
 * không trộn được đề theo chủ đề, và người học cũng không duyệt theo chủ đề được.
 */
export const signAssignTopicsApi = (params: {
  signIds: string[];
  topicIds: string[];
  replace?: boolean;
  setPrimary?: boolean;
}) =>
  defHttp.post<{ affected: number }>(
    { url: `${Api.BASE}/assign-topics`, data: params, timeout: 60 * 1000 },
    { successMessageMode: 'message' },
  );

/**
 * Nhập hàng loạt.
 * File .xlsx được phân tích ngay ở trình duyệt bằng thư viện xlsx mà vben
 * đã có sẵn, rồi gửi lên dưới dạng JSON — backend không cần Apache POI.
 *
 * Luôn gọi với dryRun=true trước để người dùng xem trước, đúng nguyên tắc
 * "nhập nhầm 4.000 dòng rồi mới phát hiện là thảm hoạ khó gỡ".
 */
export const signImportApi = (params: SignImportParams) =>
  defHttp.post<SignImportResult>(
    // timeout thuộc AxiosRequestConfig (tham số 1), không phải RequestOptions (tham số 2).
    // Nhập 4.000 dòng có thể chạy vài phút nên phải nới mặc định.
    { url: Api.IMPORT, data: params, timeout: 5 * 60 * 1000 },
    { errorMessageMode: 'modal' },
  );

// ==================== Video ====================

export const signVideoListApi = (signId: string) =>
  defHttp.get<SignVideoModel[]>({ url: `${Api.BASE}/${signId}/videos` });

export const signVideoUploadApi = (
  signId: string,
  file: File,
  meta: { region?: Region; viewAngle?: ViewAngle; signerLabel?: string; captionVi?: string },
) => {
  const formData = new FormData();
  formData.append('file', file);

  const params = new URLSearchParams();
  if (meta.region) params.append('region', meta.region);
  if (meta.viewAngle) params.append('viewAngle', meta.viewAngle);
  if (meta.signerLabel) params.append('signerLabel', meta.signerLabel);
  if (meta.captionVi) params.append('captionVi', meta.captionVi);

  const query = params.toString();
  return defHttp.post<SignVideoModel>(
    {
      url: `${Api.BASE}/${signId}/videos${query ? `?${query}` : ''}`,
      data: formData,
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 10 * 60 * 1000,
    },
    { successMessageMode: 'message' },
  );
};

export const signVideoSetPrimaryApi = (signId: string, videoId: string) =>
  defHttp.put<SignVideoModel>(
    { url: `${Api.BASE}/${signId}/videos/${videoId}/primary` },
    { successMessageMode: 'message' },
  );

export const signVideoDeleteApi = (signId: string, videoId: string) =>
  defHttp.delete<void>(
    { url: `${Api.BASE}/${signId}/videos/${videoId}` },
    { successMessageMode: 'message' },
  );

// ==================== Hướng dẫn từng bước ====================

export const signStepListApi = (signId: string) =>
  defHttp.get<SignStepModel[]>({ url: `${Api.BASE}/${signId}/steps` });

export const signStepCreateApi = (signId: string, params: SignStepSaveParams) =>
  defHttp.post<SignStepModel>(
    { url: `${Api.BASE}/${signId}/steps`, data: params },
    { successMessageMode: 'message' },
  );

export const signStepUpdateApi = (signId: string, stepId: string, params: SignStepSaveParams) =>
  defHttp.put<SignStepModel>(
    { url: `${Api.BASE}/${signId}/steps/${stepId}`, data: params },
    { successMessageMode: 'message' },
  );

export const signStepDeleteApi = (signId: string, stepId: string) =>
  defHttp.delete<void>(
    { url: `${Api.BASE}/${signId}/steps/${stepId}` },
    { successMessageMode: 'message' },
  );

/** Gửi nguyên mảng id theo thứ tự mới — cùng cách sắp xếp câu hỏi đề thi và nội dung bài học */
export const signStepReorderApi = (signId: string, orderedIds: string[]) =>
  defHttp.put<SignStepModel[]>(
    { url: `${Api.BASE}/${signId}/steps/reorder`, data: { orderedIds } },
    { successMessageMode: 'message' },
  );

export const signStepImageUploadApi = (signId: string, stepId: string, file: File) => {
  const formData = new FormData();
  formData.append('file', file);
  return defHttp.post<SignStepModel>(
    {
      url: `${Api.BASE}/${signId}/steps/${stepId}/image`,
      data: formData,
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 2 * 60 * 1000,
    },
    { successMessageMode: 'message' },
  );
};

export const signStepImageDeleteApi = (signId: string, stepId: string) =>
  defHttp.delete<void>(
    { url: `${Api.BASE}/${signId}/steps/${stepId}/image` },
    { successMessageMode: 'message' },
  );
