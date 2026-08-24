package io.github.mpcoredeveloper.javaportico.model;

import java.util.List;

/**
 * Immutable intermediate representation produced by the OpenAPI parser/mapper and
 * consumed by the emitters. Mirrors SharpPortico's {@code GrpcModel}.
 */
public record GrpcModel(
        String serviceName,
        String namespace,
        String protoPackage,
        String fileHintName,
        List<MessageModel> messages,
        List<ServiceModel> services,
        List<EnumModel> enums,
        List<AuthSchemeModel> authSchemes,
        ProxyConfigModel proxy) {

    public GrpcModel {
        messages = List.copyOf(messages);
        services = List.copyOf(services);
        enums = List.copyOf(enums);
        authSchemes = List.copyOf(authSchemes);
    }
}
