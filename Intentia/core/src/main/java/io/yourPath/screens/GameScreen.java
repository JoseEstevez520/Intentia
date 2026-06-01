package io.yourPath.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;

import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.viewport.FitViewport;
import io.yourPath.Main;
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
    private int tilesAncho;
    private int tilesAlto;

    private int[] capasFondo;
    private int[] capasFrontales;

    private NPC npcInteractivo;
    private Label labelInteraccion;
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

            capasFondo = obtenerIndicesCapas(new String[]{"Ground", "Road", "Water", "Flowers", "RockSlopes_Auto"});
            capasFrontales = obtenerIndicesCapas(new String[]{"Object Layer 1"});

            jugador = new Player(10 * 16, 20 * 16);

            gameState = game.getStoryManager().getGameState();

            npcManager = new NpcManager();
            npcManager.loadFromTiledLayer(mapa, "TileSet/Tiled/Tilemaps/Beginning Fields.tmx");

            triggerManager = new TriggerManager();
            triggerManager.loadFromTiledLayer(mapa, "Beginning Fields", gameState);

            stage = new Stage(new FitViewport(640, 360));
            skin = SkinUtil.crear();

            crearUIInteraccion();
            inicializado = true;
        }
        Gdx.input.setInputProcessor(stage);
        pausaAbierta = false;
        snapCamara = true;
    }

    private void crearUIInteraccion() {
        labelInteraccion = new Label("", skin, "interaccion");
        labelInteraccion.setVisible(true);
        labelInteraccion.setPosition(280, 30);
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
        npcManager.renderSortedWithPlayer(renderer.getBatch(), jugador);
        renderer.getBatch().end();

        renderer.render(capasFrontales);

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

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) && !pausaAbierta) {
            pausaAbierta = true;
            DialogOverlayScreen dialog = new DialogOverlayScreen(game);
            dialog.setFondo(this);
            dialog.irAPausaDirecto();
            game.setScreen(dialog);
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
            labelInteraccion.setPosition(
                viewport.getWorldWidth() / 2f - labelInteraccion.getWidth() / 2f,
                30
            );
            labelInteraccion.setVisible(true);

            if (Gdx.input.isKeyJustPressed(Input.Keys.E) && !pausaAbierta) {
                NPC npcParaDialogo = npcInteractivo;
                npcParaDialogo.setTalking(true);
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
            labelInteraccion.setText("");
            labelInteraccion.setVisible(false);
        }
    }

    private int[] obtenerIndicesCapas(String[] nombres) {
        int[] indices = new int[nombres.length];
        for (int i = 0; i < nombres.length; i++) {
            indices[i] = mapa.getLayers().getIndex(nombres[i]);
        }
        return indices;
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
    @Override public void hide() {}
}
