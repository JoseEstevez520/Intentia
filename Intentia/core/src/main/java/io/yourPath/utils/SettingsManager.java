package io.yourPath.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class SettingsManager {
    private static final String FILE_NAME = "config.json";
    private static final Json json = new Json();
    private static final float RATIO_16_9 = 16f / 9f;
    private static final float TOLERANCIA = 0.02f;

    private static SettingsManager instancia;
    private SettingsData data;
    private final List<Runnable> listeners = new ArrayList<>(4);

    private SettingsManager() {
        json.setOutputType(JsonWriter.OutputType.json);
    }

    public static SettingsManager inst() {
        if (instancia == null) {
            instancia = new SettingsManager();
            instancia.load();
        }
        return instancia;
    }

    public void load() {
        FileHandle file = Gdx.files.local(FILE_NAME);
        if (file.exists()) {
            try {
                data = json.fromJson(SettingsData.class, file);
                data.migrate();
            } catch (Exception e) {
                System.err.println("Config corrupta, defaults: " + e.getMessage());
                data = new SettingsData();
            }
        } else {
            data = new SettingsData();
        }
    }

    public void save() {
        try {
            Gdx.files.local(FILE_NAME).writeString(json.prettyPrint(data), false);
        } catch (Exception e) {
            System.err.println("No se pudo guardar config: " + e.getMessage());
        }
    }

    public void aplicarFullscreen() {
        try {
            Graphics.DisplayMode monitor = Gdx.graphics.getDisplayMode();
            data.resolutionX = monitor.width;
            data.resolutionY = monitor.height;
            Gdx.graphics.setFullscreenMode(monitor);
            data.fullscreen = true;
            save();
        } catch (Exception e) {
            System.err.println("Error al cambiar a pantalla completa: " + e.getMessage());
            data.fullscreen = false;
            aplicarVentana();
        }
        notificar();
    }

    public void aplicarVentana() {
        try {
            data.resolutionX = 640;
            data.resolutionY = 360;
            Gdx.graphics.setWindowedMode(640, 360);
            data.fullscreen = false;
            save();
        } catch (Exception e) {
            System.err.println("Error al cambiar a ventana: " + e.getMessage());
        }
        notificar();
    }

    public void toggleFullscreen() {
        if (data.fullscreen) {
            aplicarVentana();
        } else {
            aplicarFullscreen();
        }
    }

    public static List<Resolution> getResolucionesDisponibles() {
        Graphics.DisplayMode[] raw = Gdx.graphics.getDisplayModes();
        Set<Resolution> unicas = new LinkedHashSet<>();
        for (Graphics.DisplayMode m : raw) {
            float ratio = (float) m.width / m.height;
            if (Math.abs(ratio - RATIO_16_9) < TOLERANCIA && m.width >= 640 && m.height >= 360) {
                unicas.add(new Resolution(m.width, m.height));
            }
        }
        List<Resolution> ordenadas = new ArrayList<>(unicas);
        ordenadas.sort(Comparator.comparingInt(r -> r.width));
        if (ordenadas.isEmpty()) {
            ordenadas.add(new Resolution(640, 360));
            ordenadas.add(new Resolution(960, 540));
            ordenadas.add(new Resolution(1280, 720));
            ordenadas.add(new Resolution(1920, 1080));
        }
        return ordenadas;
    }

    public void setResolution(int w, int h) {
        data.resolutionX = w;
        data.resolutionY = h;
        if (!data.fullscreen) {
            try {
                Gdx.graphics.setWindowedMode(w, h);
            } catch (Exception ignored) {}
        }
        save();
    }

    public void setMusicVolume(float v) {
        data.musicVolume = Math.max(0, Math.min(1, v));
        save();
        notificar();
    }

    public void setSfxVolume(float v) {
        data.sfxVolume = Math.max(0, Math.min(1, v));
        save();
        notificar();
    }

    public SettingsData data() { return data; }
    public int getResolutionX() { return data.resolutionX; }
    public int getResolutionY() { return data.resolutionY; }
    public boolean isFullscreen() { return data.fullscreen; }
    public float getMusicVolume() { return data.musicVolume; }
    public float getSfxVolume() { return data.sfxVolume; }

    public void addListener(Runnable l) { listeners.add(l); }
    public void removeListener(Runnable l) { listeners.remove(l); }
    private void notificar() {
        new ArrayList<>(listeners).forEach(Runnable::run);
    }
}
