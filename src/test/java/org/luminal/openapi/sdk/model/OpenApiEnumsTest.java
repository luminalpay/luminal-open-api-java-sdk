package org.luminal.openapi.sdk.model;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class OpenApiEnumsTest {

    @Test
    void exposesOAuthScopeWireValues() {
        String[] expected = {
                "openapi:account:read",
                "openapi:card:read",
                "openapi:card:issue",
                "openapi:card:freeze",
                "openapi:card:cancel",
                "openapi:card:limit:write",
                "openapi:card:detail:read",
                "openapi:card-group:read",
                "openapi:card-group:write",
                "openapi:recharge-card:issue",
                "openapi:recharge-card:recharge",
                "openapi:recharge-card:withdraw",
                "openapi:shared-account:read",
                "openapi:shared-account:create",
                "openapi:shared-account:deposit",
                "openapi:shared-account:withdraw",
                "openapi:shared-account:cancel"
        };

        String[] actual = Arrays.stream(OpenApiEnums.OpenApiScope.values())
                .map(OpenApiEnums.OpenApiScope::code)
                .toArray(String[]::new);

        assertArrayEquals(expected, actual);
    }

    @Test
    void exposesPublicRechargeCardOperationTypes() {
        String[] actual = Arrays.stream(OpenApiEnums.RechargeCardOperationType.values())
                .map(OpenApiEnums.RechargeCardOperationType::name)
                .toArray(String[]::new);

        assertArrayEquals(new String[]{"RECHARGE", "WITHDRAW", "MODIFY_LIMITS"}, actual);
    }
}
