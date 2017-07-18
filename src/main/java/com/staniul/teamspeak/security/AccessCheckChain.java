package com.staniul.teamspeak.security;

import java.util.Arrays;
import java.util.List;

/**
 * Chain of access checks for object of a specified type.
 * @param <T> type of object that is checked in access checks in this chain.
 */
public class AccessCheckChain <T> implements AccessCheck <T> {
    private List<AccessCheck<T>> accessCheckList;

    @SafeVarargs
    public AccessCheckChain (AccessCheck<T>... accessChecks) {
        accessCheckList = Arrays.asList(accessChecks);
    }

    @Override
    public Boolean apply(T t) {
        for (AccessCheck<T> accessCheck : accessCheckList)
            if (!accessCheck.apply(t))
                return false;

        return true;
    }
}
