package io.yourPath.ui;

import com.badlogic.gdx.ApplicationAdapter;
import io.yourPath.logic.StoryManager;
import io.yourPath.models.DialogNode;
import io.yourPath.models.DialogOption;
import io.yourPath.utils.SaveSystem;
import io.yourPath.utils.JsonDataLoader;
import io.yourPath.models.GameState;

import java.util.Scanner;

public class TerminalApp extends ApplicationAdapter {
    @Override
    public void create() {
        GameState state = SaveSystem.loadGame();
        StoryManager storyManager = new StoryManager(JsonDataLoader.loadStory("story.json"), state != null ? state : new GameState());

        if (state == null) {
            storyManager.start("intro_dream");
        }
        Scanner scanner = new Scanner(System.in);

        while (true) {
            DialogNode current = storyManager.getCurrentNode();
            if (current == null) break;

            printHeader(storyManager);

            System.out.println("\n------------------------------------------------");
            System.out.println(current.getText());
            System.out.println("------------------------------------------------");

            if (current.getOptions().isEmpty()) {
                if (current.getNextId() != null) {
                    System.out.println("\n[Pulsa ENTER para continuar...]");
                    scanner.nextLine();
                    storyManager.advance(current.getNextId());
                    SaveSystem.saveGame(storyManager.getGameState());
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
                            SaveSystem.saveGame(storyManager.getGameState());
                            break;
                        }
                    }
                }
            }
        }

    }
    
    private void printHeader(StoryManager storyManager) {
        GameState state = storyManager.getGameState();
        DialogNode current = storyManager.getCurrentNode();
        
        System.out.println("\n\n\n\n\n\n");
        System.out.println("=================================================");
        System.out.println("                INTENTIA: NEBULA                 ");
        System.out.println("=================================================");
        
        if (current != null) {
            if (current.getSpeakerId() != null) {
                System.out.println(" PERSONAJE: [" + current.getSpeakerId().toUpperCase() + "]");
            }
            if (current.getMusicTrack() != null) {
                System.out.println(" MÚSICA:    ♪ " + current.getMusicTrack());
            }
        }

        System.out.print(" OBJETOS:  ");
        if (state.getFlags().isEmpty()) {
            System.out.print("[Ninguno]");
        } else {
            System.out.print(state.getFlags());
        }
        
        if (state.getTotalPossibleScore() > 0) {
            System.out.print("\n PRUEBA:   " + state.getCurrentTrialScore() + "/" + state.getTotalPossibleScore());
        }
        
        System.out.println("\n=================================================");
    }
}

