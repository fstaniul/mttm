package com.staniul.api;

import com.staniul.api.security.AuthUtil;
import com.staniul.api.security.auth.ApiClientDetails;
import com.staniul.teamspeak.query.Query;
import com.staniul.teamspeak.query.QueryException;
import com.staniul.teamspeak.query.servergroups.Servergroup;
import com.staniul.teamspeak.query.servergroups.ServergroupExtended;
import com.staniul.xmlconfig.CustomXMLConfiguration;
import com.staniul.xmlconfig.annotations.UseConfig;
import com.staniul.xmlconfig.annotations.WireConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@UseConfig("api/servergroups.xml")
@RequestMapping("/api/servergroups")
public class ServergroupsController {
    private static Logger log = LogManager.getLogger(ServergroupsController.class);

    @WireConfig
    private CustomXMLConfiguration config;
    private final Query query;

    @Autowired
    public ServergroupsController(Query query) {this.query = query;}

    @GetMapping(value = "/games", produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<?> getListOfGames() {
        int sortMin = getSortMin();
        int sortMax = getSortMax();

        try {
            List<ServergroupExtended> gamesList = query.getServergroupsList().stream()
                    .filter(s -> s.getSort() > sortMin && s.getSort() < sortMax)
                    .collect(Collectors.toList());

            return new ResponseEntity<>(gamesList, HttpStatus.OK);
        } catch (QueryException e) {
            log.error("Failed to get servergroup list from server as requested by client in a rest api.", e);
            return new ResponseEntity<>("Failed to get list of servergroups from teamspeak 3 server!", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<?> getListOfClientGroups (Authentication authentication) {
        ApiClientDetails clientDetails = AuthUtil.getClientDetails(authentication);

        try {
            List<Servergroup> clientServergroups = query.getServergroupsOfClient(clientDetails.getDatabaseId());
            return new ResponseEntity<>(clientServergroups, HttpStatus.OK);
        } catch (QueryException e) {
            log.error("Failed to get client servergroups from teamspeak 3 server.", e);
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> setClientServergroups (@RequestBody ServergroupsPostRequest request, Authentication authentication) {
        try {
            List<Integer> servergroupList = query.getServergroupsListNarrowedBySortId(getSortMin(), getSortMax())
                    .stream()
                    .map(ServergroupExtended::getId)
                    .collect(Collectors.toList());

            if (request != null && request.getGroups() != null) {
                for (Integer group : request.getGroups()) {
                    if (!servergroupList.contains(group))
                        request.getGroups().remove(group);
                }

                ApiClientDetails clientDetails = AuthUtil.getClientDetails(authentication);

                List<Integer> clientsGroups = query.getServergroupsOfClient(clientDetails.getDatabaseId())
                        .stream()
                        .map(Servergroup::getId)
                        .collect(Collectors.toList());

                List<Integer> removeFrom = clientsGroups.stream()
                        .filter(s -> !request.getGroups().contains(s))
                        .collect(Collectors.toList());

                List<Integer> addInto = request.getGroups().stream()
                        .filter(s -> !clientsGroups.contains(s))
                        .collect(Collectors.toList());

                query.servergroupDeleteClient(clientDetails.getDatabaseId(), removeFrom.toArray(new Integer[removeFrom.size()]));
                query.servergroupAddClient(clientDetails.getDatabaseId(), addInto.toArray(new Integer[addInto.size()]));
            }

        } catch (QueryException e) {
            log.error("Failed to get servergroups list from teamspeak 3 server!", e);
        }

        return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    public static class ServergroupsPostRequest {
        private List<Integer> groups;

        public ServergroupsPostRequest() {
        }

        public ServergroupsPostRequest(List<Integer> groups) {
            this.groups = groups;
        }

        public List<Integer> getGroups() {
            return groups;
        }
    }

    private int getSortMax() {
        return config.getInt("games.sort[@max]");
    }

    private int getSortMin() {
        return config.getInt("games.sort[@min]");
    }
}
