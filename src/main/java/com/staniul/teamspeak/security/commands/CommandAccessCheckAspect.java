package com.staniul.teamspeak.security.commands;

import com.staniul.query.Client;
import com.staniul.teamspeak.commands.CommandExecutionStatus;
import com.staniul.teamspeak.commands.Teamspeak3Command;
import com.staniul.teamspeak.commands.CommandResponse;
import com.staniul.teamspeak.security.AccessCheck;
import com.staniul.teamspeak.security.PermitAllAccessCheck;
import com.staniul.teamspeak.security.clientaccesscheck.ClientGroupAccessCheck;
import com.staniul.util.AspectUtil;
import org.apache.log4j.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

@Component
@Aspect
public class CommandAccessCheckAspect {
    private static Logger log = Logger.getLogger(CommandAccessCheckAspect.class);

    @Pointcut(value = "execution(com.staniul.teamspeak.commands.CommandResponse * (com.staniul.query.Client, java.lang.String)) && " +
            "@annotation(com.staniul.teamspeak.commands.Teamspeak3Command) && " +
            "args(client, params)", argNames = "client,params")
    public void invokeCommand(Client client, String params) {
    }

    @Around(value = "invokeCommand(client, params)", argNames = "pjp,client,params")
    public Object checkClientAccess(ProceedingJoinPoint pjp, Client client, String params) throws Throwable {
        Teamspeak3Command ann = AspectUtil.getAnnotationOfAspectMethod(pjp, Teamspeak3Command.class);
        AccessCheck<Client> accessCheck;
        if (ann.groups().length == 0) accessCheck = new PermitAllAccessCheck<>();
        else accessCheck = ClientGroupAccessCheck.create(ann.groups(), ann.check());

        if (accessCheck == null) {
            return new CommandResponse(CommandExecutionStatus.EXECUTION_TERMINATED, null);
        }

        if (accessCheck.apply(client)) {
                return pjp.proceed();
        }

        return new CommandResponse(CommandExecutionStatus.ACCESS_DENIED, null);
    }
}