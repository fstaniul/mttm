package com.staniul.teamspeak.commands.core;

import com.staniul.teamspeak.Teamspeak3CoreController;
import com.staniul.teamspeak.commands.CommandDescription;
import com.staniul.teamspeak.commands.CommandResponse;
import com.staniul.teamspeak.commands.Teamspeak3Command;
import com.staniul.teamspeak.commands.validators.CommandDetailedDescription;
import com.staniul.teamspeak.query.Client;
import com.staniul.teamspeak.security.clientaccesscheck.ClientGroupAccess;
import com.staniul.teamspeak.security.clientaccesscheck.ClientGroupAccessCheck;
import com.staniul.util.reflection.MethodContainer;
import com.staniul.xmlconfig.CustomXMLConfiguration;
import com.staniul.xmlconfig.annotations.UseConfig;
import com.staniul.xmlconfig.annotations.WireConfig;
import javafx.util.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

@Component
@UseConfig("corecmd.xml")
public class CoreCommands {
    @WireConfig
    private CustomXMLConfiguration config;
    private final Teamspeak3CoreController coreController;
    private final Environment environment;

    @Autowired
    public CoreCommands(Teamspeak3CoreController coreController, Environment environment) {
        this.coreController = coreController;
        this.environment = environment;
    }

//    @Teamspeak3Command("!help")
//    public CommandResponse listCommands (Client client, String params) {
//        List<Command> commands = config.getClasses(Command.class, "command-list.cmd");
//        List<Integer> clientScopes = getClientScopes (client);
//        List<String> messages = commands.stream()
//                .filter(c -> clientScopes.contains(c.getScope()))
//                .map(c -> "[b]" + c.getCommand() + "[/b] - " + c.getDescription())
//                .collect(Collectors.toList());
//        return new CommandResponse(messages.toArray(new String[]{}));
//    }

    @Teamspeak3Command("!help")
    @CommandDescription("Pokazuje listę dostepnych dla użytkownika komend.")
    @CommandDetailedDescription("Pokazuje listę dostepnych dla użytkownika komend. " +
            "Przyjmuje jeden parametr (STRING), który jest nazwą komendy dla której ma wyświetlić szczegółowe informacje." +
            "Niepodanie parametru skutkuje wyświetleniem listy komend wraz z ich krótkim opisem.")
    public CommandResponse showCommandInfo (Client client, String params) {
        //When we are asking for every command:
        if (params.length() > 0) {
            List<Pair<String, MethodContainer>> commandsThatCanBeCalled = new ArrayList<>();
            HashMap<String, MethodContainer> commands = coreController.getCommands();
            commands.forEach((name, container) -> {
                Method method = container.getMethod();
                if (clientCanAccess(client, method))
                    commandsThatCanBeCalled.add(new Pair<>(name, container));
            });

            commandsThatCanBeCalled.sort(Comparator.comparing(Pair::getKey));

            List<String> messages = new ArrayList<>();
            for (Pair<String, MethodContainer> command : commandsThatCanBeCalled) {
                String msg = "[b]" + command.getKey() + "[/b]: " + getDescription(command.getValue().getMethod());
                messages.add(msg);
            }

            return new CommandResponse(messages.toArray(new String[messages.size()]));
        }

        //We are interested in one particular command:
        else {
            String command = "!" + params;
            MethodContainer container = coreController.getCommands().get(command);
            if (container == null) {
                return new CommandResponse(config.getString("commands.help[@not-found]"));
            }
            else if (clientCanAccess(client, container.getMethod())) {
                String desc = getDetailedDescription(container.getMethod());
                if (desc == null) desc = getDescription(container.getMethod());
                return new CommandResponse("[b]" + command + "[/b]: " + desc);
            }
            else {
                return new CommandResponse(config.getString("commands.help[@access-denied]"));
            }
        }
    }

    private String getDetailedDescription (Method method) {
        CommandDetailedDescription description = method.getAnnotation(CommandDetailedDescription.class);
        return description == null ? null : description.value();
    }

    private String getDescription (Method method) {
        CommandDescription description = method.getAnnotation(CommandDescription.class);
        return description == null ? "" : description.value();
    }

    private boolean clientCanAccess (Client client, Method method) {
        ClientGroupAccess[] anns = method.getAnnotationsByType(ClientGroupAccess.class);

        if (anns == null || anns.length == 0)
            return true;

        else {
            boolean canBeAdded = true;
            for (ClientGroupAccess ann : anns) {
                Set<Integer> accessGroups = Arrays.stream(environment.getProperty(ann.value()).split(","))
                        .map(Integer::parseInt).collect(Collectors.toSet());
                ClientGroupAccessCheck accessCheck = ClientGroupAccessCheck.create(accessGroups, ann.check());

                if (accessCheck == null || !accessCheck.apply(client)) {
                    canBeAdded = false;
                    break;
                }
            }

            return canBeAdded;
        }
    }

    private List<Integer> getClientScopes(Client client) {
        List<Scope> scopes = config.getClasses(Scope.class, "scopes.scope");
        List<Integer> clientScopes = new ArrayList<>();
        for (Scope scope : scopes) {
            if (client.isInServergroup(scope.getGroups()))
                clientScopes.add(scope.getId());
        }

        return clientScopes;
    }
}
