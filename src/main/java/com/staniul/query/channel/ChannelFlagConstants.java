package com.staniul.query.channel;

import java.util.Map;

/**
 * Represents constant values for channel flags!
 * Channel flag for inherited max family clients is not supported, and is set to 0 in {@link ChannelFlags#toString()}
 */
public interface ChannelFlagConstants {
    /**
     * Indicates if channel is a permanent channels. Used most of the time.
     */
    int PERMANENT = 1;

    /**
     * Indicates that channel is semi permanent which means will be destroyed after creator leaves teamspeak 3 server or
     * if at the time of creators leaving, channel is not empty, then after last person in the channel leaves channel.
     */
    int SEMI_PERMANENT = 1 << 1;

    /**
     * Indicates that channel have unlimited slots. Ofter used with another channel property that is maxclients.
     * Setting this flag on {@code ChannelProperties} will result in maxclients property being set to {@code -1}.
     */
    int MAXCLIENTS_UNLIMITED = 1 << 2;

    /**
     * Indicates that channel maxfamily clients are unlimited. Often used with another channel property that is {@code maxfamilyclients}.
     * Setting this flag on {@code ChannelProperties} will result in maxfamilyclients property being set to {@code -1}
     */
    int MAXFAMILYCLIENTS_UNLIMITED = 1 << 3;

    /**
     * Indicates if channel has password or not.
     * It is not possible to set this property directly and that's why it is omitted in {@link ChannelFlags#toString()} method.
     */
    int PASSWORD = 1 << 4;

    /**
     * Indicates that channel is the default channel. It is possible to use this flag on new created channel, but is strongly
     * advised to not use it. It is used to check if channel is the default one, but default channel should not change often
     * and that shouldn't be part of bot's job.
     */
    int DEFAULT = 1 << 5;

    /**
     * Set of flags that are used commonly together. This contains flags: {@code PERMANENT, MAXCLIENTS_UNLIMITED and MAXFAMILYCLIENTS_UNLIMITED}.
     * When creating a new channel through {@code ChannelProperties} you should always set the flags. Failing to do so
     * will result in flags being set to 0 and that to unwanted behaviour.
     */
    int __DEFAULT_FLAGS = PERMANENT | MAXCLIENTS_UNLIMITED | MAXFAMILYCLIENTS_UNLIMITED;
}
