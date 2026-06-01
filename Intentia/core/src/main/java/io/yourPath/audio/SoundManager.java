package io.yourPath.audio;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.MathUtils;
import io.yourPath.utils.SettingsManager;

public class SoundManager {

    private static SoundManager instancia;
    private Sound click;
    private Sound typewriter;
    private Sound page;
    private Sound confirm;
    private Sound deny;
    private Sound pasoIzq;
    private Sound pasoDer;
    private Sound interact;
    private float volumen = 0.7f;
    private boolean listo;

    private SoundManager() {}

    public static SoundManager inst() {
        if (instancia == null) instancia = new SoundManager();
        return instancia;
    }

    public void init() {
        click = cargar("_click", WavGenerator.click());
        typewriter = cargar("_typewriter", WavGenerator.typewriter());
        page = cargar("_page", WavGenerator.page());
        confirm = cargar("_confirm", WavGenerator.confirm());
        deny = cargar("_deny", WavGenerator.deny());
        pasoIzq = cargar("_paso_izq", WavGenerator.paso(true));
        pasoDer = cargar("_paso_der", WavGenerator.paso(false));
        interact = cargar("_interact", WavGenerator.interact());
        volumen = SettingsManager.inst().getSfxVolume();
        listo = true;
    }

    private Sound cargar(String nombre, byte[] wav) {
        FileHandle cache = Gdx.files.local("sfx/" + nombre + ".wav");
        if (!cache.exists()) {
            cache.writeBytes(wav, false);
        }
        try {
            return Gdx.audio.newSound(cache);
        } catch (Exception e) {
            return null;
        }
    }

    public void click() { play(click, 1f, 1.2f, 1f); }

    public void typewriter() { typewriter(' '); }

    public void typewriter(char c) {
        if (c == '.' || c == ',' || c == ';' || c == ':' || c == ')' || c == '(') {
            play(typewriter, 1f, 0.75f, 0.9f);
        } else if (c == '!' || c == '?' || c == '¿' || c == '¡') {
            play(typewriter, 1f, 1.2f, 1f);
        } else {
            float pitch = 0.95f + MathUtils.random(0.1f);
            float vol = 0.75f + MathUtils.random(0.2f);
            play(typewriter, 1f, pitch, vol);
        }
    }

    public void paso(boolean izquierda) {
        play(izquierda ? pasoIzq : pasoDer, 1f, 0.9f + MathUtils.random(0.2f), 0.15f);
    }

    public void interact() { play(interact, 1f, 1f, 1f); }

    public void page() { play(page, 1f, 1f, 1f); }
    public void confirm() { play(confirm, 1f, 1f, 0.8f); }
    public void deny() { play(deny, 1f, 1f, 1f); }

    private void play(Sound sound, float v, float pitch, float vMod) {
        if (!listo || sound == null || volumen <= 0) return;
        sound.play(MathUtils.clamp(volumen * vMod, 0, 1), MathUtils.clamp(pitch, 0.5f, 2f), 0f);
    }

    public void setVolumen(float vol) {
        this.volumen = Math.max(0, Math.min(1, vol));
    }

    public void dispose() {
        if (click != null) click.dispose();
        if (typewriter != null) typewriter.dispose();
        if (page != null) page.dispose();
        if (confirm != null) confirm.dispose();
        if (deny != null) deny.dispose();
        if (pasoIzq != null) pasoIzq.dispose();
        if (pasoDer != null) pasoDer.dispose();
        if (interact != null) interact.dispose();
        listo = false;
    }
}
