package com.staniul.modules.registerers;

import com.staniul.taskcontroller.Task;
import com.staniul.teamspeak.query.ClientDatabase;
import com.staniul.teamspeak.query.Query;
import com.staniul.teamspeak.query.QueryException;
import com.staniul.util.lang.SerializeUtil;
import com.staniul.xmlconfig.CustomXMLConfiguration;
import com.staniul.xmlconfig.annotations.UseConfig;
import com.staniul.xmlconfig.annotations.WireConfig;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
@UseConfig("modules/regc.xml")
public class RegisterCounter {
    private static Logger log = Logger.getLogger(RegisterCounter.class);

    private final String dataFile = "./data/regc.data";
    private Map<String, Map<Integer, Integer>> data;

    @WireConfig
    private CustomXMLConfiguration config;
    private final Query query;

    @Autowired
    public RegisterCounter(Query query) {
        this.query = query;
    }

    @PostConstruct
    private void init() {
        File file = new File(dataFile);
        if (file.exists() && file.isFile()) {
            try {
                data = SerializeUtil.deserialize(dataFile);
            } catch (IOException | ClassNotFoundException e) {
                log.error("Failed to load serialized data about admins registered clients count.", e);
                createDataFromLogs();
            }
        }
        else createDataFromLogs();
    }

    private void createDataFromLogs() {
        File folder = new File(config.getString("log[@folder]"));
        if (!folder.exists() || !folder.isDirectory()) throw new IllegalStateException("Specified folder is not a log folder!");

        List<File> logfiles = Arrays.stream(folder.listFiles())
                .filter(f -> f.getName().matches("ts3server_.*_1.log"))
                .collect(Collectors.toList());

        String regGroupsRegex = String.join("|", config.getList(String.class, "register-groups.group[@id]"));
        //2016-02-01 18:06:53.904660|INFO    |VirtualServer |  1| client (id:27010) was added to servergroup 'Zarejestrowany'(id:641) by client 'Markus'(id:25767)
        Pattern regPattern = Pattern.compile("^(\\d{4}-\\d{2}-\\d{2}).*client \\(id:(\\d+)\\) was added to servergroup .*\\(id:(" + regGroupsRegex + ")\\) by client .*\\(id:(\\d+)\\)");

        Map<String, Map<Integer, Set<Integer>>> data = new HashMap<>();

        for (File file : logfiles) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    Matcher match = regPattern.matcher(line);
                    if (match.find()) {
                        String date = match.group(1);
                        int clientId = Integer.parseInt(match.group(2));
                        int adminId = Integer.parseInt(match.group(4));

                        data.putIfAbsent(date, new HashMap<>());
                        Map<Integer, Set<Integer>> dataAtDate = data.get(date);
                        dataAtDate.putIfAbsent(adminId, new HashSet<>());
                        Set<Integer> regClients = dataAtDate.get(adminId);
                        regClients.add(clientId);
                    }
                }
            } catch (IOException e) {
                log.error(String.format("Failed to read from log file %s", file), e);
            }
        }

        this.data = new HashMap<>();
        data.forEach((k, v) -> {
            HashMap<Integer, Integer> inner = new HashMap<>();
            v.forEach((a, b) -> inner.put(a, b.size()));
            this.data.put(k, inner);
        });

        try {
            List<Integer> adminIds = query.servergroupClientList(config.getIntSet("admin-groups[@ids]"));
            this.data.forEach((date, regMap) ->
                    regMap.forEach((adminId, regCount) ->
                            regMap.compute(adminId, (aId, rC) -> adminIds.contains(aId) ? rC : null)));
        } catch (QueryException e) {
            log.error("Failed to get admin list from teamspeak 3 server!", e);
        }

//        this.data = new HashMap<>();
//        for (Map.Entry<String, Map<Integer, Set<Integer>>> entry : data.entrySet()) {
//            this.data.putIfAbsent(entry.getKey(), new HashMap<>());
//            for (Map.Entry<Integer, Set<Integer>> inEntry : entry.getValue().entrySet()) {
//                this.data.get(entry.getKey())
//                        .putIfAbsent(inEntry.getKey(), inEntry.getValue().size());
//            }
//        }
    }

    @PreDestroy
    private void save() {
        try {
            SerializeUtil.serialize(dataFile, data);
        } catch (IOException e) {
            log.error("Failed to serialize file with data about admins registered clients count.", e);
        }
    }

    @Task(delay = 24 * 60 * 60 * 1000, hour = 0, minute = 5, second = 0)
    public void countRegisteredAtNoon () {
        File logFolder = new File(config.getString("log[@folder"));
        File[] logFiles = logFolder.listFiles();

        if (logFiles == null) throw new IllegalStateException("Log folder empty!");

        DateTime yesterday = DateTime.now().minusDays(1);
        DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd");

        List<File> files = Arrays.stream(logFiles).filter(f -> f.getName().matches("ts3server_.*_1.log")).collect(Collectors.toList());
        files.sort(Comparator.comparing(File::getName));

        Pattern pattern = Pattern.compile("ts3server_" + formatter.print(yesterday) + ".*_1.log");
        List<File> chosen = new ArrayList<>(10);
        chosen.addAll(files.stream().filter(f -> pattern.matcher(f.getName()).matches()).collect(Collectors.toList()));
        chosen.add(files.get(chosen.size()));

        String regGroupsRegex = String.join("|", config.getList(String.class, "register-groups.group[@id]"));
        Pattern regPattern = Pattern.compile(formatter.print(yesterday) + ".*client \\(id:(\\d+)\\) was added to servergroup .*\\(id:(" + regGroupsRegex + ")\\) by client .*\\(id:(\\d+)\\)");

        Map<Integer, Set<Integer>> registeredYesterday = new HashMap<>();

        for (File file : chosen) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    Matcher matcher = regPattern.matcher(line);
                    if (matcher.find()) {
                        int clintId = Integer.parseInt(matcher.group(1));
                        int adminId = Integer.parseInt(matcher.group(3));

                        registeredYesterday.putIfAbsent(adminId, new HashSet<>());
                        registeredYesterday.get(adminId).add(clintId);
                    }
                }
            } catch (IOException e) {
                log.error(String.format("Failed to read log file %s", file), e);
            }
        }

        Map<Integer, Integer> countedReg = new HashMap<>();
        registeredYesterday.forEach((k, v) -> countedReg.put(k, v.size()));

        try {
            List<Integer> adminIds = query.servergroupClientList(config.getIntSet("admin-groups[@ids]"));
            countedReg.forEach((adminId, regCount) -> countedReg.compute(adminId, (aId, rC) -> adminIds.contains(aId) ? rC : null));
        } catch (QueryException e) {
            log.error("Failed to get admin list from teamspeak 3 server!", e);
        }

        data.put(formatter.print(yesterday), countedReg);

        refreshDisplay(formatter.print(yesterday));
    }

    public Map<Integer, Integer> getRegisteredAtDate (String date) {
        return Collections.unmodifiableMap(data.get(date));
    }

    private void refreshDisplay (String date) {
        try {
            Map<Integer, Integer> regAtDate = getRegisteredAtDate(date);
//            List<ClientDatabase> adminList = new ArrayList<>();
//            for (Map.Entry<Integer, Integer> e : regAtDate.entrySet()) {
//                adminList.add(query.getClientDatabaseInfo(e.getKey()));
//            }

            List<Integer> adminIds = query.servergroupClientList(config.getIntSet("admin-groups[@ids]"));
            List<ClientDatabase> adminList = new ArrayList<>();
            for (Integer adminId : adminIds)
                adminList.add(query.getClientDatabaseInfo(adminId));


            StringBuilder sb = new StringBuilder();
            sb.append(config.getString("registered-display[@header]").replace("$DATE$", date)).append("\n");
            for (ClientDatabase admin : adminList) {
                Integer regAtThisDate = regAtDate.get(admin.getDatabaseId());
                sb.append("[B]")
                        .append(String.format("%3d", regAtThisDate == null ? 0 : regAtThisDate))
                        .append("[/B]")
                        .append(" | ")
                        .append(admin.getNickname())
                        .append("\n");
            }

            query.channelChangeDescription(sb.toString(), config.getInt("registered-display[@id]"));
        } catch (QueryException e) {
            log.error("Failed to display registered clients by admins at date " + date, e);
        }
    }
}