package io.github.mpcoredeveloper.javaportico.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Container annotation for repeatable {@link OpenApiToGrpc} declarations. */
@Documented
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.PACKAGE)
public @interface OpenApiToGrpcs {
    OpenApiToGrpc[] value();
}
