package io.yourPath.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.objects.TiledMapTileMapObject;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.FitViewport;

import io.yourPath.Main;
import io.yourPath.audio.SoundManager;
import io.yourPath.entities.Direction;
import io.yourPath.entities.NPC;
import io.yourPath.entities.NpcManager;
import io.yourPath.entities.Player;
import io.yourPath.entities.TriggerManager;
import io.yourPath.models.GameState;
import io.yourPath.models.Trigger;
import io.yourPath.utils.SkinUtil;

public class GameScreen implements Screen {
    private Main game;
    private Stage stage;
    private Skin skin;
    private OrthographicCamera camara;
    private FitViewport viewport;
    private TiledMap mapa;
    private OrthogonalTiledMapRenderer renderer;
    private Player jugador;
    private NpcManager npcManager;
    private TriggerManager triggerManager;
    private GameState gameState;
    private boolean pausaAbierta = false;
    private boolean esFondo = false;
    private int tilesAncho;
    private int tilesAlto;

    private int[] capasFondo;

    private NPC npcInteractivo;
    private Label labelInteraccion;
    private Table pausaPanel;
    private boolean inicializado;
    private boolean snapCamara = true;

    public GameScreen(Main game) {
        this.game = game;
    }

    @Override
    public void show() {
        if (!inicializado) {
            viewport = new FitViewport(640, 360);
            camara = new OrthographicCamera();
            viewport.setCamera(camara);

            TmxMapLoader.Parameters params = new TmxMapLoader.Parameters();
            params.textureMinFilter = Texture.TextureFilter.Nearest;
            params.textureMagFilter = Texture.TextureFilter.Nearest;

            mapa = new TmxMapLoader().load("TileSet/Tiled/Tilemaps/Beginning Fields.tmx", params);
            renderer = new OrthogonalTiledMapRenderer(mapa, 1f);

            TiledMapTileLayer capaBase = (TiledMapTileLayer) mapa.getLayers().get("Ground");
            tilesAncho = capaBase.getWidth();
            tilesAlto = capaBase.getHeight();

            capasFondo = obtenerIndicesCapas(new String[]{"Ground", "Flowers", "Road", "RockSlopes_Auto", "Water"});

            jugador = new Player(10 * 16, 20 * 16);

            gameState = game.getStoryManager().getGameState();

            npcManager = new NpcManager();
            npcManager.loadFromTiledLayer(mapa, "TileSet/Tiled/Tilemaps/Beginning Fields.tmx");

            triggerManager = new TriggerManager();
            triggerManager.loadFromTiledLayer(mapa, "Beginning Fields", gameState);

            stage = new Stage(new FitViewport(640, 360));
            skin = SkinUtil.crear();

            crearUIInteraccion();
            crearUIPausa();
            inicializado = true;
        }
        Gdx.input.setInputProcessor(stage);
        pausaAbierta = false;
        esFondo = false;
        snapCamara = true;
    }

    private void crearUIInteraccion() {
        labelInteraccion = new Label("", skin, "interaccion");
        labelInteraccion.setVisible(false);
        labelInteraccion.setOrigin(com.badlogic.gdx.utils.Align.center);
        stage.addActor(labelInteraccion);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.15f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        if (!pausaAbierta) {
            jugador.actualizar(delta);
        }

        npcManager.update(delta, jugador.posicion);

        float mitadAncho = viewport.getWorldWidth() / 2f;
        float mitadAlto = viewport.getWorldHeight() / 2f;
        float mapaAncho = tilesAncho * 16;
        float mapaAlto = tilesAlto * 16;

        float destinoX = jugador.posicion.x + 8;
        float destinoY = jugador.posicion.y + 16;

        if (mapaAncho > viewport.getWorldWidth()) {
            destinoX = MathUtils.clamp(destinoX, mitadAncho, mapaAncho - mitadAncho);
        } else {
            destinoX = mapaAncho / 2f;
        }
        if (mapaAlto > viewport.getWorldHeight()) {
            destinoY = MathUtils.clamp(destinoY, mitadAlto, mapaAlto - mitadAlto);
        } else {
            destinoY = mapaAlto / 2f;
        }

        if (snapCamara) {
            camara.position.set(destinoX, destinoY, 0);
            snapCamara = false;
        } else {
            camara.position.lerp(new Vector3(destinoX, destinoY, 0), 0.08f);
        }
        camara.update();

        renderer.setView(camara);

        renderer.render(capasFondo);

        renderer.getBatch().begin();
        MapLayer objetosLayer = mapa.getLayers().get("Object Layer 1");
        if (objetosLayer != null) {
            renderObjetos(objetosLayer);
        }
        npcManager.renderSortedWithPlayer(renderer.getBatch(), jugador);
        renderer.getBatch().end();

        if (!pausaAbierta) {
            Trigger triggerActivado = triggerManager.checkTriggers(jugador.posicion, gameState);
            if (triggerActivado != null) {
                switch (triggerActivado.getType()) {
                    case DIALOG:
                        pausaAbierta = true;
                        abrirDialogo(triggerActivado.getNodeId(), () -> {
                            pausaAbierta = false;
                        });
                        break;
                    case SET_FLAG:
                        break;
                }
            }
        }

        triggerManager.checkExits(jugador.posicion);

        manejarInteraccion();

        stage.act(delta);
        stage.draw();

        if (!esFondo && Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            if (pausaPanel.isVisible()) {
                cerrarPausa();
            } else {
                abrirPausa();
            }
        }
    }

    private void abrirDialogo(String nodeId, Runnable onFinish) {
        DialogOverlayScreen dialog = new DialogOverlayScreen(game, nodeId, onFinish);
        dialog.setFondo(this);
        game.setScreen(dialog);
    }

    private void manejarInteraccion() {
        npcInteractivo = npcManager.getInteractiveNPC(jugador.posicion);

        if (npcInteractivo != null) {
            labelInteraccion.setText("[E] " + obtenerNombreNPC(npcInteractivo.getNpcId()));
            labelInteraccion.pack();

            float npcCentroX = npcInteractivo.getX() + 16;
            float npcCabezaY = npcInteractivo.getY() + 54;
            float screenX = (npcCentroX - camara.position.x) + viewport.getWorldWidth() / 2f;
            float screenY = (npcCabezaY - camara.position.y) + viewport.getWorldHeight() / 2f;
            labelInteraccion.setPosition(
                screenX - labelInteraccion.getWidth() / 2f,
                screenY);
            labelInteraccion.setVisible(true);

            if (labelInteraccion.getActions().size == 0) {
                labelInteraccion.addAction(Actions.forever(
                    Actions.sequence(
                        Actions.scaleTo(1.1f, 1.1f, 0.6f),
                        Actions.scaleTo(1f, 1f, 0.6f)
                    )
                ));
            }

            if (Gdx.input.isKeyJustPressed(Input.Keys.E) && !pausaAbierta) {
                SoundManager.inst().interact();
                NPC npcParaDialogo = npcInteractivo;
                npcParaDialogo.setTalking(true);
                npcParaDialogo.setDirection(direccionHaciaJugador(npcParaDialogo));
                pausaAbierta = true;

                String resolvedNodeId = npcParaDialogo.getDialogNodeId();
                if (game.getDialogRouter() != null) {
                    resolvedNodeId = game.getDialogRouter().resolve(
                        npcParaDialogo.getNpcId(),
                        resolvedNodeId
                    );
                }
                abrirDialogo(resolvedNodeId, () -> {
                    npcParaDialogo.setTalking(false);
                    pausaAbierta = false;
                });
            }
        } else {
            labelInteraccion.setVisible(false);
            labelInteraccion.clearActions();
            labelInteraccion.setScale(1f);
        }
    }

    private Direction direccionHaciaJugador(NPC npc) {
        float dx = jugador.posicion.x - npc.getX();
        float dy = jugador.posicion.y - npc.getY();
        if (Math.abs(dx) > Math.abs(dy)) {
            return dx > 0 ? Direction.DERECHA : Direction.IZQUIERDA;
        } else {
            return dy > 0 ? Direction.ARRIBA : Direction.ABAJO;
        }
    }

    private void crearUIPausa() {
        pausaPanel = new Table(skin);
        pausaPanel.setBackground(skin.newDrawable("pausa-fondo"));
        pausaPanel.pad(12);

        Label titulo = new Label("PAUSA", skin, "nombre");

        TextButton btnContinuar = new TextButton("VOLVER AL JUEGO", skin);
        btnContinuar.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                cerrarPausa();
            }
        });

        TextButton btnGuardar = new TextButton("GUARDAR PARTIDA", skin);
        btnGuardar.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                io.yourPath.utils.SaveSystem.saveGame(gameState);
                cerrarPausa();
            }
        });

        TextButton btnSalir = new TextButton("SALIR AL MENU", skin);
        btnSalir.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                pausaPanel.setVisible(false);
                pausaAbierta = false;
                game.transitarA(new MainMenuScreen(game), TransitionConfig.fadeToBlack());
            }
        });

        pausaPanel.add(titulo).colspan(2).center().padBottom(12).row();
        pausaPanel.add(btnContinuar).fillX().padBottom(4).colspan(2).row();
        pausaPanel.add(btnGuardar).fillX().padBottom(4).colspan(2).row();
        pausaPanel.add(btnSalir).fillX().colspan(2);

        pausaPanel.pack();
        pausaPanel.setPosition(
            (640 - pausaPanel.getWidth()) / 2f,
            (360 - pausaPanel.getHeight()) / 2f);
        pausaPanel.setVisible(false);
        stage.addActor(pausaPanel);
    }

    private void abrirPausa() {
        pausaAbierta = true;
        pausaPanel.setVisible(true);
    }

    private void cerrarPausa() {
        pausaAbierta = false;
        pausaPanel.setVisible(false);
    }

    private int[] obtenerIndicesCapas(String[] nombres) {
        int[] indices = new int[nombres.length];
        for (int i = 0; i < nombres.length; i++) {
            indices[i] = mapa.getLayers().getIndex(nombres[i]);
        }
        return indices;
    }

    private void renderObjetos(MapLayer layer) {
        for (MapObject objeto : layer.getObjects()) {
            if (objeto instanceof TiledMapTileMapObject) {
                TiledMapTileMapObject tileObj = (TiledMapTileMapObject) objeto;
                TiledMapTile tile = tileObj.getTile();
                if (tile != null) {
                    TextureRegion region = tile.getTextureRegion();
                    if (region != null) {
                        float x = tileObj.getX();
                        float y = tileObj.getY();
                        renderer.getBatch().draw(region, x, y);
                    }
                }
            }
        }
    }

    private String obtenerNombreNPC(String npcId) {
        if (npcId == null) return "NPC";
        switch (npcId) {
            case "abuelo": return "Abuelo Elías";
            case "padre": return "Javier (Padre)";
            case "madre": return "Marta (Madre)";
            default: return npcId;
        }
    }

    @Override
    public void resize(int ancho, int alto) {
        viewport.update(ancho, alto, true);
        stage.getViewport().update(ancho, alto, true);
    }

    @Override
    public void dispose() {
        renderer.dispose();
        mapa.dispose();
        stage.dispose();
        skin.dispose();
        npcManager.dispose();
        jugador.dispose();
    }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() { esFondo = true; }
}
