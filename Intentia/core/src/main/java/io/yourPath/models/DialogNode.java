package io.yourPath.models;

import java.util.ArrayList;
import java.util.List;

public class DialogNode {
    private String id;
    private String text;
    private String nextId;
    private String speakerId;
    private String musicTrack;
    private TrialEvaluation trialEvaluation;
    private List<DialogOption> options;
    private List<String> actions;

    public DialogNode() {
        this.options = new ArrayList<>();
        this.actions = new ArrayList<>();
    }


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getNextId() {
        return nextId;
    }

    public void setNextId(String nextId) {
        this.nextId = nextId;
    }

    public String getSpeakerId() {
        return speakerId;
    }

    public void setSpeakerId(String speakerId) {
        this.speakerId = speakerId;
    }

    public String getMusicTrack() {
        return musicTrack;
    }

    public void setMusicTrack(String musicTrack) {
        this.musicTrack = musicTrack;
    }

    public TrialEvaluation getTrialEvaluation() {
        return trialEvaluation;
    }

    public void setTrialEvaluation(TrialEvaluation trialEvaluation) {
        this.trialEvaluation = trialEvaluation;
    }

    public List<DialogOption> getOptions() {
        return options;
    }


    public void setOptions(List<DialogOption> options) {
        this.options = options;
    }

    public List<String> getActions() {
        return actions;
    }

    public void setActions(List<String> actions) {
        this.actions = actions;
    }
}
