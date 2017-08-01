package com.staniul.api;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.aspectj.weaver.IUnwovenClassFile;
import org.aspectj.weaver.ResolvableTypeList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.staniul.api.security.AuthUtil;
import com.staniul.api.security.auth.ApiClientDetails;
import com.staniul.teamspeak.modules.channelsmanagers.privatechannels.PrivateChannel;
import com.staniul.teamspeak.modules.channelsmanagers.privatechannels.PrivateChannelManager;
import com.staniul.teamspeak.modules.channelsmanagers.vipchannels.VipChannel;
import com.staniul.teamspeak.modules.channelsmanagers.vipchannels.VipChannelManager;
import com.staniul.teamspeak.query.Channel;
import com.staniul.teamspeak.query.Query;
import com.staniul.teamspeak.query.QueryException;
import com.staniul.teamspeak.query.servergroups.Servergroup;
import com.staniul.util.collections.SetUtil;
import com.staniul.xmlconfig.CustomXMLConfiguration;
import com.staniul.xmlconfig.annotations.UseConfig;
import com.staniul.xmlconfig.annotations.WireConfig;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@UseConfig("api/channelinformation.xml")
@RequestMapping("/api/channel")
public class ChannelInformationController {
    private static Logger log = LogManager.getLogger(ChannelInformationController.class);

    @WireConfig
    private CustomXMLConfiguration config;
    private final VipChannelManager vipChannelManager;
    private final PrivateChannelManager privateChannelManager;
    private final Query query;

    @Autowired
    public ChannelInformationController(VipChannelManager vipChannelManager,
            PrivateChannelManager privateChannelManager, Query query) {
        this.vipChannelManager = vipChannelManager;
        this.privateChannelManager = privateChannelManager;
        this.query = query;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getClientsChannel(Authentication auth) {
        ApiClientDetails clientDetails = AuthUtil.getClientDetails(auth);
        try {
            if (isClientAnVip(clientDetails.getDatabaseId())) {
                VipChannel vipChannel = vipChannelManager.getClientsChannel(clientDetails.getDatabaseId()); //Get clients vip channel
                Channel channelInfo = query.getChannelInfo(vipChannel.getId()); //Get vip parent channel information

                ChannelInformationResponse response = new ChannelInformationResponse(); //Create a response object
                //Stuff the information into the response
                response.name = channelInfo.getName();
                response.id = channelInfo.getId();
                response.number = vipChannel.getNumber();
                response.vip = true;

                //Search for subchannels names:
                List<Channel> channels = query.getChannelList().stream()
                        .filter(c -> c.getParentId() == vipChannel.getId()).collect(Collectors.toList());
                response.subs = channels.stream().map(Channel::getName).collect(Collectors.toList())
                        .toArray(new String[] {});

                return new ResponseEntity<>(response, HttpStatus.OK);
            }

            PrivateChannel privateChannel = privateChannelManager.getClientsChannel(clientDetails.getDatabaseId());
            if (privateChannel == null) { //Check if client has channel if not -> then return an no content response
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }

            Channel channelInfo = query.getChannelInfo(privateChannel.getId());

            ChannelInformationResponse response = new ChannelInformationResponse();
            response.name = channelInfo.getName();
            response.id = channelInfo.getId();
            response.number = privateChannel.getNumber();
            response.vip = false;

            List<Channel> channels = query.getChannelList().stream()
                    .filter(c -> c.getParentId() == privateChannel.getId()).collect(Collectors.toList());
            response.subs = channels.stream().map(Channel::getName).collect(Collectors.toList())
                    .toArray(new String[] {});

            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (QueryException e) {
            log.error(
                    "Failed to get client information for resolving client channel infromation from teamspeak 3 server.",
                    e);

            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private boolean isClientAnVip(int clientDatabaseId) throws QueryException {
        Set<Integer> groups = query.getServergroupsOfClient(clientDatabaseId).stream().map(Servergroup::getId)
                .collect(Collectors.toSet());

        Set<Integer> vipGroups = config.getIntSet("groups.server.vip");

        long intersection = SetUtil.countIntersection(groups, vipGroups);

        return intersection == config.getLong("intersection-size");
    }

    private class ChannelInformationResponse {
        private String name;
        private boolean vip;
        private int id;
        private int number;
        private String[] subs;
    }
}