package io.yourPath.models;

import java.util.HashSet;
import java.util.Set;

public class GameState {
    private Set<String> flags;
    private String currentNodeId;
    private int currentTrialScore;
    private int totalPossibleScore;

    public GameState() {
        this.flags = new HashSet<>();
        this.currentTrialScore = 0;
        this.totalPossibleScore = 0;
    }


    public boolean hasFlag(String flag) {
        return flags.contains(flag);
    }

    public void addFlag(String flag) {
        flags.add(flag);
    }

    public Set<String> getFlags() {
        return flags;
    }

    public void setFlags(Set<String> flags) {
        this.flags = flags;
    }

    public String getCurrentNodeId() {
        return currentNodeId;
    }

    public void setCurrentNodeId(String currentNodeId) {
        this.currentNodeId = currentNodeId;
    }

    public int getCurrentTrialScore() {
        return currentTrialScore;
    }

    public void setCurrentTrialScore(int currentTrialScore) {
        this.currentTrialScore = currentTrialScore;
    }

    public int getTotalPossibleScore() {
        return totalPossibleScore;
    }

    public void setTotalPossibleScore(int totalPossibleScore) {
        this.totalPossibleScore = totalPossibleScore;
    }

    public void addTrialScore(int points, int max) {
        this.currentTrialScore += points;
        this.totalPossibleScore += max;
    }

    public float getScorePercentage() {
        if (totalPossibleScore == 0) return 0;
        return (float) currentTrialScore / totalPossibleScore;
    }

    public void resetTrialScore() {
        this.currentTrialScore = 0;
        this.totalPossibleScore = 0;
    }
}

