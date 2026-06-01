package io.yourPath.screens;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Interpolation;
import static io.yourPath.utils.Colors.VERDE_AGUA;

public class TransitionConfig {

    public enum Mode {
        FADE_TO_COLOR,
        CROSSFADE
    }

    public final Mode mode;
    public final float fadeOutDuration;
    public final float holdDuration;
    public final float fadeInDuration;
    public final Color color;
    public final Interpolation easing;

    private TransitionConfig(Mode mode, float out, float hold, float in, Color color, Interpolation easing) {
        this.mode = mode;
        this.fadeOutDuration = out;
        this.holdDuration = hold;
        this.fadeInDuration = in;
        this.color = color;
        this.easing = easing;
    }

    public static TransitionConfig fadeToBlack(float fadeOut, float fadeIn) {
        return new TransitionConfig(Mode.FADE_TO_COLOR, fadeOut, 0f, fadeIn, Color.BLACK, Interpolation.pow2);
    }

    public static TransitionConfig fadeToBlack() {
        return fadeToBlack(0.25f, 0.25f);
    }

    public static TransitionConfig flashVerdeAgua() {
        return new TransitionConfig(Mode.FADE_TO_COLOR, 0.1f, 0.08f, 0.35f, VERDE_AGUA, Interpolation.pow3);
    }

    public static TransitionConfig crossfade(float duration) {
        return new TransitionConfig(Mode.CROSSFADE, duration, 0f, duration, null, Interpolation.fade);
    }

    public static TransitionConfig crossfade() {
        return crossfade(0.35f);
    }

    public static TransitionConfig fadeToColor(float out, float in, Color color) {
        return new TransitionConfig(Mode.FADE_TO_COLOR, out, 0f, in, color, Interpolation.pow2);
    }

    public float totalDuration() {
        return fadeOutDuration + holdDuration + fadeInDuration;
    }
}
