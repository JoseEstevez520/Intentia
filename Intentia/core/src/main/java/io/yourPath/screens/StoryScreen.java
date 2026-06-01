package io.yourPath.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import io.yourPath.Main;
import io.yourPath.logic.StoryManager;
import io.yourPath.models.CharacterProfile;
import io.yourPath.models.DialogNode;
import io.yourPath.models.NarrativeNode;
import io.yourPath.models.UIState;
import io.yourPath.utils.SaveSystem;

import java.util.Map;

public class StoryScreen implements Screen {
    private Main game;
    private StoryManager storyManager;
    private Map<String, CharacterProfile> characters;
    private UIState currentState = UIState.DIALOGANDO;
    private boolean esperandoEnter = false;

    public StoryScreen(Main game, StoryManager storyManager, Map<String, CharacterProfile> characters) {
        this.game = game;
        this.storyManager = storyManager;
        this.characters = characters;
    }

    @Override
    public void render(float delta) {
        switch (currentState) {
            case DIALOGANDO:
                drawStory();
                break;
            case MENU_PAUSA:
                drawPauseMenu();
                break;
        }
    }

    private void drawStory() {
        NarrativeNode node = storyManager.getCurrentNode();
        if (node == null) return;

        for (int i = 0; i < 50; i++) System.out.println();

        CharacterProfile speaker = characters.get(node.getSpeakerId());
        String name = (speaker != null) ? speaker.getName() : "Narrador";

        System.out.println("\n------------------------------------------------");
        System.out.println("[ " + name.toUpperCase() + " ]");
        System.out.println(node.getText());

        if (node instanceof DialogNode) {
            DialogNode dialogNode = (DialogNode) node;
            if (dialogNode.getOptions() != null && !dialogNode.getOptions().isEmpty()) {
                System.out.println("0. [ MENU DE PAUSA ]");
                for (int i = 0; i < dialogNode.getOptions().size(); i++) {
                    System.out.println((i + 1) + ". " + dialogNode.getOptions().get(i).getText());
                }

                for (int i = 0; i <= dialogNode.getOptions().size(); i++) {
                    if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_0 + i)) {
                        if (i == 0) {
                            currentState = UIState.MENU_PAUSA;
                        } else if (i > 0 && i <= dialogNode.getOptions().size()) {
                            storyManager.advance(dialogNode.getOptions().get(i - 1));
                        }
                    }
                }
            } else if (dialogNode.getNextId() != null) {
                if (!esperandoEnter) {
                    System.out.println("\n(Pulsa Enter para continuar...)");
                    esperandoEnter = true;
                }
                if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
                    esperandoEnter = false;
                    storyManager.advance(dialogNode.getNextId());
                }
            } else {
                System.out.println("\n--- FIN DE LA PROLOGO ---");
                System.out.println("1. Volver al menu");
                if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) {
                    game.setScreen(new MainMenuScreen(game));
                }
            }
        }
    }

    private void drawPauseMenu() {
        System.out.println("\n=== MENU DE PAUSA ===");
        System.out.println("1. Volver al juego");
        System.out.println("2. Guardar partida");
        System.out.println("3. Salir del juego");
        System.out.print("\nSelecciona: ");

        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) {
            currentState = UIState.DIALOGANDO;
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) {
            SaveSystem.saveGame(storyManager.getGameState());
            currentState = UIState.DIALOGANDO;
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_3)) {
            Gdx.app.exit();
        }
    }



    @Override public void show() {}
    @Override public void resize(int width, int height) {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
    @Override public void dispose() {}
}
