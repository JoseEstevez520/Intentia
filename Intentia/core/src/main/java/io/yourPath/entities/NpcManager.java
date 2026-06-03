package io.yourPath.entities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

public class NpcManager {
    private Array<NPC> npcs;
    private Rectangle rectPlayerBounds = new Rectangle();

    public NpcManager() {
        this.npcs = new Array<>();
    }

    public void loadFromTiledLayer(TiledMap map, String mapPath, float mapWidth, float mapHeight) {
        for (NPC npc : npcs) {
            npc.dispose();
        }
        npcs.clear();

        MapLayer layer = map.getLayers().get("NPCs");
        if (layer == null) {
            Gdx.app.log("NpcManager", "No 'NPCs' layer found in map");
            cargarNPCsPorDefecto();
            aplicarWanderBounds(mapWidth, mapHeight);
            return;
        }

        for (MapObject obj : layer.getObjects()) {
            if (!(obj instanceof RectangleMapObject)) continue;

            RectangleMapObject rectObj = (RectangleMapObject) obj;
            Rectangle rect = rectObj.getRectangle();

            String npcId = getString(obj, "npcId", null);
            String dialogNodeId = getString(obj, "dialogNodeId", null);
            String movementStr = getString(obj, "movement", "static");
            float speed = getFloat(obj, "speed", 60f);
            float patrolDist = getFloat(obj, "patrolDist", 3f);
            String colorStr = getString(obj, "color", "white");
            String spriteWalk = getString(obj, "spriteWalk", null);
            String spriteIdle = getString(obj, "spriteIdle", null);

            if (npcId == null) continue;

            if (dialogNodeId == null || spriteWalk == null) {
                switch (npcId) {
                    case "elio":
                        if (dialogNodeId == null) dialogNodeId = "npc_elio_saludo";
                        if (spriteWalk == null) spriteWalk = "sprites/npc/abuelo_walk.png";
                        if (spriteIdle == null) spriteIdle = "sprites/npc/abuelo_idle.png";
                        break;
                    case "alba":
                        if (dialogNodeId == null) dialogNodeId = "npc_alba_abrazo";
                        if (spriteWalk == null) spriteWalk = "sprites/npc/mujer_c_walk.png";
                        if (spriteIdle == null) spriteIdle = "sprites/npc/mujer_c_idle.png";
                        break;
                    case "leo":
                        if (dialogNodeId == null) dialogNodeId = "npc_leo_despide";
                        if (spriteWalk == null) spriteWalk = "sprites/npc/hombre_walk.png";
                        if (spriteIdle == null) spriteIdle = "sprites/npc/hombre_idle.png";
                        break;
                }
            }

            NPC.MovementType movementType = parseMovement(movementStr);
            Color color = parseColor(colorStr);

            NPC npc;
            if (spriteWalk != null) {
                npc = new NPC(npcId, dialogNodeId, rect.x, rect.y, movementType, speed, patrolDist, spriteWalk, spriteIdle);
            } else {
                npc = new NPC(npcId, dialogNodeId, rect.x, rect.y, movementType, speed, patrolDist, color);
            }
            npcs.add(npc);
        }

        aplicarWanderBounds(mapWidth, mapHeight);
        Gdx.app.log("NpcManager", "Loaded " + npcs.size + " NPCs from map");
    }

    private void aplicarWanderBounds(float mapWidth, float mapHeight) {
        for (NPC npc : npcs) {
            npc.setWanderBounds(32, mapWidth - 32, 32, mapHeight - 32);
        }
    }

    private void cargarNPCsPorDefecto() {
        npcs.add(new NPC("elio", "npc_elio_saludo", 320, 320, NPC.MovementType.PATROL_H, 40f, 3f,
            "sprites/npc/abuelo_walk.png", "sprites/npc/abuelo_idle.png"));
        npcs.add(new NPC("leo", "npc_leo_despide", 240, 240, NPC.MovementType.STATIC, 0, 0,
            "sprites/npc/hombre_walk.png", "sprites/npc/hombre_idle.png"));
        npcs.add(new NPC("alba", "npc_alba_abrazo", 400, 240, NPC.MovementType.WANDER, 30f, 0,
            "sprites/npc/mujer_c_walk.png", "sprites/npc/mujer_c_idle.png"));
        Gdx.app.log("NpcManager", "No NPCs layer, loaded 3 default NPCs");
    }

    private NPC.MovementType parseMovement(String str) {
        if (str == null) return NPC.MovementType.STATIC;
        switch (str.toLowerCase()) {
            case "patrol_h": return NPC.MovementType.PATROL_H;
            case "patrol_v": return NPC.MovementType.PATROL_V;
            case "wander": return NPC.MovementType.WANDER;
            default: return NPC.MovementType.STATIC;
        }
    }

    private static String getString(MapObject obj, String key, String fallback) {
        Object val = obj.getProperties().get(key);
        return val != null ? val.toString() : fallback;
    }

    private static float getFloat(MapObject obj, String key, float fallback) {
        Object val = obj.getProperties().get(key);
        if (val == null) return fallback;
        try {
            return Float.parseFloat(val.toString());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private Color parseColor(String str) {
        if (str == null) return Color.WHITE;
        switch (str.toLowerCase()) {
            case "brown": return Color.BROWN;
            case "navy": return Color.NAVY;
            case "magenta": return Color.MAGENTA;
            case "green": return Color.GREEN;
            case "red": return Color.RED;
            case "blue": return Color.BLUE;
            case "yellow": return Color.YELLOW;
            case "orange": return Color.ORANGE;
            case "cyan": return Color.CYAN;
            default: return Color.WHITE;
        }
    }

    public void update(float delta, Vector2 playerPosition) {
        for (NPC npc : npcs) {
            npc.update(delta);
        }
    }

    public void render(Batch batch) {
        npcs.sort((a, b) -> Float.compare(b.getY(), a.getY()));

        for (NPC npc : npcs) {
            npc.draw(batch);
        }
    }

    public void renderSortedWithPlayer(Batch batch, Player jugador) {
        Array<NPC> sorted = new Array<>(npcs);
        sorted.sort((a, b) -> Float.compare(b.getY(), a.getY()));

        float playerY = jugador.posicion.y;
        boolean playerDrawn = false;
        for (NPC npc : sorted) {
            if (!playerDrawn && npc.getY() <= playerY) {
                jugador.dibujar(batch);
                playerDrawn = true;
            }
            npc.draw(batch);
        }
        if (!playerDrawn) {
            jugador.dibujar(batch);
        }
    }

    public NPC getInteractiveNPC(Vector2 playerPos) {
        rectPlayerBounds.set(playerPos.x, playerPos.y, 16, 24);
        NPC closest = null;
        float minDist = Float.MAX_VALUE;
        for (NPC npc : npcs) {
            if (npc.getState() == NPC.State.TALKING) continue;
            if (rectPlayerBounds.overlaps(npc.getInteractionBounds())) {
                float dx = npc.getX() + 16 - playerPos.x - 8;
                float dy = npc.getY() + 24 - playerPos.y - 12;
                float dist = dx * dx + dy * dy;
                if (dist < minDist) {
                    minDist = dist;
                    closest = npc;
                }
            }
        }
        return closest;
    }

    public NPC getNpcById(String npcId) {
        for (NPC npc : npcs) {
            if (npc.getNpcId().equals(npcId)) return npc;
        }
        return null;
    }

    public Array<NPC> getNpcs() {
        return npcs;
    }

    public void dispose() {
        for (NPC npc : npcs) {
            npc.dispose();
        }
        npcs.clear();
    }
}
