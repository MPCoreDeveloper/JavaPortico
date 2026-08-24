package io.github.mpcoredeveloper.javaportico.runtime;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;

/**
 * Attaches an API key header to every outgoing gRPC call.
 */
public final class ApiKeyClientInterceptor implements ClientInterceptor {

    private final String headerName;
    private final String key;

    public ApiKeyClientInterceptor(String headerName, String key) {
        this.headerName = headerName;
        this.key = key;
    }

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
            MethodDescriptor<ReqT, RespT> method,
            CallOptions callOptions,
            Channel next) {
        return new ForwardingClientCall.SimpleForwardingClientCall<>(next.newCall(method, callOptions)) {
            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {
                headers.merge(AuthMetadata.apiKey(headerName, key));
                super.start(responseListener, headers);
            }
        };
    }
}
