package com.staniul.teamspeak.commands.validators;

import com.staniul.util.validation.Validator;

import java.util.regex.Pattern;

public class TwoIntegerParamsValidator implements Validator<String> {
    public static Pattern getPattern () {
        return Pattern.compile("^(\\d+)[ \t]+(\\d+)$");
    }

    @Override
    public boolean validate(String element) {
        return getPattern().matcher(element).matches();
    }
}
