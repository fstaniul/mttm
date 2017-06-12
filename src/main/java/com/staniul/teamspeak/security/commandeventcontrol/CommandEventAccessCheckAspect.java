package com.staniul.teamspeak.security.commandeventcontrol;

import com.staniul.query.Client;
import com.staniul.teamspeak.commands.CommandExecutionStatus;
import com.staniul.teamspeak.commands.CommandResponse;
import com.staniul.teamspeak.commands.Teamspeak3Command;
import com.staniul.teamspeak.security.clientaccesscheck.ClientGroupAccess;
import com.staniul.teamspeak.security.clientaccesscheck.ClientGroupAccessCheck;
import com.staniul.util.AroundAspectUtil;
import org.apache.log4j.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Aspect which controls command invocation by checking if client has access to command based on {@link Teamspeak3Command} annotation.
 */
@Component
@Aspect
public class CommandEventAccessCheckAspect {
    private static Logger log = Logger.getLogger(CommandEventAccessCheckAspect.class);

    @Pointcut(value = "execution(com.staniul.teamspeak.commands.CommandResponse * (com.staniul.query.Client, ..)) && " +
            "@annotation(com.staniul.teamspeak.commands.Teamspeak3Command) && " +
            "args(client, ..)", argNames = "client")
    public void invokeCommand(Client client) {
    }

    @Pointcut("execution(* * (com.staniul.query.Client)) && " +
            "@annotation(com.staniul.teamspeak.events.Teamspeak3Event) && " +
            "args(client)")
    public void joinEvent (Client client) {
    }

    @Around(value = "invokeCommand(client)", argNames = "pjp,client")
    public Object checkCommandClientAccess(ProceedingJoinPoint pjp, Client client) throws Throwable {
        boolean access = checkClientAccess(pjp, client);

        if (access)
            return pjp.proceed();

        return new CommandResponse(CommandExecutionStatus.ACCESS_DENIED, null);
    }

    @Around(value = "joinEvent(client)", argNames = "pjp,client")
    public Object checkEventClientAccess (ProceedingJoinPoint pjp, Client client) throws Throwable {
        boolean access = checkClientAccess(pjp, client);

        if (access)
            return pjp.proceed();

        return null;
    }

    private boolean checkClientAccess (ProceedingJoinPoint pjp, Client client) {
        List<ClientGroupAccess> anns = AroundAspectUtil.getAnnotationsOfAspectMethod(pjp, ClientGroupAccess.class);
        boolean access = true;
        for (ClientGroupAccess ann : anns) {
            ClientGroupAccessCheck check = ClientGroupAccessCheck.create(ann.value(), ann.check());
            if (check == null || !check.apply(client))
                access = false;
        }

        return access;
    }
}
