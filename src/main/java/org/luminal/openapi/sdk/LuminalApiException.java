package org.luminal.openapi.sdk;

/**
 * Reports an HTTP transport failure or a non-success Luminal API result.
 */
public final class LuminalApiException extends RuntimeException {

    private final int httpStatus;
    private final Integer apiCode;
    private final String responseBody;

    /**
     * Creates an exception for a received HTTP response.
     *
     * @param message human-readable failure description
     * @param httpStatus received HTTP status
     * @param apiCode Luminal business code, or {@code null} when unavailable
     * @param responseBody unmodified response body
     */
    public LuminalApiException(String message, int httpStatus, Integer apiCode, String responseBody) {
        super(message);
        this.httpStatus = httpStatus;
        this.apiCode = apiCode;
        this.responseBody = responseBody;
    }

    /**
     * Creates an exception for a transport or decoding failure without an HTTP response.
     *
     * @param message human-readable failure description
     * @param cause original failure
     */
    public LuminalApiException(String message, Throwable cause) {
        super(message, cause);
        this.httpStatus = -1;
        this.apiCode = null;
        this.responseBody = null;
    }

    /** Returns the HTTP status, or {@code -1} when no response was received. */
    public int httpStatus() {
        return httpStatus;
    }

    /** Returns the Luminal business code, or {@code null} when unavailable. */
    public Integer apiCode() {
        return apiCode;
    }

    /** Returns the unmodified response body, or {@code null} when unavailable. */
    public String responseBody() {
        return responseBody;
    }
}
