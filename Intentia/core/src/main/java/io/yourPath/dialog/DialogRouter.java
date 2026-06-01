package io.yourPath.dialog;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import io.yourPath.models.GameState;

import java.util.*;

public class DialogRouter {
    private Map<String, List<DialogRule>> rulesByNpc;
    private GameState gameState;

    public DialogRouter(Map<String, List<DialogRule>> rulesByNpc, GameState gameState) {
        this.rulesByNpc = rulesByNpc;
        this.gameState = gameState;
    }

    public String resolve(String npcId, String fallbackNodeId) {
        List<DialogRule> rules = rulesByNpc.get(npcId);
        if (rules != null) {
            for (DialogRule rule : rules) {
                if (rule.getCheckType().equals("flag") && gameState.hasFlag(rule.getFlag())) {
                    return rule.getNodeId();
                }
                if (rule.getCheckType().equals("default")) {
                    return rule.getNodeId();
                }
            }
        }
        return fallbackNodeId;
    }

    public static DialogRouter loadFromJson(String jsonPath, GameState gameState) {
        Map<String, List<DialogRule>> rules = new HashMap<>();

        try {
            JsonReader reader = new JsonReader();
            JsonValue root = reader.parse(Gdx.files.internal(jsonPath));

            for (JsonValue npcEntry : root) {
                String npcId = npcEntry.name();
                List<DialogRule> npcRules = new ArrayList<>();

                for (JsonValue ruleVal : npcEntry) {
                    String checkType = ruleVal.getString("check", "default");
                    String flag = ruleVal.getString("flag", null);
                    String nodeId = ruleVal.getString("nodeId", null);
                    if (nodeId != null) {
                        npcRules.add(new DialogRule(checkType, flag, nodeId));
                    }
                }

                rules.put(npcId, npcRules);
            }
        } catch (Exception e) {
            Gdx.app.log("DialogRouter", "Could not load dialog rules: " + e.getMessage());
        }

        return new DialogRouter(rules, gameState);
    }
}
