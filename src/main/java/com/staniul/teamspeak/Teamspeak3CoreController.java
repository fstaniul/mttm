package com.staniul.teamspeak;

import com.staniul.teamspeak.commands.CommandExecutionStatus;
import com.staniul.teamspeak.commands.CommandMessenger;
import com.staniul.teamspeak.commands.CommandResponse;
import com.staniul.teamspeak.commands.Teamspeak3Command;
import com.staniul.teamspeak.events.EventType;
import com.staniul.teamspeak.events.Teamspeak3Event;
import com.staniul.teamspeak.query.Client;
import com.staniul.util.reflection.MethodContainer;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.reflections.Reflections;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * Controller of teamspeak 3 behaviour, that is events and commands.
 */
@Component
public class Teamspeak3CoreController {
    private static Logger log = LogManager.getLogger(Teamspeak3CoreController.class);

    private ApplicationContext applicationContext;
    private final CommandMessenger commandMessenger;
    private final Reflections reflections;
    private HashMap<String, MethodContainer> commands;
    private Set<MethodContainer> joinEvents;
    private Set<MethodContainer> leaveEvents;

    @Autowired
    public Teamspeak3CoreController(Reflections reflections, CommandMessenger messenger) throws ConfigurationException {
        this.reflections = reflections;
        this.commandMessenger = messenger;
        commands = new HashMap<>();
        joinEvents = new HashSet<>();
        leaveEvents = new HashSet<>();
    }

    /**
     * Finds methods that are events, commands and tasks.
     */
    void init() {
        findCommands();
        findEvents();
    }

    /**
     * Finds events declared in project.
     */
    private void findEvents() {
        Set<Method> methods = reflections.getMethodsAnnotatedWith(Teamspeak3Event.class);
        for (Method method : methods) {
            Teamspeak3Event ann = method.getAnnotation(Teamspeak3Event.class);

            Class<?> targetClass = method.getDeclaringClass();
            Object target = applicationContext.getBean(targetClass);

            if (target != null) {
                if (ann.value() == EventType.JOIN)
                    joinEvents.add(new MethodContainer(target, method));
                else leaveEvents.add(new MethodContainer(target, method));
            }
        }
    }

    /**
     * Find commands declared in project.
     */
    private void findCommands() {
        Set<Method> methods = reflections.getMethodsAnnotatedWith(Teamspeak3Command.class);
        for (Method method : methods) {
            Teamspeak3Command ann = method.getAnnotation(Teamspeak3Command.class);
            String key = ann.value();

            Class<?> targetClass = method.getDeclaringClass();
            Object target = applicationContext.getBean(targetClass);

            if (target != null) {
                commands.putIfAbsent(key, new MethodContainer(target, method));
            }
        }
    }

    /**
     * Calls command by its name.
     *
     * @param command Command name.
     * @param client  Client that is invoking command.
     * @param params  Parameters to pass to command.
     */
    public void callCommand(String command, Client client, String params) {
        log.info(String.format("Client (%s, %d) called command (%s) with params (%s)", client.getNickname(), client.getDatabaseId(), command, params));
        MethodContainer mc = commands.get(command);

        if (mc == null)
            commandMessenger.sendResponseToClient(client, new CommandResponse(CommandExecutionStatus.NOT_FOUND, null));

        else {
            mc.invoke(client, params);
        }
    }

    /**
     * Calls all join events with Client information that joined server.
     *
     * @param target Client that joined server.
     */
    public void callJoinEvents(Client target) {
        internalCallEvents(joinEvents, target);
    }

    /**
     * Calls all leave events with id of client that left server.
     *
     * @param target Id of client that left server.
     */
    public void callLeaveEvents(Integer target) {
        internalCallEvents(leaveEvents, target);
    }

    /**
     * Inthernal method responsible for calling events.
     *
     * @param events Set of events to call.
     * @param param  Parameter that should be passed to event method.
     */
    private void internalCallEvents(Set<MethodContainer> events, Object param) {
        events.forEach(mc -> mc.invoke(param));
    }

    void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public HashMap<String, MethodContainer> getCommands() {
        return commands;
    }
}
