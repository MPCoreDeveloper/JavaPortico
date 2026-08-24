package io.github.mpcoredeveloper.javaportico.runtime;

/** Response of a REST call; body is the raw payload (typically JSON). */
public record RestResponse(int statusCode, byte[] body, String contentType) {

    public RestResponse {
        body = body == null ? new byte[0] : body;
        contentType = contentType == null || contentType.isBlank() ? "application/json" : contentType;
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
