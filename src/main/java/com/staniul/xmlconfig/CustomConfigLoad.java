package com.staniul.xmlconfig;

import java.lang.annotation.*;

/**
 * Classes annotated with this annotation have a custom config loading.<br>
 * You can specify different name that will occur after cls: in config. "cls" stands for class.<br>
 * If value is missing then {@code Class.getSimpleName().toLowerCase()} is used.
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface CustomConfigLoad {
    /**
     * Custom name for configuration, that will appear after cls. All custom classes xml entries needs to start with cls.
     */
    String value() default "";
}
