package org.luminal.openapi.sdk.model;

import java.util.List;

/**
 * Models for querying card pools available to the current member.
 */
public final class CardPoolModels {

    private CardPoolModels() {
    }

    /**
     * Card-pool list filters.
     *
     * <p>The current public endpoint filters by pool identifier and pool name. Card BIN identifiers are not a
     * card-pool query condition; select a BIN through the card-BIN endpoint when the selected pool is known.</p>
     *
     * @param cardPoolId optional card-pool identifier
     * @param poolName   optional card-pool name, matched by the server as a partial name
     */
    public record CardPoolRequest(Long cardPoolId, String poolName) {
    }

    /**
     * Card-pool information returned by the public API.
     *
     * @param cardPoolId         card-pool identifier
     * @param poolName           user-visible card-pool name
     * @param availableCount     number of available card BIN products in the pool
     * @param sharedAccountCount number of shared accounts already opened by the current member
     * @param cardBins           BIN values available in the pool
     * @param canApplyAccount    whether a shared account may be opened; {@code 0} means no and {@code 1} means yes
     * @param canApply           whether a card may be issued from the pool; {@code 0} means no and {@code 1} means yes
     */
    public record CardPoolResponse(
            Long cardPoolId,
            String poolName,
            Integer availableCount,
            Integer sharedAccountCount,
            List<String> cardBins,
            Integer canApplyAccount,
            Integer canApply) {
        public CardPoolResponse {
            cardBins = cardBins == null ? null : List.copyOf(cardBins);
        }
    }
}
