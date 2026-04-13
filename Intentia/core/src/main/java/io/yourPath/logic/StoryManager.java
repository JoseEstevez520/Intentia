package io.yourPath.logic;

import io.yourPath.models.DialogNode;
import io.yourPath.models.GameState;
import java.util.Map;

public class StoryManager {
    private Map<String, DialogNode> nodes;
    private GameState gameState;

    public StoryManager(Map<String, DialogNode> nodes, GameState gameState) {
        this.nodes = nodes;
        this.gameState = gameState;
    }

    public void start(String startNodeId) {
        gameState.setCurrentNodeId(startNodeId);
        processActions(nodes.get(startNodeId));
    }

    public void advance(String targetId) {
        if (nodes.containsKey(targetId)) {
            gameState.setCurrentNodeId(targetId);
            processActions(nodes.get(targetId));
        }
    }

    public DialogNode getCurrentNode() {
        return nodes.get(gameState.getCurrentNodeId());
    }

    private void processActions(DialogNode node) {
        if (node != null && node.getActions() != null) {
            for (String action : node.getActions()) {
                gameState.addFlag(action);
                System.out.println("\n[AVANCE: " + action + "]");
            }
        }
    }

    public GameState getGameState() {
        return gameState;
    }
}
