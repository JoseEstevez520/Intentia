package io.yourPath.audio;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.math.MathUtils;

public class MusicManager {
    private Music musicaActual;
    private MusicCommand comandoActual;
    private MusicCommand comandoPendiente;

    private enum Estado { QUIETO, SALIENDO, ENTRANDO, CRUZANDO }
    private Estado estado = Estado.QUIETO;
    private float temporizador = 0;
    private float volumenMaestro = 0.5f;

    public void play(MusicCommand cmd) {
        if (cmd == null) return;

        if (comandoActual != null && cmd.prioridad.esMasPrioritariaQue(comandoActual.prioridad)) {
            comandoPendiente = cmd;
            iniciarSalida();
            return;
        }

        if (comandoActual != null && cmd.ruta != null && cmd.ruta.equals(comandoActual.ruta)) {
            return;
        }

        if (comandoActual == null) {
            if (cmd.ruta == null) return;
            comandoPendiente = cmd;
            iniciarEntrada();
            return;
        }

        if (cmd.ruta == null) {
            comandoPendiente = cmd;
            iniciarSalida();
            return;
        }

        if (cmd.prioridad.valor < comandoActual.prioridad.valor) {
            return;
        }
    }

    private void iniciarSalida() {
        if (musicaActual == null) {
            pasarSiguiente();
            return;
        }
        estado = Estado.SALIENDO;
        temporizador = comandoActual != null ? comandoActual.fadeOut : 1f;
    }

    private void iniciarEntrada() {
        if (comandoPendiente == null || comandoPendiente.ruta == null) {
            estado = Estado.QUIETO;
            comandoPendiente = null;
            return;
        }
        try {
            Music nueva = Gdx.audio.newMusic(Gdx.files.internal(comandoPendiente.ruta));
            nueva.setVolume(0);
            nueva.setLooping(comandoPendiente.loop);
            nueva.play();

            if (musicaActual != null) {
                musicaActual.stop();
                musicaActual.dispose();
            }

            musicaActual = nueva;
            comandoActual = comandoPendiente;
            comandoPendiente = null;

            estado = Estado.ENTRANDO;
            temporizador = comandoActual.fadeIn;
        } catch (Exception e) {
            estado = Estado.QUIETO;
            comandoPendiente = null;
        }
    }

    private void pasarSiguiente() {
        if (comandoPendiente != null) {
            iniciarEntrada();
        } else {
            estado = Estado.QUIETO;
        }
    }

    public void update(float delta) {
        switch (estado) {
            case SALIENDO:
                temporizador -= delta;
                if (musicaActual != null && comandoActual != null && comandoActual.fadeOut > 0) {
                    musicaActual.setVolume(MathUtils.clamp(temporizador / comandoActual.fadeOut, 0, 1) * volumenMaestro);
                }
                if (temporizador <= 0) {
                    if (musicaActual != null) {
                        musicaActual.stop();
                        musicaActual.dispose();
                        musicaActual = null;
                    }
                    Runnable cb = comandoActual != null ? comandoActual.alCompletar : null;
                    comandoActual = null;
                    pasarSiguiente();
                    if (cb != null) cb.run();
                }
                break;

            case ENTRANDO:
                temporizador -= delta;
                if (musicaActual != null && comandoActual != null && comandoActual.fadeIn > 0) {
                    float progreso = 1f - MathUtils.clamp(temporizador / comandoActual.fadeIn, 0, 1);
                    musicaActual.setVolume(progreso * volumenMaestro);
                }
                if (temporizador <= 0) {
                    if (musicaActual != null) {
                        musicaActual.setVolume(volumenMaestro);
                    }
                    estado = Estado.QUIETO;
                }
                break;

            case QUIETO:
                break;

            case CRUZANDO:
                break;
        }
    }

    public void stop(float fade) {
        if (musicaActual == null) return;
        comandoPendiente = new MusicCommand(null, comandoActual.prioridad, 0, fade, false, null);
        iniciarSalida();
    }

    public void pausarTodo() {
        if (musicaActual != null) musicaActual.pause();
    }

    public void reanudarTodo() {
        if (musicaActual != null) musicaActual.play();
    }

    public void setVolumen(float vol) {
        volumenMaestro = MathUtils.clamp(vol, 0, 1);
        if (musicaActual != null && estado == Estado.QUIETO) {
            musicaActual.setVolume(volumenMaestro);
        }
    }

    public void dispose() {
        if (musicaActual != null) {
            musicaActual.stop();
            musicaActual.dispose();
            musicaActual = null;
        }
        comandoActual = null;
        comandoPendiente = null;
        estado = Estado.QUIETO;
    }
}
