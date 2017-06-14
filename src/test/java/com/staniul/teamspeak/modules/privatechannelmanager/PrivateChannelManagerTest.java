package com.staniul.teamspeak.modules.privatechannelmanager;

import org.junit.Test;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PrivateChannelManagerTest {

    @Test
    public void testTheMoveName () throws Exception {
        System.out.println(testMoveName("[002] no wez MOVE kurwa mac kanal"));
    }

    private List<String> testMoveName (String name) throws Exception {
        List<String> result = new LinkedList<>();

        Pattern pattern = Pattern.compile(".*MOVE ([0-9]+)?.*");
        Matcher matcher = pattern.matcher(name);
        if (matcher.find()) {
            System.out.println(matcher.groupCount());
            int groupsNumber = matcher.groupCount();
            for (int  i = 0; i <= groupsNumber; i++)
                result.add(matcher.group(i));
        }

        return  result;
    }
}