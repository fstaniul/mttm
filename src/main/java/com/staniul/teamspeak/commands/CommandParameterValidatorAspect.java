package com.staniul.teamspeak.commands;

import com.staniul.teamspeak.commands.validators.ValidateParams;
import com.staniul.teamspeak.query.Client;
import com.staniul.util.spring.AroundAspectUtil;
import com.staniul.util.validation.Validator;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Aspect
@Component
@Order(1)
public class CommandParameterValidatorAspect {
    private final CommandMessenger commandMessenger;

    @Autowired
    public CommandParameterValidatorAspect(CommandMessenger commandMessenger) {
        this.commandMessenger = commandMessenger;
    }


    @Pointcut("execution(com.staniul.teamspeak.commands.CommandResponse * (com.staniul.teamspeak.query.Client, java.lang.String)) && " +
            "args(client,params) && " +
            "@annotation(com.staniul.teamspeak.commands.Teamspeak3Command) && @annotation(com.staniul.teamspeak.commands.validators.ValidateParams)")
    public void teamspeak3command(Client client, String params) {}

    @Around(value = "teamspeak3command(client,params)", argNames = "pjp,client,params")
    public Object validateCommandParameter(ProceedingJoinPoint pjp, Client client, String params) throws Throwable {
        Method method = AroundAspectUtil.getTargetMethodOfAspect(pjp);
        boolean valid = true;

        if (method.isAnnotationPresent(ValidateParams.class)) {
            ValidateParams[] anns = method.getAnnotationsByType(ValidateParams.class);
            for (ValidateParams ann : anns) {
                Validator<String> validator = ann.value().newInstance();

                if (!validator.validate(params))
                    valid = false;
            }
        }

        if (valid) return pjp.proceed();

        CommandResponse response = new CommandResponse(CommandExecutionStatus.INVALID_PARAMETERS, null);
        commandMessenger.sendResponseToClient(client, response);

        return null;
    }
}
