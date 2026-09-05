package io.github.mpcoredeveloper.javaportico.runtime;

import java.util.Arrays;
import java.util.Objects;

/** Response of a REST call; body is the raw payload (typically JSON). */
public record RestResponse(int statusCode, byte[] body, String contentType) {

    public RestResponse {
        body = body == null ? new byte[0] : body;
        contentType = contentType == null || contentType.isBlank() ? "application/json" : contentType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RestResponse that)) return false;
        return statusCode == that.statusCode
                && Arrays.equals(body, that.body)
                && Objects.equals(contentType, that.contentType);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(statusCode, contentType);
        return 31 * result + Arrays.hashCode(body);
    }

    @Override
    public String toString() {
        return "RestResponse[statusCode=" + statusCode + ", body=" + Arrays.toString(body)
                + ", contentType=" + contentType + "]";
    }

    public int getStatusCode() {
        return statusCode;
    }

    public byte[] getBody() {
        return body;
    }

    public String getContentType() {
        return contentType;
    }
}
