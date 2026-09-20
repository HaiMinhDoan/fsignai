export interface SystemSettingModel {
  id: string;
  key: string;
  value: unknown;
  description?: string;
  updatedByName?: string;
  updatedAt: string;
}

export interface SystemSettingSaveParams {
  key: string;
  value: unknown;
  description?: string;
}
