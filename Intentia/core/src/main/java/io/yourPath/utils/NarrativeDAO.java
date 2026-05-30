package io.yourPath.utils;

import io.yourPath.models.CharacterProfile;
import io.yourPath.models.DialogNode;
import java.util.Map;

public interface NarrativeDAO {
    Map<String, CharacterProfile> getAllCharacters() throws IntentiaException;
    Map<String, DialogNode> getAllDialogNodes() throws IntentiaException;
}
