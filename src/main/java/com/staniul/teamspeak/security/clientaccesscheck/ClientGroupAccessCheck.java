package com.staniul.teamspeak.security.clientaccesscheck;

import com.staniul.teamspeak.query.Client;
import com.staniul.teamspeak.security.AccessCheck;
import org.apache.log4j.Logger;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Set;

/**
 * Abstract class that defines group access check. Has one protected field that should be used in subclasses to determine
 * if client has access to resource. The class needs to have a constructor with Set of Integers present so it can be created
 * by system.
 */
public abstract class ClientGroupAccessCheck implements AccessCheck<Client> {
    private static Logger log = Logger.getLogger(ClientGroupAccessCheck.class);

    /**
     * Creates {@code ClientGroupAccessCheck} based on a subclass of {@code ClientGroupAccessCheck} with groups that are allowed
     * by that resource.
     * @param groups Groups that can access the resource.
     * @param aClass Subclass of {@code ClientGroupAccessCheck}
     * @return Instantiated object of given subclass.
     */
    public static ClientGroupAccessCheck create (Set<Integer> groups, Class<? extends ClientGroupAccessCheck> aClass) {
        if (groups.size() == 0) return new ClientPermitAllAccessCheck();

        try {
            Constructor<? extends ClientGroupAccessCheck> constructor = aClass.getConstructor(Set.class);
            return constructor.newInstance(groups);
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
