package com.staniul.modules.channelsmanagers;

import com.staniul.taskcontroller.Task;
import com.staniul.teamspeak.query.Channel;
import com.staniul.teamspeak.query.Query;
import com.staniul.teamspeak.query.QueryException;
import com.staniul.teamspeak.query.channel.ChannelFlagConstants;
import com.staniul.teamspeak.query.channel.ChannelProperties;
import com.staniul.xmlconfig.CustomXMLConfiguration;
import com.staniul.xmlconfig.annotations.UseConfig;
import com.staniul.xmlconfig.annotations.WireConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
@UseConfig("modules/pcc.xml")
public class PublicChannelCreator {
    @WireConfig
    private CustomXMLConfiguration config;
    private Query query;

    @Autowired
    public PublicChannelCreator(Query query) {
        this.query = query;
    }

    @Task(delay = 10000)
    public void checkPublicChannels() throws QueryException {
        List<Channel> channels = query.getChannelList();

        int parentId = config.getInt("parentchannel[@id]");
        List<Channel> parentChannels = channels.stream().filter(ch -> ch.getParentId() == parentId).collect(Collectors.toList());
        for (Channel parent : parentChannels) {
            List<Channel> subs = channels.stream().filter(ch -> ch.getParentId() == parent.getId()).collect(Collectors.toList());

            if (subs.size() > 2) {
                for (int i = subs.size() - 1;
                     i >= 2 && subs.get(i).getTotalClients() == 0 && subs.get(i-1).getTotalClients() == 0;
                     i--)

                    query.channelDelete(subs.get(i).getId());
            }

            int full = subs.stream().filter(ch -> ch.getTotalClients() > 0).mapToInt(ch -> 1).sum();
            if (full == subs.size()) {

                Matcher matcher = Pattern.compile(".*MAX ([0-9]).*").matcher(parent.getName());
                if (matcher.find()) {

                    int slotNumber = Integer.parseInt(matcher.group(1));
                    ChannelProperties properties = publicChannelProperties(subs.size() + 1, slotNumber, "publicchannel");
                    query.channelCreate(properties);

                } else {
                    if (parent.getName().matches(".*UNLIMITED")) {

                        ChannelProperties properties = publicChannelProperties(subs.size() + 1, -1, "unlimited");
                        query.channelCreate(properties);
                    }
                }
            }
        }
    }

    private ChannelProperties publicChannelProperties(int channelNumber, int slotNumber, String type) {
        String configTemplate = type + "[@%s]";
        String name = String.format(configTemplate, "name");
        String topic = String.format(configTemplate, "topic");
        String description = String.format(configTemplate, "description");

        ChannelProperties properties = new ChannelProperties()
                .setName(config.getString(name).replace("$SLOTS$", formatSlots(slotNumber)))
                .setTopic(config.getString(topic).replace("$NUMBER$", Integer.toString(channelNumber)))
                .setDescription(config.getString(description).replace("$SLOTS$", Integer.toString(slotNumber)))
                .setParent(config.getInt("parentchannel[@id]"));

        if (slotNumber == -1)
            properties.setFlag(ChannelFlagConstants.MAXCLIENTS_UNLIMITED | ChannelFlagConstants.MAXFAMILYCLIENTS_UNLIMITED);
        else properties.setMaxFamilyClients(slotNumber).setMaxClients(slotNumber);

        return properties;
    }

    private String formatSlots(int slotNumber) {
        String template = "%d %s";
        String slot = slotNumber < 5 ? config.getString("slot[@singular]") : config.getString("slot[@plural]");
        return String.format(template, slotNumber, slot);
    }
}
