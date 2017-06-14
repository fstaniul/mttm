package com.staniul.teamspeak.query.channel;

import org.junit.Test;

import java.util.HashMap;

import static org.junit.Assert.*;
import static com.staniul.teamspeak.query.channel.ChannelFlagConstants.*;

public class ChannelFlagConstantsTest {
    @Test
    public void flagsAsString() throws Exception {
        String flags = ChannelFlagConstants.flagsToString(__DEFAULT_FLAGS);
        String expected = "channel_flag_permanent=1 " +
                "channel_flag_semi_permanent=0 " +
                "channel_flag_maxclients_unlimited=1 " +
                "channel_flag_maxfamilyclients_unlimited=1 " +
                "channel_flag_maxfamilyclients_inherited=0 " +
                "channel_flag_default=0";

        assertEquals(expected, flags);
    }

    @Test
    public void flagsAsString1 () throws Exception {
        String flags = ChannelFlagConstants.flagsToString(SEMI_PERMANENT | MAXFAMILYCLIENTS_UNLIMITED | DEFAULT);
        String expected = "channel_flag_permanent=0 " +
                "channel_flag_semi_permanent=1 " +
                "channel_flag_maxclients_unlimited=0 " +
                "channel_flag_maxfamilyclients_unlimited=1 " +
                "channel_flag_maxfamilyclients_inherited=0 " +
                "channel_flag_default=1";

        assertEquals(expected, flags);
    }

    @Test
    public void parseFlags() throws Exception {
        HashMap<String, String> testMap = new HashMap<>();
        testMap.putIfAbsent("channel_flag_permanent", "0");
        testMap.putIfAbsent("channel_flag_semi_permanent", "1");
        testMap.putIfAbsent("channel_maxclients", "12");
        testMap.putIfAbsent("channel_maxfamilyclients", "12");
        testMap.putIfAbsent("channel_flag_default", "1");
        testMap.putIfAbsent("channel_flag_password", "1");
        int flags = ChannelFlagConstants.parseFlags(testMap);

        assertEquals(0, flags & PERMANENT);
        assertEquals(1, (flags & SEMI_PERMANENT) / SEMI_PERMANENT);
        assertEquals(0, (flags & MAXCLIENTS_UNLIMITED) / MAXCLIENTS_UNLIMITED);
        assertEquals(0, (flags & MAXFAMILYCLIENTS_UNLIMITED) / MAXFAMILYCLIENTS_UNLIMITED);
        assertEquals(1, (flags & DEFAULT) / DEFAULT);
        assertEquals(1, (flags & PASSWORD) / PASSWORD);
    }
}