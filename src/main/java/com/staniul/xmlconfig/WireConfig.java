package com.staniul.xmlconfig;

import java.lang.annotation.*;

/**
 * Fields annotated with this annotation should be of type: CustomXMLConfiguration or one of its super classes interfaces.
 * Fields with this annotation will have configuration injected into it after the bean has been created.
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface WireConfig {
}
