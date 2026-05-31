package io.yourPath.utils;

import io.yourPath.models.CharacterProfile;
import io.yourPath.models.NarrativeNode;
import java.util.Map;

public interface NarrativeDAO {
    Map<String, CharacterProfile> getAllCharacters() throws IntentiaException;
    Map<String, NarrativeNode> getAllDialogNodes() throws IntentiaException;
}
