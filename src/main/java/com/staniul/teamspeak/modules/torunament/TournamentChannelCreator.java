package com.staniul.teamspeak.modules.torunament;

import com.staniul.teamspeak.commands.CommandResponse;
import com.staniul.teamspeak.commands.Teamspeak3Command;
import com.staniul.teamspeak.query.Channel;
import com.staniul.teamspeak.query.Client;
import com.staniul.teamspeak.query.Query;
import com.staniul.teamspeak.query.QueryException;
import com.staniul.teamspeak.query.channel.ChannelProperties;
import com.staniul.teamspeak.security.clientaccesscheck.ClientGroupAccess;
import com.staniul.teamspeak.taskcontroller.Task;
import com.staniul.teamspeak.modules.torunament.data.TournamentPlayer;
import com.staniul.teamspeak.modules.torunament.data.TournamentTeam;
import com.staniul.xmlconfig.CustomXMLConfiguration;
import com.staniul.xmlconfig.annotations.UseConfig;
import com.staniul.xmlconfig.annotations.WireConfig;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@UseConfig("modules/tournament.xml")
public class TournamentChannelCreator {
    private final Query query;

    @WireConfig
    private CustomXMLConfiguration config;

    public TournamentChannelCreator(Query query) {
        this.query = query;
    }

    private JdbcTemplate getJdbcTemplate() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(config.getString("jdbc.url"), config.getString("jdbc.login"), config.getString("jdbc.password"));
        dataSource.setDriverClassName("com.mysql.jdbc.Driver");
        return new JdbcTemplate(dataSource);
    }

    @Teamspeak3Command("!ctc")
    @ClientGroupAccess("servergroups.headadmins")
    public CommandResponse createTournamentChannelsCommand(Client client, String params) throws QueryException {
        createTournamentChannelsInner();
        return new CommandResponse(config.getString("messages.create.successful"));
    }

    @Task(delay = 60 * 60 * 1000)
    public void createTournamentChannelsTask () throws QueryException {
        createTournamentChannelsInner();
    }

    private void createTournamentChannelsInner () throws QueryException {
        final JdbcTemplate jdbcTemplate = getJdbcTemplate();
        List<TournamentTeam> teams = jdbcTemplate.query(config.getString("query"), TournamentTeam.rowMapper());

        for (TournamentTeam tournamentTeam : teams) {
            tournamentTeam.setPlayers(
                    jdbcTemplate.query(config.getString("queryTeammates"), new Object[]{tournamentTeam.getName()}, TournamentPlayer.rowMapper())
            );
        }

        List<Channel> channels = query.getChannelList();
        List<Channel> subchannels = channels.stream()
                .filter(channel -> channel.getParentId() == config.getInt("parentChannelId"))
                .collect(Collectors.toList());

        Channel parentchannel = channels.stream()
                .filter(channel -> channel.getId() == config.getInt("parentChannelId"))
                .findFirst()
                .orElse(null);

        if (parentchannel == null) throw new IllegalStateException("Parent channel for tournaments not found!");

        String channelName = config.getString("parentChannelName").replace("%TEAM_COUNT%", Integer.toString(teams.size()));
        if (!parentchannel.getName().equalsIgnoreCase(channelName))
            query.channelRename(channelName, parentchannel.getId());

        for (TournamentTeam tournamentTeam : teams) {
            Channel channel = subchannels.stream()
                    .filter(channel1 -> channel1.getName().equalsIgnoreCase(tournamentTeam.getName().length() > 40 ? tournamentTeam.getName().substring(0, 40) : tournamentTeam.getName()))
                    .findFirst()
                    .orElse(null);

            if (channel == null) {
                ChannelProperties properties = new ChannelProperties()
                        .setName(tournamentTeam.getName())
                        .setParent(config.getInt("parentChannelId"))
                        .setDescription(
                                config.getString("channel.description")
                                        .replace("%TEAM_NAME%", tournamentTeam.getName())
                                        .replace("%TEAM_SQUAD%", tournamentTeam.getPlayers().stream()
                                                .map(tp -> String.format("%s [b]%s[/b] (%s)", tp.getName(), tp.getNickname(), tp.getPosition()))
                                                .collect(Collectors.joining("\n")))
                        )
                        .setTopic(config.getString("channel.topic").replace("%TEAM_NAME%", tournamentTeam.getName()))
                        .setCodec(4)
                        .setCodecQuality(10)
                        .setMaxClients(5)
                        .setMaxFamilyClients(5);

                try {
                    query.channelCreate(properties);
                } catch (QueryException e) {
                    if (e.getErrorId() != 771) throw e;
                }
            }
        }
    }
}
