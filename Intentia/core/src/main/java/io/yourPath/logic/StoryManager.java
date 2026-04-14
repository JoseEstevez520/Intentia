package io.yourPath.logic;

import io.yourPath.models.DialogNode;
import io.yourPath.models.DialogOption;
import io.yourPath.models.GameState;
import io.yourPath.models.TrialEvaluation;
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
            DialogNode previousNode = getCurrentNode();
            if (previousNode != null) {
                for (DialogOption option : previousNode.getOptions()) {
                    if (option.getTargetId().equals(targetId)) {
                        if (option.getScoreValue() != null) {
                            gameState.addTrialScore(option.getScoreValue(), 1);
                        }
                        break;
                    }
                }
            }

            gameState.setCurrentNodeId(targetId);
            DialogNode currentNode = nodes.get(targetId);
            processActions(currentNode);
            checkTrialEvaluation(currentNode);
        }
    }

    private void checkTrialEvaluation(DialogNode node) {
        if (node != null && node.getTrialEvaluation() != null) {
            TrialEvaluation eval = node.getTrialEvaluation();
            boolean success = gameState.getScorePercentage() >= eval.getThreshold();
            
            if (success && eval.getSuccessFlag() != null) {
                gameState.addFlag(eval.getSuccessFlag());
            }

            String nextId = success ? eval.getSuccessTargetId() : eval.getFailTargetId();
            gameState.resetTrialScore();
            advance(nextId);
        }
    }

    public DialogNode getCurrentNode() {
        return nodes.get(gameState.getCurrentNodeId());
    }

    private void processActions(DialogNode node) {
        if (node != null && node.getActions() != null) {
            for (String action : node.getActions()) {
                gameState.addFlag(action);
            }
        }
    }

    public GameState getGameState() {
        return gameState;
    }
}

