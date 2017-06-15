package com.staniul.modules.registerers;

import org.joda.time.DateTime;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

public class DateRegisters implements Serializable {
    private DateTime dateTime;
    private List<RegisteredCount> registered;

    public DateRegisters(DateTime dateTime, List<RegisteredCount> registered) {
        this.dateTime = dateTime.withMillisOfSecond(0).withSecondOfMinute(0).withMinuteOfHour(0).withHourOfDay(0);
        this.registered = registered;
    }

    public DateTime getDateTime() {
        return dateTime;
    }

    public List<RegisteredCount> getRegistered() {
        return Collections.unmodifiableList(registered);
    }
}
