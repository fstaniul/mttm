package com.staniul.teamspeak.commands.validators;

import com.staniul.teamspeak.commands.validators.ValidateParams;

import java.lang.annotation.*;

/**
 * Container for repeatable {@code @ValidateParams} annotation.
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidateParamsContainer {
    ValidateParams[] value();
}
