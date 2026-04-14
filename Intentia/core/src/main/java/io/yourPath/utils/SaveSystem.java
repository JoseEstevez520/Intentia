package io.yourPath.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Json;
import io.yourPath.models.GameState;

public class SaveSystem {
    private static final String SAVE_FILE = "save.json";
    private static final Json json = new Json();

    public static void saveGame(GameState state) {
        Gdx.files.local(SAVE_FILE).writeString(json.prettyPrint(state), false);
    }

    public static GameState loadGame() {
        if (Gdx.files.local(SAVE_FILE).exists()) {
            return json.fromJson(GameState.class, Gdx.files.local(SAVE_FILE));
        }
        return null;
    }

    public static boolean exists() {
        return Gdx.files.local(SAVE_FILE).exists();
    }
}
