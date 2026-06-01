package io.yourPath.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;

public class ConfigManager {
    private static final String ARCHIVO = "config.json";
    private static final Json json = new Json();

    public float volumenMusica = 0.5f;
    public float volumenSFX = 0.7f;
    public boolean pantallaCompleta = false;
    public int anchoVentana = 640;
    public int altoVentana = 360;

    public void guardar() {
        try {
            json.setOutputType(JsonWriter.OutputType.json);
            String datos = json.prettyPrint(this);
            Gdx.files.local(ARCHIVO).writeString(datos, false);
        } catch (Exception e) {
            System.err.println("No se pudo guardar config: " + e.getMessage());
        }
    }

    public static ConfigManager cargar() {
        FileHandle archivo = Gdx.files.local(ARCHIVO);
        if (archivo.exists()) {
            try {
                return json.fromJson(ConfigManager.class, archivo);
            } catch (Exception e) {
                System.err.println("Config corrupta, usando valores por defecto: " + e.getMessage());
            }
        }
        return new ConfigManager();
    }
}
