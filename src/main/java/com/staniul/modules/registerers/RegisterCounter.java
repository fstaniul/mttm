package com.staniul.modules.registerers;

import com.staniul.security.clientaccesscheck.ClientGroupAccess;
import com.staniul.taskcontroller.Task;
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
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.ReadablePartial;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
@UseConfig("modules/regc.xml")
public class RegisterCounter {
    private static Logger log = LogManager.getLogger(RegisterCounter.class);

    @WireConfig
    private CustomXMLConfiguration config;

    private final Query query;
    private final String dataFile = "./data/regc.data";
    private final DateTimeFormatter dateFormatter = DateTimeFormat.forPattern("yyyy-MM-dd");

    private HashMap<String, HashMap<Integer, Integer>> data;

    @Autowired
    public RegisterCounter(Query query) {
        this.query = query;
    }

    @PostConstruct
    private void init() {
        log.info("Loading data for Register Counter...");
        File file = new File(dataFile);
        if (file.exists() && file.isFile()) {
            log.info("File exists loading data from file...");
            HashMap<String, HashMap<Integer, Integer>> data = null;

            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                int numberOfLines = Integer.parseInt(reader.readLine());
                data = new HashMap<>(numberOfLines * 100 / 70);

                String line;
                while ((line = reader.readLine()) != null) {
                    String[] spl = line.split("\\s+");
                    data.putIfAbsent(spl[0], new HashMap<>());

                    HashMap<Integer, Integer> adminRegs = data.get(spl[0]);
                    for (int i = 1; i + 1 < spl.length; i++) {
                        int adminId = Integer.parseInt(spl[i]);
                        int regCount = Integer.parseInt(spl[i + 1]);
                        adminRegs.put(adminId, regCount);
                    }
                }

                log.info("Done.");
            } catch (Exception e) {
                log.error("Error occurred while loading from file, falling back to reading from teamspeak 3 logs.");
                loadFromTeamspeak3ServerLogs();
            }

            this.data = data;
        }
        else loadFromTeamspeak3ServerLogs();
    }

    private void loadFromTeamspeak3ServerLogs() {
        List<File> logFiles = getLogFiles();

        HashMap<String, HashMap<Integer, Integer>> data = new HashMap<>();
        Pattern regPattern = Pattern.compile(getMatcherStringWithoutDeclaredDate());

        for (File file : logFiles) {
            log.info("Counting admin reg in file " + file.getName() + ".");
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    Matcher matcher = regPattern.matcher(line);
                    if (matcher.find()) {
                        try {
                            String date = matcher.group(1);
                            int adminId = Integer.parseInt(matcher.group(5));

                            data.putIfAbsent(date, new HashMap<>());
                            HashMap<Integer, Integer> adminRegs = data.get(date);
                            adminRegs.putIfAbsent(adminId, 0);
                            adminRegs.put(adminId, adminRegs.get(adminId) + 1);
                        } catch (NumberFormatException e) {
                            log.error("Admin id is not an integer number! SOMETHING WENT WRONG!", e);
                        }
                    }
                }
            } catch (IOException e) {
                log.error("Failed to read log file while reading registered clients from teamspeak 3 server logs.", e);
            }
        }

        this.data = data;
    }

    @PreDestroy
    private void save() {
        List<Map.Entry<String, HashMap<Integer, Integer>>> dataEntrySet = new LinkedList<>(data.entrySet());
        dataEntrySet.sort(Comparator.comparing(Map.Entry::getKey));

        try (PrintWriter writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(dataFile, false), StandardCharsets.UTF_8)), true)) {
            writer.printf("%d\n", dataEntrySet.size());
            for (Map.Entry<String, HashMap<Integer, Integer>> dateEntry : dataEntrySet) {
                writer.print(dateEntry.getKey());
                for (Map.Entry<Integer, Integer> adminRegEntry : dateEntry.getValue().entrySet()) {
                    writer.printf(" %d %d", adminRegEntry.getKey(), adminRegEntry.getValue());
                }
                writer.print("\n");
            }
            writer.flush();
        } catch (IOException e) {
            log.error("Failed to registered data to a file!", e);
        }
    }

    private List<File> getLogFiles() {
        String folderPath = config.getString("logs.folder");
        File folder = new File(folderPath);

        if (!folder.exists() && !folder.isDirectory())
            throw new IllegalStateException("Incorrect config entry for teamspeak 3 log folder!");

        File[] files = folder.listFiles();

        if (files == null)
            throw new IllegalStateException("There are no files in the folder! This folder is not teamspeak 3 log folder!");

        int serverId = config.getInt("logs.serverid");
        String matcherStr = "ts3server_.*_" + serverId + "\\.log";

        return Arrays.stream(files)
                .filter(f -> f.getName().matches(matcherStr))
                .collect(Collectors.toList());
    }

    /**
     * Returns a matcher string for a log file line to check for registered clients by admins.
     * Groups:
     * 1: Date
     * 2: Client Id
     * 3: Servergroup Id
     * 4: Admins Nickname
     * 5: Admins Id
     *
     * @param date Date for which this string will match
     *
     * @return A string that is matcher for log file line for registering client.
     */

    private String getMatcherStringAtDate(ReadablePartial date) {
        String dateStr = dateFormatter.print(date);
        return getMatcherStringAtDate(dateStr);
    }

    private String getMatcherStringAtDate(String date) {
        String registeredGroups = config.getString("groups.registered").replace(",", "|");
        return String.format("(%s) .*client \\(id:(\\d+)\\) was added to servergroup '.*'\\(id:(%s)\\) by client '(.*)'\\(id:(\\d+)\\)", date, registeredGroups);
    }


    /**
     * Returns a matcher string for a log file line to check for registered clients by admins.
     * Any date will be valid for this.
     * Groups:
     * 1: Date
     * 2: Client Id
     * 3: Servergroup Id
     * 4: Admins Nickname
     * 5: Admins Id
     *
     * @return A string that is matcher for log file line for registering client.
     */
    private String getMatcherStringWithoutDeclaredDate() {
        String registeredGroups = config.getString("groups.registered").replace(",", "|");
        return String.format("(\\d{4}-\\d{2}-\\d{2}) .*client \\(id:(\\d+)\\) was added to servergroup '.*'\\(id:(%s)\\) by client '(.*)'\\(id:(\\d+)\\)", registeredGroups);
    }

    public HashMap<Integer, Integer> getRegisteredAtDate(LocalDate date) {
        return getRegisteredAtDate(dateFormatter.print(date));
    }

    public HashMap<Integer, Integer> getRegisteredAtDate(DateTime date) {
        String dateStr = dateFormatter.print(date);
        return getRegisteredAtDate(dateStr);
    }

    public HashMap<Integer, Integer> getRegisteredAtDate(String date) {
        HashMap<Integer, Integer> atDate = data.get(date);
        return atDate == null ? new HashMap<>() : atDate;
    }

    @Task(delay = 24 * 60 * 60 * 1000, hour = 0, minute = 0, second = 10)
    public void countRegisteredAtNoon() throws QueryException {
        List<File> files = getLogFiles();
        files.sort(Comparator.comparing(File::getName));

        HashMap<Integer, Integer> registeredYesterday = new HashMap<>();

        LocalDate yesterdayDate = LocalDate.now().minusDays(1);

        Pattern fileDatePattern = Pattern.compile("ts3server_(\\d{4}-\\d{2}-\\d{2})__.*\\.log");
        Pattern registeredLinePattern = Pattern.compile(getMatcherStringAtDate(yesterdayDate));

            for (int i = files.size() - 1; i >= 0; i--) {
                File file = files.get(i);
                Matcher fileDateMatcher = fileDatePattern.matcher(file.getName());
                if (fileDateMatcher.find()) {

                //Read the file and get info from it:
                try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        Matcher lineMatcher = registeredLinePattern.matcher(line);
                        if (lineMatcher.find()) {
                            int adminId = Integer.parseInt(lineMatcher.group(5));
                            registeredYesterday.putIfAbsent(adminId, 0);
                            registeredYesterday.put(adminId, registeredYesterday.get(adminId) + 1);
                        }
                    }
                } catch (IOException e) {
                    log.error("Failed to read the info from log file " + file.getName(), e);
                }

                LocalDate fileDate = dateFormatter.parseLocalDate(fileDateMatcher.group(1));
                if (fileDate.isBefore(yesterdayDate)) break;
            }
        }

        data.put(dateFormatter.print(yesterdayDate), registeredYesterday);

        refreshDisplay();
    }

    private void refreshDisplay() throws QueryException {
        //Date that represents yesterday
        String date = dateFormatter.print(LocalDate.now().minusDays(1));

        //Get admins information from teamspeak 3 server.
        Set<Integer> adminGroups = config.getIntSet("groups.admins");
        List<ClientDatabase> admins = new LinkedList<>();
        for (int adminGroupId : adminGroups)
            admins.addAll(query.getClientDatabaseListInServergroup(adminGroupId));
        admins.sort(Comparator.comparing(ClientDatabase::getNickname));

        HashMap<Integer, Integer> regYesterday = data.get(date);
        if (regYesterday == null) {
            countRegisteredAtNoon();
            regYesterday = data.get(date);
        }

        //Create description
        StringBuilder desc = new StringBuilder();
        desc.append(config.getString("display.header").replace("$DATE$", date)).append("\n");
        for (ClientDatabase admin : admins) {
            int ryCount = regYesterday.get(admin.getDatabaseId()) == null ? 0 : regYesterday.get(admin.getDatabaseId());
            desc.append("[b]")
                    .append(ryCount)
                    .append("[/b] | ")
                    .append(admin.getNickname())
                    .append("\n");
        }

        //Update description
        query.channelChangeDescription(desc.toString(), config.getInt("display.channel-id"));
    }

    @Teamspeak3Command("!refregc")
    @ClientGroupAccess("servergroups.headadmins")
    public CommandResponse refreshRegisterCounterDisplay(Client client, String params) throws QueryException {
        refreshDisplay();
        return new CommandResponse("Refreshed Register Counter display.");
    }
}
