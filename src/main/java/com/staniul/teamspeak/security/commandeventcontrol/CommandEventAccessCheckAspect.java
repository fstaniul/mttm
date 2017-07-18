package com.staniul.teamspeak.security.commandeventcontrol;

import com.staniul.teamspeak.security.clientaccesscheck.ClientGroupAccess;
import com.staniul.teamspeak.security.clientaccesscheck.ClientGroupAccessCheck;
import com.staniul.teamspeak.commands.CommandExecutionStatus;
import com.staniul.teamspeak.commands.CommandMessenger;
import com.staniul.teamspeak.commands.CommandResponse;
import com.staniul.teamspeak.commands.Teamspeak3Command;
import com.staniul.teamspeak.query.Client;
import com.staniul.util.spring.AroundAspectUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Aspect which controls command invocation by checking if client has access to command based on {@link Teamspeak3Command} annotation.
 */
@Component
@Aspect
@Order(0)
public class CommandEventAccessCheckAspect {
    private static Logger log = LogManager.getLogger(CommandEventAccessCheckAspect.class);

    private Environment environment;
    private CommandMessenger commandMessenger;

    @Autowired
    public CommandEventAccessCheckAspect (Environment environment, CommandMessenger commandMessenger) {
        this.environment = environment;
        this.commandMessenger = commandMessenger;
    }

    @Pointcut(value = "execution(com.staniul.teamspeak.commands.CommandResponse * (com.staniul.teamspeak.query.Client, ..)) && " +
            "@annotation(com.staniul.teamspeak.commands.Teamspeak3Command) && @annotation(com.staniul.teamspeak.security.clientaccesscheck.ClientGroupAccess) && " +
            "args(client, ..)")
    public void invokeCommand(Client client) {
    }

    @Pointcut("execution(public void * (com.staniul.teamspeak.query.Client)) && " +
            "@annotation(com.staniul.teamspeak.events.Teamspeak3Event) && @annotation(com.staniul.teamspeak.security.clientaccesscheck.ClientGroupAccess) && " +
            "args(client)")
    public void joinEvent (Client client) {
    }

    @Around(value = "invokeCommand(client)", argNames = "pjp,client")
    public Object checkCommandClientAccess(ProceedingJoinPoint pjp, Client client) throws Throwable {
        boolean access = checkClientAccess(pjp, client);

        if (access)
            return pjp.proceed();

        CommandResponse response = new CommandResponse(CommandExecutionStatus.ACCESS_DENIED, null);
        commandMessenger.sendResponseToClient(client, response);

        return response;
    }

    @Around(value = "joinEvent(client)", argNames = "pjp,client")
    public Object checkEventClientAccess (ProceedingJoinPoint pjp, Client client) throws Throwable {
        boolean access = checkClientAccess(pjp, client);

        if (access) {
            log.info(String.format("Client (%d %s) access to method (%s) approved.", client.getDatabaseId(), client.getNickname(),
                    ((MethodSignature) pjp.getSignature()).getMethod().getName()));
            return pjp.proceed();
        }

        log.info(String.format("Client (%d %s) access to method (%s) revoked.", client.getDatabaseId(), client.getNickname(),
                ((MethodSignature) pjp.getSignature()).getMethod().getName()));

        return null;
    }

    private boolean checkClientAccess (ProceedingJoinPoint pjp, Client client) {
        List<ClientGroupAccess> anns = AroundAspectUtil.getAnnotationsOfAspectMethod(pjp, ClientGroupAccess.class);
        boolean access = true;
        for (ClientGroupAccess ann : anns) {
            Set<Integer> groups = Arrays.stream(environment.getProperty(ann.value()).split(",")).map(Integer::parseInt).collect(Collectors.toSet());
            log.info(String.format("Restricted by (%s), clients groups (%s)", groups, client.getServergroups()));
            ClientGroupAccessCheck check = ClientGroupAccessCheck.create(groups, ann.check());
            if (check == null || !check.apply(client))
                access = false;
        }

        return access;
    }
}
