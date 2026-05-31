package io.yourPath.models;

import java.util.ArrayList;
import java.util.List;

public class DialogNode extends NarrativeNode {
    private String nextId;
    private List<DialogOption> options;

    public DialogNode() {
        super();
        this.options = new ArrayList<>();
    }

    public String getNextId() {
        return nextId;
    }

    public void setNextId(String nextId) {
        this.nextId = nextId;
    }

    public List<DialogOption> getOptions() {
        return options;
    }

    public void setOptions(List<DialogOption> options) {
        this.options = options;
    }

    @Override
    public String getNextTargetId() {
        return nextId;
    }
}
