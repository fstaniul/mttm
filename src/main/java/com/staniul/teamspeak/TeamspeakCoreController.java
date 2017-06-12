package com.staniul.teamspeak;

import com.staniul.configuration.ConfigurationLoader;
import com.staniul.configuration.annotations.ConfigFile;
import com.staniul.query.Client;
import com.staniul.teamspeak.commands.Teamspeak3Command;
import com.staniul.teamspeak.events.EventType;
import com.staniul.teamspeak.events.Teamspeak3Event;
import com.staniul.util.ReflectionUtil;
import javafx.scene.effect.Reflection;
import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.log4j.Logger;
import org.reflections.Reflections;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Controller of teamspeak 3 behaviour, that is events and commandeventscontrol. All commandeventscontrol called on teamspeak 3 server must be
 * registered within this controller to be invoked. All events that should be aware of clients joining and leaving
 * teamspeak 3 server should be registered within this controller.
 */
@Component
@ConfigFile("teamspeak.xml")
public class TeamspeakCoreController implements ApplicationContextAware {
    private static Logger log = Logger.getLogger(TeamspeakCoreController.class);

    private ApplicationContext applicationContext;
    private Reflections reflections;
    private XMLConfiguration config;
    private HashMap<String, MethodContainer> commands;
    private Set<MethodContainer> joinEvents;
    private Set<MethodContainer> leaveEvents;

    @Autowired
    public TeamspeakCoreController(Reflections reflections) throws ConfigurationException {
        this.reflections = reflections;
        config = ConfigurationLoader.load(TeamspeakCoreController.class);
        commands = new HashMap<>();
        joinEvents = new HashSet<>();
        leaveEvents = new HashSet<>();
    }

    @PostConstruct
    public void findMethods() {
        Set<Class<?>> types = reflections.getTypesAnnotatedWith(Teamspeak3Module.class);
        for (Class<?> type : types) {
            findCommands(type);
            findEvents(type);
        }
    }

    private void findEvents(Class<?> type) {
        Set<Method> events = ReflectionUtil.getMethodsAnnotatedWith(type, Teamspeak3Event.class);
        for (Method event : events) {
            Teamspeak3Event ann = event.getAnnotation(Teamspeak3Event.class);
            Object target = applicationContext.getBean(type);

            if (ann.value() == EventType.JOIN)
                joinEvents.add(new MethodContainer(target, event));
            else leaveEvents.add(new MethodContainer(target, event));
        }
    }

    private void findCommands (Class<?> type) {
        Set<Method> commands = ReflectionUtil.getMethodsAnnotatedWith(type, Teamspeak3Command.class);
        log.debug("################# Found commandeventscontrol: " + commands.toString());
        for (Method command : commands) {
            Teamspeak3Command ann = command.getAnnotation(Teamspeak3Command.class);
            Object target = applicationContext.getBean(type);
            this.commands.putIfAbsent(ann.value(), new MethodContainer(target, command));
        }
    }

    public void callCommand (String command, Client client, String params) {
        MethodContainer mc = commands.get(command);
        log.debug(mc);
        try {
            mc.method.invoke(mc.target, client, params);
        } catch (IllegalAccessException | InvocationTargetException e) {
            log.error("Failed to invoke command!", e);
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public static class MethodContainer {
        private Object target;
        private Method method;

        MethodContainer(Object target, Method method) {
            this.target = target;
            this.method = method;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof MethodContainer &&
                    ((MethodContainer) obj).method.equals(method) &&
                    ((MethodContainer) obj).target.equals(target);
        }

        @Override
        public String toString() {
            return String.format("Method: %s, Target: %s", method, target);
        }
    }
}
