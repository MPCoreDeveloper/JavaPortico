package io.github.mpcoredeveloper.javaportico.runtime;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;

/**
 * Attaches an OAuth2 access token (as a Bearer credential) to every outgoing gRPC call.
 */
public final class OAuth2ClientInterceptor implements ClientInterceptor {

    private final String token;

    public OAuth2ClientInterceptor(String token) {
        this.token = token;
    }

    @Override
    public <T, R> ClientCall<T, R> interceptCall(
            MethodDescriptor<T, R> method,
            CallOptions callOptions,
            Channel next) {
        return new ForwardingClientCall.SimpleForwardingClientCall<>(next.newCall(method, callOptions)) {
            @Override
            public void start(Listener<R> responseListener, Metadata headers) {
                headers.merge(AuthMetadata.oauth2(token));
                super.start(responseListener, headers);
            }
        };
    }
}
