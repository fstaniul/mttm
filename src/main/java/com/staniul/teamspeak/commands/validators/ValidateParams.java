package com.staniul.teamspeak.commands.validators;

import com.staniul.validation.Validator;

import java.lang.annotation.*;

/**
 * Commands annotated with this annotation will have its parameters checked before the command can be invoked.
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(ValidateParamsContainer.class)
public @interface ValidateParams {
    /**
     * Class used to validate parameters of command.
     */
    Class<? extends Validator<String>> value();
}
