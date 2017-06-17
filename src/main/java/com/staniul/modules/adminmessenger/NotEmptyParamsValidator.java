package com.staniul.modules.adminmessenger;

import com.staniul.util.validation.Validator;

public class NotEmptyParamsValidator implements Validator<String> {
    @Override
    public boolean validate(String element) {
        return !"".equals(element);
    }
}
