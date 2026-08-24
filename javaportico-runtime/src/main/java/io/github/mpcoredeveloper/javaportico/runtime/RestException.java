package io.github.mpcoredeveloper.javaportico.runtime;

/** Unchecked exception thrown when a REST dispatch fails (transport level). */
public class RestException extends RuntimeException {

    public RestException(Throwable cause) {
        super(cause);
    }
}
