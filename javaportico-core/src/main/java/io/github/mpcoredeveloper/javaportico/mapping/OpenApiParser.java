package io.github.mpcoredeveloper.javaportico.mapping;

import io.github.mpcoredeveloper.javaportico.model.AuthKind;
import io.github.mpcoredeveloper.javaportico.model.AuthSchemeModel;
import io.github.mpcoredeveloper.javaportico.model.EnumModel;
import io.github.mpcoredeveloper.javaportico.model.FieldKind;
import io.github.mpcoredeveloper.javaportico.model.FieldModel;
import io.github.mpcoredeveloper.javaportico.model.GrpcModel;
import io.github.mpcoredeveloper.javaportico.model.MessageModel;
import io.github.mpcoredeveloper.javaportico.model.ProxyConfigModel;
import io.github.mpcoredeveloper.javaportico.model.RpcKind;
import io.github.mpcoredeveloper.javaportico.model.RpcModel;
import io.github.mpcoredeveloper.javaportico.model.ServiceModel;
import io.github.mpcoredeveloper.javaportico.model.WorkItem;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityScheme;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.github.mpcoredeveloper.javaportico.mapping.NameSanitizer.sanitizePascal;

/**
 * Parses OpenAPI 3.0/3.1 documents and maps them to the immutable {@link GrpcModel} IR.
 * Ported from SharpPortico's {@code OpenApiParser}.
 */
public final class OpenApiParser {

    private static final String STREAMING_HINT_EXTENSION = "x-grpc-streaming";

    private OpenApiParser() {
    }

    public static ParseResult parseAndMap(WorkItem item) {
        OpenApiParse.Parsed parsed = OpenApiParse.parse(item.content());
        if (!parsed.ok()) {
            return ParseResult.failure(item, parsed.messages());
        }
        OpenAPI document = parsed.document();
        if (document.getInfo() == null) {
            return ParseResult.failure(item, List.of("missing info/title section"));
        }

        String serviceName = deriveServiceName(item, document);
        String ns = (item.namespaceName() == null || item.namespaceName().isBlank())
                ? serviceName
                : item.namespaceName();
        String protoPackage = ns.toLowerCase();

        // Map schemas.
        SchemaMapper schemaMapper = new SchemaMapper(document);
        List<MessageModel> allMessages = new ArrayList<>();
        List<EnumModel> allEnums = new ArrayList<>();

        if (document.getComponents() != null && document.getComponents().getSchemas() != null) {
            for (Map.Entry<String, Schema> entry : document.getComponents().getSchemas().entrySet()) {
                String name = entry.getKey();
                Schema<?> schema = entry.getValue();
                if (schemaMapper.isEnum(schema)) {
                    schemaMapper.mapEnum(name, schema);
                } else {
                    MessageModel msg = schemaMapper.mapSchemaToMessage(name, schema, false, false);
                    if (msg != null) allMessages.add(msg);
                }
            }
        }
        allEnums.addAll(schemaMapper.allEnums());

        // Map paths -> RPCs + request/response messages.
        List<RpcModel> rpcs = new ArrayList<>();
        int[] operIndex = {0};
        List<String> diagnostics = new ArrayList<>();
        if (document.getPaths() != null) {
            for (Map.Entry<String, PathItem> entry : document.getPaths().entrySet()) {
                String path = entry.getKey();
                PathItem pathItem = entry.getValue();
                if (pathItem == null) continue;
                for (Map.Entry<PathItem.HttpMethod, Operation> opEntry : pathItem.readOperationsMap().entrySet()) {
                    RpcModel rpc = mapOperation(path, opEntry.getKey(), opEntry.getValue(),
                            item, schemaMapper, allMessages, diagnostics, operIndex);
                    if (rpc != null) rpcs.add(rpc);
                }
            }
        }

        // Auth schemes.
        List<AuthSchemeModel> authSchemes = List.of();
        if (item.options().isGenerateAuthMetadataHelpers() || item.options().isGenerateAuthInterceptors()) {
            authSchemes = mapAuthSchemes(document);
        }

        // Proxy config.
        ProxyConfigModel proxy = null;
        if (item.options().isEnableProxyGeneration()) {
            String baseUrl = (item.options().getProxyBaseUrl() != null && !item.options().getProxyBaseUrl().isBlank())
                    ? item.options().getProxyBaseUrl()
                    : resolveBaseUrl(document);
            proxy = new ProxyConfigModel(
                    true,
                    baseUrl,
                    item.options().getProxyApiKeyHeaderName(),
                    item.options().getProxyCacheTtlSeconds(),
                    item.options().getProxyBypassCacheMetadataKey(),
                    item.options().getProxyClientKeyHeaderName(),
                    item.options().getProxyClientKeyMode().ordinal(),
                    item.options().isProxyAuditEnabled());
        }

        GrpcModel model = new GrpcModel(
                serviceName,
                ns,
                protoPackage,
                item.hintName(),
                allMessages,
                List.of(new ServiceModel(serviceName, rpcs)),
                allEnums,
                authSchemes,
                proxy);

        return ParseResult.success(item, model, diagnostics);
    }

    private static String deriveServiceName(WorkItem item, OpenAPI document) {
        if (item.serviceName() != null && !item.serviceName().isBlank()) return item.serviceName();
        String stem = item.hintName();
        String title = document.getInfo() != null ? document.getInfo().getTitle() : null;
        if (title != null && !title.isBlank() && ("Service".equals(stem) || stem == null || stem.isBlank())) {
            stem = sanitizePascal(title.split(" ")[0]);
        }
        String baseName = sanitizePascal(stem);
        if (baseName.isEmpty()) baseName = "Service";
        String suffix = item.options().getServiceNameSuffix();
        return baseName.endsWith(suffix) ? baseName : baseName + suffix;
    }

    private static String resolveBaseUrl(OpenAPI document) {
        if (document.getServers() != null && !document.getServers().isEmpty()) {
            String url = document.getServers().get(0).getUrl();
            return url == null ? "" : url;
        }
        return "";
    }

    private static RpcModel mapOperation(
            String path,
            PathItem.HttpMethod method,
            Operation op,
            WorkItem item,
            SchemaMapper schemaMapper,
            List<MessageModel> messages,
            List<String> diagnostics,
            int[] operIndex) {

        String httpMethod = method.name();
        String opId = (op.getOperationId() == null || op.getOperationId().isBlank())
                ? "Op" + (operIndex[0]++) + "_" + httpMethod
                : op.getOperationId();
        String methodName = sanitizePascal(opId);
        if (methodName.isEmpty()) methodName = httpMethod + "Operation";

        String streamingHint = getStreamingHint(op);
        RpcKind kind = RpcKind.UNARY;
        boolean requestStreams = false;
        boolean responseStreams = false;
        if (item.options().isRespectStreamingHints() && streamingHint != null) {
            switch (streamingHint.toLowerCase()) {
                case "client": kind = RpcKind.CLIENT_STREAMING; requestStreams = true; break;
                case "server": kind = RpcKind.SERVER_STREAMING; responseStreams = true; break;
                case "bidi": kind = RpcKind.BIDI_STREAMING; requestStreams = responseStreams = true; break;
                default: kind = RpcKind.UNARY; break;
            }
        }

        // Request message: parameters (path/query/header) + body.
        List<FieldModel> requestFields = new ArrayList<>();
        int[] paramIndex = {0};
        if (op.getParameters() != null) {
            for (Parameter p : op.getParameters()) {
                if (p.getSchema() == null) continue;
                SchemaMapper.MapParameterResult r = schemaMapper.mapParameter(p, item.options(), paramIndex);
                if (r.field() != null) requestFields.add(r.field());
            }
        }

        // Request body.
        boolean hasBody = op.getRequestBody() != null
                && op.getRequestBody().getContent() != null
                && !op.getRequestBody().getContent().isEmpty();
        if (hasBody) {
            Content content = op.getRequestBody().getContent();
            if (content.containsKey("application/octet-stream")) {
                requestFields.add(new FieldModel("Body", "body", ++paramIndex[0], FieldKind.BYTES,
                        "com.google.protobuf.ByteString", "com.google.protobuf.ByteString", false));
            } else {
                Schema<?> bodySchema = content.values().stream()
                        .filter(m -> m.getSchema() != null)
                        .map(MediaType::getSchema)
                        .findFirst().orElse(null);
                if (bodySchema != null) {
                    String refName = schemaMapper.resolveSchemaName(bodySchema);
                    if (refName != null) {
                        requestFields.add(new FieldModel("Body", "body", ++paramIndex[0], FieldKind.MESSAGE, refName, refName, false));
                    } else {
                        MessageModel nested = schemaMapper.mapSchemaToMessage(methodName + "Body", bodySchema, false, false);
                        if (nested != null) {
                            messages.add(nested);
                            requestFields.add(new FieldModel("Body", "body", ++paramIndex[0], FieldKind.MESSAGE, nested.name(), nested.name(), false));
                        }
                    }
                }
            }
            // Large payload + POST -> client-streaming option when hint absent.
            if (item.options().isRespectStreamingHints() && streamingHint == null
                    && "POST".equals(httpMethod)
                    && estimatePayloadSize(op.getRequestBody()) >= item.options().getLargePayloadStreamingThresholdBytes()) {
                kind = RpcKind.CLIENT_STREAMING;
                requestStreams = true;
            }
        }

        if (requestFields.isEmpty()) {
            requestFields.add(new FieldModel("_HasValue", "has_value", 1, FieldKind.BOOL, "boolean"));
        }
        String requestMsgName = methodName + "Request";
        messages.add(new MessageModel(requestMsgName, requestFields, true, false));
        // Response message.
        List<FieldModel> responseFields = new ArrayList<>();
        int[] respIndex = {0};
        ApiResponse successResponse = resolveSuccessResponse(op);
        if (successResponse != null && successResponse.getContent() != null && !successResponse.getContent().isEmpty()) {
            Content content = successResponse.getContent();
            if (content.containsKey("application/octet-stream")) {
                responseFields.add(new FieldModel("Body", "body", ++respIndex[0], FieldKind.BYTES,
                        "com.google.protobuf.ByteString", "com.google.protobuf.ByteString", false));
            } else {
                Schema<?> respSchema = content.values().stream()
                        .filter(m -> m.getSchema() != null)
                        .map(MediaType::getSchema)
                        .findFirst().orElse(null);
                if (respSchema != null) {
                    if ("array".equals(respSchema.getType())) {
                        Schema<?> items = respSchema.getItems();
                        if (items != null && items.get$ref() != null) {
                            String elemName = sanitizePascal(NameSanitizer.refId(items.get$ref()));
                            boolean isEnum = schemaMapper.isEnum(items);
                            responseFields.add(new FieldModel("Items", "items", ++respIndex[0],
                                    isEnum ? FieldKind.ENUM : FieldKind.MESSAGE, elemName, elemName, true));
                        } else if (items != null) {
                            String elemName = schemaMapper.inlineArrayElementName(respSchema, methodName + "Item");
                            MessageModel nested = schemaMapper.mapSchemaToMessage(elemName, items, false, true);
                            if (nested != null && messages.stream().noneMatch(m -> m.name().equals(nested.name()))) {
                                messages.add(nested);
                            }
                            responseFields.add(new FieldModel("Items", "items", ++respIndex[0], FieldKind.MESSAGE, elemName, elemName, true));
                        }
                    } else {
                        String refName = schemaMapper.resolveSchemaName(respSchema);
                        if (refName != null) {
                            responseFields.add(new FieldModel("Data", "data", ++respIndex[0], FieldKind.MESSAGE, refName, refName, false));
                        } else {
                            MessageModel nested = schemaMapper.mapSchemaToMessage(methodName + "Data", respSchema, false, true);
                            if (nested != null) {
                                messages.add(nested);
                                responseFields.add(new FieldModel("Data", "data", ++respIndex[0], FieldKind.MESSAGE, nested.name(), nested.name(), false));
                            }
                        }
                    }
                }
            }
        } else if (successResponse == null) {
            if (item.options().isEmitGoogleRpcStatusWrapper()) {
                String statusName = methodName + "Error";
                List<FieldModel> statusFields = List.of(
                        new FieldModel("Code", "code", 1, FieldKind.INT32, "int"),
                        new FieldModel("Message", "message", 2, FieldKind.STRING, "String"));
                messages.add(new MessageModel(statusName, statusFields, false, true));
                responseFields.add(new FieldModel("Error", "error", ++respIndex[0], FieldKind.MESSAGE, statusName, statusName, false));
            }
        }

        if (responseFields.isEmpty()) {
            responseFields.add(new FieldModel("_HasValue", "has_value", 1, FieldKind.BOOL, "boolean"));
        }
        String responseMsgName = methodName + "Response";
        messages.add(new MessageModel(responseMsgName, responseFields, false, true));

        // Pagination detection: page/limit/cursor/next_page_token.
        boolean hasPagination = false;
        if (item.options().isDetectPagination()) {
            Set<String> paramNames = new HashSet<>();
            for (FieldModel f : requestFields) {
                if (f.protoName() != null && !f.protoName().isEmpty()) paramNames.add(f.protoName());
            }
            hasPagination = paramNames.contains(item.options().getPaginationPageParameter())
                    || paramNames.contains(item.options().getPaginationLimitParameter())
                    || paramNames.contains(item.options().getPaginationCursorParameter())
                    || paramNames.contains(item.options().getPaginationNextPageTokenParameter());
            if (hasPagination && streamingHint == null) {
                if (paramNames.contains(item.options().getPaginationCursorParameter())
                        || paramNames.contains(item.options().getPaginationNextPageTokenParameter())) {
                    kind = RpcKind.SERVER_STREAMING;
                    responseStreams = true;
                }
            }
        }

        return new RpcModel(
                methodName,
                path,
                httpMethod,
                requestMsgName,
                responseMsgName,
                kind,
                hasPagination,
                requestStreams ? requestMsgName : null,
                responseStreams ? responseMsgName : null);
    }
    private static String getStreamingHint(Operation op) {
        if (op.getExtensions() == null) return null;
        Object ext = op.getExtensions().get(STREAMING_HINT_EXTENSION);
        return ext instanceof String s ? s : null;
    }

    private static long estimatePayloadSize(RequestBody body) {
        long total = 0;
        if (body.getContent() == null) return 0;
        for (MediaType media : body.getContent().values()) {
            if (media.getSchema() != null && "object".equals(media.getSchema().getType())
                    && media.getSchema().getProperties() != null) {
                total += media.getSchema().getProperties().size() * 64L;
            } else {
                total += 256;
            }
        }
        return total;
    }

    private static ApiResponse resolveSuccessResponse(Operation op) {
        if (op.getResponses() == null || op.getResponses().isEmpty()) return null;
        for (String code : new String[]{"200", "201", "204"}) {
            ApiResponse r = op.getResponses().get(code);
            if (r != null) return r;
        }
        return op.getResponses().get("default");
    }

    private static List<AuthSchemeModel> mapAuthSchemes(OpenAPI document) {
        if (document.getComponents() == null || document.getComponents().getSecuritySchemes() == null
                || document.getComponents().getSecuritySchemes().isEmpty()) {
            return List.of();
        }
        List<AuthSchemeModel> result = new ArrayList<>();
        for (Map.Entry<String, SecurityScheme> entry : document.getComponents().getSecuritySchemes().entrySet()) {
            String name = entry.getKey();
            SecurityScheme scheme = entry.getValue();
            if (scheme == null) continue;
            if (scheme.getType() == SecurityScheme.Type.HTTP && "bearer".equalsIgnoreCase(scheme.getScheme())) {
                result.add(new AuthSchemeModel(name, AuthKind.BEARER, null, null));
            } else if (scheme.getType() == SecurityScheme.Type.APIKEY) {
                String headerName = scheme.getIn() == SecurityScheme.In.HEADER ? scheme.getName() : null;
                String queryName = scheme.getIn() == SecurityScheme.In.QUERY ? scheme.getName() : null;
                result.add(new AuthSchemeModel(name, AuthKind.API_KEY, headerName, queryName));
            } else if (scheme.getType() == SecurityScheme.Type.OAUTH2 || scheme.getType() == SecurityScheme.Type.OPENIDCONNECT) {
                result.add(new AuthSchemeModel(name, AuthKind.OAUTH2, null, null));
            } else {
                result.add(new AuthSchemeModel(name, AuthKind.UNKNOWN, null, null));
            }
        }
        return result;
    }
}

