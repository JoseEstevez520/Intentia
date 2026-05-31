package io.yourPath.models;

public class TrialNode extends NarrativeNode {
    private TrialEvaluation trialEvaluation;

    public TrialNode() {
        super();
    }

    public TrialEvaluation getTrialEvaluation() {
        return trialEvaluation;
    }

    public void setTrialEvaluation(TrialEvaluation trialEvaluation) {
        this.trialEvaluation = trialEvaluation;
    }

    @Override
    public String getNextTargetId() {
        return null;
    }
}
