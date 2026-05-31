package io.yourPath.logic;

import io.yourPath.models.DialogNode;
import io.yourPath.models.DialogOption;
import io.yourPath.models.GameState;
import io.yourPath.models.NarrativeNode;
import io.yourPath.models.TrialEvaluation;
import io.yourPath.models.TrialNode;

import java.util.Map;

public class StoryManager {
    private Map<String, NarrativeNode> nodes;
    private GameState gameState;

    public StoryManager(Map<String, NarrativeNode> nodes, GameState gameState) {
        this.nodes = nodes;
        this.gameState = gameState;
    }

    public void start(String startNodeId) {
        gameState.setCurrentNodeId(startNodeId);
        processActions(nodes.get(startNodeId));
    }

    public void advance(DialogOption option) {
        if (option != null) {
            if (option.getScoreValue() != null) {
                gameState.addTrialScore(option.getScoreValue(), 1);
            }
            advance(option.getTargetId());
        }
    }

    public void advance(String targetId) {
        if (nodes.containsKey(targetId)) {
            gameState.setCurrentNodeId(targetId);
            NarrativeNode currentNode = nodes.get(targetId);
            processActions(currentNode);
            if (currentNode instanceof TrialNode) {
                checkTrialEvaluation((TrialNode) currentNode);
            }
        }
    }

    private void checkTrialEvaluation(TrialNode node) {
        TrialEvaluation eval = node.getTrialEvaluation();
        if (eval == null) return;
        boolean success = gameState.getScorePercentage() >= eval.getThreshold();

        if (success && eval.getSuccessFlag() != null) {
            gameState.addFlag(eval.getSuccessFlag());
        }

        String nextId = success ? eval.getSuccessTargetId() : eval.getFailTargetId();
        gameState.resetTrialScore();
        advance(nextId);
    }

    public NarrativeNode getCurrentNode() {
        return nodes.get(gameState.getCurrentNodeId());
    }

    private void processActions(NarrativeNode node) {
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
