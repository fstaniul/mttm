package com.staniul.teamspeak.security.clientaccesscheck;

import com.staniul.query.Client;
import com.staniul.teamspeak.security.AccessCheck;
import com.staniul.util.SetUtil;
import org.apache.log4j.Logger;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public abstract class ClientGroupAccessCheck implements AccessCheck<Client> {
    private static Logger log = Logger.getLogger(ClientGroupAccessCheck.class);

    public static ClientGroupAccessCheck create (int[] groups, Class<? extends ClientGroupAccessCheck> aClass) {
        Set<Integer> groupSet = SetUtil.intSet(groups);
        try {
            Constructor<? extends ClientGroupAccessCheck> constructor = aClass.getConstructor(Set.class);
            return constructor.newInstance(groupSet);
        } catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
            log.error("Failed to create a client group access check from given class!", e);
            return null;
        }
    }

    protected Set<Integer> groups;

    public ClientGroupAccessCheck (Set<Integer> groups) {
        this.groups = groups;
    }
}
