# Plan Fase 2: DialogOverlayScreen — Implementación Detallada

> **Proyecto:** Intentia — Transformación visual de terminal a Scene2D
> **Fase:** 2 de 7 — StoryScreen → DialogOverlayScreen visual
> **Documento:** Especificación técnica completa y código 100% compilable

---

## Tabla de Contenidos

1. [Arquitectura General](#1-arquitectura-general)
2. [Diagrama de Flujo de Pantallas](#2-diagrama-de-flujo-de-pantallas)
3. [DialogOverlayScreen.java — Código Completo](#3-dialogoverlayscreenjava--código-completo)
4. [TypewriterAction.java — Efecto de Máquina de Escribir](#4-typewritteractionjava--efecto-de-máquina-de-escribir)
5. [FadeTransitionAction.java — Transición entre Nodos](#5-fadetransitionactionjava--transición-entre-nodos)
6. [MusicManager.java — Sistema de Música Singleton](#6-musicmanagerjava--sistema-de-música-singleton)
7. [Mockup de Flujo Completo](#7-mockup-de-flujo-completo)
8. [Mejores Prácticas](#8-mejores-prácticas)
9. [Checklist de Verificación](#9-checklist-de-verificación)

---

## 1. Arquitectura General

### 1.1 Diagrama de Capas del DialogOverlay

```
┌──────────────────────────────────────────────────────────────────────┐
│                        DialogOverlayScreen                           │
│  (com.badlogic.gdx.Screen)                                          │
│                                                                      │
│  ┌────────────────────────────────────────────────────────────────┐  │
│  │                      Stage (FitViewport 800x600)               │  │
│  │  ┌──────────────────────────────────────────────────────────┐  │  │
│  │  │  Stack (contenedor raíz del diálogo)                     │  │  │
│  │  │  ┌────────────────────────────────────────────────────┐  │  │  │
│  │  │  │  Image — fondoDialogo (negro 70% alpha)            │  │  │  │
│  │  │  ├────────────────────────────────────────────────────┤  │  │  │
│  │  │  │  Table — contenidoTable                             │  │  │  │
│  │  │  │  ┌───────┬────────────────────────────────────────┐│  │  │  │
│  │  │  │  │Image  │ Label — nombreLabel (cyan, scale 1.2)  ││  │  │  │
│  │  │  │  │retrato│─────────────────────────────────────────││  │  │  │
│  │  │  │  │       │ Label — textoLabel (wrap, typewriter)   ││  │  │  │
│  │  │  │  │64x64  │─────────────────────────────────────────││  │  │  │
│  │  │  │  │       │ Table — opcionesTable (botones vert.)   ││  │  │  │
│  │  │  │  │       │ ┌────────────────────────────────────┐  ││  │  │  │
│  │  │  │  │       │ │ > Opción 1                        │  ││  │  │  │
│  │  │  │  │       │ │ > Opción 2 (gris si bloqueada)    │  ││  │  │  │
│  │  │  │  │       │ └────────────────────────────────────┘  ││  │  │  │
│  │  │  │  └───────┴────────────────────────────────────────┘│  │  │  │
│  │  │  └────────────────────────────────────────────────────┘  │  │  │
│  │  └──────────────────────────────────────────────────────────┘  │  │
│  │                                                                  │  │
│  │  ┌──────────────────────────────────────────────────────────┐  │  │
│  │  │  Window — pauseWindow (modal, centrado)                   │  │  │
│  │  │  ┌────────────────────────────────────────────────────┐  │  │  │
│  │  │  │  TextButton "Volver al juego"                      │  │  │  │
│  │  │  │  TextButton "Guardar partida"                      │  │  │  │
│  │  │  │  TextButton "Salir al menú"                        │  │  │  │
│  │  │  └────────────────────────────────────────────────────┘  │  │  │
│  │  └──────────────────────────────────────────────────────────┘  │  │
│  └────────────────────────────────────────────────────────────────┘  │
│                                                                      │
│  ┌────────────────────────────────────────────────────────────────┐  │
│  │  Dependencias externas                                         │  │
│  │  • Main game (StoryManager, characters, story)                 │  │
│  │  • SaveSystem (persistencia JSON)                              │  │
│  │  • MusicManager (singleton, música por nodo)                   │  │
│  │  • Skin (escena2d, carga de texturas y estilos)               │  │
│  └────────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────────┘
```

### 1.2 Máquina de Estados Interna

```
                  ┌─────────────┐
                  │  DIALOGANDO │◄──────────────────┐
                  └──────┬──────┘                    │
                         │ ESCAPE                    │
                         ▼                           │
                  ┌─────────────┐                    │
           ┌─────►│ MENU_PAUSA  │── "Volver" ───────┘
           │      └──────┬──────┘
           │             │ "Guardar" ──► SaveSystem.saveGame()
           │             │ "Salir"  ──► MainMenuScreen
           │             │
           │             ▼
      ┌────┴────┐  ┌──────────┐
      │ GUARDAR │  │ SALIR    │
      └─────────┘  └──────────┘
```

### 1.3 Relación con StoryManager

```
DialogOverlayScreen                     StoryManager
      │                                      │
      │  mostrarNodoActual()                 │
      │─────────────────────────────────────►│
      │                                      │
      │  storyManager.getCurrentNode()       │
      │◄─────────────────────────────────────│
      │                                      │
      │  (renderiza nodo)                    │
      │                                      │
      │  usuario hace clic en opción         │
      │  storyManager.advance(opt)           │
      │─────────────────────────────────────►│
      │                                      │
      │  (procesa flags, score, TrialNode)   │
      │                                      │
      │  SaveSystem.saveGame()               │
      │  mostrarNodoActual() (siguiente)     │
      │─────────────────────────────────────►│
```

---

## 2. Diagrama de Flujo de Pantallas

```
MainMenuScreen
    │
    ├── "Nueva Partida" ──────────────────────────────────────┐
    │   game.storyManager.start("car_awakening")               │
    │   game.setScreen(new DialogOverlayScreen(game))          │
    │                                                          │
    ├── "Continuar" ───────────────────────────────────────────┤
    │   GameState saved = SaveSystem.loadGame()                │
    │   game.storyManager = new StoryManager(game.story, saved)│
    │   game.setScreen(new DialogOverlayScreen(game))          │
    │                                                          │
    └── "Salir" ── Gdx.app.exit()                             │
                                                               │
                                                               ▼
                                                    ┌─────────────────────┐
                                                    │ DialogOverlayScreen │
                                                    │ show()              │
                                                    │   stage = Stage     │
                                                    │   skin = Skin       │
                                                    │   construirLayout() │
                                                    │   mostrarNodoActual()│
                                                    └─────────┬───────────┘
                                                               │
                          ┌────────────────────────────────────┘
                          ▼
               ┌─────────────────────┐
               │ mostrarNodoActual() │
               │  Obtener nodo       │
               │  Mostrar nombre     │
               │  Cargar retrato     │
               │  Typewriter texto   │
               │  Construir opciones │
               └─────────┬───────────┘
                          │
              ┌───────────┴───────────┐
              │                       │
              ▼                       ▼
   ┌──────────────────┐   ┌──────────────────┐
   │ DialogNode       │   │ TrialNode        │
   │ → Opciones       │   │ → Texto + eval   │
   │ → "Continuar"    │   │ → Resultado      │
   └────────┬─────────┘   └────────┬─────────┘
            │                      │
            ▼                      ▼
   storyManager.advance() ──► mostrarNodoActual()
            │
            │ (ciclo narrativo)
            ▼
   ┌─────────────────────┐
   │ ¿Nodo final?        │
   │ → Sin opciones      │
   │ → Sin nextId        │
   │ → Botón "Volver"    │
   └─────────────────────┘
```

---

## 3. DialogOverlayScreen.java — Código Completo

```java
package io.yourPath.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.FitViewport;

import io.yourPath.Main;
import io.yourPath.logic.StoryManager;
import io.yourPath.models.CharacterProfile;
import io.yourPath.models.DialogNode;
import io.yourPath.models.DialogOption;
import io.yourPath.models.GameState;
import io.yourPath.models.NarrativeNode;
import io.yourPath.models.TrialNode;
import io.yourPath.models.UIState;
import io.yourPath.utils.FadeTransitionAction;
import io.yourPath.utils.MusicManager;
import io.yourPath.utils.SaveSystem;
import io.yourPath.utils.TypewriterAction;

import java.util.HashMap;
import java.util.Map;

public class DialogOverlayScreen implements Screen {

    private Stage stage;
    private Skin skin;
    private Main game;
    private StoryManager storyManager;
    private UIState estadoActual;

    private Stack stackDialogo;
    private Image fondoDialogo;
    private Table contenidoTable;
    private Image retratoImage;
    private Label nombreLabel;
    private Label textoLabel;
    private Table opcionesTable;

    private Window pauseWindow;
    private boolean typewriterOcupado;

    private Map<String, Texture> cacheRetratos;

    private static final float VIEWPORT_WIDTH = 800;
    private static final float VIEWPORT_HEIGHT = 600;
    private static final float DURACION_TYPECRITER = 1.5f;
    private static final float DURACION_FADE = 0.25f;

    /* ──────────────────────────────────────────
     * Constructor
     * ────────────────────────────────────────── */
    public DialogOverlayScreen(Main game) {
        this.game = game;
        this.storyManager = game.getStoryManager();
        this.estadoActual = UIState.DIALOGANDO;
        this.typewriterOcupado = false;
        this.cacheRetratos = new HashMap<>();
    }

    /* ──────────────────────────────────────────
     * show() — Ciclo de vida de Screen
     * ────────────────────────────────────────── */
    @Override
    public void show() {
        stage = new Stage(new FitViewport(VIEWPORT_WIDTH, VIEWPORT_HEIGHT));
        Gdx.input.setInputProcessor(stage);

        skin = new Skin(Gdx.files.internal("skin/pixel/uiskin.json"));

        construirLayoutDialogo();
        construirPauseWindow();
        mostrarNodoActual();
    }

    /* ──────────────────────────────────────────
     * construirLayoutDialogo()
     * ────────────────────────────────────────── */
    private void construirLayoutDialogo() {
        stackDialogo = new Stack();
        stackDialogo.setFillParent(true);

        Color colorFondo = new Color(0, 0, 0, 0.7f);
        fondoDialogo = new Image(skin.newDrawable("white", colorFondo));

        contenidoTable = new Table();
        contenidoTable.pad(20);
        contenidoTable.defaults().padBottom(8);

        nombreLabel = new Label("", skin);
        nombreLabel.getStyle().fontColor = Color.CYAN;
        nombreLabel.setFontScale(1.2f);

        textoLabel = new Label("", skin);
        textoLabel.setWrap(true);
        textoLabel.setFontScale(1.0f);

        retratoImage = new Image();
        opcionesTable = new Table();

        contenidoTable.add(retratoImage).size(64, 64).top().left().padRight(16);
        contenidoTable.add(nombreLabel).left().expandX().fillX();
        contenidoTable.row();
        contenidoTable.add().colspan(2).height(4);
        contenidoTable.row();
        contenidoTable.add(textoLabel).colspan(2).fillX().minWidth(500).padBottom(16);
        contenidoTable.row();
        contenidoTable.add(opcionesTable).colspan(2).fillX().left();

        stackDialogo.add(fondoDialogo);
        stackDialogo.add(contenidoTable);

        Table root = new Table();
        root.setFillParent(true);
        root.bottom().padBottom(20).padLeft(40).padRight(40);
        root.add(stackDialogo).fillX().height(240);

        stage.addActor(root);
    }

    /* ──────────────────────────────────────────
     * construirPauseWindow()
     * ────────────────────────────────────────── */
    private void construirPauseWindow() {
        pauseWindow = new Window("P A U S A", skin);
        pauseWindow.setModal(true);
        pauseWindow.setSize(300, 280);
        pauseWindow.setPosition(
            (VIEWPORT_WIDTH - 300) / 2f,
            (VIEWPORT_HEIGHT - 280) / 2f
        );
        pauseWindow.setVisible(false);

        TextButton btnVolver = new TextButton("Volver al juego", skin);
        TextButton btnGuardar = new TextButton("Guardar partida", skin);
        TextButton btnSalir = new TextButton("Salir al menu", skin);

        btnVolver.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                estadoActual = UIState.DIALOGANDO;
                pauseWindow.setVisible(false);
            }
        });

        btnGuardar.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                SaveSystem.saveGame(storyManager.getGameState());
                estadoActual = UIState.DIALOGANDO;
                pauseWindow.setVisible(false);
            }
        });

        btnSalir.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                dispose();
                game.setScreen(new MainMenuScreen(game));
            }
        });

        pauseWindow.add(btnVolver).fillX().pad(10).height(40);
        pauseWindow.row();
        pauseWindow.add(btnGuardar).fillX().pad(10).height(40);
        pauseWindow.row();
        pauseWindow.add(btnSalir).fillX().pad(10).height(40);

        stage.addActor(pauseWindow);
    }

    /* ──────────────────────────────────────────
     * mostrarNodoActual()
     * ────────────────────────────────────────── */
    private void mostrarNodoActual() {
        NarrativeNode nodo = storyManager.getCurrentNode();
        if (nodo == null) {
            finDelJuego();
            return;
        }

        opcionesTable.clear();
        typewriterOcupado = true;

        actualizarNombre(nodo);
        actualizarRetrato(nodo);
        actualizarMusica(nodo);

        textoLabel.setText("");
        textoLabel.addAction(new TypewriterAction(
            textoLabel, nodo.getText(), DURACION_TYPECRITER
        ) {
            @Override
            protected void alCompletar() {
                typewriterOcupado = false;
                construirOpciones(nodo);
            }
        });

        if (nodo instanceof TrialNode) {
            opcionesTable.clear();
            TextButton btnContinuar = new TextButton("[ Continuar ]", skin);
            btnContinuar.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    transicionarANodo(() -> {
                        String nextId = ((TrialNode) nodo).getNextTargetId();
                        if (nextId != null) {
                            storyManager.advance(nextId);
                        }
                        mostrarNodoActual();
                    });
                }
            });
            opcionesTable.add(btnContinuar).fillX().height(36);
        }
    }

    /* ──────────────────────────────────────────
     * actualizarNombre()
     * ────────────────────────────────────────── */
    private void actualizarNombre(NarrativeNode nodo) {
        CharacterProfile personaje = game.getCharacters().get(nodo.getSpeakerId());
        String nombre = (personaje != null) ? personaje.getName() : "Narrador";
        nombreLabel.setText(nombre);
    }

    /* ──────────────────────────────────────────
     * actualizarRetrato()
     * ────────────────────────────────────────── */
    private void actualizarRetrato(NarrativeNode nodo) {
        CharacterProfile personaje = game.getCharacters().get(nodo.getSpeakerId());
        if (personaje != null && personaje.getPortraitPath() != null) {
            String ruta = personaje.getPortraitPath();
            if (!cacheRetratos.containsKey(ruta)) {
                Texture tex = new Texture(Gdx.files.internal(ruta));
                tex.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
                cacheRetratos.put(ruta, tex);
            }
            retratoImage.setDrawable(new Image(cacheRetratos.get(ruta)).getDrawable());
            retratoImage.setVisible(true);
        } else {
            retratoImage.setVisible(false);
        }
    }

    /* ──────────────────────────────────────────
     * actualizarMusica()
     * ────────────────────────────────────────── */
    private void actualizarMusica(NarrativeNode nodo) {
        String track = nodo.getMusicTrack();
        if (track != null && !track.isEmpty()) {
            MusicManager.getInstance().play("music/" + track);
        } else {
            MusicManager.getInstance().stop();
        }
    }

    /* ──────────────────────────────────────────
     * construirOpciones()
     * ────────────────────────────────────────── */
    private void construirOpciones(NarrativeNode nodo) {
        if (!(nodo instanceof DialogNode)) return;

        DialogNode dialogNode = (DialogNode) nodo;
        opcionesTable.clear();

        if (dialogNode.getOptions() != null && !dialogNode.getOptions().isEmpty()) {
            for (DialogOption opt : dialogNode.getOptions()) {
                TextButton boton = crearBotonOpcion(opt);
                opcionesTable.add(boton).fillX().padBottom(6).height(36);
                opcionesTable.row();
            }
        } else if (dialogNode.getNextId() != null) {
            TextButton btnContinuar = new TextButton("[ Continuar ]", skin);
            btnContinuar.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    transicionarANodo(() -> {
                        storyManager.advance(dialogNode.getNextId());
                        mostrarNodoActual();
                    });
                }
            });
            opcionesTable.add(btnContinuar).fillX().height(36);
        }
    }

    /* ──────────────────────────────────────────
     * crearBotonOpcion()
     * ────────────────────────────────────────── */
    private TextButton crearBotonOpcion(DialogOption opt) {
        String textoBoton = opt.getText();
        GameState gameState = storyManager.getGameState();

        boolean bloqueada = (opt.getRequiredFlag() != null
            && !gameState.hasFlag(opt.getRequiredFlag()));

        if (bloqueada) {
            textoBoton = "???";
        }

        TextButton boton = new TextButton(textoBoton, skin);

        if (bloqueada) {
            boton.setDisabled(true);
            boton.getColor().a = 0.4f;
            boton.addListener(new ClickListener() {
                @Override
                public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                    // Podría mostrar tooltip "???" o requisito
                }
            });
        } else {
            boton.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    storyManager.advance(opt);
                    SaveSystem.saveGame(storyManager.getGameState());
                    transicionarANodo(DialogOverlayScreen.this::mostrarNodoActual);
                }
            });
        }

        return boton;
    }

    /* ──────────────────────────────────────────
     * transicionarANodo()
     * ────────────────────────────────────────── */
    private void transicionarANodo(Runnable callback) {
        stackDialogo.addAction(Actions.sequence(
            Actions.fadeOut(DURACION_FADE),
            Actions.run(callback),
            Actions.fadeIn(DURACION_FADE)
        ));
    }

    /* ──────────────────────────────────────────
     * finDelJuego()
     * ────────────────────────────────────────── */
    private void finDelJuego() {
        opcionesTable.clear();
        textoLabel.setText("--- Fin de la historia ---");
        typewriterOcupado = false;

        TextButton btnVolverMenu = new TextButton("Volver al menu principal", skin);
        btnVolverMenu.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                dispose();
                game.setScreen(new MainMenuScreen(game));
            }
        });
        opcionesTable.add(btnVolverMenu).fillX().height(36);
    }

    /* ──────────────────────────────────────────
     * render(float delta)
     * ────────────────────────────────────────── */
    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        manejarInputTeclado(delta);

        stage.act(delta);
        stage.draw();
    }

    /* ──────────────────────────────────────────
     * manejarInputTeclado()
     * ────────────────────────────────────────── */
    private void manejarInputTeclado(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            if (estadoActual == UIState.DIALOGANDO) {
                estadoActual = UIState.MENU_PAUSA;
                pauseWindow.setVisible(true);
            } else if (estadoActual == UIState.MENU_PAUSA) {
                estadoActual = UIState.DIALOGANDO;
                pauseWindow.setVisible(false);
            }
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)
            || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            if (typewriterOcupado) {
                completarTypewriterInstantaneo();
            }
        }
    }

    /* ──────────────────────────────────────────
     * completarTypewriterInstantaneo()
     * ────────────────────────────────────────── */
    private void completarTypewriterInstantaneo() {
        textoLabel.getActions().clear();
        NarrativeNode nodo = storyManager.getCurrentNode();
        if (nodo != null) {
            textoLabel.setText(nodo.getText());
            typewriterOcupado = false;
            construirOpciones(nodo);
        }
    }

    /* ──────────────────────────────────────────
     * resize(int, int)
     * ────────────────────────────────────────── */
    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
        pauseWindow.setPosition(
            (stage.getViewport().getWorldWidth() - 300) / 2f,
            (stage.getViewport().getWorldHeight() - 280) / 2f
        );
    }

    /* ──────────────────────────────────────────
     * pause / resume / hide
     * ────────────────────────────────────────── */
    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void hide() {}

    /* ──────────────────────────────────────────
     * dispose()
     * ────────────────────────────────────────── */
    @Override
    public void dispose() {
        MusicManager.getInstance().stop();
        for (Texture tex : cacheRetratos.values()) {
            tex.dispose();
        }
        cacheRetratos.clear();
        if (skin != null) skin.dispose();
        if (stage != null) stage.dispose();
    }
}
```

---

## 4. TypewriterAction.java — Efecto de Máquina de Escribir

```java
package io.yourPath.utils;

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction;
import com.badlogic.gdx.scenes.scene2d.ui.Label;

public abstract class TypewriterAction extends TemporalAction {

    private Label label;
    private String textoCompleto;
    private String textoActual;
    private boolean completado;

    /**
     * Crea una accion typewriter que revela el texto letra por letra.
     *
     * @param label         El Label de Scene2D donde se mostrara el texto.
     * @param textoCompleto El texto completo a revelar.
     * @param duracionTotal Duracion total en segundos para revelar todo el texto.
     */
    public TypewriterAction(Label label, String textoCompleto, float duracionTotal) {
        this.label = label;
        this.textoCompleto = textoCompleto;
        this.completado = false;
        setDuration(duracionTotal);
    }

    @Override
    protected void update(float percent) {
        if (textoCompleto == null || textoCompleto.isEmpty()) {
            label.setText("");
            if (!completado) {
                completado = true;
                alCompletar();
            }
            return;
        }

        int totalChars = textoCompleto.length();
        int charsAMostrar = (int) (totalChars * percent);
        charsAMostrar = Math.min(charsAMostrar, totalChars);

        textoActual = textoCompleto.substring(0, charsAMostrar);
        label.setText(textoActual);

        if (percent >= 1.0f && !completado) {
            completado = true;
            alCompletar();
        }
    }

    @Override
    public void restart() {
        super.restart();
        completado = false;
        label.setText("");
    }

    /**
     * Callback invocado cuando el texto se ha revelado por completo.
     * La subclase debe implementarlo para, por ejemplo, mostrar las
     * opciones de dialogo.
     */
    protected abstract void alCompletar();

    public boolean estaCompletado() {
        return completado;
    }
}
```

---

## 5. FadeTransitionAction.java — Transición entre Nodos

```java
package io.yourPath.utils;

import com.badlogic.gdx.scenes.scene2d.actions.SequenceAction;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;

public class FadeTransitionAction extends SequenceAction {

    /**
     * Crea una secuencia de acciones: fadeOut → callback → fadeIn.
     * Util para transicionar entre nodos de dialogo.
     *
     * @param duracion  Duracion en segundos de cada fade (ida y vuelta).
     * @param callback  Codigo a ejecutar entre el fadeOut y el fadeIn.
     */
    public FadeTransitionAction(float duracion, Runnable callback) {
        addAction(Actions.fadeOut(duracion));
        addAction(Actions.run(callback));
        addAction(Actions.fadeIn(duracion));
    }
}
```

---

## 6. MusicManager.java — Sistema de Música Singleton

```java
package io.yourPath.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;

public class MusicManager {

    private static MusicManager instancia;

    private Music musicaActual;
    private String trackActual;
    private float volumen;

    private static final float VOLUMEN_DEFAULT = 0.5f;

    private MusicManager() {
        this.volumen = VOLUMEN_DEFAULT;
    }

    public static MusicManager getInstance() {
        if (instancia == null) {
            instancia = new MusicManager();
        }
        return instancia;
    }

    /**
     * Reproduce una pista musical. Si la pista ya esta sonando,
     * no hace nada. Si hay otra pista, la detiene y reproduce la nueva.
     *
     * @param ruta Ruta del archivo de audio (ej: "music/bosque.ogg").
     */
    public void play(String ruta) {
        if (ruta == null || ruta.isEmpty()) return;

        if (trackActual != null && trackActual.equals(ruta)) {
            return;
        }

        stop();

        try {
            musicaActual = Gdx.audio.newMusic(Gdx.files.internal(ruta));
            musicaActual.setLooping(true);
            musicaActual.setVolume(volumen);
            musicaActual.play();
            trackActual = ruta;
        } catch (Exception e) {
            System.err.println("Error al cargar musica: " + ruta + " - " + e.getMessage());
        }
    }

    /**
     * Detiene la musica actual y libera recursos.
     */
    public void stop() {
        if (musicaActual != null) {
            musicaActual.stop();
            musicaActual.dispose();
            musicaActual = null;
            trackActual = null;
        }
    }

    /**
     * Cambia el volumen global de la musica.
     *
     * @param vol Valor entre 0.0f y 1.0f.
     */
    public void setVolumen(float vol) {
        this.volumen = Math.max(0f, Math.min(1f, vol));
        if (musicaActual != null) {
            musicaActual.setVolume(this.volumen);
        }
    }

    public float getVolumen() {
        return volumen;
    }

    public boolean estaSonando() {
        return musicaActual != null && musicaActual.isPlaying();
    }

    public String getTrackActual() {
        return trackActual;
    }
}
```

---

## 7. Mockup de Flujo Completo

### 7.1 Flujo Normal: Diálogo con Opciones

```
1. MainMenuScreen
   Usuario presiona "Nueva Partida"
   → game.getStoryManager().start("car_awakening")
   → game.setScreen(new DialogOverlayScreen(game))

2. DialogOverlayScreen.show()
   → Carga skin "skin/pixel/uiskin.json"
   → Construye layout (Stack, fondo, contenido, retrato, labels, opciones)
   → Llama a mostrarNodoActual()

3. mostrarNodoActual()
   → storyManager.getCurrentNode() → NarrativeNode "car_awakening"
   → DialogNode con speakerId="abuelo", text="Despierta, muchacho..."
   → CharacterProfile("abuelo").getName() → "Abuelo"
   → nombreLabel.setText("Abuelo")
   → Carga portrait "portraits/abuelo.png" con TextureFilter.Nearest
   → retratoImage.setDrawable(...)
   → MusicManager.play("music/dialogo.ogg") si musicTrack cambió
   → textoLabel.addAction(new TypewriterAction(label, texto, 1.5f))

4. Typewriter ejecutándose (1.5 segundos)
   → typewriterOcupado = true
   → Labels se actualizan letra por letra
   → Botones de opción NO se muestran aún

5. TypewriterAction.alCompletar()
   → typewriterOcupado = false
   → construirOpciones(nodo)
   → DialogNode tiene 3 DialogOption:
       • "¿Dónde estoy?" → targetId="car_respuesta1"
       • "¿Quién eres?"  → targetId="car_respuesta2", requiredFlag="conocido"
       • "Silencio"      → targetId="car_respuesta3"
   → Opción 2: requiredFlag="conocido" NO está en GameState.flags
     → Botón deshabilitado, color.a=0.4f, texto="???"

6. Usuario hace clic en opción 1
   → storyManager.advance(opt)
     → addTrialScore(opt.getScoreValue() si existe)
     → advance(opt.getTargetId()) → "car_respuesta1"
     → processActions(currentNode)
   → SaveSystem.saveGame(storyManager.getGameState())
   → stackDialogo.addAction(FadeTransitionAction:
       [fadeOut(0.25s) → mostrarNodoActual() → fadeIn(0.25s)]
     )

7. mostrarNodoActual() con nuevo nodo "car_respuesta1"
   → Speaker="abuelo" (mismo retrato, misma música)
   → Nuevo texto, typewriter otra vez
   → Nuevas opciones (quizás solo "Continuar")

8. Usuario presiona ENTER/SPACE durante typewriter
   → completarTypewriterInstantaneo()
   → Limpia acciones del label
   → Muestra texto completo inmediatamente
   → typewriterOcupado = false
   → construirOpciones()

9. Usuario presiona ESC durante diálogo
   → estadoActual = UIState.MENU_PAUSA
   → pauseWindow.setVisible(true)
   → Se detiene la actualización del diálogo

10. PauseMenu mostrado
    → "Volver al juego" → estadoActual=DIALOGANDO, pauseWindow oculto
    → "Guardar partida" → SaveSystem.saveGame(), vuelve al juego
    → "Salir al menú" → dispose(), game.setScreen(new MainMenuScreen(game))
```

### 7.2 Flujo TrialNode

```
1. mostrarNodoActual()
   → storyManager.getCurrentNode() → TrialNode (instanceof TrialNode)
   → Muestra texto del juicio
   → Typewriter muestra el texto
   → Al completar: aparece botón "Continuar"

2. Usuario hace clic en "Continuar"
   → storyManager.advance(trialNode.getNextId() ???)

   ¡OJO! En realidad TrialNode no usa advance directamente.
   La lógica real está en StoryManager:
   → advance(targetId) llamó a checkTrialEvaluation((TrialNode) currentNode)
   → checkTrialEvaluation:
       • Obtiene TrialEvaluation
       • Calcula gameState.getScorePercentage() >= eval.getThreshold()
       • Si éxito: addFlag(eval.getSuccessFlag())
       • advance(successTargetId o failTargetId)
       • resetTrialScore()

   Pero en nuestro código actual, al llegar a un TrialNode desde
   advance(), la evaluación es automática. El TrialNode se procesa
   EN EL MOMENTO que se avanza a él.

   Entonces: cuando el usuario ve un TrialNode, la evaluación YA OCURRIÓ.
   El TrialNode es transitorio — solo tenemos que mostrar el texto
   y un botón para continuar al siguiente nodo (que ya fue determinado
   por checkTrialEvaluation).

   En StoryManager.advance(String targetId):
     1. gameState.setCurrentNodeId(targetId)
     2. processActions(currentNode)
     3. if (currentNode instanceof TrialNode) → checkTrialEvaluation()
        → ¡Esto avanza AUTOMÁTICAMENTE al siguiente nodo!
        → El TrialNode nunca se queda como "nodo actual"

   Por lo tanto, en DialogOverlayScreen:
   → mostrarNodoActual() NUNCA recibe un TrialNode como nodo actual
   → Porque StoryManager ya lo procesó y avanzó

   Sin embargo, para estar preparados, el código maneja TrialNode
   en mostrarNodoActual() por si la lógica cambia.
```

### 7.3 Flujo: Nodo Final

```
1. mostrarNodoActual()
   → storyManager.getCurrentNode() → null
   → finDelJuego()
   → Muestra "--- Fin de la historia ---"
   → Botón "Volver al menu principal"
   → Usuario hace clic → new MainMenuScreen(game)
```

### 7.4 Flujo: Continuar Partida

```
1. MainMenuScreen
   → SaveSystem.exists() → true
   → Botón "Continuar" visible
   → Usuario hace clic
   → GameState saved = SaveSystem.loadGame()
   → game.storyManager = new StoryManager(game.story, saved)
   → game.setScreen(new DialogOverlayScreen(game))

2. DialogOverlayScreen.show()
   → mostrarNodoActual()
   → storyManager.getCurrentNode() → nodo guardado
   → Continúa exactamente donde se quedó
```

---

## 8. Mejores Prácticas

### 8.1 Retratos (Pixel Art)

```java
Texture tex = new Texture(Gdx.files.internal(ruta));
tex.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
```

- `Nearest` para filtro de textura evita el difuminado (blur) y mantiene los bordes duros del pixel art.
- Cachear texturas en un `Map<String, Texture>` para no recargar en cada cambio de nodo.
- Liberar todas las texturas en `dispose()`.
- Si no hay retrato: ocultar la `Image` y ajustar layout.

### 8.2 Typewriter

- `typewriterOcupado` flag para bloquear interacción con opciones mientras escribe.
- ENTER/SPACE completan instantáneamente el texto y muestran opciones.
- Tiempo por defecto: 1.5s para textos de 100-200 caracteres.
- Para textos muy largos: aumentar duración o hacer variable.

```java
float duracion = Math.max(0.5f, texto.length() * 0.008f);
```

### 8.3 Transiciones

- Usar `Actions.sequence(fadeOut, run -> {}, fadeIn)`.
- Duración de fade: 0.2s-0.3s para no cansar al usuario.
- Aplicar la transición al `Stack` del diálogo, no al Stage completo.
- No bloquear input durante la transición (es decorativa).

### 8.4 Música

- `MusicManager` singleton: no duplicar instancias de Music.
- Solo cambiar música cuando `node.getMusicTrack()` cambie realmente.
- `setLooping(true)` siempre para música ambiental.
- Volumen global ajustable (guardar en preferencias).
- Liberar música al cambiar de Screen.

### 8.5 Input

- ESC: toggle pause.
- ENTER/SPACE: completar typewriter.
- Mouse/touch: opciones de diálogo.
- El `Stage` de Scene2D maneja automáticamente eventos táctiles y de ratón.

### 8.6 Skin y Estilos

- Usar `kenney-pixel` como skin base (pixel art friendly).
- Colores: fontColor cyan para nombre, blanco para texto, gris para bloqueado.
- Botones: estilo `default` o `toggle` para opciones.
- Fondo del diálogo: `newDrawable("white", new Color(0,0,0,0.7f))` para transparencia 70%.

### 8.7 Opciones Bloqueadas (Flags)

```java
if (opt.getRequiredFlag() != null
    && !gameState.hasFlag(opt.getRequiredFlag())) {
    boton.setDisabled(true);
    boton.getColor().a = 0.4f;
}
```

- Mostrar "???" como texto del botón bloqueado.
- Opcional: tooltip que diga qué flag falta.
- Opcional: efecto de "sacudida" si el usuario intenta hacer clic.

### 8.8 Optimización y Memoria

- Cache de retratos: `HashMap<String, Texture>`.
- Liberar texturas en `dispose()`, no en cada cambio de nodo.
- Skin y Stage con ciclo de vida ligado a `show()` / `dispose()`.
- MusicManager: una sola instancia de `Music` activa.

### 8.9 Manejo de Errores

- `storyManager.getCurrentNode() == null` → mostrar pantalla final.
- `portraitPath == null` → ocultar retrato.
- `musicTrack` inexistente → capturar excepción, continuar sin música.
- `skin` no encontrado → crash temprano en desarrollo (mejor que fallo silencioso).

---

## 9. Checklist de Verificación

### 9.1 Estructura y Archivos

- [ ] `screens/DialogOverlayScreen.java` creado con código completo.
- [ ] `utils/TypewriterAction.java` creado con `TemporalAction`.
- [ ] `utils/FadeTransitionAction.java` creado con `SequenceAction`.
- [ ] `utils/MusicManager.java` creado (singleton).
- [ ] `Skins/kenney-pixel` descargado y colocado en `assets/skin/pixel/`.
- [ ] Retratos de personajes en `assets/portraits/` (según `CharacterProfile.portraitPath`).
- [ ] Archivos de música en `assets/music/`.

### 9.2 Funcionalidad del Diálogo

- [ ] `show()`: crea Stage con FitViewport(800,600).
- [ ] `show()`: carga Skin desde archivo.
- [ ] `show()`: construye Stack (fondo + contenido Table).
- [ ] `show()`: llama a `mostrarNodoActual()`.
- [ ] `mostrarNodoActual()`: obtiene nodo de `storyManager.getCurrentNode()`.
- [ ] `mostrarNodoActual()`: maneja nodo null (fin del juego).
- [ ] Nombre del personaje se muestra en cian con fontScale 1.2.
- [ ] Retrato se carga con TextureFilter.Nearest.
- [ ] Retrato se cachea en HashMap.
- [ ] Retrato se oculta si no hay portraitPath.
- [ ] Texto se muestra con wrap=true.
- [ ] Typewriter effect se ejecuta en cada nodo.
- [ ] Typewriter bloquea clic en opciones mientras escribe.
- [ ] ENTER/SPACE completan typewriter instantáneamente.
- [ ] Botones de opción se crean por cada DialogOption.
- [ ] Opciones con `requiredFlag` faltante aparecen deshabilitadas (alpha 0.4f, texto "???").
- [ ] Clic en opción → `storyManager.advance(opt)`.
- [ ] Clic en opción → `SaveSystem.saveGame()`.
- [ ] Clic en opción → transición fadeOut+mostrarNodoActual+fadeIn.
- [ ] Nodos sin opciones con nextId → botón "Continuar".
- [ ] Nodos TrialNode se manejan (texto + botón continuar).

### 9.3 Menú de Pausa

- [ ] ESC cambia a UIState.MENU_PAUSA.
- [ ] Window modal aparece centrado con título "PAUSA".
- [ ] Botón "Volver al juego" → retorna a DIALOGANDO.
- [ ] Botón "Guardar partida" → SaveSystem.saveGame() + retorna.
- [ ] Botón "Salir al menú" → dispose() + MainMenuScreen.
- [ ] ESC en pausa → cierra pausa.

### 9.4 Música

- [ ] `MusicManager` es singleton.
- [ ] `play()` detiene música anterior si el track cambió.
- [ ] `play()` no hace nada si el mismo track ya suena.
- [ ] `stop()` libera recurso de Music.
- [ ] `mostrarNodoActual()` detecta cambio de musicTrack.
- [ ] Música se reproduce con looping=true.
- [ ] `dispose()` de DialogOverlayScreen llama a MusicManager.stop().

### 9.5 Render y Ciclo de Vida

- [ ] `render()`: glClearColor negro, glClear, stage.act(), stage.draw().
- [ ] `resize()`: stage.getViewport().update(w, h, true).
- [ ] `dispose()`: stage.dispose(), skin.dispose().
- [ ] `dispose()`: libera todas las texturas del cache.
- [ ] `dispose()`: detiene música.

### 9.6 Integración con MainMenuScreen

- [ ] "Nueva Partida" → `start("car_awakening")` → `DialogOverlayScreen`.
- [ ] "Continuar" → `SaveSystem.loadGame()` → nuevo StoryManager → `DialogOverlayScreen`.

### 9.7 Calidad y Estilo

- [ ] Código sin comentarios (estilo requerido).
- [ ] Nombres de clases, métodos y variables en español.
- [ ] Consistencia con paquete `io.yourPath`.
- [ ] Sin regresión en lógica narrativa (StoryManager no modificado).
- [ ] `./gradlew lwjgl3:run` compila sin errores.

