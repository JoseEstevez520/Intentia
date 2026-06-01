package io.yourPath.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.FitViewport;
import io.yourPath.Main;
import io.yourPath.audio.SoundManager;
import io.yourPath.utils.SettingsManager;
import io.yourPath.utils.SkinUtil;
import static io.yourPath.screens.TransitionConfig.*;

public class SettingsScreen implements Screen {

    private Main game;
    private Stage stage;
    private Skin skin;
    private Label lblVolMusica;
    private Label lblVolSfx;

    public SettingsScreen(Main game) {
        this.game = game;
    }

    @Override
    public void show() {
        stage = new Stage(new FitViewport(640, 360));
        Gdx.input.setInputProcessor(stage);
        skin = SkinUtil.crear();
        SettingsManager cfg = SettingsManager.inst();

        Table raiz = new Table();
        raiz.setFillParent(true);
        raiz.center();

        Table panel = new Table(skin);
        panel.setBackground(skin.newDrawable("pausa-fondo"));
        panel.pad(8);

        Label titulo = new Label("AJUSTES", skin, "nombre");

        Label lblVideo = new Label("VIDEO", skin, "dialogo-texto");

        TextButton btnFullscreen = new TextButton(
            cfg.isFullscreen() ? "VENTANA" : "PANTALLA COMPLETA", skin);
        btnFullscreen.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                SoundManager.inst().click();
                cfg.toggleFullscreen();
                btnFullscreen.setText(cfg.isFullscreen() ? "VENTANA" : "PANTALLA COMPLETA");
            }
        });

        Label lblAudio = new Label("AUDIO", skin, "dialogo-texto");

        Slider sliderMusica = new Slider(0f, 1f, 0.05f, false, skin, "default-slider");
        sliderMusica.setValue(cfg.getMusicVolume());
        lblVolMusica = new Label((int)(cfg.getMusicVolume() * 100) + "%", skin, "dialogo-texto");
        sliderMusica.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                cfg.setMusicVolume(sliderMusica.getValue());
                lblVolMusica.setText((int)(sliderMusica.getValue() * 100) + "%");
            }
        });

        Slider sliderSfx = new Slider(0f, 1f, 0.05f, false, skin, "default-slider");
        sliderSfx.setValue(cfg.getSfxVolume());
        lblVolSfx = new Label((int)(cfg.getSfxVolume() * 100) + "%", skin, "dialogo-texto");
        sliderSfx.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                cfg.setSfxVolume(sliderSfx.getValue());
                lblVolSfx.setText((int)(sliderSfx.getValue() * 100) + "%");
            }
        });

        TextButton btnVolver = new TextButton("VOLVER", skin);
        btnVolver.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                SoundManager.inst().click();
                game.transitarA(new MainMenuScreen(game), crossfade());
            }
        });

        float anchoSlider = 118f;
        int indent = 8;

        panel.add(titulo).colspan(4).center().padBottom(12).row();
        panel.add(lblVideo).colspan(4).left().padBottom(4).row();
        panel.add(btnFullscreen).colspan(4).fillX().padBottom(2).row();
        panel.add(lblAudio).colspan(4).left().padBottom(4).padTop(6).row();
        panel.add().width(indent);
        panel.add(new Label("Musica", skin, "dialogo-texto")).left();
        panel.add(sliderMusica).width(anchoSlider).padLeft(4).padRight(4);
        panel.add(lblVolMusica).left().row();
        panel.add().width(indent);
        panel.add(new Label("SFX", skin, "dialogo-texto")).left();
        panel.add(sliderSfx).width(anchoSlider).padLeft(4).padRight(4);
        panel.add(lblVolSfx).left().padBottom(12).row();
        panel.add(btnVolver).colspan(4).center().padTop(4);

        raiz.add(panel).width(240);
        stage.addActor(raiz);

        stage.addAction(Actions.sequence(Actions.fadeOut(0), Actions.fadeIn(0.3f)));
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.2f, 0.6f, 0.85f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.transitarA(new MainMenuScreen(game), crossfade());
            return;
        }

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int w, int h) {
        stage.getViewport().update(w, h, true);
    }

    @Override
    public void dispose() {
        stage.dispose();
        skin.dispose();
    }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
}
