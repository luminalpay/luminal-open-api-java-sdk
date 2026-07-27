package org.luminal.openapi.sdk.model;

import java.time.LocalDateTime;

/**
 * Models for organizing cards into product-type-specific groups.
 */
public final class CardGroupModels {

    private CardGroupModels() {
    }

    /**
     * Card-group list filters.
     *
     * @param pageNo   one-based page number; the server default is {@code 1}
     * @param pageSize number of records requested per page; the server default is {@code 10}
     * @param cardType optional card product type; see {@link OpenApiEnums.CardType}
     */
    public record CardGroupRequest(Integer pageNo, Integer pageSize, String cardType) {
    }

    /**
     * Card-group information.
     *
     * @param cardGroupId   card-group identifier
     * @param cardGroupName user-visible card-group name
     * @param cardType      card product type; see {@link OpenApiEnums.CardType}
     * @param createTime    card-group creation time
     */
    public record CardGroupResponse(
            Long cardGroupId,
            String cardGroupName,
            String cardType,
            LocalDateTime createTime) {
    }

    /**
     * Card-group creation parameters.
     *
     * @param cardGroupName new user-visible card-group name
     * @param cardType      required card product type; see {@link OpenApiEnums.CardType}
     */
    public record CardGroupCreateRequest(String cardGroupName, String cardType) {
    }

    /**
     * Card-group update parameters.
     *
     * @param cardGroupId   target card-group identifier
     * @param cardGroupName new user-visible card-group name
     */
    public record CardGroupUpdateRequest(Long cardGroupId, String cardGroupName) {
    }

    /**
     * Card-group deletion parameters.
     *
     * @param cardGroupId target card-group identifier
     */
    public record CardGroupDeleteRequest(Long cardGroupId) {
    }
}
