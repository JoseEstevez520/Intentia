package io.yourPath.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.FitViewport;
import io.yourPath.Main;
import io.yourPath.audio.MusicCommand;
import io.yourPath.audio.SoundManager;
import io.yourPath.logic.StoryManager;
import io.yourPath.models.GameState;
import io.yourPath.utils.SaveSystem;
import io.yourPath.utils.SkinUtil;
import static io.yourPath.screens.TransitionConfig.*;

public class MainMenuScreen implements Screen {
    private Main game;
    private Stage stage;
    private Skin skin;
    private Texture titleTex;
    private boolean transicionando = false;

    public MainMenuScreen(Main game) {
        this.game = game;
    }

    @Override
    public void show() {
        stage = new Stage(new FitViewport(640, 360));
        Gdx.input.setInputProcessor(stage);
        skin = SkinUtil.crear();

        game.getMusicManager().play(MusicCommand.menu("music/menu.mp3"));

        Table raiz = new Table();
        raiz.setFillParent(true);
        raiz.center();
        stage.addActor(raiz);

        titleTex = new Texture("title.png");
        titleTex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        Image tituloImg = new Image(titleTex);
        tituloImg.setScaling(Scaling.fit);

        TextButton btnNuevo = new TextButton("NUEVA PARTIDA", skin);
        TextButton btnContinuar = new TextButton("CONTINUAR", skin);
        TextButton btnExplorar = new TextButton("EXPLORAR (BETA)", skin);
        TextButton btnPrueba = new TextButton("PRUEBA DEL DRAGON (TEST)", skin);
        TextButton btnAjustes = new TextButton("AJUSTES", skin);
        TextButton btnSalir = new TextButton("SALIR", skin);

        btnContinuar.setVisible(SaveSystem.exists());

        btnNuevo.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (transicionando) return;
                SoundManager.inst().click();
                transicionando = true;
                game.getStoryManager().start("car_awakening");
                Screen anterior = game.getScreen();
                game.setScreen(new CinematicScreen(game, new DialogOverlayScreen(game)));
                if (anterior != null) anterior.dispose();
            }
        });

        btnContinuar.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (transicionando) return;
                SoundManager.inst().click();
                transicionando = true;
                GameState guardado = SaveSystem.loadGame();
                if (guardado != null) {
                    game.setStoryManager(new StoryManager(game.getStory(), guardado));
                    game.transitarA(new DialogOverlayScreen(game), fadeToBlack());
                }
            }
        });

        btnExplorar.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (transicionando) return;
                SoundManager.inst().click();
                transicionando = true;
                game.transitarA(new GameScreen(game), crossfade());
            }
        });

        btnPrueba.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (transicionando) return;
                SoundManager.inst().click();
                transicionando = true;
                game.getStoryManager().getGameState().resetTrialScore();
                game.transitarA(new DialogOverlayScreen(game, "trial_start"), fadeToBlack());
            }
        });

        btnAjustes.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (transicionando) return;
                SoundManager.inst().click();
                transicionando = true;
                game.transitarA(new SettingsScreen(game), crossfade());
            }
        });

        btnSalir.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                SoundManager.inst().click();
                Gdx.app.exit();
            }
        });

        Table panelBotones = new Table(skin);
        panelBotones.setBackground(skin.newDrawable("pausa-fondo"));
        panelBotones.pad(8);

        panelBotones.add(btnNuevo).width(220).height(30).padBottom(4);
        panelBotones.row();
        panelBotones.add(btnContinuar).width(220).height(30).padBottom(4);
        panelBotones.row();
        panelBotones.add(btnExplorar).width(220).height(30).padBottom(4);
        panelBotones.row();
        panelBotones.add(btnPrueba).width(220).height(30).padBottom(4);
        panelBotones.row();
        panelBotones.add(btnAjustes).width(220).height(30).padBottom(4);
        panelBotones.row();
        panelBotones.add(btnSalir).width(220).height(30);

        raiz.add(tituloImg).size(240, 134).padBottom(6).row();
        raiz.add(panelBotones).width(240);

        stage.addAction(Actions.sequence(
            Actions.fadeOut(0),
            Actions.fadeIn(0.5f)
        ));
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.2f, 0.6f, 0.85f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void dispose() {
        stage.dispose();
        skin.dispose();
        if (titleTex != null) titleTex.dispose();
    }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
}
