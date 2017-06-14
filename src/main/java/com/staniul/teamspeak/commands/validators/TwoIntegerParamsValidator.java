package com.staniul.teamspeak.commands.validators;

import com.staniul.validation.Validator;

public class TwoIntegerParamsValidator implements Validator<String> {
    @Override
    public boolean validate(String element) {
        String[] spl = element.split("\\s+");
        if (spl.length == 2) {
            Validator<String> valid = new IntegerParamsValidator();

            for (String s : spl)
                if (!valid.validate(s))
                    return false;

            return true;
        }

        return false;
    }
}
