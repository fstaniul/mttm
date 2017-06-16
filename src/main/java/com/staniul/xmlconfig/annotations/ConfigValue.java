package com.staniul.xmlconfig.annotations;

import com.staniul.xmlconfig.convert.NullTypeConverter;
import com.staniul.xmlconfig.convert.StringToTypeConverter;

import java.lang.annotation.*;

/**
 * Fields annotated with this annotation will have configuration entry injected into them after
 * bean creation.
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ConfigValue {
    String value();
    Class<? extends StringToTypeConverter<?>> converter () default NullTypeConverter.class;
}
