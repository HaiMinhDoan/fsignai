import { defHttp } from '@/utils/http/axios';
import type { ExemplarJobStatus, ExemplarModel } from './model/contentModel';

enum Api {
  SIGNS = '/api/v1/admin/signs',
  EXEMPLARS = '/api/v1/admin/exemplars',
  JOB = '/api/v1/admin/ai/exemplars',
}

export const exemplarListApi = (signId: string) =>
  defHttp.get<ExemplarModel[]>({ url: `${Api.SIGNS}/${signId}/exemplars` });

/** Chạy MediaPipe trên từng video của từ — đồng bộ, vài giây mỗi video nên nới timeout */
export const exemplarRebuildApi = (signId: string) =>
  defHttp.post<ExemplarModel[]>({
    url: `${Api.SIGNS}/${signId}/exemplars/rebuild`,
    timeout: 3 * 60 * 1000,
  });

export const exemplarSetActiveApi = (id: string, active: boolean) =>
  defHttp.request<void>({ method: 'PATCH', url: `${Api.EXEMPLARS}/${id}/active?active=${active}` });

export const exemplarJobStatusApi = () =>
  defHttp.get<ExemplarJobStatus>({ url: `${Api.JOB}/status` });

export const exemplarJobStartApi = (limit: number, retryFailed: boolean) =>
  defHttp.post<ExemplarJobStatus>({
    url: `${Api.JOB}/build-missing?limit=${limit}&retryFailed=${retryFailed}`,
  });
