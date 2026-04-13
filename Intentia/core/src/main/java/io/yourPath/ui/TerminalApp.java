package io.yourPath.ui;

import com.badlogic.gdx.ApplicationAdapter;
import io.yourPath.logic.StoryManager;
import io.yourPath.models.DialogNode;
import io.yourPath.models.DialogOption;
import io.yourPath.utils.JsonDataLoader;
import io.yourPath.models.GameState;

import java.util.Scanner;

public class TerminalApp extends ApplicationAdapter {
    @Override
    public void create() {
        StoryManager storyManager = new StoryManager(
            JsonDataLoader.loadStory("story.json"),
            new GameState()
        );

        storyManager.start("intro_dream");
        Scanner scanner = new Scanner(System.in);

        while (true) {
            DialogNode current = storyManager.getCurrentNode();
            if (current == null) break;

            printHeader(storyManager.getGameState());
            System.out.println("\n------------------------------------------------");
            System.out.println(current.getText());
            System.out.println("------------------------------------------------");

            if (current.getOptions().isEmpty()) {
                if (current.getNextId() != null) {
                    System.out.println("\n[Pulsa ENTER para continuar...]");
                    scanner.nextLine();
                    storyManager.advance(current.getNextId());
                    continue;
                } else {
                    System.out.println("\n[FIN DEL PROTOTIPO]");
                    break;
                }
            }

            int optionIndex = 1;
            for (DialogOption option : current.getOptions()) {
                if (option.getRequiredFlag() == null || storyManager.getGameState().hasFlag(option.getRequiredFlag())) {
                    System.out.println(optionIndex + ". " + option.getText());
                    optionIndex++;
                }
            }

            System.out.print("\nElige una opción: ");
            int choice = 0;
            if (scanner.hasNextInt()) {
                choice = scanner.nextInt();
            } else {
                scanner.next();
            }

            if (choice > 0) {
                int validCount = 0;
                for (DialogOption option : current.getOptions()) {
                    if (option.getRequiredFlag() == null || storyManager.getGameState().hasFlag(option.getRequiredFlag())) {
                        validCount++;
                        if (validCount == choice) {
                            storyManager.advance(option.getTargetId());
                            break;
                        }
                    }
                }
            }
        }

    }
    
    private void printHeader(GameState state) {
        System.out.println("\n\n\n\n\n\n"); // Separación para simular limpieza de pantalla
        System.out.println("=================================================");
        System.out.println("                INTENTIA: NEBULA                 ");
        System.out.println("=================================================");
        System.out.print(" OBJETOS: ");
        if (state.getFlags().isEmpty()) {
            System.out.print("[Ninguno]");
        } else {
            // Filtrar flags que parecen objetos (puedes ajustar esto luego)
            System.out.print(state.getFlags());
        }
        System.out.println("\n=================================================");
    }
}
