package com.staniul.query.client;

public enum Platform {
    Windows, OSX, Android, Unknown;

    public static Platform parse(String platform) {
        if ("Windows".equals(platform)) return Windows;
        if ("Android".equals(platform)) return Android;
        if ("OS X".equals(platform)) return OSX;
        return Unknown;
    }
}
