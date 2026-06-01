package io.yourPath;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import io.yourPath.audio.MusicManager;
import io.yourPath.dialog.DialogRouter;
import io.yourPath.logic.StoryManager;
import io.yourPath.models.CharacterProfile;
import io.yourPath.models.GameState;
import io.yourPath.models.NarrativeNode;
import io.yourPath.screens.MainMenuScreen;
import io.yourPath.utils.IntentiaException;
import io.yourPath.utils.NarrativeDAO;
import io.yourPath.utils.NarrativeDAOImplementation;
import io.yourPath.utils.SettingsManager;

import java.util.HashMap;
import java.util.Map;

public class Main extends Game {
    private StoryManager storyManager;
    private Map<String, CharacterProfile> characters;
    private Map<String, NarrativeNode> story;
    private MusicManager musicManager;
    private NarrativeDAO narrativeDAO;
    private DialogRouter dialogRouter;

    @Override
    public void create() {
        musicManager = new MusicManager();

        try {
            SettingsManager.inst();
        } catch (Exception e) {
            System.err.println("Error al cargar config: " + e.getMessage());
        }

        musicManager.setVolumen(SettingsManager.inst().getMusicVolume());
        SettingsManager.inst().addListener(() -> {
            musicManager.setVolumen(SettingsManager.inst().getMusicVolume());
        });

        if (SettingsManager.inst().isFullscreen()) {
            try {
                Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
            } catch (Exception e) {
                SettingsManager.inst().aplicarVentana();
            }
        }

        narrativeDAO = new NarrativeDAOImplementation("database/intentia.db");
        try {
            characters = narrativeDAO.getAllCharacters();
            story = narrativeDAO.getAllDialogNodes();
        } catch (IntentiaException e) {
            System.err.println(e.getMessage());
            characters = new HashMap<>();
            story = new HashMap<>();
        }
        storyManager = new StoryManager(story, new GameState());
        dialogRouter = DialogRouter.loadFromJson("dialog/dialog_routes.json", storyManager.getGameState());

        setScreen(new MainMenuScreen(this));
    }

    @Override
    public void render() {
        float delta = Gdx.graphics.getDeltaTime();

        if (Gdx.input.isKeyJustPressed(Input.Keys.F11)) {
            SettingsManager.inst().toggleFullscreen();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.PLUS) || Gdx.input.isKeyJustPressed(Input.Keys.EQUALS)) {
            SettingsManager.inst().setMusicVolume(SettingsManager.inst().getMusicVolume() + 0.1f);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.MINUS)) {
            SettingsManager.inst().setMusicVolume(SettingsManager.inst().getMusicVolume() - 0.1f);
        }

        musicManager.update(delta);
        super.render();
    }

    @Override
    public void pause() {
        musicManager.pausarTodo();
        SettingsManager.inst().save();
    }

    @Override
    public void resume() {
        musicManager.reanudarTodo();
    }

    @Override
    public void dispose() {
        SettingsManager.inst().save();
        musicManager.dispose();
    }

    public MusicManager getMusicManager() { return musicManager; }
    public StoryManager getStoryManager() { return storyManager; }
    public void setStoryManager(StoryManager sm) { storyManager = sm; }
    public Map<String, CharacterProfile> getCharacters() { return characters; }
    public Map<String, NarrativeNode> getStory() { return story; }
    public DialogRouter getDialogRouter() { return dialogRouter; }

}
