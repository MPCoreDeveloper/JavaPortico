package io.github.mpcoredeveloper.javaportico.model;

import java.util.List;

/** A generated gRPC service definition. */
public record ServiceModel(String name, List<RpcModel> rpcMethods) {
    public ServiceModel {
        rpcMethods = List.copyOf(rpcMethods);
    }
}
