package io.yourPath.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.video.VideoPlayer;
import com.badlogic.gdx.video.VideoPlayerCreator;
import io.yourPath.Main;
import static io.yourPath.screens.TransitionConfig.*;

public class CinematicScreen implements Screen {
    private Main juego;
    private Screen siguiente;
    private VideoPlayer reproductor;
    private SpriteBatch batch;
    private OrthographicCamera camara;
    private FitViewport viewport;
    private boolean terminado = false;
    private boolean listo = false;

    public CinematicScreen(Main juego, Screen siguiente) {
        this.juego = juego;
        this.siguiente = siguiente;
    }

    @Override
    public void show() {
        batch = new SpriteBatch();
        camara = new OrthographicCamera();
        viewport = new FitViewport(640, 360);
        viewport.setCamera(camara);

        juego.getMusicManager().play(io.yourPath.audio.MusicCommand.silencio(1f));

        reproductor = VideoPlayerCreator.createVideoPlayer();
        FileHandle video = Gdx.files.internal("videos/intro.webm");
        try {
            reproductor.load(video);
            reproductor.play();
            listo = true;
        } catch (Exception e) {
            System.err.println("Error al cargar video: " + e.getMessage());
            terminado = true;
        }
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) ||
            Gdx.input.isKeyJustPressed(Input.Keys.SPACE) ||
            Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            terminado = true;
        }

        if (!listo) {
            if (terminado) {
                dispose();
                juego.transitarA(siguiente, flashVerdeAgua());
            }
            return;
        }

        reproductor.update();

        if (!reproductor.isPlaying()) {
            terminado = true;
        }

        if (terminado) {
            dispose();
            juego.transitarA(siguiente, flashVerdeAgua());
            return;
        }

        Texture frame = reproductor.getTexture();
        if (frame != null) {
            batch.begin();
            float escalaX = (float) viewport.getWorldWidth() / frame.getWidth();
            float escalaY = (float) viewport.getWorldHeight() / frame.getHeight();
            float escala = Math.min(escalaX, escalaY);
            float ancho = frame.getWidth() * escala;
            float alto = frame.getHeight() * escala;
            float x = (viewport.getWorldWidth() - ancho) / 2f;
            float y = (viewport.getWorldHeight() - alto) / 2f;
            batch.draw(frame, x, y, ancho, alto);
            batch.end();
        }
    }

    @Override
    public void resize(int ancho, int alto) {
        if (viewport != null) viewport.update(ancho, alto, true);
    }

    @Override
    public void dispose() {
        if (reproductor != null) {
            reproductor.stop();
            reproductor.dispose();
        }
        if (batch != null) {
            batch.dispose();
        }
    }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
}
