package com.staniul.teamspeak.commands.validators;

import com.staniul.util.validation.Validator;

public class IntegerParamsValidator implements Validator<String> {
    @Override
    public boolean validate(String element) {
        return element.matches("^-?[1-9]\\d*$");
    }
}
