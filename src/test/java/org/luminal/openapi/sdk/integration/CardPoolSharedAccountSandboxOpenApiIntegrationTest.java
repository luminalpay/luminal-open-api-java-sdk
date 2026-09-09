package org.luminal.openapi.sdk.integration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Full Sandbox shared-card flow backed by a selected card pool.
 *
 * <p>The 30 shared-card {@code @Test} methods are inherited from {@link ShareCardSandboxOpenApiIntegrationTest}; this
 * class adds an independent shared-account case that passes both a card BIN and its owning card pool. Its Sandbox
 * configuration follows the fixed configuration in the inherited test.</p>
 */
@TestMethodOrder(OrderAnnotation.class)
class CardPoolSharedAccountSandboxOpenApiIntegrationTest extends ShareCardSandboxOpenApiIntegrationTest {

    /**
     * Hides the superclass lifecycle method so the pool flow is enabled before the shared-card resources are resolved.
     */
    @BeforeAll
    static void startWebhookServer() {
        ShareCardSandboxOpenApiIntegrationTest.startWebhookServer();
        ShareCardSandboxOpenApiIntegrationTest.useCardPoolFlow();
    }

    @Test
    @Order(9)
    void listCardPoolsFromSandbox() {
        assertNotNull(ShareCardSandboxOpenApiIntegrationTest.selectedCardPool());
    }

    @Test
    @Order(11)
    void createSharedAccountWithCardBinAndCardPoolFromSandbox() {
        assertNotNull(ShareCardSandboxOpenApiIntegrationTest.createSharedAccountWithCardBinAndCardPool());
    }
}
