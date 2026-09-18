package com.sunmoon.backend.midleware;

import com.sunmoon.backend.constant.context.SecurityContextHolder;
import com.sunmoon.backend.constant.enums.RoleType;
import com.sunmoon.backend.customizeanotations.RequireAuth;
import com.sunmoon.backend.dto.AuthInfo;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.UUID;

@Aspect
@Component
public class AuthorizationAspect {

    @Around("@annotation(requireAuth)")
    public Object checkAuthorization(ProceedingJoinPoint joinPoint, RequireAuth requireAuth) throws Throwable {
        AuthInfo authInfo = SecurityContextHolder.getAuthInfo();
        if(authInfo == null) {
            throw new AccessDeniedException("Access denied");
        }
        if(authInfo.hasAnyRole(RoleType.SYSTEM_ADMIN)){
            return joinPoint.proceed();
        }

        String[] requiredRoles = requireAuth.roles();
        if (requiredRoles == null || requiredRoles.length == 0) {
            return joinPoint.proceed();
        }

        if(Arrays.stream(requiredRoles).toList().contains(RoleType.ALL)){
            return joinPoint.proceed();
        }

        boolean hasAccess = requireAuth.rolesLogic() == RequireAuth.LogicType.OR
                ? authInfo.hasAnyRole(requiredRoles) : authInfo.hasAllRoles(requiredRoles);
        if (!hasAccess) {
            throw new AccessDeniedException("Vai trò không đủ");
        }

        return joinPoint.proceed();
    }

}
