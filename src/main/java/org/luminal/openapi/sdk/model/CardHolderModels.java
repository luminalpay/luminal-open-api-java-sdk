package org.luminal.openapi.sdk.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Request and response models for cardholder management.
 */
public final class CardHolderModels {
    private CardHolderModels() {
    }

    /**
     * Parameters used to create a cardholder.
     */
    public record CardHolderCreateRequest(
            String lastName,
            String firstName,
            LocalDate birthDate,
            String mail,
            String phone,
            String areaCode,
            Long countryId,
            String postalCode,
            String state,
            String city,
            String addressLine1,
            String addressLine2) {
    }

    /**
     * Parameters used to update all editable cardholder fields.
     */
    public record CardHolderModifyRequest(
            Long cardHolderId,
            String lastName,
            String firstName,
            LocalDate birthDate,
            String mail,
            String phone,
            String areaCode,
            Long countryId,
            String postalCode,
            String state,
            String city,
            String addressLine1,
            String addressLine2) {
    }

    /**
     * Cardholder page filters.
     */
    public record CardHolderPageRequest(
            Integer pageNo,
            Integer pageSize,
            Long cardHolderId,
            String name,
            String phone,
            String mail,
            Long[] createTime) {
        public CardHolderPageRequest {
            createTime = createTime == null ? null : createTime.clone();
        }
    }

    /**
     * Filters for cards associated with a cardholder.
     */
    public record CardHolderCardPageRequest(
            Integer pageNo,
            Integer pageSize,
            Long cardHolderId,
            Long memberCardId) {
    }

    /**
     * Complete cardholder profile returned by the details endpoint.
     */
    public record CardHolderDetailResponse(
            Long cardHolderId,
            String lastName,
            String firstName,
            LocalDate birthDate,
            String mail,
            String phone,
            String areaCode,
            Long countryId,
            String postalCode,
            String state,
            String city,
            String addressLine1,
            String addressLine2,
            LocalDateTime createTime) {
    }

    /**
     * Cardholder list item.
     */
    public record CardHolderPageResponse(
            LocalDateTime createTime,
            Long cardHolderId,
            String fullName,
            String fullPhone,
            String mail) {
    }

    /**
     * Card associated with a cardholder.
     */
    public record CardHolderCardResponse(
            Long memberCardId,
            String maskCardNo,
            LocalDateTime openTime) {
    }

    /**
     * Country or region available for cardholder creation and updates.
     */
    public record CardHolderCountryResponse(
            Long countryId,
            String countryName,
            String countryCode,
            String areaCode,
            Integer phoneMaxLength) {
    }
}
