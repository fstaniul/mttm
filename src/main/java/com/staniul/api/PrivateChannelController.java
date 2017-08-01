package com.staniul.api;

import com.staniul.api.security.AuthUtil;
import com.staniul.api.security.auth.ApiClientDetails;
import com.staniul.teamspeak.modules.channelsmanagers.privatechannels.PrivateChannel;
import com.staniul.teamspeak.modules.channelsmanagers.privatechannels.PrivateChannelManager;
import com.staniul.teamspeak.query.Channel;
import com.staniul.teamspeak.query.ClientDatabase;
import com.staniul.teamspeak.query.Query;
import com.staniul.teamspeak.query.QueryException;

import java.util.List;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/privatechannel")
public class PrivateChannelController {
    private static final Logger log = LogManager.getLogger(PrivateChannelController.class);

    private final PrivateChannelManager channelManager;
    private final Query query;

    @Autowired
    public PrivateChannelController(PrivateChannelManager channelManager, Query query) {
        this.channelManager = channelManager;
        this.query = query;
    }

    @GetMapping("/create")
    @ResponseBody
    public ResponseEntity<?> createChannelForClients(Authentication auth) {
        ApiClientDetails clientDetails = AuthUtil.getClientDetails(auth);
        try {
            ClientDatabase clientDatabase = query.getClientDatabaseInfo(clientDetails.getDatabaseId());
            PrivateChannel channel = channelManager.createChannel(clientDetails.getDatabaseId(),
                    clientDatabase.getNickname());
            if (channel != null) {
                return new ResponseEntity<>(channel, HttpStatus.OK);
            }
        } catch (QueryException e) {
            log.error("Failed to create channel for client as requested in api!", e);
        }

        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @PostMapping(path = "/edit/name", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> editChannelName(@RequestParam int type, @RequestBody EditChannelNameRequest request,
            Authentication auth) {
        ApiClientDetails clientDetails = AuthUtil.getClientDetails(auth);
        PrivateChannel clientsChannel = channelManager.getClientsChannel(clientDetails.getDatabaseId());

        try {
            if (clientsChannel == null)
                return new ResponseEntity<>(HttpStatus.FORBIDDEN);

            if (type == 1 || type == 2) {
                changeSubChannelName(request, clientsChannel, type);
            } else {
                query.channelRename(request.name, clientsChannel.getId());
            }

            return new ResponseEntity<>(HttpStatus.OK);
        } catch (QueryException e) {
            log.error("Failed to change channel name!", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private void changeSubChannelName(EditChannelNameRequest request, PrivateChannel clientsChannel, int type)
            throws QueryException {
        int number = type - 1;
        List<Channel> subChannels = getClientsSubChannelsInfo(clientsChannel, query.getChannelList());
        query.channelRename(request.name, subChannels.get(number).getId());
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<?> getClientsChannel(Authentication auth) {
        ApiClientDetails clientDetails = AuthUtil.getClientDetails(auth);
        PrivateChannel clientsChannel = channelManager.getClientsChannel(clientDetails.getDatabaseId());

        if (clientsChannel != null) {
            try {
                List<Channel> channels = query.getChannelList();
                Channel channelInfo = getClientsChannelInfo(clientsChannel, channels);
                List<Channel> subs = getClientsSubChannelsInfo(clientsChannel, channels);

                GetClientsChannelResponse response = new GetClientsChannelResponse(clientsChannel.getNumber(),
                        channelInfo.getName(), channelInfo.getTopic(), subs.get(0).getName(), subs.get(1).getName());

                return new ResponseEntity<>(response, HttpStatus.OK);
            } catch (QueryException e) {
                log.error("Failed to get channel information from teamspeak 3 server!", e);
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } else {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
    }

    private Channel getClientsChannelInfo(PrivateChannel clientsChannel, List<Channel> channels) {
        return channels.stream().filter(c -> c.getId() == clientsChannel.getId()).findFirst().orElse(null);
    }

    private List<Channel> getClientsSubChannelsInfo(PrivateChannel clientsChannel, List<Channel> channels) {
        return channels.stream().filter(c -> c.getParentId() == clientsChannel.getId()).collect(Collectors.toList());
    }

    public class GetClientsChannelResponse {
        private int number;
        private String name;
        private String topic;
        private String subName1;
        private String subName2;

        public GetClientsChannelResponse() {
        }

        public GetClientsChannelResponse(int number, String name, String topic, String subName1, String subName2) {
            this.number = number;
            this.name = name;
            this.topic = topic;
            this.subName1 = subName1;
            this.subName2 = subName2;
        }
    }

    public class EditChannelNameRequest {
        private String name;
    }
}
