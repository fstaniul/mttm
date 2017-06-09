package com.staniul.query.channel;

import org.junit.Test;

import java.awt.geom.FlatteningPathIterator;
import java.util.HashMap;

import static org.junit.Assert.*;
import static com.staniul.query.channel.ChannelFlagConstants.*;

public class ChannelFlagConstantsTest {
    @Test
    public void flagsAsString() throws Exception {
        String flags = new ChannelFlags(__DEFAULT_FLAGS).toString();
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
        String flags = new ChannelFlags(SEMI_PERMANENT | MAXFAMILYCLIENTS_UNLIMITED | DEFAULT).toString();
        String expected = "channel_flag_permanent=0 " +
                "channel_flag_semi_permanent=1 " +
                "channel_flag_maxclients_unlimited=0 " +
                "channel_flag_maxfamilyclients_unlimited=0 " +
                "channel_flag_maxfamilyclients_inherited=1 " +
                "channel_flag_default=1";
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
        ChannelFlags flags = ChannelFlags.parseFlags(testMap);

        assertFalse(flags.isPermanent());
        assertTrue(flags.isSemiPermanent());
        assertFalse(flags.areMaxClientsUnlimited());
        assertFalse(flags.areFamilyMaxClientsUnlimited());
        assertTrue(flags.isDefaultChannel());
        assertTrue(flags.isPasswordProtected());
    }

}