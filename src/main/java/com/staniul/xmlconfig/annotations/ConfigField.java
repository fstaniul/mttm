package com.staniul.xmlconfig.annotations;

import com.staniul.xmlconfig.convert.NullTypeConverter;
import com.staniul.xmlconfig.convert.StringToTypeConverter;

import java.lang.annotation.*;

/**
 * Fields annotated with this annotation will have different config load. Instead of taking fields name value of this annotation
 * will be taken to retrieve field from configuration.
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ConfigField {
    String value();
    Class<? extends StringToTypeConverter<?>> converter () default NullTypeConverter.class;
}
