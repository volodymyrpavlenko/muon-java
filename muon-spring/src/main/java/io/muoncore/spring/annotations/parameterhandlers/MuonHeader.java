package io.muoncore.spring.annotations.parameterhandlers;

import java.lang.annotation.*;

/**
 * Parameter, annotated by this annotation, would get a single
 * Muon request header. Header name is provided in the value attribute.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MuonHeader {
    /**
     * Muon header name
     *
     */
    String value();
}
