package io.yourPath.models;

public class TrialEvaluation {
    private float threshold;
    private String successTargetId;
    private String failTargetId;
    private String successFlag;

    public TrialEvaluation() {
    }

    public float getThreshold() {
        return threshold;
    }

    public void setThreshold(float threshold) {
        this.threshold = threshold;
    }

    public String getSuccessTargetId() {
        return successTargetId;
    }

    public void setSuccessTargetId(String successTargetId) {
        this.successTargetId = successTargetId;
    }

    public String getFailTargetId() {
        return failTargetId;
    }

    public void setFailTargetId(String failTargetId) {
        this.failTargetId = failTargetId;
    }

    public String getSuccessFlag() {
        return successFlag;
    }

    public void setSuccessFlag(String successFlag) {
        this.successFlag = successFlag;
    }
}
