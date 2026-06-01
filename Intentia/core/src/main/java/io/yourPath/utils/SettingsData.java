package io.yourPath.utils;

public class SettingsData {
    public int version = 1;
    public int resolutionX = 640;
    public int resolutionY = 360;
    public boolean fullscreen = false;
    public float musicVolume = 0.5f;
    public float sfxVolume = 0.7f;
    public String language = "es";

    public void migrate() {
        if (version < 1) {
            resolutionX = 640;
            resolutionY = 360;
            fullscreen = false;
            version = 1;
        }
    }
}
