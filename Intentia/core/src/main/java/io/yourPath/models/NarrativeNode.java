package io.yourPath.models;

import java.util.ArrayList;
import java.util.List;

public abstract class NarrativeNode {
    private String id;
    private String text;
    private String speakerId;
    private String musicTrack;
    private List<String> actions;

    public NarrativeNode() {
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

    public List<String> getActions() {
        return actions;
    }

    public void setActions(List<String> actions) {
        this.actions = actions;
    }

    public abstract String getNextTargetId();
}
