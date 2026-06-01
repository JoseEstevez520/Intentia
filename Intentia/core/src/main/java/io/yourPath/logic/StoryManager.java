package io.yourPath.logic;

import com.badlogic.gdx.Gdx;
import io.yourPath.models.DialogNode;
import io.yourPath.models.DialogOption;
import io.yourPath.models.GameState;
import io.yourPath.models.NarrativeNode;
import io.yourPath.models.TrialEvaluation;
import io.yourPath.models.TrialNode;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
                gameState.setTrialCurrentQuestion(gameState.getTrialCurrentQuestion() + 1);
            }
            advance(option.getTargetId());
        }
    }

    public void advance(String targetId) {
        if (nodes.containsKey(targetId)) {
            gameState.setCurrentNodeId(targetId);
            NarrativeNode currentNode = nodes.get(targetId);
            processActions(currentNode);
        } else {
            Gdx.app.log("StoryManager", "advance: targetId '" + targetId + "' not found in nodes map");
        }
    }

    public static class TrialResult {
        public final boolean success;
        public final int score;
        public final int total;
        public final float percentage;
        public final String nextNodeId;
        public final String earnedFlag;

        TrialResult(boolean success, int score, int total, String nextNodeId, String earnedFlag) {
            this.success = success;
            this.score = score;
            this.total = total;
            this.percentage = total > 0 ? (float) score / total : 0;
            this.nextNodeId = nextNodeId;
            this.earnedFlag = earnedFlag;
        }
    }

    public TrialResult evaluateCurrentTrial() {
        NarrativeNode node = getCurrentNode();
        if (!(node instanceof TrialNode)) return null;
        TrialNode trialNode = (TrialNode) node;
        TrialEvaluation eval = trialNode.getTrialEvaluation();
        if (eval == null) return null;

        int score = gameState.getCurrentTrialScore();
        int total = gameState.getTotalPossibleScore();
        boolean success = gameState.getScorePercentage() >= eval.getThreshold();

        String earnedFlag = null;
        if (success && eval.getSuccessFlag() != null) {
            gameState.addFlag(eval.getSuccessFlag());
            earnedFlag = eval.getSuccessFlag();
        }

        String nextId = success ? eval.getSuccessTargetId() : eval.getFailTargetId();
        gameState.resetTrialScore();
        gameState.setTrialCurrentQuestion(0);

        return new TrialResult(success, score, total, nextId, earnedFlag);
    }

    public int countTrialQuestions(String startNodeId) {
        if (!nodes.containsKey(startNodeId)) return 0;
        int count = 0;
        String currentId = startNodeId;
        Set<String> visited = new HashSet<>();

        while (currentId != null && nodes.containsKey(currentId) && visited.add(currentId)) {
            NarrativeNode node = nodes.get(currentId);

            if (node instanceof DialogNode) {
                DialogNode dn = (DialogNode) node;
                boolean hasScore = dn.getOptions() != null && dn.getOptions().stream()
                    .anyMatch(opt -> opt.getScoreValue() != null);
                if (hasScore) {
                    count++;
                    if (!dn.getOptions().isEmpty() && dn.getOptions().get(0).getTargetId() != null) {
                        currentId = dn.getOptions().get(0).getTargetId();
                        continue;
                    }
                }
                if (dn.getNextId() != null) {
                    currentId = dn.getNextId();
                    continue;
                }
            }

            if (node instanceof TrialNode) break;
            break;
        }

        return count;
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
