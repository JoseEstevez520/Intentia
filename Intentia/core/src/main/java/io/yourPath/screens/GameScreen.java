package io.yourPath.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;

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
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import java.util.ArrayList;
import java.util.List;

import io.yourPath.Main;
import io.yourPath.utils.TypewriterAction;
import io.yourPath.audio.SoundManager;
import io.yourPath.entities.Direction;
import io.yourPath.entities.NPC;
import io.yourPath.entities.NpcManager;
import io.yourPath.entities.Player;
import io.yourPath.entities.TriggerManager;
import io.yourPath.models.GameState;
import io.yourPath.models.NarrativeNode;
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
    private MapLayer capaColisiones;
    private MapLayer capaExits;
    private MapObject cartelInteractivo;
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
    private boolean dialogoPendiente = false;
    private boolean snapCamara = true;

    private boolean narradorActivo = false;
    private List<String> paginasNarrador;
    private int paginaNarradorActual;
    private Stage narradorStage;
    private Label labelNarrador;
    private Label labelNarradorPagina;
    private boolean debugColisiones = false;
    private ShapeRenderer debugRenderer;
    private Rectangle rectColision = new Rectangle();
    private Rectangle rectSalida = new Rectangle();

    private String mapaActualPath;

    public GameScreen(Main game) {
        this.game = game;
    }

    @Override
    public void show() {
        if (!inicializado) {
            viewport = new FitViewport(640, 360);
            camara = new OrthographicCamera();
            viewport.setCamera(camara);

            stage = new Stage(new FitViewport(640, 360));
            skin = SkinUtil.crear();

            debugRenderer = new ShapeRenderer();
            gameState = game.getStoryManager().getGameState();

            npcManager = new NpcManager();
            triggerManager = new TriggerManager();

            crearUIInteraccion();
            crearUIPausa();
            crearUINarrador();

            float sx = 100f;
            float sy = 400f;
            System.out.println(">>> SPAWNING at (" + sx + ", " + sy + ")");
            cargarMapa("TileSet/Tiled/Tilemaps/LaParadaDeLaBruma.tmx", sx, sy);

            NarrativeNode pending = game.getStoryManager().getCurrentNode();
            if (pending != null && !gameState.hasFlag("intro_played")) {
                dialogoPendiente = true;
            }

            inicializado = true;
        }
        Gdx.input.setInputProcessor(stage);
        pausaAbierta = false;
        esFondo = false;
        snapCamara = true;
    }

    private void cargarMapa(String path, float spawnX, float spawnY) {
        if (mapa != null) {
            renderer.dispose();
            mapa.dispose();
        }

        TmxMapLoader.Parameters params = new TmxMapLoader.Parameters();
        params.textureMinFilter = Texture.TextureFilter.Nearest;
        params.textureMagFilter = Texture.TextureFilter.Nearest;

        mapaActualPath = path;
        mapa = new TmxMapLoader().load(path, params);
        renderer = new OrthogonalTiledMapRenderer(mapa, 1f);

        MapLayer rawLayer = mapa.getLayers().get("Ground");
        if (rawLayer instanceof TiledMapTileLayer) {
            TiledMapTileLayer capaBase = (TiledMapTileLayer) rawLayer;
            tilesAncho = capaBase.getWidth();
            tilesAlto = capaBase.getHeight();
        } else {
            tilesAncho = 1;
            tilesAlto = 1;
        }

        capasFondo = obtenerIndicesCapas(new String[]{"Ground", "Flowers", "Road", "Trees", "RockSlopes_Auto", "Water"});

        capaColisiones = mapa.getLayers().get("Collisions");
        capaExits = mapa.getLayers().get("Exits");
        cartelInteractivo = buscarCartel();

        if (jugador != null) jugador.dispose();
        jugador = new Player(spawnX, spawnY);

        npcManager.loadFromTiledLayer(mapa, path, tilesAncho * 16f, tilesAlto * 16f);
        triggerManager.loadFromTiledLayer(mapa, path, gameState);

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
        Gdx.gl.glClearColor(0f, 0f, 0f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        if (dialogoPendiente) {
            dialogoPendiente = false;
            NarrativeNode pending = game.getStoryManager().getCurrentNode();
            if (pending != null && "narrador".equals(pending.getSpeakerId())) {
                iniciarNarrador(pending);
                gameState.addFlag("intro_played");
            } else if (pending != null) {
                pausaAbierta = true;
                DialogOverlayScreen dialog = new DialogOverlayScreen(game, null, () -> {
                    pausaAbierta = false;
                });
                dialog.setFondo(this);
                game.setScreen(dialog);
                gameState.addFlag("intro_played");
            }
            if (narradorActivo) {
                narradorStage.act(delta);
                narradorStage.getRoot().setColor(1, 1, 1, 1);
                narradorStage.draw();
            }
            return;
        }

        if (!pausaAbierta) {
            float prevX = jugador.posicion.x;
            float prevY = jugador.posicion.y;
            jugador.actualizar(delta);
            if (capaColisiones != null) {
                rectColision.set(jugador.posicion.x + 8, jugador.posicion.y, 16, 12);
                for (MapObject obj : capaColisiones.getObjects()) {
                    if (obj instanceof com.badlogic.gdx.maps.objects.RectangleMapObject) {
                        Rectangle r = ((com.badlogic.gdx.maps.objects.RectangleMapObject) obj).getRectangle();
                        if (rectColision.overlaps(r)) {
                            jugador.posicion.set(prevX, prevY);
                            break;
                        }
                    }
                }
            }
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

        int idxAbove = mapa.getLayers().getIndex("AbovePlayer");
        if (idxAbove >= 0) {
            renderer.render(new int[]{idxAbove});
        }

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

        if (!pausaAbierta) {
            triggerManager.checkExits(jugador.posicion);
        }

        if (!pausaAbierta && capaExits != null) {
            rectSalida.set(jugador.posicion.x + 8, jugador.posicion.y, 16, 12);
            for (MapObject obj : capaExits.getObjects()) {
                if (obj instanceof com.badlogic.gdx.maps.objects.RectangleMapObject) {
                    Rectangle r = ((com.badlogic.gdx.maps.objects.RectangleMapObject) obj).getRectangle();
                    if (rectSalida.overlaps(r)) {
                        String targetMap = obj.getProperties().get("targetMap", String.class);
                        float sx = 0f, sy = 0f;
                        try { sx = Float.parseFloat(obj.getProperties().get("spawnX", "0", String.class)); } catch (Exception ignored) {}
                        try { sy = Float.parseFloat(obj.getProperties().get("spawnY", "0", String.class)); } catch (Exception ignored) {}
                        if (targetMap != null && !targetMap.isEmpty()) {
                            cargarMapa(targetMap, sx, sy);
                            break;
                        }
                    }
                }
            }
        }

        manejarInteraccion();

        stage.act(delta);
        stage.draw();

        if (narradorActivo) {
            narradorStage.act(delta);
            narradorStage.draw();
            if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) ||
                Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
                avanzarNarrador();
            }
        }

        if (!esFondo && Gdx.input.isKeyJustPressed(Input.Keys.B)) {
            debugColisiones = !debugColisiones;
        }
        if (debugColisiones) {
            dibujarDebugColisiones();
        }

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
        } else if (cartelInteractivo != null) {
            float cx = cartelInteractivo.getProperties().get("x", Float.class);
            float cy = cartelInteractivo.getProperties().get("y", Float.class);
            float dist = Vector2.dst(jugador.posicion.x + 16, jugador.posicion.y + 24, cx + 22, cy + 21);
            if (dist < 50) {
                labelInteraccion.setText("[E] Leer cartel");
                labelInteraccion.pack();
                float screenX = (cx - camara.position.x) + viewport.getWorldWidth() / 2f;
                float screenY = (cy - camara.position.y) + viewport.getWorldHeight() / 2f + 30;
                labelInteraccion.setPosition(screenX - labelInteraccion.getWidth() / 2f, screenY);
                labelInteraccion.setVisible(true);
                if (labelInteraccion.getActions().size == 0) {
                    labelInteraccion.addAction(Actions.forever(
                        Actions.sequence(Actions.scaleTo(1.1f, 1.1f, 0.6f), Actions.scaleTo(1f, 1f, 0.6f))));
                }
                if (Gdx.input.isKeyJustPressed(Input.Keys.E) && !pausaAbierta) {
                    SoundManager.inst().interact();
                    pausaAbierta = true;
                    abrirDialogo("parada_info", () -> pausaAbierta = false);
                }
            } else {
                labelInteraccion.setVisible(false);
                labelInteraccion.clearActions();
                labelInteraccion.setScale(1f);
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

    private void crearUINarrador() {
        narradorStage = new Stage(new FitViewport(640, 360));

        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(0, 0, 0, 1);
        pixmap.fill();
        Texture blackTex = new Texture(pixmap);
        pixmap.dispose();
        Image blackBg = new Image(new TextureRegionDrawable(new TextureRegion(blackTex)));
        blackBg.setFillParent(true);

        Table table = new Table();
        table.setFillParent(true);
        table.center();

        labelNarrador = new Label("", skin, "dialogo-texto");
        labelNarrador.setWrap(true);
        labelNarrador.setAlignment(Align.center);
        Color verdeAgua = new Color(0x7F / 255f, 0xFF / 255f, 0xD4 / 255f, 1);
        labelNarrador.getStyle().fontColor = verdeAgua;

        labelNarradorPagina = new Label("", skin, "dialogo-texto");
        labelNarradorPagina.getStyle().fontColor = verdeAgua;
        labelNarradorPagina.setVisible(false);

        table.add(labelNarrador).width(480f).padBottom(12f).row();
        table.add(labelNarradorPagina);

        narradorStage.addActor(blackBg);
        narradorStage.addActor(table);
        narradorStage.getRoot().setVisible(false);
        paginasNarrador = new ArrayList<>();
    }

    private void iniciarNarrador(NarrativeNode nodo) {
        if (nodo == null) return;
        if (labelInteraccion != null) labelInteraccion.setVisible(false);
        narradorActivo = true;
        String texto = nodo.getText();
        paginasNarrador = dividirEnPaginasNarrador(texto);
        paginaNarradorActual = 0;
        labelNarrador.setText("");
        if (paginasNarrador.size() > 1) {
            labelNarradorPagina.setText("1/" + paginasNarrador.size());
            labelNarradorPagina.setVisible(true);
        } else {
            labelNarradorPagina.setVisible(false);
        }
        narradorStage.getRoot().setVisible(true);
        narradorStage.getRoot().setColor(1, 1, 1, 1);
        escribirPaginaNarrador(0);
    }

    private void escribirPaginaNarrador(int idx) {
        if (idx >= paginasNarrador.size()) return;
        String texto = paginasNarrador.get(idx);
        labelNarrador.setText("");
        labelNarrador.clearActions();
        TypewriterAction tw = new TypewriterAction(labelNarrador, texto);
        tw.setOnCharReveal(c -> SoundManager.inst().typewriter(c));
        labelNarrador.addAction(Actions.sequence(tw));
    }

    private void avanzarNarrador() {
        if (!narradorActivo) return;

        labelNarrador.clearActions();
        String textoActual = paginasNarrador.get(paginaNarradorActual);
        String displayed = labelNarrador.getText().toString();

        if (displayed.length() < textoActual.length()) {
            labelNarrador.setText(textoActual);
            return;
        }

        paginaNarradorActual++;
        if (paginaNarradorActual < paginasNarrador.size()) {
            if (paginasNarrador.size() > 1) {
                labelNarradorPagina.setText((paginaNarradorActual + 1) + "/" + paginasNarrador.size());
            }
            escribirPaginaNarrador(paginaNarradorActual);
        } else {
            narradorStage.getRoot().addAction(Actions.sequence(
                Actions.fadeOut(0.5f),
                Actions.run(() -> {
                    narradorActivo = false;
                    narradorStage.getRoot().setVisible(false);
                })
            ));
        }
    }

    private List<String> dividirEnPaginasNarrador(String texto) {
        List<String> paginas = new ArrayList<>();
        if (texto == null || texto.isEmpty()) {
            paginas.add("");
            return paginas;
        }
        String[] parrafos = texto.split("\n");
        StringBuilder pagina = new StringBuilder();
        for (String parrafo : parrafos) {
            String prueba = pagina.length() > 0 ? pagina + "\n" + parrafo : parrafo;
            if (prueba.length() < 300 && pagina.length() > 0) {
                pagina = new StringBuilder(prueba);
            } else if (prueba.length() >= 300 && pagina.length() > 0) {
                paginas.add(pagina.toString().trim());
                pagina = new StringBuilder(parrafo);
            } else {
                pagina = new StringBuilder(parrafo);
            }
        }
        if (pagina.length() > 0) paginas.add(pagina.toString().trim());
        return paginas;
    }

    private int[] obtenerIndicesCapas(String[] nombres) {
        int[] tmp = new int[nombres.length];
        int count = 0;
        for (String nombre : nombres) {
            int idx = mapa.getLayers().getIndex(nombre);
            if (idx >= 0) tmp[count++] = idx;
        }
        int[] indices = new int[count];
        System.arraycopy(tmp, 0, indices, 0, count);
        return indices;
    }

    private void dibujarDebugColisiones() {
        debugRenderer.setProjectionMatrix(camara.combined);
        debugRenderer.begin(ShapeType.Line);

        if (capaColisiones != null) {
            debugRenderer.setColor(Color.RED);
            for (MapObject obj : capaColisiones.getObjects()) {
                if (obj instanceof com.badlogic.gdx.maps.objects.RectangleMapObject) {
                    Rectangle r = ((com.badlogic.gdx.maps.objects.RectangleMapObject) obj).getRectangle();
                    debugRenderer.rect(r.x, r.y, r.width, r.height);
                }
            }
        }

        debugRenderer.setColor(Color.GREEN);
        float hx = jugador.posicion.x + 8;
        float hy = jugador.posicion.y;
        debugRenderer.rect(hx, hy, 16, 12);

        debugRenderer.end();
    }

    private MapObject buscarCartel() {
        MapLayer layer = mapa.getLayers().get("Object Layer 1");
        if (layer == null) return null;
        for (MapObject obj : layer.getObjects()) {
            if ("BulletinBoard_1".equals(obj.getName())) return obj;
            if (obj instanceof TiledMapTileMapObject) {
                TiledMapTile tile = ((TiledMapTileMapObject) obj).getTile();
                if (tile != null && tile.getId() == 452) return obj;
            }
        }
        return null;
    }

    private void renderObjetos(MapLayer layer) {
        for (MapObject objeto : layer.getObjects()) {
            if (objeto instanceof TiledMapTileMapObject) {
                TiledMapTileMapObject tileObj = (TiledMapTileMapObject) objeto;
                TiledMapTile tile = tileObj.getTile();
                if (tile == null) continue;
                TextureRegion region = tile.getTextureRegion();
                if (region == null) continue;
                renderer.getBatch().draw(region, tileObj.getX(), tileObj.getY());
            }
        }
    }

    private String obtenerNombreNPC(String npcId) {
        if (npcId == null) return "NPC";
        switch (npcId) {
            case "elio": return "Elio";
            case "leo": return "Leo";
            case "alba": return "Alba";
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
        if (renderer != null) renderer.dispose();
        if (mapa != null) mapa.dispose();
        if (stage != null) stage.dispose();
        if (skin != null) skin.dispose();
        if (npcManager != null) npcManager.dispose();
        if (jugador != null) jugador.dispose();
        if (debugRenderer != null) debugRenderer.dispose();
    }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() { esFondo = true; }
}
