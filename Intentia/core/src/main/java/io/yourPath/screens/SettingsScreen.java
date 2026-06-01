package io.yourPath.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.FitViewport;
import io.yourPath.Main;
import io.yourPath.utils.SettingsManager;
import io.yourPath.utils.SkinUtil;

public class SettingsScreen implements Screen {
    private Main game;
    private Stage stage;
    private Skin skin;

    public SettingsScreen(Main game) {
        this.game = game;
    }

    @Override
    public void show() {
        stage = new Stage(new FitViewport(640, 360));
        Gdx.input.setInputProcessor(stage);
        skin = SkinUtil.crear();

        SettingsManager cfg = SettingsManager.inst();

        Table root = new Table();
        root.setFillParent(true);
        root.pad(20);

        Label titulo = new Label("AJUSTES", skin);
        titulo.setFontScale(1.8f);

        Label lblVideo = new Label("VIDEO", skin, "nombre");

        TextButton btnFullscreen = new TextButton(
            cfg.isFullscreen() ? "VENTANA" : "PANTALLA COMPLETA", skin);
        btnFullscreen.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                cfg.toggleFullscreen();
                btnFullscreen.setText(cfg.isFullscreen() ? "VENTANA" : "PANTALLA COMPLETA");
            }
        });

        Label lblAudio = new Label("AUDIO", skin, "nombre");

        Label lblVolMusica = new Label("Musica: " + (int)(cfg.getMusicVolume() * 100) + "%", skin);
        TextButton btnMusMenos = new TextButton("-", skin);
        TextButton btnMusMas = new TextButton("+", skin);
        btnMusMenos.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                cfg.setMusicVolume(cfg.getMusicVolume() - 0.1f);
                lblVolMusica.setText("Musica: " + (int)(cfg.getMusicVolume() * 100) + "%");
            }
        });
        btnMusMas.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                cfg.setMusicVolume(cfg.getMusicVolume() + 0.1f);
                lblVolMusica.setText("Musica: " + (int)(cfg.getMusicVolume() * 100) + "%");
            }
        });

        Label lblVolSfx = new Label("SFX: " + (int)(cfg.getSfxVolume() * 100) + "%", skin);
        TextButton btnSfxMenos = new TextButton("-", skin);
        TextButton btnSfxMas = new TextButton("+", skin);
        btnSfxMenos.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                cfg.setSfxVolume(cfg.getSfxVolume() - 0.1f);
                lblVolSfx.setText("SFX: " + (int)(cfg.getSfxVolume() * 100) + "%");
            }
        });
        btnSfxMas.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                cfg.setSfxVolume(cfg.getSfxVolume() + 0.1f);
                lblVolSfx.setText("SFX: " + (int)(cfg.getSfxVolume() * 100) + "%");
            }
        });

        TextButton btnVolver = new TextButton("VOLVER AL MENU", skin);
        btnVolver.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.setScreen(new MainMenuScreen(game));
            }
        });

        root.add(titulo).colspan(4).padBottom(24).row();
        root.add(lblVideo).colspan(4).left().padBottom(8).row();
        root.add(btnFullscreen).colspan(4).fillX().padBottom(20).row();
        root.add(lblAudio).colspan(4).left().padBottom(8).row();
        root.add(lblVolMusica).left().colspan(2);
        root.add(btnMusMenos).width(30);
        root.add(btnMusMas).width(30).row();
        root.add(lblVolSfx).left().colspan(2);
        root.add(btnSfxMenos).width(30);
        root.add(btnSfxMas).width(30).padBottom(24).row();
        root.add(btnVolver).colspan(4).center();

        stage.addActor(root);

        stage.addAction(Actions.sequence(Actions.fadeOut(0), Actions.fadeIn(0.3f)));
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.05f, 0.05f, 0.1f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
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
