package io.yourPath.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import io.yourPath.Main;

public class TransitionScreen implements Screen {

    private static final int ANCHO = 640;
    private static final int ALTO = 360;

    private Main game;
    private Screen origin;
    private Screen destination;
    private TransitionConfig config;

    private float elapsed;
    private boolean switched;

    private SpriteBatch batch;
    private OrthographicCamera camera;
    private FitViewport viewport;
    private Texture pixelBlanco;

    private FrameBuffer fboOrigen;
    private Texture texOrigen;
    private boolean usarFBO;

    private enum Fase { FADE_OUT, HOLD, FADE_IN, COMPLETADO }
    private Fase fase = Fase.FADE_OUT;

    public TransitionScreen(Main game, Screen origin, Screen destination, TransitionConfig config) {
        this.game = game;
        this.origin = origin;
        this.destination = destination;
        this.config = config;
    }

    @Override
    public void show() {
        batch = new SpriteBatch();
        camera = new OrthographicCamera();
        viewport = new FitViewport(ANCHO, ALTO);
        viewport.setCamera(camera);
        camera.position.set(ANCHO / 2f, ALTO / 2f, 0);
        camera.update();

        Pixmap pixmap = new Pixmap(1, 1, Format.RGBA8888);
        pixmap.setColor(1, 1, 1, 1);
        pixmap.fill();
        pixelBlanco = new Texture(pixmap);
        pixmap.dispose();

        elapsed = 0f;
        switched = false;
        fase = Fase.FADE_OUT;

        if (config.mode == TransitionConfig.Mode.CROSSFADE) {
            try {
                fboOrigen = new FrameBuffer(Format.RGBA8888, ANCHO, ALTO, false);
                fboOrigen.begin();
                Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
                origin.render(0f);
                fboOrigen.end();
                texOrigen = fboOrigen.getColorBufferTexture();
                texOrigen.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

                destination.show();
                switched = true;

                usarFBO = true;
            } catch (Exception e) {
                System.err.println("FBO no disponible, usando fade-to-black: " + e.getMessage());
                config = TransitionConfig.fadeToBlack();
                usarFBO = false;
            }
        }
    }

    @Override
    public void render(float delta) {
        if (fase == Fase.COMPLETADO) {
            destination.render(delta);
            return;
        }

        elapsed += delta;

        switch (fase) {
            case FADE_OUT:
                renderFadeOut(delta);
                if (elapsed >= config.fadeOutDuration) {
                    fase = Fase.HOLD;
                    elapsed = 0f;
                }
                break;
            case HOLD:
                renderHold(delta);
                if (elapsed >= config.holdDuration) {
                    realizarSwitch();
                    fase = Fase.FADE_IN;
                    elapsed = 0f;
                }
                break;
            case FADE_IN:
                renderFadeIn(delta);
                if (elapsed >= config.fadeInDuration) {
                    fase = Fase.COMPLETADO;
                    limpiar();
                    game.setScreenFinal(destination);
                }
                break;
        }
    }

    private void renderFadeOut(float delta) {
        float progress = MathUtils.clamp(elapsed / config.fadeOutDuration, 0f, 1f);
        float alpha = config.easing.apply(progress);

        if (usarFBO) {
            Gdx.gl.glClearColor(0, 0, 0, 1);
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

            destination.render(delta);

            batch.setProjectionMatrix(camera.combined);
            batch.begin();
            batch.setColor(1, 1, 1, 1f - alpha);
            batch.draw(texOrigen, 0, ALTO, ANCHO, -ALTO);
            batch.end();
            batch.setColor(1, 1, 1, 1);
        } else {
            origin.render(delta);
            dibujarOverlay(config.color, alpha);
        }
    }

    private void renderHold(float delta) {
        if (usarFBO) {
            destination.render(delta);
        } else {
            dibujarOverlay(config.color, 1f);
        }
    }

    private void renderFadeIn(float delta) {
        float progress = MathUtils.clamp(elapsed / config.fadeInDuration, 0f, 1f);
        float alpha = 1f - config.easing.apply(progress);
        alpha = MathUtils.clamp(alpha, 0f, 1f);

        if (usarFBO) {
            destination.render(delta);
        } else {
            destination.render(delta);
            dibujarOverlay(config.color, alpha);
        }
    }

    private void dibujarOverlay(com.badlogic.gdx.graphics.Color color, float alpha) {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        batch.setColor(color.r, color.g, color.b, alpha);
        batch.draw(pixelBlanco, 0, 0, ANCHO, ALTO);
        batch.end();

        batch.setColor(1, 1, 1, 1);
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void realizarSwitch() {
        if (switched) return;
        switched = true;

        destination.show();
        if (origin != null) {
            origin.hide();
            origin.dispose();
            origin = null;
        }
    }

    private void limpiar() {
        if (fboOrigen != null) {
            fboOrigen.dispose();
            fboOrigen = null;
            texOrigen = null;
        }
        if (pixelBlanco != null) {
            pixelBlanco.dispose();
            pixelBlanco = null;
        }
        if (batch != null) {
            batch.dispose();
            batch = null;
        }
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
        if (usarFBO && fase != Fase.COMPLETADO) {
            destination.resize(width, height);
        }
    }

    @Override
    public void dispose() {
        limpiar();
    }

    @Override
    public void pause() {
        if (destination != null) destination.pause();
    }

    @Override
    public void resume() {
        if (destination != null) destination.resume();
    }

    @Override
    public void hide() {}
}
