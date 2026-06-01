package io.yourPath.models;

import com.badlogic.gdx.math.Rectangle;

public class Trigger {
    public enum Type {
        DIALOG,
        SET_FLAG
    }

    private String id;
    private Type type;
    private Rectangle bounds;
    private String nodeId;
    private String flag;
    private boolean oneShot;
    private boolean spent;
    private boolean active;

    public Trigger(String id, Type type, Rectangle bounds, String nodeId, String flag, boolean oneShot) {
        this.id = id;
        this.type = type;
        this.bounds = bounds;
        this.nodeId = nodeId;
        this.flag = flag;
        this.oneShot = oneShot;
        this.spent = false;
        this.active = true;
    }

    public String getId() { return id; }
    public Type getType() { return type; }
    public Rectangle getBounds() { return bounds; }
    public String getNodeId() { return nodeId; }
    public String getFlag() { return flag; }
    public boolean isOneShot() { return oneShot; }
    public boolean isSpent() { return spent; }
    public void setSpent(boolean spent) { this.spent = spent; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
