package io.github.mpcoredeveloper.javaportico.emit;

import io.github.mpcoredeveloper.javaportico.mapping.NameSanitizer;
import io.github.mpcoredeveloper.javaportico.model.EnumModel;
import io.github.mpcoredeveloper.javaportico.model.EnumValueModel;
import io.github.mpcoredeveloper.javaportico.model.FieldKind;
import io.github.mpcoredeveloper.javaportico.model.FieldModel;
import io.github.mpcoredeveloper.javaportico.model.GrpcModel;
import io.github.mpcoredeveloper.javaportico.model.MessageModel;
import io.github.mpcoredeveloper.javaportico.model.RpcKind;
import io.github.mpcoredeveloper.javaportico.model.RpcModel;
import io.github.mpcoredeveloper.javaportico.model.ServiceModel;
import io.github.mpcoredeveloper.javaportico.model.WellKnownTypes;

import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Emits a proto3 descriptor for a {@link GrpcModel}. Ported from SharpPortico's ProtoEmitter.
 */
public final class ProtoEmitter {

    private ProtoEmitter() {
    }

    public static String emit(GrpcModel model) {
        CodeWriter w = new CodeWriter();
        w.line("syntax = \"proto3\";");
        w.line();
        w.line("package " + model.protoPackage() + ";");
        w.line();
        emitImports(w, model);
        w.line("option java_package = \"" + model.namespace() + "\";");
        w.line("option java_multiple_files = true;");
        w.line();
        emitEnums(w, model.enums());
        emitMessages(w, model.messages());
        emitServices(w, model.services());
        return w.toString();
    }

    /** Emits the imports the model's fields require, alphabetically ordered and without duplicates. */
    private static void emitImports(CodeWriter w, GrpcModel model) {
        List<String> imports = requiredImports(model);
        for (String path : imports) {
            w.line("import \"" + path + "\";");
        }
        if (!imports.isEmpty()) w.line();
    }

    /**
     * The imports the model's fields require.
     *
     * <p>A field of a well-known type without the import that declares it produces a descriptor protoc
     * refuses to compile, so the imports are derived from the model rather than assumed.
     * </p>
     */
    private static List<String> requiredImports(GrpcModel model) {
        SortedSet<String> imports = new TreeSet<>();
        for (MessageModel msg : model.messages()) {
            for (FieldModel f : msg.fields()) {
                // A message field with no resolved type name is emitted as google.protobuf.Empty, so that is the type
                // whose import it needs.
                String referenced = switch (f.kind()) {
                    case TIMESTAMP -> WellKnownTypes.PROTO_TIMESTAMP;
                    case MESSAGE -> f.typeName() != null ? f.typeName() : WellKnownTypes.PROTO_EMPTY;
                    default -> null;
                };
                if (referenced == null) continue;
                String path = WellKnownTypes.protoImport(referenced);
                if (path != null) imports.add(path);
            }
        }
        return List.copyOf(imports);
    }

    private static void emitEnums(CodeWriter w, List<EnumModel> enums) {
        for (EnumModel enumModel : enums) {
            w.block("enum " + enumModel.name(), () -> {
                for (EnumValueModel v : enumModel.values()) {
                    w.line(NameSanitizer.toProtoEnumName(v.name()) + " = " + v.number() + ";");
                }
            });
            w.line();
        }
    }

    private static void emitMessages(CodeWriter w, List<MessageModel> messages) {
        for (MessageModel msg : messages) {
            w.block("message " + msg.name(), () -> {
                for (FieldModel f : msg.fields()) {
                    String keyword = f.repeated() ? "repeated " : "";
                    w.line(keyword + protoType(f) + " " + f.protoName() + " = " + f.number() + ";");
                }
            });
            w.line();
        }
    }

    private static void emitServices(CodeWriter w, List<ServiceModel> services) {
        for (ServiceModel svc : services) {
            w.block("service " + svc.name(), () -> {
                for (RpcModel rpc : svc.rpcMethods()) {
                    String reqStream = (rpc.kind() == RpcKind.CLIENT_STREAMING || rpc.kind() == RpcKind.BIDI_STREAMING)
                            ? "stream " : "";
                    String respStream = (rpc.kind() == RpcKind.SERVER_STREAMING || rpc.kind() == RpcKind.BIDI_STREAMING)
                            ? "stream " : "";
                    w.line("rpc " + rpc.name() + " (" + reqStream + rpc.requestType() + ") returns ("
                            + respStream + rpc.responseType() + ");");
                }
            });
            w.line();
        }
    }

    private static String protoType(FieldModel f) {
        return switch (f.kind()) {
            case STRING -> "string";
            case INT32 -> "int32";
            case INT64 -> "int64";
            case UINT32 -> "uint32";
            case UINT64 -> "uint64";
            case FLOAT -> "float";
            case DOUBLE -> "double";
            case BOOL -> "bool";
            case BYTES -> "bytes";
            case ENUM -> f.typeName() != null ? f.typeName() : "int32";
            case MESSAGE -> f.typeName() != null ? f.typeName() : WellKnownTypes.PROTO_EMPTY;
            case TIMESTAMP -> WellKnownTypes.PROTO_TIMESTAMP;
        };
    }
}
