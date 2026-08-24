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
import io.github.mpcoredeveloper.javaportico.model.WorkItem;

/**
 * Emits a proto3 descriptor for a {@link GrpcModel}. Ported from SharpPortico's ProtoEmitter.
 */
public final class ProtoEmitter {

    private ProtoEmitter() {
    }

    public static String emit(GrpcModel model, WorkItem item) {
        CodeWriter w = new CodeWriter();
        w.line("syntax = \"proto3\";");
        w.line();
        w.line("package " + model.protoPackage() + ";");
        w.line();
        w.line("option java_package = \"" + model.namespace() + "\";");
        w.line("option java_multiple_files = true;");
        w.line();

        boolean needsTimestamp = model.messages().stream()
                .flatMap(m -> m.fields().stream())
                .anyMatch(f -> f.kind() == FieldKind.TIMESTAMP);
        if (needsTimestamp) {
            w.line("import \"google/protobuf/timestamp.proto\";");
            w.line();
        }

        for (EnumModel enumModel : model.enums()) {
            w.block("enum " + enumModel.name(), () -> {
                for (EnumValueModel v : enumModel.values()) {
                    w.line(NameSanitizer.toProtoEnumName(v.name()) + " = " + v.number() + ";");
                }
            });
            w.line();
        }

        for (MessageModel msg : model.messages()) {
            w.block("message " + msg.name(), () -> {
                for (FieldModel f : msg.fields()) {
                    String keyword = f.repeated() ? "repeated " : "";
                    String type = protoType(f);
                    w.line(keyword + type + " " + f.protoName() + " = " + f.number() + ";");
                }
            });
            w.line();
        }

        for (ServiceModel svc : model.services()) {
            w.block("service " + svc.name(), () -> {
                for (RpcModel rpc : svc.rpcMethods()) {
                    String reqStream = (rpc.kind() == RpcKind.CLIENT_STREAMING || rpc.kind() == RpcKind.BIDI_STREAMING)
                            ? "stream " : "";
                    String respStream = (rpc.kind() == RpcKind.SERVER_STREAMING || rpc.kind() == RpcKind.BIDI_STREAMING)
                            ? "stream " : "";
                    w.line("rpc " + rpc.name() + " (" + reqStream + rpc.requestType() + ") returns (" + respStream + rpc.responseType() + ");");
                }
            });
            w.line();
        }

        return w.toString();
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
            case MESSAGE -> f.typeName() != null ? f.typeName() : "google.protobuf.Empty";
            case TIMESTAMP -> "google.protobuf.Timestamp";
        };
    }
}
