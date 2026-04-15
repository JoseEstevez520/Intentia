package io.yourPath;

import com.badlogic.gdx.Game;
import io.yourPath.logic.StoryManager;
import io.yourPath.models.CharacterProfile;
import io.yourPath.models.DialogNode;
import io.yourPath.models.GameState;
import io.yourPath.ui.TerminalApp;
import io.yourPath.utils.JsonDataLoader;

import java.awt.*;
import java.util.Map;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class Main extends Game {
    @Override
    public void create() {
        Map<String, CharacterProfile> characters = JsonDataLoader.loadCharacters("characters.json");
        Map<String, DialogNode> story = JsonDataLoader.loadStory("story.json");
        StoryManager storyManager = new StoryManager(story,new GameState());

        setScreen(FirstScreen(this,storyManager,characters));
    }
}
