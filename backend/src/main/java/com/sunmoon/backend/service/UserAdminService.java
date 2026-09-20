package com.sunmoon.backend.service;

import com.sunmoon.backend.constant.enums.AccountKind;
import com.sunmoon.backend.constant.enums.UserStatus;
import com.sunmoon.backend.constant.enums.VslRoleStatus;
import com.sunmoon.backend.dto.request.auth.UserRolesRequest;
import com.sunmoon.backend.dto.request.auth.UserStatusRequest;
import com.sunmoon.backend.dto.request.auth.VslRoleDecisionRequest;
import com.sunmoon.backend.dto.response.PageResponse;
import com.sunmoon.backend.dto.response.auth.UserAdminResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface UserAdminService {

    PageResponse<UserAdminResponse> filter(String keyword, UserStatus status, AccountKind accountKind,
                                            VslRoleStatus vslRoleStatus, Pageable pageable);

    UserAdminResponse detail(UUID id);

    UserAdminResponse setStatus(UUID actingAdminId, UUID id, UserStatusRequest request);

    UserAdminResponse setRoles(UUID actingAdminId, UUID id, UserRolesRequest request);

    UserAdminResponse decideVslRole(UUID actingAdminId, UUID id, VslRoleDecisionRequest request);
}
