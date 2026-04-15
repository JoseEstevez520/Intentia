package io.yourPath.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import io.yourPath.Main;
import io.yourPath.logic.StoryManager;
import io.yourPath.models.GameState;
import io.yourPath.utils.SaveSystem;
import java.util.Scanner;

public class MainMenuScreen implements Screen {
    private Main game;
    private Scanner scanner;

    public MainMenuScreen(Main game) {
        this.game = game;
        this.scanner = new Scanner(System.in);
    }

    @Override
    public void render(float delta) {
        boolean partidaGuardada = SaveSystem.exists();

        System.out.println("\n===============================");
        System.out.println("   INTENTIA: EL LEGADO    ");
        System.out.println("===============================");
        System.out.println("1. Nueva Partida");
        if (partidaGuardada) {
            System.out.println("2. Continuar Partida");
            System.out.println("3. Salir");
        } else {
            System.out.println("2. Salir");
        }
        System.out.print("\nSelecciona una opción: ");

        if (scanner.hasNextInt()) {
            int opcion = scanner.nextInt();
            scanner.nextLine();

            if (opcion == 1) {
                game.storyManager.start("car_awakening");
                game.setScreen(new StoryScreen(game, game.storyManager, game.characters));
            } else if (opcion == 2 && partidaGuardada) {
                GameState guardado = SaveSystem.loadGame();
                if (guardado != null) {
                    game.storyManager = new StoryManager(game.story, guardado);
                    game.setScreen(new StoryScreen(game, game.storyManager, game.characters));
                }
            } else if ((opcion == 2 && !partidaGuardada) || opcion == 3) {
                Gdx.app.exit();
            }
        }
    }

    @Override public void show() {}
    @Override public void resize(int width, int height) {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
    @Override public void dispose() {}
}
