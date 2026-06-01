package io.yourPath.utils;

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction;
import com.badlogic.gdx.scenes.scene2d.ui.Label;

public class TypewriterAction extends TemporalAction {
    private Label label;
    private String textoCompleto;
    private boolean completado;

    public TypewriterAction(Label label, String textoCompleto, float duracion) {
        this.label = label;
        this.textoCompleto = textoCompleto;
        this.completado = false;
        setDuration(duracion);
    }

    @Override
    protected void update(float percent) {
        int chars = (int) (textoCompleto.length() * percent);
        chars = Math.max(0, Math.min(chars, textoCompleto.length()));
        label.setText(textoCompleto.substring(0, chars));
        if (percent >= 1f) {
            completado = true;
        }
    }

    public boolean estaCompletado() {
        return completado;
    }

    public void completarInstantaneo() {
        label.setText(textoCompleto);
        completado = true;
    }
}
