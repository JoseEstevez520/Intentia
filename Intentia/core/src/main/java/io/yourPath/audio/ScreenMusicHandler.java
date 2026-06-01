package io.yourPath.audio;

public interface ScreenMusicHandler {

    MusicCommand getMusicCommand();

    default void onMusicStart() {}

    default void onMusicEnd() {}
}
