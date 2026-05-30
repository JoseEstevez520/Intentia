package io.yourPath;

import com.badlogic.gdx.Game;
import io.yourPath.logic.StoryManager;
import io.yourPath.models.CharacterProfile;
import io.yourPath.models.DialogNode;
import io.yourPath.models.GameState;
import io.yourPath.screens.MainMenuScreen;
import io.yourPath.utils.NarrativeDAO;
import io.yourPath.utils.NarrativeDAOImplementation;

import java.awt.*;
import java.util.Map;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class Main extends Game {
    public StoryManager storyManager;
    public Map<String, CharacterProfile> characters;
    public Map<String, DialogNode> story;
    private NarrativeDAO narrativeDAO;

    @Override
    public void create() {
        narrativeDAO = new NarrativeDAOImplementation("database/intentia.db");

        characters = narrativeDAO.getAllCharacters();
        story = narrativeDAO.getAllDialogNodes();
        storyManager = new StoryManager(story,new GameState());

        setScreen(new MainMenuScreen(this));

    }
}
