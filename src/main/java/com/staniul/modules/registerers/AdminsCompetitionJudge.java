package com.staniul.modules.registerers;

import com.staniul.modules.registerers.adminoftheweek.AdminOfTheWeek;
import com.staniul.security.clientaccesscheck.ClientGroupAccess;
import com.staniul.teamspeak.commands.CommandResponse;
import com.staniul.teamspeak.query.Client;
import com.staniul.teamspeak.query.ClientDatabase;
import com.staniul.teamspeak.query.Query;
import com.staniul.teamspeak.query.QueryException;
import com.staniul.xmlconfig.CustomXMLConfiguration;
import com.staniul.xmlconfig.annotations.WireConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

//@Component
//@UseConfig("modules/aotw.xml")
@Deprecated
public class AdminsCompetitionJudge {
    private static Logger log = LogManager.getLogger(AdminsCompetitionJudge.class);

    private final String dataFile = "./data/aotw.data";

    @WireConfig
    private CustomXMLConfiguration config;
    private Query query;
    private RegisterCounter registerCounter;
    private AdminOfTheWeek currentAdminOfTheWeek;

    @Autowired
    public AdminsCompetitionJudge(Query query, RegisterCounter registerCounter) {
        this.query = query;
        this.registerCounter = registerCounter;
    }

    @PostConstruct
    private void init() {
        log.info("Loading admin of the week data...");
        File file = new File(dataFile);
        if (file.exists() && file.isFile()) {
            log.info("File exists, loading data...");
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line = reader.readLine();
                String[] split = line.split("\\s+");
                currentAdminOfTheWeek = new AdminOfTheWeek(Integer.parseInt(split[0]), Integer.parseInt(split[1]));
            } catch (IOException e) {
                log.error("Failed to load admin of the week from file, so there will be no admin of the week", e);
                currentAdminOfTheWeek = new AdminOfTheWeek(-1, -1);
            } catch (Exception e) {
                log.error("Malformed admin of the week data!", e);
                currentAdminOfTheWeek = new AdminOfTheWeek(-1, -1);
            }
        }
        else {
            log.info("Admin of the week data file does not exists, so there is no admin of the week.");
            currentAdminOfTheWeek = new AdminOfTheWeek(-1, -1);
        }

    }

    @PreDestroy
    private void save() {
        log.info("Saving admin of the week data to file...");
        try (PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(dataFile)), true)) {
            writer.printf("%d %d\n", currentAdminOfTheWeek.getClientDatabaseId(), currentAdminOfTheWeek.getPreviousAdminGroup());
        } catch (IOException e) {
            log.error("Failed to write admin of the week data to a file.", e);
        }
    }

//    @Teamspeak3Command("!refaotw")
    @ClientGroupAccess("servergroups.headadmins")
    public CommandResponse refreshAdminOfTheWeekOnCall(Client client, String params) {
        try {
            assignNewAdminOfTheWeek();
            return new CommandResponse(config.getString("commands.refaotw[@success]"));
        } catch (QueryException e) {
            log.error("Failed to assign new admin of the week!", e);
            return new CommandResponse(config.getString("commands.refaotw[@fail]"));
        }
    }

//    @Task(delay = 7 * 24 * 60 * 60 * 1000, day = 7, hour = 0, minute = 5, second = 0)
    public void assignNewAdminOfTheWeekTask() {
        try {
            assignNewAdminOfTheWeek();
        } catch (QueryException e) {
            log.error("Failed to assign new admin of the week!", e);
        }
    }

    private void assignNewAdminOfTheWeek() throws QueryException {
        int newAdminsId = countNewAdminOfTheWeek();
        changeAdminGroups(newAdminsId);
        updateDisplay();
    }

    private int countNewAdminOfTheWeek() {
        DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd");
        DateTime dateTime = DateTime.now().minusDays(1);

        Map<Integer, Integer> data = new HashMap<>();

        for (int i = 0; i < 7; i++) {
            Map<Integer, Integer> regData = registerCounter.getRegisteredAtDate(formatter.print(dateTime));
            if (regData != null)
                regData.forEach((k, v) -> data.compute(k, (a, b) -> b == null ? v : b + v));
            dateTime = dateTime.minusDays(1);
        }

        int bestAdmin = -1;
        int regCount = -1;
        for (Map.Entry<Integer, Integer> entry : data.entrySet()) {
            if (bestAdmin == -1 && regCount == -1) {
                bestAdmin = entry.getKey();
                regCount = entry.getValue();
            }
            else if (entry.getValue() > regCount) {
                bestAdmin = entry.getKey();
                regCount = entry.getValue();
            }
            else if (entry.getValue() == regCount) {
                bestAdmin = -1;
            }
        }

        return bestAdmin;
    }

    private void changeAdminGroups(int newAdminsId) throws QueryException {
        if (currentAdminOfTheWeek.getClientDatabaseId() != newAdminsId) {
            if (currentAdminOfTheWeek.getClientDatabaseId() != -1) {
                query.servergroupAddClient(currentAdminOfTheWeek.getClientDatabaseId(), currentAdminOfTheWeek.getPreviousAdminGroup());
                query.servergroupDeleteClient(currentAdminOfTheWeek.getClientDatabaseId(), config.getInt("aotw-group[@id]"));
            }
        }

        if (newAdminsId == -1)
            currentAdminOfTheWeek = new AdminOfTheWeek(-1, -1);
        else {
            if (currentAdminOfTheWeek.getClientDatabaseId() != newAdminsId) {
                int previousAdminGroupId = getPreviousAdminGroupId(newAdminsId);
                query.servergroupAddClient(newAdminsId, config.getInt("aotw-group[@id]"));
                query.servergroupDeleteClient(newAdminsId, previousAdminGroupId);
                currentAdminOfTheWeek = new AdminOfTheWeek(newAdminsId, previousAdminGroupId);
            }
        }
    }

    private int getPreviousAdminGroupId(int newAdminsId) throws QueryException {
        Set<Integer> adminGroups = config.getIntSet("admin-groups[@ids]");
        for (Integer adminGroup : adminGroups) {
            List<Integer> adminIds = query.servergroupClientList(adminGroup);
            if (adminIds.contains(newAdminsId))
                return adminGroup;
        }

        throw new IllegalArgumentException("Client is not an administrator!");
    }

    private void updateDisplay() throws QueryException {
        if (currentAdminOfTheWeek.getClientDatabaseId() == -1) {
            query.channelRename(config.getString("no-display[@name]"), config.getInt("no-display[@id]"));
            query.channelChangeDescription("", config.getInt("no-display[@id]"));
            return;
        }

        ClientDatabase adminDatabaseInfo = query.getClientDatabaseInfo(currentAdminOfTheWeek.getClientDatabaseId());

        String channelName = config.getString("display[@name]").replace("$NICKNAME$", adminDatabaseInfo.getNickname());
        try {
            query.channelRename(channelName, config.getInt("display[@id]"));
        } catch (QueryException e) {
            log.error("Failed to change admin of the week channel name, probably new aotw is same admin.", e);
        }

        StringBuilder description = new StringBuilder("[CENTER][SIZE=13][COLOR=ORANGE]");
        description.append(config.getString("display[@header]")).append("[/COLOR]\n\n")
                .append("[IMG]").append(config.getString("display[@icon]")).append("[/IMG]")
                .append(" [B]").append(adminDatabaseInfo.getNickname()).append("[/B]\n\n");

        try {
            String avatar = moveAvatar(adminDatabaseInfo);
            description.append("[IMG]").append(avatar).append("[/IMG]");
        } catch (IOException e) {
            log.error("Failed to move clients avatar.", e);
            //Avatar does not exists so we just skip adding it.
        }

        query.channelChangeDescription(description.toString(), config.getInt("display[@id]"));
    }

    private String moveAvatar(ClientDatabase adminDatabaseInfo) throws IOException {
        String origin = config.getString("avatars[@origin]").replace("$BASE64HASH$", adminDatabaseInfo.getHash64UniqueId());

        String date = DateTimeFormat.forPattern("yyyy-MM-dd").print(DateTime.now());
        String destination = config.getString("avatars[@destination]").replace("$DATE$", date);

        Files.copy(Paths.get(origin), Paths.get(destination), StandardCopyOption.REPLACE_EXISTING);

        return config.getString("avatars[@url]").replace("$DATE$", date);
    }

    public AdminOfTheWeek getCurrentAdminOfTheWeek() {
        return currentAdminOfTheWeek;
    }
}
