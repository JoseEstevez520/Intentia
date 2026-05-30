package io.yourPath.utils;

import io.yourPath.models.CharacterProfile;
import io.yourPath.models.DialogNode;
import io.yourPath.models.DialogOption;
import io.yourPath.models.TrialEvaluation;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class NarrativeDAOImplementation implements NarrativeDAO {
    private String url;

    public NarrativeDAOImplementation(String dbPath) {
        this.url = "jdbc:sqlite:" + dbPath;
    }

    @Override
    public Map<String, CharacterProfile> getAllCharacters() {
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
            System.out.println("Error al cargar personajes: " + e.getMessage());
        }
        return characters;
    }

    @Override
    public Map<String, DialogNode> getAllDialogNodes() {
        Map<String, DialogNode> nodes = new HashMap<>();
        
        try (Connection conn = DriverManager.getConnection(url)) {
            // 1. Cargar Nodos
            String sqlNodes = "SELECT * FROM dialog_nodes";
            try (PreparedStatement pstmt = conn.prepareStatement(sqlNodes);
                 ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    DialogNode node = new DialogNode();
                    node.setId(rs.getString("id"));
                    node.setText(rs.getString("text"));
                    node.setNextId(rs.getString("next_id"));
                    node.setSpeakerId(rs.getString("speaker_id"));
                    node.setMusicTrack(rs.getString("music_track"));
                    nodes.put(node.getId(), node);
                }
            }
            
            // 2. Cargar Opciones
            String sqlOptions = "SELECT * FROM dialog_options";
            try (PreparedStatement pstmt = conn.prepareStatement(sqlOptions);
                 ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String nodeId = rs.getString("node_id");
                    DialogNode node = nodes.get(nodeId);
                    if (node != null) {
                        DialogOption option = new DialogOption();
                        option.setText(rs.getString("text"));
                        option.setTargetId(rs.getString("target_id"));
                        option.setRequiredFlag(rs.getString("required_flag"));
                        // Manejo de Integer (puede ser null en la BD)
                        int score = rs.getInt("score_value");
                        if (!rs.wasNull()) {
                            option.setScoreValue(score);
                        }
                        node.getOptions().add(option);
                    }
                }
            }
            
            // 3. Cargar Acciones
            String sqlActions = "SELECT * FROM dialog_actions";
            try (PreparedStatement pstmt = conn.prepareStatement(sqlActions);
                 ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String nodeId = rs.getString("node_id");
                    DialogNode node = nodes.get(nodeId);
                    if (node != null) {
                        node.getActions().add(rs.getString("action_name"));
                    }
                }
            }
            
            // 4. Cargar Pruebas (Trials)
            String sqlTrials = "SELECT * FROM trial_evaluations";
            try (PreparedStatement pstmt = conn.prepareStatement(sqlTrials);
                 ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String nodeId = rs.getString("node_id");
                    DialogNode node = nodes.get(nodeId);
                    if (node != null) {
                        TrialEvaluation trial = new TrialEvaluation();
                        trial.setThreshold(rs.getFloat("threshold"));
                        trial.setSuccessTargetId(rs.getString("success_target_id"));
                        trial.setFailTargetId(rs.getString("fail_target_id"));
                        trial.setSuccessFlag(rs.getString("success_flag"));
                        node.setTrialEvaluation(trial);
                    }
                }
            }

        } catch (SQLException e) {
            System.out.println("Error al cargar nodos de diálogo: " + e.getMessage());
        }

        return nodes;
    }
}
