package io.github.mpcoredeveloper.javaportico.runtime;

import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;

/**
 * Captures the request {@link Metadata} into {@link ProxyContext#REQUEST_METADATA} so generated
 * proxies (which extend an {@code ImplBase}) can read client keys and bypass flags.
 *
 * <p>Register with: {@code ServerInterceptors.intercept(bindService, new ProxyMetadataInterceptor())}.</p>
 */
public final class ProxyMetadataInterceptor implements ServerInterceptor {

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {
        Context ctx = Context.current().withValue(ProxyContext.REQUEST_METADATA, headers);
        return Contexts.interceptCall(ctx, call, headers, next);
    }
}
