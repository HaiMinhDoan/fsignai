package com.sunmoon.backend.customizeanotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireAuth {
    String[] roles() default {}; // Các role được phép

    LogicType rolesLogic() default LogicType.OR;

    // API có bắt buộc chạy trong ngữ cảnh một org hay không.
    // RequireAuthOperationCustomizer đọc cờ này để thêm header orgId/lang vào Swagger.
    boolean inWorkspace() default false;

    enum LogicType {
        AND, OR
    }
}
