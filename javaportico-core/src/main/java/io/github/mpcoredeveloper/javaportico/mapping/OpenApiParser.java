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
    private static final String BYTES_JAVA_TYPE = "com.google.protobuf.ByteString";
    private static final String SCHEMA_TYPE_ARRAY = "array";

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
        mapComponentSchemas(document, schemaMapper, allMessages);
        List<EnumModel> allEnums = new ArrayList<>(schemaMapper.allEnums());

        // Map paths -> RPCs + request/response messages.
        List<RpcModel> rpcs = mapPathsToRpcs(document, item, schemaMapper, allMessages);

        // Auth schemes.
        List<AuthSchemeModel> authSchemes = List.of();
        if (item.options().isGenerateAuthMetadataHelpers() || item.options().isGenerateAuthInterceptors()) {
            authSchemes = mapAuthSchemes(document);
        }

        // Proxy config.
        ProxyConfigModel proxy = buildProxyConfig(item, document);

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

        return ParseResult.success(item, model, new ArrayList<>());
    }

    /** Maps each component schema to an enum or message and registers it on the mapper. */
    private static void mapComponentSchemas(OpenAPI document, SchemaMapper schemaMapper, List<MessageModel> allMessages) {
        if (document.getComponents() == null || document.getComponents().getSchemas() == null) return;
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

    /** Iterates the OpenAPI paths and maps every HTTP operation to an {@link RpcModel}. */
    private static List<RpcModel> mapPathsToRpcs(OpenAPI document, WorkItem item, SchemaMapper schemaMapper,
            List<MessageModel> messages) {
        List<RpcModel> rpcs = new ArrayList<>();
        int[] operIndex = {0};
        if (document.getPaths() != null) {
            for (Map.Entry<String, PathItem> entry : document.getPaths().entrySet()) {
                String path = entry.getKey();
                PathItem pathItem = entry.getValue();
                if (pathItem == null) continue;
                for (Map.Entry<PathItem.HttpMethod, Operation> opEntry : pathItem.readOperationsMap().entrySet()) {
                    RpcModel rpc = mapOperation(path, opEntry.getKey(), opEntry.getValue(),
                            item, schemaMapper, messages, operIndex);
                    if (rpc != null) rpcs.add(rpc);
                }
            }
        }
        return rpcs;
    }

    private static ProxyConfigModel buildProxyConfig(WorkItem item, OpenAPI document) {
        if (!item.options().isEnableProxyGeneration()) return null;
        String baseUrl = (item.options().getProxyBaseUrl() != null && !item.options().getProxyBaseUrl().isBlank())
                ? item.options().getProxyBaseUrl()
                : resolveBaseUrl(document);
        return new ProxyConfigModel(
                true,
                baseUrl,
                item.options().getProxyApiKeyHeaderName(),
                item.options().getProxyCacheTtlSeconds(),
                item.options().getProxyBypassCacheMetadataKey(),
                item.options().getProxyClientKeyHeaderName(),
                item.options().getProxyClientKeyMode().ordinal(),
                item.options().isProxyAuditEnabled());
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
            int[] operIndex) {

        String httpMethod = method.name();
        String methodName = operationName(op, httpMethod, operIndex);
        String streamingHint = getStreamingHint(op);
        StreamState state = new StreamState();
        if (item.options().isRespectStreamingHints() && streamingHint != null) {
            state.applyHint(streamingHint);
        }

        // Request message: parameters (path/query/header) + body.
        List<FieldModel> requestFields = new ArrayList<>();
        int[] paramIndex = {0};
        collectRequestParameters(op, item, schemaMapper, requestFields, paramIndex);
        if (hasRequestBody(op)) {
            mapRequestBody(op, schemaMapper, methodName, requestFields, messages, paramIndex);
            maybeEnableClientStreaming(op, item, httpMethod, streamingHint, state);
        }
        if (requestFields.isEmpty()) {
            requestFields.add(new FieldModel("_HasValue", "has_value", 1, FieldKind.BOOL, "boolean"));
        }
        String requestMsgName = methodName + "Request";
        messages.add(new MessageModel(requestMsgName, requestFields, true, false));
        // Response message.
        List<FieldModel> responseFields = new ArrayList<>();
        int[] respIndex = {0};
        mapResponseBody(op, item, schemaMapper, methodName, responseFields, messages, respIndex);
        if (responseFields.isEmpty()) {
            responseFields.add(new FieldModel("_HasValue", "has_value", 1, FieldKind.BOOL, "boolean"));
        }
        String responseMsgName = methodName + "Response";
        messages.add(new MessageModel(responseMsgName, responseFields, false, true));

        // Pagination detection: page/limit/cursor/next_page_token.
        boolean hasPagination = detectPagination(item, requestFields, streamingHint, state);

        return new RpcModel(
                methodName,
                path,
                httpMethod,
                requestMsgName,
                responseMsgName,
                state.kind(),
                hasPagination,
                state.requestStreaming() ? requestMsgName : null,
                state.responseStreaming() ? responseMsgName : null);
    }
    private static String operationName(Operation op, String httpMethod, int[] operIndex) {
        String opId = (op.getOperationId() == null || op.getOperationId().isBlank())
                ? "Op" + (operIndex[0]++) + "_" + httpMethod
                : op.getOperationId();
        String methodName = sanitizePascal(opId);
        return methodName.isEmpty() ? httpMethod + "Operation" : methodName;
    }

    private static boolean hasRequestBody(Operation op) {
        return op.getRequestBody() != null
                && op.getRequestBody().getContent() != null
                && !op.getRequestBody().getContent().isEmpty();
    }

    private static void collectRequestParameters(Operation op, WorkItem item, SchemaMapper schemaMapper,
            List<FieldModel> requestFields, int[] paramIndex) {
        if (op.getParameters() == null) return;
        for (Parameter p : op.getParameters()) {
            if (p.getSchema() == null) continue;
            SchemaMapper.MapParameterResult r = schemaMapper.mapParameter(p, item.options(), paramIndex);
            if (r.field() != null) requestFields.add(r.field());
        }
    }

    private static void mapRequestBody(Operation op, SchemaMapper schemaMapper, String methodName,
            List<FieldModel> requestFields, List<MessageModel> messages, int[] paramIndex) {
        Content content = op.getRequestBody().getContent();
        if (content.containsKey("application/octet-stream")) {
            requestFields.add(new FieldModel("Body", "body", ++paramIndex[0], FieldKind.BYTES,
                    BYTES_JAVA_TYPE, BYTES_JAVA_TYPE, false));
            return;
        }
        Schema<?> bodySchema = content.values().stream()
                .filter(m -> m.getSchema() != null)
                .map(MediaType::getSchema)
                .findFirst().orElse(null);
        if (bodySchema == null) return;
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

    /** Large payload + POST -> client-streaming option when no streaming hint is present. */
    private static void maybeEnableClientStreaming(Operation op, WorkItem item, String httpMethod,
            String streamingHint, StreamState state) {
        if (item.options().isRespectStreamingHints() && streamingHint == null
                && "POST".equals(httpMethod)
                && estimatePayloadSize(op.getRequestBody()) >= item.options().getLargePayloadStreamingThresholdBytes()) {
            state.clientStreaming();
        }
    }

    private static void mapResponseBody(Operation op, WorkItem item, SchemaMapper schemaMapper, String methodName,
            List<FieldModel> responseFields, List<MessageModel> messages, int[] respIndex) {
        ApiResponse successResponse = resolveSuccessResponse(op);
        if (successResponse == null) {
            mapGoogleRpcErrorStatus(item, methodName, responseFields, messages, respIndex);
            return;
        }
        if (successResponse.getContent() == null || successResponse.getContent().isEmpty()) return;
        Content content = successResponse.getContent();
        if (content.containsKey("application/octet-stream")) {
            responseFields.add(new FieldModel("Body", "body", ++respIndex[0], FieldKind.BYTES,
                    BYTES_JAVA_TYPE, BYTES_JAVA_TYPE, false));
            return;
        }
        Schema<?> respSchema = content.values().stream()
                .filter(m -> m.getSchema() != null)
                .map(MediaType::getSchema)
                .findFirst().orElse(null);
        if (respSchema == null) return;
        if (SCHEMA_TYPE_ARRAY.equals(respSchema.getType())) {
            mapArrayResponseSchema(respSchema, schemaMapper, methodName, responseFields, messages, respIndex);
        } else {
            mapDataResponseSchema(respSchema, schemaMapper, methodName, responseFields, messages, respIndex);
        }
    }

    private static void mapGoogleRpcErrorStatus(WorkItem item, String methodName,
            List<FieldModel> responseFields, List<MessageModel> messages, int[] respIndex) {
        if (!item.options().isEmitGoogleRpcStatusWrapper()) return;
        String statusName = methodName + "Error";
        List<FieldModel> statusFields = List.of(
                new FieldModel("Code", "code", 1, FieldKind.INT32, "int"),
                new FieldModel("Message", "message", 2, FieldKind.STRING, "String"));
        messages.add(new MessageModel(statusName, statusFields, false, true));
        responseFields.add(new FieldModel("Error", "error", ++respIndex[0], FieldKind.MESSAGE, statusName, statusName, false));
    }

    private static void mapArrayResponseSchema(Schema<?> respSchema, SchemaMapper schemaMapper, String methodName,
            List<FieldModel> responseFields, List<MessageModel> messages, int[] respIndex) {
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
    }

    private static void mapDataResponseSchema(Schema<?> respSchema, SchemaMapper schemaMapper, String methodName,
            List<FieldModel> responseFields, List<MessageModel> messages, int[] respIndex) {
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

    /** Detects pagination parameters and upgrades cursor pagination to server streaming. */
    private static boolean detectPagination(WorkItem item, List<FieldModel> requestFields,
            String streamingHint, StreamState state) {
        if (!item.options().isDetectPagination()) return false;
        Set<String> paramNames = new HashSet<>();
        for (FieldModel f : requestFields) {
            if (f.protoName() != null && !f.protoName().isEmpty()) paramNames.add(f.protoName());
        }
        boolean hasPagination = paramNames.contains(item.options().getPaginationPageParameter())
                || paramNames.contains(item.options().getPaginationLimitParameter())
                || paramNames.contains(item.options().getPaginationCursorParameter())
                || paramNames.contains(item.options().getPaginationNextPageTokenParameter());
        if (hasPagination && streamingHint == null
                && (paramNames.contains(item.options().getPaginationCursorParameter())
                    || paramNames.contains(item.options().getPaginationNextPageTokenParameter()))) {
            state.serverStreaming();
        }
        return hasPagination;
    }

    /** Mutable streaming flags resolved across an operation's mapping phases. */
    private static final class StreamState {
        private RpcKind kind = RpcKind.UNARY;
        private boolean requestStreams;
        private boolean responseStreams;

        void applyHint(String hint) {
            switch (hint.toLowerCase()) {
                case "client" -> { kind = RpcKind.CLIENT_STREAMING; requestStreams = true; }
                case "server" -> { kind = RpcKind.SERVER_STREAMING; responseStreams = true; }
                case "bidi" -> { kind = RpcKind.BIDI_STREAMING; requestStreams = true; responseStreams = true; }
                default -> { kind = RpcKind.UNARY; }
            }
        }

        void clientStreaming() {
            kind = RpcKind.CLIENT_STREAMING;
            requestStreams = true;
        }

        void serverStreaming() {
            kind = RpcKind.SERVER_STREAMING;
            responseStreams = true;
        }

        RpcKind kind() {
            return kind;
        }

        boolean requestStreaming() {
            return requestStreams;
        }

        boolean responseStreaming() {
            return responseStreams;
        }
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
            SecurityScheme scheme = entry.getValue();
            if (scheme != null) {
                result.add(toAuthSchemeModel(entry.getKey(), scheme));
            }
        }
        return result;
    }

    private static AuthSchemeModel toAuthSchemeModel(String name, SecurityScheme scheme) {
        AuthKind kind;
        String headerName = null;
        String queryName = null;
        switch (scheme.getType()) {
            case HTTP -> kind = "bearer".equalsIgnoreCase(scheme.getScheme()) ? AuthKind.BEARER : AuthKind.UNKNOWN;
            case APIKEY -> {
                kind = AuthKind.API_KEY;
                if (scheme.getIn() == SecurityScheme.In.HEADER) headerName = scheme.getName();
                if (scheme.getIn() == SecurityScheme.In.QUERY) queryName = scheme.getName();
            }
            case OAUTH2, OPENIDCONNECT -> kind = AuthKind.OAUTH2;
            default -> kind = AuthKind.UNKNOWN;
        }
        return new AuthSchemeModel(name, kind, headerName, queryName);
    }
}

