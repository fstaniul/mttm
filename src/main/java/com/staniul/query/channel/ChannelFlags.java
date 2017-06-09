package com.staniul.query.channel;

import java.util.Map;

/**
 * Stores channel flags
 */
public class ChannelFlags implements ChannelFlagConstants {
    /**
     * Flags stored in {@code int} number. Each bit represents a flag.
     */
    private int flags;

    public ChannelFlags() {
        this(0);
    }

    public ChannelFlags(int flags) {
        this.flags = flags;
    }

    public boolean isPermanent () {
        return (flags & PERMANENT) == 1;
    }

    public boolean isSemiPermanent () {
        return (flags & SEMI_PERMANENT) == SEMI_PERMANENT;
    }

    public boolean areMaxClientsUnlimited () {
        return (flags & MAXCLIENTS_UNLIMITED) == MAXCLIENTS_UNLIMITED;
    }

    public boolean areFamilyMaxClientsUnlimited () {
        return (flags & MAXFAMILYCLIENTS_UNLIMITED) == MAXFAMILYCLIENTS_UNLIMITED;
    }

    public boolean isPasswordProtected () {
        return (flags & PASSWORD) == PASSWORD;
    }

    public boolean isDefaultChannel () {
        return (flags & DEFAULT) == DEFAULT;
    }

    /**
     * Sets a flag. If flag was already set it will disable the flag. It just simply does a XOR with flag.
     * @param flag One of {@link ChannelFlagConstants}.
     */
    public void set (int flag) {
        flags ^= flag;
    }

    /**
     * Transforms flags stored as {@code int} number into a readable by teamspeak 3 server query set of flags
     * contained in one string. Used by channel properties for setting flags on new created channel and by Channel for
     * reading flags.
     *
     * @return String containing flags readable by teamspeak 3 query.
     */
    @Override
    public String toString() {
        return "channel_flag_permanent=" + (flags & PERMANENT) + " " +
                "channel_flag_semi_permanent=" + (flags & SEMI_PERMANENT) / SEMI_PERMANENT + " " +
                "channel_flag_maxclients_unlimited=" + (flags & MAXCLIENTS_UNLIMITED) / MAXCLIENTS_UNLIMITED + " " +
                "channel_flag_maxfamilyclients_unlimited=" + (flags & MAXFAMILYCLIENTS_UNLIMITED) / MAXFAMILYCLIENTS_UNLIMITED + " " +
                "channel_flag_maxfamilyclients_inherited=0 " +
                "channel_flag_default=" + (flags & DEFAULT) / DEFAULT;
    }

    /**
     * <p>Reads flags from map containing channel information. This method should be used only by {@link
     * com.staniul.query.Query Query} and {@link com.staniul.query.Channel} classes to parse teamspeak 3 server query
     * information.</p>
     * <p>If you want to get channel flags and/or information go see {@link
     * com.staniul.query.Query#getChannelInfo(int)}</p>
     *
     * @param channelInfo Information about channel read from teamspeak 3 query contained in map.
     *
     * @return {@code int} that represents flags of teamspeak 3 channel.
     */
    public static ChannelFlags parseFlags(Map<String, String> channelInfo) {
        int flags = 0;
        flags |= Integer.parseInt(channelInfo.get("channel_flag_permanent")) * PERMANENT;
        flags |= Integer.parseInt(channelInfo.get("channel_flag_semi_permanent")) * SEMI_PERMANENT;

        int maxclients = Integer.parseInt(channelInfo.get("channel_maxclients"));
        flags |= maxclients == -1 ? MAXCLIENTS_UNLIMITED : 0;

        int maxfamilyclients = Integer.parseInt(channelInfo.get("channel_maxfamilyclients"));
        flags |= maxfamilyclients == -1 ? MAXFAMILYCLIENTS_UNLIMITED : 0;

        flags |= Integer.parseInt(channelInfo.get("channel_flag_password")) * PASSWORD;
        flags |= Integer.parseInt(channelInfo.get("channel_flag_default")) * DEFAULT;

        return new ChannelFlags(flags);
    }
}
