import { defHttp } from '@/utils/http/axios';
import type { PageResult } from '@/api/content/model/contentModel';
import type { RoleOption, UserAdminModel } from './model/systemModel';

enum Api {
  BASE = '/api/v1/admin/users',
  ROLES = '/api/v1/admin/users/roles',
}

export const userFilterApi = (params: {
  keyword?: string;
  status?: string;
  accountKind?: string;
  vslRoleStatus?: string;
  page?: number;
  size?: number;
}) => defHttp.get<PageResult<UserAdminModel>>({ url: Api.BASE, params });

export const userDetailApi = (id: string) => defHttp.get<UserAdminModel>({ url: `${Api.BASE}/${id}` });

export const userRoleOptionsApi = () => defHttp.get<RoleOption[]>({ url: Api.ROLES }, { errorMessageMode: 'none' });

export const userSetStatusApi = (id: string, status: string, banReason?: string, bannedUntil?: string) =>
  defHttp.put<UserAdminModel>(
    { url: `${Api.BASE}/${id}/status`, data: { status, banReason, bannedUntil } },
    { successMessageMode: 'message' },
  );

export const userSetRolesApi = (id: string, roleCodes: string[]) =>
  defHttp.put<UserAdminModel>(
    { url: `${Api.BASE}/${id}/roles`, data: { roleCodes } },
    { successMessageMode: 'message' },
  );

export const userDecideVslRoleApi = (id: string, decision: 'VERIFIED' | 'REJECTED') =>
  defHttp.put<UserAdminModel>(
    { url: `${Api.BASE}/${id}/vsl-role`, data: { decision } },
    { successMessageMode: 'message' },
  );
