package com.sunmoon.backend.configs;

import com.sunmoon.backend.customizeanotations.RequireAuth;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;

@Component
public class RequireAuthOperationCustomizer implements OperationCustomizer {
    @Override
    public Operation customize(Operation operation, HandlerMethod handlerMethod) {
        RequireAuth requireAuth = handlerMethod.getMethodAnnotation(RequireAuth.class);
        if (requireAuth != null) {
            // Thêm Authorization requirement
            operation.addSecurityItem(new SecurityRequirement().addList("bearer-jwt"));

            // Xây dựng description
            StringBuilder description = new StringBuilder();

            if (operation.getDescription() != null && !operation.getDescription().isEmpty()) {
                description.append(operation.getDescription()).append("\n\n");
            }

            description.append("---\n\n");
            description.append("###   Authorization Requirements\n\n");

            // Tạo bảng thông tin
            description.append("| Requirement | Details | Logic |\n");
            description.append("|-------------|---------|-------|\n");
            String[] roles = requireAuth.roles();

            // Roles
            if (roles.length > 0) {
                description.append("| **Roles** | ");
                description.append(formatArray(roles));
                description.append(" | `").append(requireAuth.rolesLogic()).append("` |\n");
            }

            // Org and Lang
            if(requireAuth.inWorkspace()){
                description.append("| **Org Context** | Required | - |\n");
                Parameter orgIdParam = new Parameter()
                        .in("header")
                        .name("orgId")
                        .description("ID của org (bắt buộc khi inWorkspace = true)")
                        .required(true)
                        .schema(new StringSchema())
                        .example("550e8400-e29b-41d4-a716-446655440000");
                Parameter langParam = new Parameter()
                        .in("header")
                        .name("lang")
                        .description("Language (en)")
                        .required(false)
                        .schema(new StringSchema())
                        .example("vi");
                operation.addParametersItem(orgIdParam);
                operation.addParametersItem(langParam);
            }
            description.append("\n");
            if(roles.length > 0){
                description.append("> **Note:** ");
                description.append("User must have at least one of the required roles");
                if (requireAuth.rolesLogic() == RequireAuth.LogicType.AND) {
                    description.append(" (ALL roles required)");
                }
                description.append(".");
                description.append("\n");
            }
            operation.setDescription(description.toString());
        }
        return operation;
    }

    private String formatArray(String[] items) {
        if (items.length == 0) return "-";
        if (items.length == 1) return "`" + items[0] + "`";

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.length; i++) {
            sb.append("`").append(items[i]).append("`");
            if (i < items.length - 1) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }
}
