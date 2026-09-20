import { defHttp } from '@/utils/http/axios';
import type { PageResult } from '@/api/content/model/contentModel';
import type { AuditLogModel } from './model/auditLogModel';

export const auditLogFilterApi = (params: {
  actorId?: string;
  action?: string;
  entityType?: string;
  page?: number;
  size?: number;
}) => defHttp.get<PageResult<AuditLogModel>>({ url: '/api/v1/admin/audit-logs', params });
