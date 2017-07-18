package com.staniul.teamspeak.modules.messengers;

import com.staniul.util.validation.Validator;

public class NotEmptyParamsValidator implements Validator<String> {
    @Override
    public boolean validate(String element) {
        return !"".equals(element);
    }
}
