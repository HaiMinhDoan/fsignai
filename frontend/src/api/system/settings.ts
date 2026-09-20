import { defHttp } from '@/utils/http/axios';
import type { SystemSettingModel, SystemSettingSaveParams } from './model/systemSettingModel';

const BASE = '/api/v1/admin/settings';

export const settingListApi = () => defHttp.get<SystemSettingModel[]>({ url: BASE });

export const settingCreateApi = (params: SystemSettingSaveParams) =>
  defHttp.post<SystemSettingModel>({ url: BASE, data: params }, { successMessageMode: 'message' });

export const settingUpdateApi = (id: string, params: SystemSettingSaveParams) =>
  defHttp.put<SystemSettingModel>({ url: `${BASE}/${id}`, data: params }, { successMessageMode: 'message' });

export const settingDeleteApi = (id: string) =>
  defHttp.delete<void>({ url: `${BASE}/${id}` }, { successMessageMode: 'message' });
