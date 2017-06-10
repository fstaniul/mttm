package com.staniul.teamspeak.security.events;

import com.staniul.query.Client;
import com.staniul.teamspeak.events.Teamspeak3Event;
import com.staniul.teamspeak.security.AccessCheck;
import com.staniul.teamspeak.security.PermitAllAccessCheck;
import com.staniul.teamspeak.security.clientaccesscheck.ClientGroupAccessCheck;
import com.staniul.util.AspectUtil;
import org.apache.log4j.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

@Aspect
public class EventAccessCheckAspect {
    private static Logger log = Logger.getLogger(EventAccessCheckAspect.class);

    @Pointcut("execution(public void * (com.staniul.query.Client)) && " +
            "@annotation(com.staniul.teamspeak.events.Teamspeak3Event) && " +
            "args(client)")
    public void joinEvent (Client client) {

    }

    @Around(value = "joinEvent(client)", argNames = "pjp,client")
    public Object checkClientAccess (ProceedingJoinPoint pjp, Client client) {
        Teamspeak3Event ann = AspectUtil.getAnnotationOfAspectMethod(pjp, Teamspeak3Event.class);
        AccessCheck<Client> accessCheck;
        if (ann.groups().length == 0) accessCheck = new PermitAllAccessCheck<>();
        else accessCheck = ClientGroupAccessCheck.create(ann.groups(), ann.check());

        if (accessCheck != null && accessCheck.apply(client)) {
            try {
                return pjp.proceed(new Object[] {client});
            } catch (Throwable e) {
                log.error("Failed to proceed with method execution in check client access of event access check aspect!");
            }
        }

        return null;
    }
}
