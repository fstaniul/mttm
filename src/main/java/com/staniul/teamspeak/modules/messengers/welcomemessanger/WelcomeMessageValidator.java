package com.staniul.teamspeak.modules.messengers.welcomemessanger;

import com.staniul.util.validation.Validator;

import java.util.regex.Pattern;

public class WelcomeMessageValidator implements Validator<String> {
    /**
     * Groups:
     * 1 - Servergroups separated by single comma ","
     * 4 - Message text
     * @return Compiled Pattern to check and extract groups and message from a welcome message.
     */
    public static Pattern getPattern() {
        return Pattern.compile("\\{((\\d+(,(?!}))?)+)}[ \\t]+(.*)");
    }

    @Override
    public boolean validate(String element) {
        return getPattern().matcher(element).matches();
    }
}
