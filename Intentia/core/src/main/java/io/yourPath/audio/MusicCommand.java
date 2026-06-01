package io.yourPath.audio;

public class MusicCommand {
    public final String ruta;
    public final MusicPriority prioridad;
    public final float fadeIn;
    public final float fadeOut;
    public final boolean loop;
    public final Runnable alCompletar;

    public MusicCommand(String ruta, MusicPriority prioridad, float fadeIn, float fadeOut, boolean loop, Runnable alCompletar) {
        this.ruta = ruta;
        this.prioridad = prioridad;
        this.fadeIn = fadeIn;
        this.fadeOut = fadeOut;
        this.loop = loop;
        this.alCompletar = alCompletar;
    }

    public static MusicCommand menu(String ruta) {
        return new MusicCommand(ruta, MusicPriority.MENU, 1.5f, 1f, true, null);
    }

    public static MusicCommand gameplay(String ruta) {
        return new MusicCommand(ruta, MusicPriority.GAMEPLAY, 1f, 1f, true, null);
    }

    public static MusicCommand cinematica(String ruta) {
        return new MusicCommand(ruta, MusicPriority.CINEMATIC, 1f, 1f, false, null);
    }

    public static MusicCommand dialogo(String ruta) {
        return new MusicCommand(ruta, MusicPriority.DIALOGUE, 0.3f, 0.3f, true, null);
    }

    public static MusicCommand silencio(float fadeOut) {
        return new MusicCommand(null, MusicPriority.CINEMATIC, 0f, fadeOut, false, null);
    }
}
