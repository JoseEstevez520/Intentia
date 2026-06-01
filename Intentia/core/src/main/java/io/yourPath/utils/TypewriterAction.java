package io.yourPath.utils;

import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;

public class TypewriterAction extends Action {

    private Label label;
    private String textoCompleto;
    private int currentChar;
    private float[] charTimes;
    private float elapsed;
    private boolean completado;
    private CharCallback onCharReveal;

    public TypewriterAction(Label label, String textoCompleto) {
        this.label = label;
        this.textoCompleto = textoCompleto;
        this.currentChar = 0;
        this.elapsed = 0;
        this.completado = false;
        calcularDelays();
    }

    private void calcularDelays() {
        int len = textoCompleto.length();
        charTimes = new float[len];
        for (int i = 0; i < len; i++) {
            char c = textoCompleto.charAt(i);
            if (Character.isWhitespace(c)) {
                charTimes[i] = 0.025f;
            } else if (c == '.' || c == ',' || c == ';' || c == ':' || c == ')' || c == '(') {
                charTimes[i] = 0.15f;
            } else if (c == '!' || c == '?' || c == '¿' || c == '¡' || c == '—') {
                charTimes[i] = 0.22f;
            } else if (c == '\n') {
                charTimes[i] = 0.35f;
            } else {
                charTimes[i] = 0.065f;
            }
        }
    }

    public void setOnCharReveal(CharCallback cb) {
        this.onCharReveal = cb;
    }

    @Override
    public boolean act(float delta) {
        if (completado) return true;

        elapsed += delta;

        while (currentChar < textoCompleto.length() && elapsed >= charTimes[currentChar]) {
            elapsed -= charTimes[currentChar];
            currentChar++;

            if (onCharReveal != null) {
                char c = textoCompleto.charAt(currentChar - 1);
                if (!Character.isWhitespace(c)) {
                    onCharReveal.onChar(c);
                }
            }
        }

        label.setText(textoCompleto.substring(0, Math.min(currentChar, textoCompleto.length())));

        if (currentChar >= textoCompleto.length()) {
            completado = true;
            return true;
        }
        return false;
    }

    public boolean estaCompletado() {
        return completado;
    }

    public void completarInstantaneo() {
        currentChar = textoCompleto.length();
        label.setText(textoCompleto);
        completado = true;
    }

    @Override
    public void reset() {
        super.reset();
        label = null;
        textoCompleto = null;
        charTimes = null;
        currentChar = 0;
        elapsed = 0;
        completado = false;
        onCharReveal = null;
        charTimes = null;
    }
}
