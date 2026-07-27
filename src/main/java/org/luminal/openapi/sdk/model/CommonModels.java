package org.luminal.openapi.sdk.model;

import java.util.List;

/**
 * Generic immutable pagination envelopes shared by Luminal Open API domains.
 */
public final class CommonModels {

    private CommonModels() {
    }

    /**
     * Standard paginated response.
     *
     * @param total total number of matching records
     * @param list  records returned for the requested page
     * @param <T>   record type
     */
    public record PageResult<T>(Long total, List<T> list) {
        public PageResult {
            list = list == null ? null : List.copyOf(list);
        }
    }

    /**
     * Paginated response with endpoint-specific metadata.
     *
     * @param total total number of matching records
     * @param list  records returned for the requested page
     * @param extra endpoint-specific metadata, or {@code null}
     * @param <T>   record type
     * @param <E>   metadata type
     */
    public record PageResultEx<T, E>(Long total, List<T> list, E extra) {
        public PageResultEx {
            list = list == null ? null : List.copyOf(list);
        }
    }
}
