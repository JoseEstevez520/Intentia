package io.yourPath.models;

import java.util.HashSet;
import java.util.Set;

public class GameState {
    private Set<String> flags;
    private String currentNodeId;

    public GameState() {
        this.flags = new HashSet<>();
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
}
