package io.yourPath.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.FitViewport;
import io.yourPath.Main;
import io.yourPath.audio.MusicCommand;
import io.yourPath.logic.StoryManager;
import io.yourPath.models.CharacterProfile;
import io.yourPath.models.DialogNode;
import io.yourPath.models.DialogOption;
import io.yourPath.models.GameState;
import io.yourPath.models.NarrativeNode;
import io.yourPath.models.TrialNode;
import io.yourPath.models.UIState;
import io.yourPath.utils.SaveSystem;
import io.yourPath.utils.SkinUtil;
import io.yourPath.utils.TypewriterAction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DialogOverlayScreen implements Screen {
    private static final String ID_JUGADOR = "protagonista";

    private Main game;
    private StoryManager storyManager;
    private Map<String, CharacterProfile> personajes;
    private GameState estado;
    private Stage stage;
    private Skin skin;
    private UIState estadoUI = UIState.DIALOGANDO;

    private Table contenedorDialogo;
    private Label labelNombre;
    private Label labelTexto;
    private Table tablaOpciones;
    private Table zonaOpciones;
    private Image retrato;
    private Label flechaContinuar;
    private TypewriterAction typewriter;

    private Array<TextButton> opcionesBotones;
    private int indiceOpcion = -1;
    private DialogOption opcionUnicaAuto;

    private static final int MAX_CHARS_POR_PAGINA = 130;
    private List<String> paginasTexto;
    private int paginaActual;
    private Label labelPagina;

    private Screen fondoJuego;
    private boolean pausaDirecto;
    private String pistaActual;
    private Map<String, Texture> cacheRetratos = new HashMap<>();

    private float tiempoEspera = 0;
    private boolean esperandoInput = false;
    private boolean pausaAbierta = false;
    private Runnable onFinish;

    public DialogOverlayScreen(Main game) {
        this(game, null, null);
    }

    public DialogOverlayScreen(Main game, String startNodeId) {
        this(game, startNodeId, null);
    }

    public DialogOverlayScreen(Main game, String startNodeId, Runnable onFinish) {
        this.game = game;
        this.storyManager = game.getStoryManager();
        this.personajes = game.getCharacters();
        this.estado = storyManager.getGameState();
        this.onFinish = onFinish;
        if (startNodeId != null && !startNodeId.isEmpty()) {
            storyManager.start(startNodeId);
        }
    }

    @Override
    public void show() {
        stage = new Stage(new FitViewport(640, 360));
        Gdx.input.setInputProcessor(stage);
        skin = SkinUtil.crear();

        Table raiz = new Table();
        raiz.setFillParent(true);
        stage.addActor(raiz);

        if (pausaDirecto) {
            estadoUI = UIState.MENU_PAUSA;
            return;
        }

        labelNombre = new Label("", skin, "nombre-dialogo");
        labelNombre.setVisible(false);

        retrato = new Image();
        retrato.setVisible(false);

        labelTexto = new Label("", skin, "dialogo-texto");
        labelTexto.setWrap(true);

        tablaOpciones = new Table();

        labelPagina = new Label("", skin, "dialogo-texto");
        labelPagina.setVisible(false);

        zonaOpciones = new Table();
        zonaOpciones.left();
        zonaOpciones.add(tablaOpciones).left().expandX().fillX();
        zonaOpciones.add(labelPagina).right().padLeft(8);

        flechaContinuar = new Label("\u25B8", skin, "dialogo-texto");
        flechaContinuar.setVisible(false);

        Table textoColumna = new Table();
        textoColumna.left();
        textoColumna.add(labelNombre).left().padBottom(2).row();
        textoColumna.add(labelTexto).left().expand().fill().row();
        textoColumna.add(zonaOpciones).left().expandX().fillX().height(28);

        contenedorDialogo = new Table(skin);
        contenedorDialogo.setBackground(skin.newDrawable("fondo-dialogo"));
        contenedorDialogo.pad(5, 7, 5, 7);

        contenedorDialogo.add(retrato).size(72, 72).left().padRight(6).center();
        contenedorDialogo.add(textoColumna).expandX().fillX().height(100);

        raiz.add().expandY().row();
        raiz.add(contenedorDialogo).width(520).height(145).expandX().center().padBottom(7);
        mostrarNodoActual();
    }

    private void mostrarNodoActual() {
        NarrativeNode nodo = storyManager.getCurrentNode();
        if (nodo == null) {
            mostrarFinHistoria();
            return;
        }

        labelNombre.setVisible(false);
        retrato.setVisible(false);
        tablaOpciones.clear();
        flechaContinuar.setVisible(false);
        esperandoInput = false;
        tiempoEspera = 0;
        opcionesBotones = new Array<>();
        indiceOpcion = -1;
        opcionUnicaAuto = null;
        labelPagina.setVisible(false);

        CharacterProfile hablante = personajes.get(nodo.getSpeakerId());
        if (hablante != null) {
            labelNombre.setText(hablante.getName());
            labelNombre.setVisible(true);
            cargarRetrato(hablante);
        }

        paginasTexto = dividirEnPaginas(nodo.getText());
        paginaActual = 0;
        String textoPagina = paginasTexto.get(0);
        typewriter = new TypewriterAction(labelTexto, textoPagina, Math.max(0.8f, textoPagina.length() * 0.04f));
        labelTexto.addAction(Actions.sequence(typewriter, Actions.run(() -> {
            alCompletarPagina(nodo);
        })));

        if (nodo.getMusicTrack() != null && !nodo.getMusicTrack().equals(pistaActual)) {
            pistaActual = nodo.getMusicTrack();
            game.getMusicManager().play(MusicCommand.dialogo("music/" + pistaActual));
        }

        contenedorDialogo.addAction(Actions.sequence(
            Actions.fadeOut(0),
            Actions.fadeIn(0.15f)
        ));
    }

    private void cargarRetrato(CharacterProfile hablante) {
        if (hablante.getPortraitPath() == null || hablante.getPortraitPath().isEmpty()) {
            retrato.setVisible(false);
            return;
        }
        try {
            if (!cacheRetratos.containsKey(hablante.getPortraitPath())) {
                Texture tex = new Texture(Gdx.files.internal(hablante.getPortraitPath()));
                tex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
                cacheRetratos.put(hablante.getPortraitPath(), tex);
            }
            Texture tex = cacheRetratos.get(hablante.getPortraitPath());
            retrato.setDrawable(new TextureRegionDrawable(new TextureRegion(tex)));
            retrato.setVisible(true);
        } catch (Exception e) {
            retrato.setVisible(false);
        }
    }

    private List<String> dividirEnPaginas(String texto) {
        List<String> paginas = new ArrayList<>();
        if (texto.length() <= MAX_CHARS_POR_PAGINA) {
            paginas.add(texto);
            return paginas;
        }
        String resto = texto;
        while (resto.length() > MAX_CHARS_POR_PAGINA) {
            int corte = resto.lastIndexOf(' ', MAX_CHARS_POR_PAGINA);
            if (corte == -1) corte = MAX_CHARS_POR_PAGINA;
            paginas.add(resto.substring(0, corte).trim());
            resto = resto.substring(corte).trim();
        }
        if (!resto.isEmpty()) paginas.add(resto);
        return paginas;
    }

    private void alCompletarPagina(NarrativeNode nodo) {
        if (paginaActual < paginasTexto.size() - 1) {
            esperandoInput = true;
            actualizarIndicadorPagina();
        } else {
            mostrarOpciones(nodo);
        }
    }

    private void actualizarIndicadorPagina() {
        labelPagina.setText((paginaActual + 1) + "/" + paginasTexto.size());
        labelPagina.setVisible(true);
    }

    private void mostrarOpciones(NarrativeNode nodo) {
        if (nodo instanceof DialogNode) {
            DialogNode dn = (DialogNode) nodo;
            if (dn.getOptions() != null && !dn.getOptions().isEmpty()) {
                opcionUnicaAuto = null;
                for (DialogOption opcion : dn.getOptions()) {
                    TextButton btn = crearBotonOpcion(opcion);
                    opcionesBotones.add(btn);
                }
                boolean unicaDesbloqueada = opcionesBotones.size == 1
                    && dn.getOptions().get(0).getRequiredFlag() == null
                    || (dn.getOptions().get(0).getRequiredFlag() != null
                        && estado.hasFlag(dn.getOptions().get(0).getRequiredFlag()));
                if (unicaDesbloqueada) {
                    opcionUnicaAuto = dn.getOptions().get(0);
                    opcionesBotones.clear();
                    esperandoInput = true;
                    tablaOpciones.add(flechaContinuar).right();
                    flechaContinuar.setVisible(true);
                } else {
                    for (TextButton btn : opcionesBotones) {
                        tablaOpciones.add(btn).expandX().fillX().uniform().padRight(3);
                    }
                    indiceOpcion = 0;
                    resaltarOpcion();
                }
            } else if (dn.getNextId() != null) {
                esperandoInput = true;
                tablaOpciones.add(flechaContinuar).right();
                flechaContinuar.setVisible(true);
            } else {
                mostrarFinHistoria();
            }
        } else if (nodo instanceof TrialNode) {
            esperandoInput = true;
            tablaOpciones.add(flechaContinuar).right();
            flechaContinuar.setVisible(true);
        }
    }

    private void resaltarOpcion() {
        for (int i = 0; i < opcionesBotones.size; i++) {
            TextButton btn = opcionesBotones.get(i);
            if (i == indiceOpcion) {
                btn.getLabel().setColor(VERDE_AGUA);
            } else {
                btn.getLabel().setColor(Color.WHITE);
            }
        }
    }

    private final Color VERDE_AGUA = new Color(0x7F / 255f, 0xFF / 255f, 0xD4 / 255f, 1);

    private void seleccionarOpcion(int index) {
        if (index < 0 || index >= opcionesBotones.size) return;
        opcionesBotones.get(index).fire(new ChangeListener.ChangeEvent());
    }

    private TextButton crearBotonOpcion(DialogOption opcion) {
        boolean bloqueada = opcion.getRequiredFlag() != null && !estado.hasFlag(opcion.getRequiredFlag());
        TextButton btn;
        if (bloqueada) {
            btn = new TextButton("???", skin, "bloqueado");
            btn.getLabel().setFontScale(0.75f);
        } else {
            btn = new TextButton(opcion.getText(), skin);
            btn.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    storyManager.advance(opcion);
                    SaveSystem.saveGame(estado);
                    labelTexto.clearActions();
                    mostrarNodoActual();
                }
            });
        }
        return btn;
    }

    private void mostrarFinHistoria() {
        if (onFinish != null) {
            contenedorDialogo.addAction(Actions.sequence(
                Actions.fadeOut(0.2f),
                Actions.run(() -> {
                    onFinish.run();
                    if (fondoJuego != null) {
                        game.setScreen(fondoJuego);
                    }
                })
            ));
            return;
        }
        labelTexto.setText("--- FIN DEL PROLOGO ---");
        tablaOpciones.clear();
        TextButton btnVolver = new TextButton("VOLVER", skin);
        btnVolver.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.setScreen(new MainMenuScreen(game));
            }
        });
        tablaOpciones.add(btnVolver).padTop(12);
    }

    public void setFondo(Screen pantalla) {
        this.fondoJuego = pantalla;
    }

    public void irAPausaDirecto() {
        this.pausaDirecto = true;
    }

    public void render(float delta) {
        if (fondoJuego != null) {
            fondoJuego.render(delta);
        } else {
            Gdx.gl.glClearColor(0, 0, 0, 1);
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        }

        switch (estadoUI) {
            case DIALOGANDO:
                if (opcionesBotones.size > 0) {
                    if (Gdx.input.isKeyJustPressed(Input.Keys.UP)) {
                        indiceOpcion = (indiceOpcion - 1 + opcionesBotones.size) % opcionesBotones.size;
                        resaltarOpcion();
                    }
                    if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) {
                        indiceOpcion = (indiceOpcion + 1) % opcionesBotones.size;
                        resaltarOpcion();
                    }
                    if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) ||
                        Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
                        seleccionarOpcion(indiceOpcion);
                    }
                } else if (esperandoInput) {
                    tiempoEspera += delta;
                    flechaContinuar.setVisible((int)(tiempoEspera * 3) % 2 == 0);
                    if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) ||
                        Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
                        if (paginaActual < paginasTexto.size() - 1) {
                            paginaActual++;
                            String textoPagina = paginasTexto.get(paginaActual);
                            labelTexto.clearActions();
                            typewriter = new TypewriterAction(labelTexto, textoPagina, Math.max(0.8f, textoPagina.length() * 0.04f));
                            labelTexto.addAction(Actions.sequence(typewriter, Actions.run(() -> {
                                alCompletarPagina(storyManager.getCurrentNode());
                            })));
                        } else if (opcionUnicaAuto != null) {
                            DialogOption opt = opcionUnicaAuto;
                            opcionUnicaAuto = null;
                            storyManager.advance(opt);
                            SaveSystem.saveGame(estado);
                            labelTexto.clearActions();
                            mostrarNodoActual();
                        } else {
                            NarrativeNode nodo = storyManager.getCurrentNode();
                            if (nodo == null) {
                                mostrarFinHistoria();
                            } else if (nodo instanceof DialogNode) {
                                DialogNode dn = (DialogNode) nodo;
                                if (dn.getNextId() != null) {
                                    storyManager.advance(dn.getNextId());
                                    mostrarNodoActual();
                                }
                            } else if (nodo instanceof TrialNode) {
                                storyManager.advance(nodo.getId());
                                mostrarNodoActual();
                            }
                        }
                        esperandoInput = false;
                    }
                } else if (typewriter != null && !typewriter.estaCompletado()) {
                    if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) ||
                        Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
                        typewriter.completarInstantaneo();
                    }
                }
                if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) && !pausaAbierta) {
                    estadoUI = UIState.MENU_PAUSA;
                }
                break;
            case MENU_PAUSA:
                dibujarMenuPausa();
                break;
        }

        stage.act(delta);
        stage.draw();
    }

    private void dibujarMenuPausa() {
        if (pausaAbierta) return;
        pausaAbierta = true;

        Table panel = new Table(skin);
        panel.setBackground(skin.newDrawable("pausa-fondo"));
        panel.pad(16).padTop(24);

        Label titulo = new Label("PAUSA", skin, "dialogo-texto");

        Runnable cerrar = () -> {
            panel.remove();
            pausaAbierta = false;
            estadoUI = UIState.DIALOGANDO;
        };

        TextButton btnContinuar = new TextButton("VOLVER AL JUEGO", skin);
        btnContinuar.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (pausaDirecto && fondoJuego != null) {
                    panel.remove();
                    pausaAbierta = false;
                    game.setScreen(fondoJuego);
                } else {
                    cerrar.run();
                }
            }
        });

        TextButton btnGuardar = new TextButton("GUARDAR PARTIDA", skin);
        btnGuardar.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                SaveSystem.saveGame(estado);
                cerrar.run();
            }
        });

        TextButton btnSalir = new TextButton("SALIR AL MENU", skin);
        btnSalir.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                panel.remove();
                pausaAbierta = false;
                game.setScreen(new MainMenuScreen(game));
            }
        });

        panel.add(titulo).colspan(1).padBottom(16).row();
        panel.add(btnContinuar).fillX().padBottom(6).row();
        panel.add(btnGuardar).fillX().padBottom(6).row();
        panel.add(btnSalir).fillX();

        panel.setSize(240, 192);
        panel.setPosition(
            (640 - 240) / 2f,
            (360 - 192) / 2f
        );

        stage.addActor(panel);
        estadoUI = UIState.DIALOGANDO;
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void dispose() {
        stage.dispose();
        skin.dispose();
        for (Texture tex : cacheRetratos.values()) {
            tex.dispose();
        }
        cacheRetratos.clear();
    }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {
    }
}
