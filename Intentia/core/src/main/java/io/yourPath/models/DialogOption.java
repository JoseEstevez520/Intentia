package io.yourPath.models;

public class DialogOption {
    private String text;
    private String targetId;
    private String requiredFlag;
    private Integer scoreValue;


    public DialogOption() {
    }

    public DialogOption(String text, String targetId) {
        this.text = text;
        this.targetId = targetId;
    }

    public DialogOption(String text, String targetId, String requiredFlag) {
        this.text = text;
        this.targetId = targetId;
        this.requiredFlag = requiredFlag;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    public String getRequiredFlag() {
        return requiredFlag;
    }

    public void setRequiredFlag(String requiredFlag) {
        this.requiredFlag = requiredFlag;
    }

    public Integer getScoreValue() {
        return scoreValue;
    }

    public void setScoreValue(Integer scoreValue) {
        this.scoreValue = scoreValue;
    }
}

