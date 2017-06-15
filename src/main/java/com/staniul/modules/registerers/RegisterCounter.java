package com.staniul.modules.registerers;

import com.staniul.util.SerializeUtil;
import com.staniul.xmlconfig.UseConfig;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.File;
import java.io.IOException;
import java.util.List;

@Component
@UseConfig("modules/regc.xml")
public class RegisterCounter {
    private static Logger log = Logger.getLogger(RegisterCounter.class);

    private final String dataFile = "./data/regc.data";
    private List<DateRegisters> data;

    @Autowired
    public RegisterCounter() {

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

    }

    @PreDestroy
    private void save() {
        try {
            SerializeUtil.serialize(dataFile, data);
        } catch (IOException e) {
            log.error("Failed to serialize file with data about admins registered clients count.", e);
        }
    }
}
