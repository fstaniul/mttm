package com.staniul.teamspeak.modules.registerers.adminoftheweek;

import com.staniul.teamspeak.modules.registerers.RegisterCounter;
import com.staniul.teamspeak.security.clientaccesscheck.ClientGroupAccess;
import com.staniul.teamspeak.taskcontroller.Task;
import com.staniul.teamspeak.commands.CommandResponse;
import com.staniul.teamspeak.commands.Teamspeak3Command;
import com.staniul.teamspeak.query.Client;
import com.staniul.teamspeak.query.ClientDatabase;
import com.staniul.teamspeak.query.Query;
import com.staniul.teamspeak.query.QueryException;
import com.staniul.xmlconfig.CustomXMLConfiguration;
import com.staniul.xmlconfig.annotations.UseConfig;
import com.staniul.xmlconfig.annotations.WireConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Component
@UseConfig("modules/aotw2.xml")
public class AdminCompetitionJudge2 {
    //Logger
    private static Logger log = LogManager.getLogger(AdminCompetitionJudge2.class);

    //Config file
    @WireConfig
    private CustomXMLConfiguration config;

    //Other modules
    private final Query query;
    private final RegisterCounter registerCounter;

    //Constants used
    private final String dataFile = "./data/aotw2.data";

    //Current admin of the week
    private AdminOfTheWeek adminOfTheWeek;

    @Autowired
    public AdminCompetitionJudge2(Query query, RegisterCounter registerCounter) {
        this.query = query;
        this.registerCounter = registerCounter;
    }

    @PostConstruct
    private void init () {
        //Read previous admin of the week from file:
        File file = new File (dataFile);
        if (file.exists() && file.isFile()) {
            String[] infoSpl = null;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
                infoSpl = reader.readLine().split("\\s+");
            } catch (IOException e) {
                log.error("Failed to load admin of the week from file!");
                adminOfTheWeek = AdminOfTheWeek.NONE;
            }
            if (infoSpl != null) {
                adminOfTheWeek = new AdminOfTheWeek(Integer.parseInt(infoSpl[0]), Integer.parseInt(infoSpl[1]));
            } else adminOfTheWeek = AdminOfTheWeek.NONE;
        }
        else adminOfTheWeek = AdminOfTheWeek.NONE;
    }

    @PreDestroy
    private void save () {
        try (PrintWriter writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(dataFile, false), StandardCharsets.UTF_8)), true)) {
            writer.print(adminOfTheWeek);
        } catch (FileNotFoundException e) {
            log.error("Failed to save admin of the week to file, dumping here: " + adminOfTheWeek);
        }
    }

    @Task(delay = 7 * 24 * 60 * 60 * 1000, day = 7, hour = 0, minute = 5, second = 0)
    public void assignNewAdminOfTheWeek () throws QueryException, IOException {
        removeCurrent();
        AdminInfo newBestAdmin = countBestAdmin();

        if (newBestAdmin == null) adminOfTheWeek = AdminOfTheWeek.NONE;
        else {
            adminOfTheWeek = new AdminOfTheWeek(newBestAdmin.getId(), newBestAdmin.getGroup());
            query.servergroupAddClient(adminOfTheWeek.getClientDatabaseId(), config.getInt("groups.aotw"));
            query.servergroupDeleteClient(adminOfTheWeek.getClientDatabaseId(), adminOfTheWeek.getPreviousAdminGroup());
        }

        refreshDisplay();
    }

    private void removeCurrent() throws QueryException {
        if (!adminOfTheWeek.equals(AdminOfTheWeek.NONE)) {
            query.servergroupDeleteClient(adminOfTheWeek.getClientDatabaseId(), config.getInt("groups.aotw"));
            query.servergroupAddClient(adminOfTheWeek.getClientDatabaseId(), adminOfTheWeek.getPreviousAdminGroup());
        }
    }

    private AdminInfo countBestAdmin() throws QueryException {
        HashMap<Integer, AdminInfo> adminsData = createMapFromAdminsFromServer ();

        //Get data for one week for each admin:
        LocalDate date = LocalDate.now().minusDays(1);
        for (int i = 0; i < 7; i++, date = date.minusDays(1)) {
            HashMap<Integer, Integer> registeredAtDate = registerCounter.getRegisteredAtDate(date);
            for (Map.Entry<Integer, Integer> entry : registeredAtDate.entrySet()) {
                if (adminsData.containsKey(entry.getKey())) {
                    adminsData.get(entry.getKey()).addRegistered(entry.getValue());
                }
            }
        }

        return adminsData.entrySet().stream()
                .max(Comparator.comparing(a -> a.getValue().getRegisteredCount()))
                .map(Map.Entry::getValue).orElse(null);
    }

    private HashMap<Integer, AdminInfo> createMapFromAdminsFromServer() throws QueryException {
        HashMap<Integer, AdminInfo> ret = new HashMap<>();
        Set<Integer> adminGroups = config.getIntSet("groups.admins");
        for (Integer group : adminGroups) {
            for (Integer admin : query.servergroupClientList(group)) {
                ret.put(admin, new AdminInfo(admin, group));
            }
        }

        return ret;
    }

    public AdminOfTheWeek getAdminOfTheWeek() {
        return adminOfTheWeek;
    }

    private void refreshDisplay() throws QueryException {
        String prefix;
        String avatar = null;
        ClientDatabase cdInfo = null;

        if (adminOfTheWeek.equals(AdminOfTheWeek.NONE)) {
            prefix = "display.none.";
        } else {
            prefix = "display.exist.";
            cdInfo = query.getClientDatabaseInfo(adminOfTheWeek.getClientDatabaseId());
            try {
                avatar = moveAvatar(cdInfo);
            } catch (IOException e) {
                avatar = "";
            }
        }

        String channelName = config.getString(prefix + "channel-name");
        String description = config.getString(prefix + "channel-desc");

        if (!adminOfTheWeek.equals(AdminOfTheWeek.NONE) && cdInfo != null) {
            channelName = channelName.replace("$NAME$", cdInfo.getNickname());
            description = description.replace("$NAME$", cdInfo.getNickname()).replace("$AVATAR$", avatar);
        }

        int channelId = config.getInt("display.channel-id");
        try {
            query.channelRename(channelName, channelId);
        } catch (QueryException e) {
            //Ignore error that channel name is already in use, it happens when new aotw is same as last weeks one.
            if (e.getErrorId() != 771) throw e;
        }
        query.channelChangeDescription(description, channelId);
    }

    private String moveAvatar(ClientDatabase adminDatabaseInfo) throws IOException {
        String origin = config.getString("avatars[@origin]").replace("$BASE64HASH$", adminDatabaseInfo.getHash64UniqueId());

        String date = DateTimeFormat.forPattern("yyyy-MM-dd").print(LocalDate.now());
        String destination = config.getString("avatars[@destination]").replace("$DATE$", date);

        File originFile = new File(origin);
        if (!originFile.exists() || !originFile.isFile()) return "";

        Files.copy(Paths.get(origin), Paths.get(destination), StandardCopyOption.REPLACE_EXISTING);

        return "[IMG]" + config.getString("avatars[@url]").replace("$DATE$", date) + "[/IMG]";
    }

    @Teamspeak3Command("!refaotw")
    @ClientGroupAccess("servergroups.headadmins")
    public CommandResponse refreshAdminOfTheWeekOnCall (Client client, String params) throws QueryException, IOException {
        assignNewAdminOfTheWeek();
        return new CommandResponse("Refreshed admin of the week on demand.");
    }
}
