package io.yourPath;

import com.badlogic.gdx.Game;
import io.yourPath.logic.StoryManager;
import io.yourPath.models.CharacterProfile;
import io.yourPath.models.NarrativeNode;
import io.yourPath.models.GameState;
import io.yourPath.screens.MainMenuScreen;
import io.yourPath.utils.IntentiaException;
import io.yourPath.utils.NarrativeDAO;
import io.yourPath.utils.NarrativeDAOImplementation;

import java.util.Map;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class Main extends Game {
    private StoryManager storyManager;
    private Map<String, CharacterProfile> characters;
    private Map<String, NarrativeNode> story;
    private NarrativeDAO narrativeDAO;

    @Override
    public void create() {
        narrativeDAO = new NarrativeDAOImplementation("database/intentia.db");

        try {
            characters = narrativeDAO.getAllCharacters();
            story = narrativeDAO.getAllDialogNodes();
        } catch (IntentiaException e) {
            System.err.println(e.getMessage());
        }
        storyManager = new StoryManager(story, new GameState());

        setScreen(new MainMenuScreen(this));
    }

    public StoryManager getStoryManager() {
        return storyManager;
    }

    public void setStoryManager(StoryManager storyManager) {
        this.storyManager = storyManager;
    }

    public Map<String, CharacterProfile> getCharacters() {
        return characters;
    }

    public Map<String, NarrativeNode> getStory() {
        return story;
    }
}
