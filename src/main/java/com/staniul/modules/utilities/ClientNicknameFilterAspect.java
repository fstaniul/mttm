package com.staniul.modules.utilities;

import com.staniul.teamspeak.query.Client;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Aspect
public class ClientNicknameFilterAspect {
    private static Logger log = LogManager.getLogger(ClientNicknameFilterAspect.class);

    private final ClientNicknameFilter nicknameFilter;

    @Autowired
    public ClientNicknameFilterAspect(ClientNicknameFilter nicknameFilter) {
        this.nicknameFilter = nicknameFilter;
    }

    @Around(value = "execution(public void com.staniul.teamspeak.Teamspeak3CoreController.callJoinEvents(com.staniul.teamspeak.query.Client)) && " +
            "args(client)",
            argNames = "pjp,client")
    public Object checkClientNickname(ProceedingJoinPoint pjp, Client client) throws Throwable {
        log.info(String.format("Checking clients nickname (%d %s)", client.getDatabaseId(), client.getNickname()));

        if (nicknameFilter.filterClientNicknameOnJoin(client)) {
            pjp.proceed();
        }

        return null;
    }
}
