package com.staniul.teamspeak.commands;

import com.staniul.teamspeak.commands.validators.ValidateParams;
import com.staniul.util.AroundAspectUtil;
import com.staniul.validation.Validator;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Aspect
@Component
public class CommandParameterValidatorAspect {
    @Pointcut("execution(com.staniul.teamspeak.commands.CommandResponse * (.., java.lang.String)) && " +
            "args(..,params) && " +
            "@annotation(com.staniul.teamspeak.commands.Teamspeak3Command)")
    public void teamspeak3comamnd(String params) {}

    @Around(value = "teamspeak3comamnd(params)", argNames = "pjp,params")
    public Object validateCommandParameter (ProceedingJoinPoint pjp, String params) throws Throwable {
        Method method = AroundAspectUtil.getTargetMethodOfAspect(pjp);
        boolean valid = true;

        if (method.isAnnotationPresent(ValidateParams.class)) {
            ValidateParams ann = method.getAnnotation(ValidateParams.class);
            Validator<String> validator = ann.value().newInstance();

            valid = validator.validate(params);
        }

        if (valid) return pjp.proceed();

        return new CommandResponse(CommandExecutionStatus.INVALID_PARAMETERS, null);
    }
}
