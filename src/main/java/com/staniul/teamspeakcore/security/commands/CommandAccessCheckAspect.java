package com.staniul.teamspeakcore.security.commands;

import com.staniul.query.Client;
import com.staniul.teamspeakcore.commands.CommandExecutionStatus;
import com.staniul.teamspeakcore.commands.Teamspeak3Command;
import com.staniul.teamspeakcore.commands.CommandResponse;
import com.staniul.teamspeakcore.security.AccessCheck;
import com.staniul.teamspeakcore.security.PermitAllAccessCheck;
import com.staniul.util.SetUtil;
import org.apache.log4j.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Aspect
public class CommandAccessCheckAspect {
    private static Logger log = Logger.getLogger(CommandAccessCheckAspect.class);

    @Pointcut(value = "execution(public com.staniul.teamspeakcore.commands.CommandResponse * (com.staniul.query.Client, java.lang.String)) && " +
            "@annotation(com.staniul.teamspeakcore.commands.Teamspeak3Command) && " +
            "args(client, params)", argNames = "client,params")
    public void invokeCommand(Client client, String params) {
    }

    @Around(value = "invokeCommand(client, params) && @args(com.staniul.teamspeakcore.commands.Teamspeak3Command)", argNames = "pjp,client,params")
    public Object checkClientAccess(ProceedingJoinPoint pjp, Client client, String params) {
        Teamspeak3Command ann = getCommandAnnotation(pjp);
        AccessCheck<Client> accessCheck = determineAccessCheck (ann);

        if (accessCheck == null) {
            return new CommandResponse(CommandExecutionStatus.EXECUTION_TERMINATED, "");
        }

        if (accessCheck.apply(client)) {
            try {
                return pjp.proceed();
            } catch (Throwable throwable) {
                return new CommandResponse(CommandExecutionStatus.EXECUTION_TERMINATED, "");
            }
        }

        return new CommandResponse(CommandExecutionStatus.ACCESS_DENIED, "");
    }

    private AccessCheck<Client> determineAccessCheck(Teamspeak3Command ann) {
        if (ann.groups().length == 0) return new PermitAllAccessCheck<>();

        try {
            Set<Integer> groups = new HashSet<>();
            Arrays.stream(ann.groups()).forEach(groups::add);
            Constructor<? extends AccessCheck<Client>> constructor = ann.check().getConstructor(Set.class);
            return constructor.newInstance(groups);

        } catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
            log.error("Failed to instantiate access check specified in annotation.", e);
            return null;
        }
    }

    private Teamspeak3Command getCommandAnnotation (ProceedingJoinPoint pjp) {
        MethodSignature methodSignature = (MethodSignature) pjp.getSignature();
        Method method = methodSignature.getMethod();
        return method.getAnnotation(Teamspeak3Command.class);
    }
}
