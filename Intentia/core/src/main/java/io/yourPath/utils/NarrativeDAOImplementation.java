package io.yourPath.utils;

import io.yourPath.models.CharacterProfile;
import io.yourPath.models.DialogNode;
import io.yourPath.models.DialogOption;
import io.yourPath.models.NarrativeNode;
import io.yourPath.models.TrialEvaluation;
import io.yourPath.models.TrialNode;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class NarrativeDAOImplementation implements NarrativeDAO {
    private String url;

    public NarrativeDAOImplementation(String dbPath) {
        this.url = "jdbc:sqlite:" + dbPath;
    }

    @Override
    public Map<String, CharacterProfile> getAllCharacters() throws IntentiaException {
        Map<String, CharacterProfile> characters = new HashMap<>();
        String sql = "SELECT * FROM characters";

        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                CharacterProfile profile = new CharacterProfile();
                profile.setId(rs.getString("id"));
                profile.setName(rs.getString("name"));
                profile.setPortraitPath(rs.getString("portrait_path"));
                characters.put(profile.getId(), profile);
            }
        } catch (SQLException e) {
            throw new IntentiaException("Error al cargar los personajes desde la base de datos: " + e.getMessage());
        }
        return characters;
    }

    @Override
    public Map<String, NarrativeNode> getAllDialogNodes() throws IntentiaException {
        Map<String, NarrativeNode> nodes = new HashMap<>();

        try (Connection conn = DriverManager.getConnection(url)) {

            String sqlNodes = "SELECT * FROM dialog_nodes";
            try (PreparedStatement pstmt = conn.prepareStatement(sqlNodes);
                 ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String type = rs.getString("type");
                    NarrativeNode node;
                    if ("trial".equals(type)) {
                        node = new TrialNode();
                    } else {
                        DialogNode dialogNode = new DialogNode();
                        dialogNode.setNextId(rs.getString("next_id"));
                        node = dialogNode;
                    }
                    node.setId(rs.getString("id"));
                    node.setText(rs.getString("text"));
                    node.setSpeakerId(rs.getString("speaker_id"));
                    node.setMusicTrack(rs.getString("music_track"));
                    nodes.put(node.getId(), node);
                }
            }

            String sqlOptions = "SELECT * FROM dialog_options";
            try (PreparedStatement pstmt = conn.prepareStatement(sqlOptions);
                 ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String nodeId = rs.getString("node_id");
                    NarrativeNode node = nodes.get(nodeId);
                    if (node instanceof DialogNode) {
                        DialogNode dialogNode = (DialogNode) node;
                        DialogOption option = new DialogOption();
                        option.setText(rs.getString("text"));
                        option.setTargetId(rs.getString("target_id"));
                        option.setRequiredFlag(rs.getString("required_flag"));
                        int score = rs.getInt("score_value");
                        if (!rs.wasNull()) {
                            option.setScoreValue(score);
                        }
                        dialogNode.getOptions().add(option);
                    }
                }
            }

            String sqlActions = "SELECT * FROM dialog_actions";
            try (PreparedStatement pstmt = conn.prepareStatement(sqlActions);
                 ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String nodeId = rs.getString("node_id");
                    NarrativeNode node = nodes.get(nodeId);
                    if (node != null) {
                        node.getActions().add(rs.getString("action_name"));
                    }
                }
            }

            String sqlTrials = "SELECT * FROM trial_evaluations";
            try (PreparedStatement pstmt = conn.prepareStatement(sqlTrials);
                 ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String nodeId = rs.getString("node_id");
                    NarrativeNode node = nodes.get(nodeId);
                    if (node instanceof TrialNode) {
                        TrialNode trialNode = (TrialNode) node;
                        TrialEvaluation trial = new TrialEvaluation();
                        trial.setThreshold(rs.getFloat("threshold"));
                        trial.setSuccessTargetId(rs.getString("success_target_id"));
                        trial.setFailTargetId(rs.getString("fail_target_id"));
                        trial.setSuccessFlag(rs.getString("success_flag"));
                        trialNode.setTrialEvaluation(trial);
                    }
                }
            }

        } catch (SQLException e) {
            throw new IntentiaException("Error al cargar los nodos de diálogo desde la base de datos: " + e.getMessage());
        }

        return nodes;
    }
}
