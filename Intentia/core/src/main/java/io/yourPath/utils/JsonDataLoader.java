package io.yourPath.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Json;
import io.yourPath.models.CharacterProfile;
import io.yourPath.models.DialogNode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class JsonDataLoader {
    public static Map<String, DialogNode> loadStory(String path) {
        Json json = new Json();
        ArrayList<DialogNode> nodesList = json.fromJson(ArrayList.class, DialogNode.class, Gdx.files.internal(path));

        Map<String, DialogNode> nodesMap = new HashMap<>();
        for (DialogNode node : nodesList) {
            nodesMap.put(node.getId(), node);
        }

        return nodesMap;
    }
    public static Map<String, CharacterProfile> loadCharacters(String path) {
        Json json = new Json();

        ArrayList<CharacterProfile> charactersList = json.fromJson(ArrayList.class, CharacterProfile.class, Gdx.files.internal(path));


        Map<String, CharacterProfile> charactersMap = new HashMap<>();
        for (CharacterProfile profile : charactersList) {
            charactersMap.put(profile.getId(), profile);
        }

        return charactersMap;
    }

}
