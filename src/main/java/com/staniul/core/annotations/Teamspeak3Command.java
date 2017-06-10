package com.staniul.core.annotations;

import com.staniul.core.security.clientaccesscheck.ClientAccessCheck;
import com.staniul.core.security.clientaccesscheck.PermitAllClientAccessCheck;

public @interface Teamspeak3Command {
    /**
     * Name of command that can be called by teamspeak 3 users.
     */
    String command();

    /**
     * Groups that should be allowed to. checkClass should be specified.
     * Failing to specify checkClass will result in default one being used.
     */
    int[] groups() default {};

    /**
     * Class that is used to check access for this command with given groups. Groups should be specified.
     * Failing to specify groups will result in default class being used.
     */
    Class<? extends ClientAccessCheck> checkClass () default PermitAllClientAccessCheck.class;
}
