export type UserStatus = 'ACTIVE' | 'PENDING_VERIFICATION' | 'DISABLED' | 'BANNED';
export type AccountKind = 'CHILD' | 'ADULT' | 'PARENT' | 'TEACHER';
export type VslRoleStatus = 'SELF_DECLARED' | 'PENDING' | 'VERIFIED' | 'REJECTED';
export type VslRole = 'LEARNER' | 'DEAF_NATIVE' | 'TEACHER' | 'INTERPRETER';

export interface UserAdminModel {
  id: string;
  email: string;
  emailVerified: boolean;
  fullName: string;
  avatarUrl?: string;

  accountKind: AccountKind;
  ageRange?: string;
  userType: string;
  region: string;

  vslRole: VslRole;
  vslRoleStatus: VslRoleStatus;
  vslRoleEvidence?: string;
  vslRoleVerifiedByName?: string;
  vslRoleVerifiedAt?: string;

  status: UserStatus;
  bannedUntil?: string;
  banReason?: string;

  roleCodes: string[];

  lastLoginAt?: string;
  createdAt: string;
}

export interface RoleOption {
  code: string;
  nameVi: string;
}
