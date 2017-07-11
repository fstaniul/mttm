package com.staniul.util;

import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.junit.Test;

public class TimesTests {
    @Test
    public void test () throws Exception {
        System.out.println(System.currentTimeMillis());
    }

    @Test
    public void test1 () throws Exception {
        DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd");
        LocalDate dateTime = formatter.parseLocalDate("2017-07-10");
        LocalDate date1 = formatter.parseLocalDate("2017-07-10");
        System.out.println(dateTime);

        System.out.println(dateTime.isBefore(date1));
    }
}