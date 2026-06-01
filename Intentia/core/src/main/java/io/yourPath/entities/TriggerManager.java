package io.yourPath.entities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import io.yourPath.models.GameState;
import io.yourPath.models.Trigger;

public class TriggerManager {
    private Array<Trigger> triggers;

    public TriggerManager() {
        this.triggers = new Array<>();
    }

    public void loadFromTiledLayer(TiledMap map, String mapName, GameState gameState) {
        triggers.clear();

        MapLayer layer = map.getLayers().get("Triggers");
        if (layer == null) {
            Gdx.app.log("TriggerManager", "No 'Triggers' layer found in map");
            return;
        }

        for (MapObject obj : layer.getObjects()) {
            if (!(obj instanceof RectangleMapObject)) continue;

            RectangleMapObject rectObj = (RectangleMapObject) obj;
            Rectangle rect = rectObj.getRectangle();

            String typeStr = getString(obj, "triggerType", null);
            if (typeStr == null) continue;

            Trigger.Type type;
            if (typeStr.equals("dialog")) {
                type = Trigger.Type.DIALOG;
            } else if (typeStr.equals("flag")) {
                type = Trigger.Type.SET_FLAG;
            } else {
                continue;
            }

            String nodeId = getString(obj, "dialogNodeId", null);
            String flag = getString(obj, "flagName", null);
            boolean oneShot = getBool(obj, "oneShot", false);

            String triggerId = "trigger_" + mapName + "_" + obj.getProperties().get("id", 0, Integer.class);

            if (oneShot && gameState != null && gameState.hasFlag(triggerId)) {
                continue;
            }

            triggers.add(new Trigger(triggerId, type, rect, nodeId, flag, oneShot));
        }

        Gdx.app.log("TriggerManager", "Loaded " + triggers.size + " triggers from map");
    }

    public Trigger checkTriggers(Vector2 playerPos, GameState gameState) {
        Rectangle playerBounds = new Rectangle(playerPos.x, playerPos.y, 32, 48);

        for (Trigger trigger : triggers) {
            if (trigger.isSpent() || !trigger.isActive()) continue;

            if (playerBounds.overlaps(trigger.getBounds())) {
                switch (trigger.getType()) {
                    case DIALOG:
                        trigger.setActive(false);
                        if (trigger.isOneShot()) {
                            trigger.setSpent(true);
                            gameState.addFlag(trigger.getId());
                        }
                        return trigger;

                    case SET_FLAG:
                        if (trigger.getFlag() != null) {
                            gameState.addFlag(trigger.getFlag());
                        }
                        trigger.setSpent(true);
                        if (trigger.isOneShot()) {
                            gameState.addFlag(trigger.getId());
                        }
                        return trigger;
                }
            }
        }
        return null;
    }

    public void rearmTrigger(Trigger trigger) {
        if (trigger != null && !trigger.isSpent()) {
            trigger.setActive(true);
        }
    }

    public void checkExits(Vector2 playerPos) {
        Rectangle playerBounds = new Rectangle(playerPos.x, playerPos.y, 32, 48);
        for (Trigger trigger : triggers) {
            if (!trigger.isActive() && !trigger.isSpent()) {
                if (!playerBounds.overlaps(trigger.getBounds())) {
                    trigger.setActive(true);
                }
            }
        }
    }

    private static String getString(MapObject obj, String key, String fallback) {
        Object val = obj.getProperties().get(key);
        return val != null ? val.toString() : fallback;
    }

    private static boolean getBool(MapObject obj, String key, boolean fallback) {
        Object val = obj.getProperties().get(key);
        if (val == null) return fallback;
        String s = val.toString().toLowerCase();
        return s.equals("true") || s.equals("1");
    }
}
