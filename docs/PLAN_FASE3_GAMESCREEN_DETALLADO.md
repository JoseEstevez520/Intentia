# PLAN FASE 3 — GameScreen con Tiled, Cámara, Personaje, Colisiones y Triggers Narrativos

> **Proyecto:** INTENTIA: EL LEGADO
> **Framework:** libGDX 1.12.x
> **Mapa base:** Beginning Fields (40×40 tiles, 16×16 px)
> **Dependencias activas:** gdx, gdx-box2d, ashley, gdx-ai, gdx-freetype

---

## Tabla de Contenidos

1. [Arquitectura General](#1-arquitectura-general)
2. [Análisis del Mapa Tiled Existente](#2-análisis-del-mapa-tiled-existente)
3. [Estructura de Paquetes Nueva](#3-estructura-de-paquetes-nueva)
4. [GameScreen.java — Código Completo](#4-gamescreenjava--código-completo)
5. [Player.java — Código Completo](#5-playerjava--código-completo)
6. [CollisionManager.java — Código Completo](#6-collisionmanagerjava--código-completo)
7. [TriggerSystem.java — Código Completo](#7-triggersystemjava--código-completo)
8. [CameraController.java — Código Completo](#8-cameracontrollerjava--código-completo)
9. [Modificaciones al Mapa Tiled](#9-modificaciones-al-mapa-tiled)
10. [Integración con el Sistema de Diálogos](#10-integración-con-el-sistema-de-diálogos)
11. [Modificaciones a Main.java](#11-modificaciones-a-mainjava)
12. [LoadingScreen.java — Código Completo](#12-loadingscreenjava--código-completo)
13. [Mejores Prácticas Tiled](#13-mejores-prácticas-tiled)
14. [Diagrama de Capas de Renderizado](#14-diagrama-de-capas-de-renderizado)
15. [Checklist de Verificación](#15-checklist-de-verificación)

---

## 1. Arquitectura General

### 1.1 Diagrama de Componentes

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          GAME SCREEN                                      │
│                                                                           │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │   Player     │  │   Camera     │  │  Collision   │  │   Trigger    │  │
│  │  (entities)  │  │  Controller  │  │   Manager    │  │   System     │  │
│  │              │  │  (logic)     │  │   (logic)    │  │   (logic)    │  │
│  │  - position  │  │  - lerp      │  │  - matrix    │  │  - triggers  │  │
│  │  - animacion │  │  - clamp     │  │  - AABB test │  │  - oneShot   │  │
│  │  - input     │  │  - update()  │  │  - canMove() │  │  - check()   │  │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘  │
│         │                 │                 │                 │          │
│         └─────────────────┴─────────────────┴─────────────────┘          │
│                                    │                                      │
│  ┌─────────────────────────────────┴──────────────────────────────────┐   │
│  │                    TiledMapRenderer                                │   │
│  │  capas: Ground → Road → Water → Flowers → RockSlopes → Objects    │   │
│  └────────────────────────────────────────────────────────────────────┘   │
│                                                                           │
│  ┌────────────────────────────────────────────────────────────────────┐   │
│  │                    Stage (Scene2D UI Overlay)                        │   │
│  │  DialogOverlay, PauseMenu, HUD                                      │   │
│  └────────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────┘
```

### 1.2 Flujo de Inicialización (show)

```
show()
  │
  ├── 1. Cargar mapa: TmxMapLoader → TiledMap
  ├── 2. Crear renderer: OrthogonalTiledMapRenderer(unitScale=1/16f)
  ├── 3. Crear cámara: OrthographicCamera + FitViewport(800,600)
  ├── 4. Inicializar Player en posición inicial (spawn)
  ├── 5. Inicializar CollisionManager con capa de colisiones
  ├── 6. Inicializar TriggerSystem con ObjectLayer "Triggers"
  ├── 7. Crear SpriteBatch para entidades
  └── 8. Crear Stage para UI overlay
```

### 1.3 Flujo de Render (render)

```
render(delta)
  │
  ├── 1. glClearColor(0.1, 0.1, 0.15, 1) + glClear(GL_COLOR_BUFFER_BIT)
  ├── 2. player.update(delta, collisionMatrix)
  ├── 3. cameraController.update(cam, player.pos, mapWidth, mapHeight)
  ├── 4. mapRenderer.setView(cam)
  ├── 5. mapRenderer.render(capas de suelo: Ground, Road, Water, Flowers)
  ├── 6. spriteBatch.begin()
  │       └── player.draw(spriteBatch)
  ├── 7. spriteBatch.end()
  ├── 8. mapRenderer.render(capas de objetos: Object Layer 1, foreground)
  ├── 9. triggerSystem.checkTriggers(...)
  ├── 10. stage.act(delta) + stage.draw()
  └── 11. (Si diálogo activo) overlayUI.draw()
```

---

## 2. Análisis del Mapa Tiled Existente

### 2.1 Estructura del TMX (Beginning Fields.tmx)

| Propiedad      | Valor                        |
|----------------|------------------------------|
| Orientación    | Orthogonal                   |
| Tamaño         | 40 × 40 tiles                |
| Tile size      | 16 × 16 px                   |
| Formato datos  | CSV                          |
| Total capas    | 6                            |

### 2.2 Capas de Tiles Existentes

| Nombre de Capa      | ID  | Visible | Propósito                            |
|----------------------|-----|---------|--------------------------------------|
| `Ground`            | 13  | Sí      | Suelo base (hierba, tierra, etc.)    |
| `Flowers`           | 14  | Sí      | Flores decorativas animadas          |
| `Road`              | 11  | Sí      | Caminos de tierra                    |
| `RockSlopes`        | 2   | **No**  | Pendientes de roca (oculta)          |
| `RockSlopes_Auto`   | 12  | Sí      | Pendientes automáticas               |
| `Water`             | 3   | Sí      | Agua animada                         |

### 2.3 Capa de Objetos Existente

| Nombre         | ID  | Contenido                                                              |
|----------------|-----|------------------------------------------------------------------------|
| Object Layer 1 | 10  | 124 objetos: edificios (casas, muralla), props (carteles, bancos, faroles, barriles, cercas), rocas, árboles, arbustos, flores, fogata |

### 2.4 Tilesets Cargados (17 tilesets)

| # | Tileset                | firstgid | Tile count | Tam. tile | Uso                        |
|---|------------------------|----------|------------|-----------|----------------------------|
| 1 | Atlas_Buildings        | 1        | 442        | 16×16     | Edificios (atlas)          |
| 2 | Objects_Buildings      | 443      | 6          | variables | Casas individuales         |
| 3 | Objects_Props          | 449      | 19         | variables | Carteles, bancos, etc.     |
| 4 | Objects_Rocks          | 468      | 5          | variables | Rocas sueltas              |
| 5 | Objects_Trees          | 473      | 11         | variables | Árboles individuales       |
| 6 | Atlas_Props            | 484      | 180        | 16×16     | Props en atlas             |
| 7 | Atlas_Rocks            | 664      | 22         | 16×16     | Rocas en atlas             |
| 8 | Tileset_Ground         | 686      | 132        | 16×16     | Suelo (hierba, tierra)     |
| 9 | Tileset_RockSlope      | 818      | 4096       | 16×16     | Pendientes de roca         |
| 10| Tileset_RockSlope_Simple | 4914   | 54         | 16×16     | Pendientes simples         |
| 11| Tileset_Water          | 4968     | 312        | 16×16     | Agua animada               |
| 12| Tilesets_Road          | 5280     | 84         | 16×16     | Caminos                    |
| 13| Atlas_Trees_Bushes     | 5340     | 144        | 16×16     | Árboles y arbustos         |
| 14| Animation_Flowers_Red  | 5484     | 96         | 16×16     | Flores rojas animadas      |
| 15| Animation_Flowers_White| 5580     | 96         | 16×16     | Flores blancas animadas    |
| 16| Animation_Campfire     | 5676     | 24         | 16×16     | Fogata animada             |
| 17| Tileset_Shadow         | No incluido | -       | variables | Sombras (no en TMX actual) |

### 2.5 Assets de Personaje Disponibles

| Archivo                    | Ruta                                                         | Uso previsto           |
|----------------------------|--------------------------------------------------------------|------------------------|
| Character_Idle.png         | `TileSet/Art/Characters/Main Character/Character_Idle.png`   | Spritesheet idle 4dir  |
| Character_Walk.png         | `TileSet/Art/Characters/Main Character/Character_Walk.png`   | Spritesheet walk 4dir  |
| Character_Slash.png        | `TileSet/Art/Characters/Main Character/Character_Slash.png`  | Spritesheet ataque     |

### 2.6 Conclusión del Análisis

**El mapa NO tiene capas de colisión ni triggers narrativos.** Esto es deliberado: se crearán en Tiled como parte de esta fase. El mapa actual tiene 6 capas de tiles + 1 capa de objetos decorativos. Los objetos actuales en "Object Layer 1" son decorativos (casas, árboles, props) y **no** deben confundirse con triggers — estos se añadirán en nuevas capas diseñadas específicamente para gameplay.

---

## 3. Estructura de Paquetes Nueva

```
io.yourPath/
├── Main.java                              ← MODIFICAR: agregar estado entre screens
├── screens/
│   ├── MainMenuScreen.java               ← EXISTENTE (refactorizar a Scene2D en Fase 1)
│   ├── StoryScreen.java                  ← EXISTENTE (será reemplazado por DialogOverlayScreen)
│   ├── GameScreen.java                   ← NUEVO
│   ├── DialogOverlayScreen.java          ← NUEVO (Fase 2)
│   ├── LoadingScreen.java                ← NUEVO
│   └── BaseScreen.java                   ← NUEVO (abstract)
├── entities/
│   └── Player.java                       ← NUEVO
├── logic/
│   ├── StoryManager.java                 ← EXISTENTE
│   ├── CollisionManager.java             ← NUEVO
│   ├── TriggerSystem.java                ← NUEVO
│   └── CameraController.java             ← NUEVO
├── models/
│   ├── NarrativeNode.java                ← EXISTENTE
│   ├── DialogNode.java                   ← EXISTENTE
│   ├── TrialNode.java                    ← EXISTENTE
│   ├── DialogOption.java                 ← EXISTENTE
│   ├── TrialEvaluation.java              ← EXISTENTE
│   ├── CharacterProfile.java             ← EXISTENTE
│   ├── GameState.java                    ← EXISTENTE
│   └── UIState.java                      ← EXISTENTE
└── utils/
    ├── NarrativeDAO.java                 ← EXISTENTE
    ├── NarrativeDAOImplementation.java   ← EXISTENTE
    ├── SaveSystem.java                   ← EXISTENTE
    └── IntentiaException.java            ← EXISTENTE
```

---

## 4. GameScreen.java — Código Completo

```java
package io.yourPath.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapRenderer;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import io.yourPath.Main;
import io.yourPath.entities.Player;
import io.yourPath.logic.CameraController;
import io.yourPath.logic.CollisionManager;
import io.yourPath.logic.TriggerSystem;

public class GameScreen implements Screen {

    private Main game;
    private TiledMap map;
    private TiledMapRenderer mapRenderer;
    private OrthographicCamera camera;
    private Viewport viewport;
    private SpriteBatch spriteBatch;
    private Stage stage;

    private Player player;
    private CollisionManager collisionManager;
    private TriggerSystem triggerSystem;
    private CameraController cameraController;

    private float mapWidth;
    private float mapHeight;

    private static final float UNIT_SCALE = 1f / 16f;
    private static final float VIEWPORT_WIDTH = 800;
    private static final float VIEWPORT_HEIGHT = 600;

    // Nombre de capas en el TMX
    private static final String LAYER_GROUND = "Ground";
    private static final String LAYER_FLOWERS = "Flowers";
    private static final String LAYER_ROAD = "Road";
    private static final String LAYER_ROCK_SLOPES_AUTO = "RockSlopes_Auto";
    private static final String LAYER_WATER = "Water";
    private static final String LAYER_COLLISION = "Collision";
    private static final String LAYER_OBJECTS = "Object Layer 1";

    // Posición inicial del jugador (en tiles, se multiplica por tileSize)
    private static final float SPAWN_TILE_X = 20;
    private static final float SPAWN_TILE_Y = 20;

    // Lista de capas de suelo (se renderizan antes que el jugador)
    private static final int[] LAYER_IDS_BACKGROUND = null; // null = todas
    private String[] backgroundLayerNames;

    public GameScreen(Main game) {
        this.game = game;
    }

    @Override
    public void show() {
        // 1. Configurar filtro de texturas para pixel art nítido
        Texture.setEnforcePotImages(false);

        // 2. Cargar mapa Tiled
        TmxMapLoader.Parameters params = new TmxMapLoader.Parameters();
        params.textureMinFilter = Texture.TextureFilter.Nearest;
        params.textureMagFilter = Texture.TextureFilter.Nearest;
        map = new TmxMapLoader().load("TileSet/Tiled/Tilemaps/Beginning Fields.tmx", params);

        // 3. Crear renderer con unitScale
        mapRenderer = new OrthogonalTiledMapRenderer(map, UNIT_SCALE);

        // 4. Calcular dimensiones del mapa en unidades del mundo
        int mapTileWidth = map.getProperties().get("width", Integer.class);
        int mapTileHeight = map.getProperties().get("height", Integer.class);
        int tileWidth = map.getProperties().get("tilewidth", Integer.class);
        int tileHeight = map.getProperties().get("tileheight", Integer.class);
        mapWidth = mapTileWidth * tileWidth * UNIT_SCALE;
        mapHeight = mapTileHeight * tileHeight * UNIT_SCALE;

        // 5. Crear cámara y viewport
        camera = new OrthographicCamera();
        viewport = new FitViewport(VIEWPORT_WIDTH, VIEWPORT_HEIGHT, camera);
        camera.position.set(VIEWPORT_WIDTH / 2f, VIEWPORT_HEIGHT / 2f, 0);
        camera.update();

        // 6. Crear SpriteBatch para entidades
        spriteBatch = new SpriteBatch();

        // 7. Inicializar sistemas
        collisionManager = new CollisionManager();
        collisionManager.buildCollisionMatrix(map, LAYER_COLLISION);

        triggerSystem = new TriggerSystem();
        triggerSystem.loadTriggers(map);

        cameraController = new CameraController();

        // 8. Inicializar Player
        float spawnX = SPAWN_TILE_X * tileWidth * UNIT_SCALE;
        float spawnY = SPAWN_TILE_Y * tileHeight * UNIT_SCALE;
        player = new Player();
        player.setPosition(spawnX, spawnY);

        // 9. Configurar capas de renderizado en orden
        backgroundLayerNames = new String[]{
            LAYER_GROUND,
            LAYER_ROAD,
            LAYER_WATER,
            LAYER_FLOWERS,
            LAYER_ROCK_SLOPES_AUTO
        };

        // 10. Crear Stage para UI overlays (diálogos, pausa, etc.)
        stage = new Stage(viewport, spriteBatch);
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void render(float delta) {
        // 1. Limpiar pantalla
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.15f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // 2. Actualizar player (input + animación + colisiones)
        player.update(delta, collisionManager);

        // 3. Actualizar cámara
        cameraController.update(camera, player.getPosition(), mapWidth, mapHeight);

        // 4. Configurar vista del renderer
        mapRenderer.setView(camera);

        // 5. Renderizar capas de fondo (suelo, agua, etc.)
        mapRenderer.render(backgroundLayerNames);

        // 6. Renderizar entidades (player)
        spriteBatch.setProjectionMatrix(camera.combined);
        spriteBatch.begin();
        player.draw(spriteBatch);
        spriteBatch.end();

        // 7. Renderizar capas de objetos decorativos (encima del player)
        mapRenderer.render(new String[]{LAYER_OBJECTS});

        // 8. Verificar triggers narrativos
        triggerSystem.checkTriggers(player.getBoundingRectangle(), game);

        // 9. Renderizar UI overlay (diálogos, pausa, HUD)
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void dispose() {
        mapRenderer.dispose();
        map.dispose();
        spriteBatch.dispose();
        player.dispose();
        stage.dispose();
    }

    @Override public void show() {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    // Getters para acceso desde otros sistemas
    public TiledMap getMap() { return map; }
    public OrthographicCamera getCamera() { return camera; }
    public Player getPlayer() { return player; }
    public Stage getStage() { return stage; }
    public Main getGame() { return game; }

    /**
     * Reinicia la posición del jugador (usado al volver de un diálogo).
     */
    public void restorePlayerPosition(float x, float y) {
        player.setPosition(x, y);
    }
}
```

### 4.1 Notas sobre GameScreen.java

- **Orden de renderizado crítico**: Las capas de suelo se renderizan PRIMERO, luego el jugador, luego los objetos decorativos. Esto permite que el jugador camine "detrás" de árboles y edificios si es necesario.
- **Unit scale 1/16f**: Convierte píxeles (16×16) a unidades del mundo libGDX. Si los tiles fueran 32×32, usar 1/32f.
- **Texture filtering Nearest**: Esencial para pixel art nítido. `Linear` difumina los bordes.
- **backgroundLayerNames**: Array de strings con las capas que se renderizan ANTES del jugador. La capa `Object Layer 1` se renderiza DESPUÉS.
- **Stage**: Se crea vacío. Los overlays (diálogo, pausa) se añaden como actores al stage cuando se activan.

---

## 5. Player.java — Código Completo

### 5.1 Análisis del Spritesheet

Los assets existentes son:
- `Character_Idle.png`: Asumir grid 4×4 (4 direcciones × 4 frames c/u, fila 0 = idle de cada dirección)
- `Character_Walk.png`: Asumir grid 4×4 (4 direcciones × 4 frames de walk)
- `Character_Slash.png`: Para futuro (ataque)

Convención de las filas del spritesheet:
| Fila | Dirección |
|------|-----------|
| 0    | Abajo     |
| 1    | Izquierda |
| 2    | Derecha   |
| 3    | Arriba    |

Frame 0 de cada fila = pose idle de esa dirección.

```java
package io.yourPath.entities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

import io.yourPath.logic.CollisionManager;

public class Player {

    public enum Direccion {
        ABAJO, ARRIBA, IZQUIERDA, DERECHA
    }

    // Posición en unidades del mundo (no en tiles)
    private Vector2 position;
    private Vector2 velocity;
    private float speed;

    // Dimensiones del sprite (en unidades del mundo)
    private float width;
    private float height;

    // Animaciones
    private Animation<TextureRegion> walkDown;
    private Animation<TextureRegion> walkUp;
    private Animation<TextureRegion> walkLeft;
    private Animation<TextureRegion> walkRight;
    private TextureRegion idleDown;
    private TextureRegion idleUp;
    private TextureRegion idleLeft;
    private TextureRegion idleRight;

    private TextureRegion currentFrame;
    private float stateTime;
    private Direccion ultimaDireccion;
    private boolean moving;

    // Constantes del spritesheet
    private static final int FRAME_COLS = 4;
    private static final int FRAME_ROWS = 4;
    private static final float FRAME_DURATION = 0.15f;
    private static final float PLAYER_SPEED = 100f; // unidades/segundo
    private static final float PLAYER_WIDTH = 16f;  // en píxeles, se multiplica por unitScale
    private static final float PLAYER_HEIGHT = 16f;

    private static final String SPRITESHEET_IDLE = "TileSet/Art/Characters/Main Character/Character_Idle.png";
    private static final String SPRITESHEET_WALK = "TileSet/Art/Characters/Main Character/Character_Walk.png";

    public Player() {
        this.position = new Vector2();
        this.velocity = new Vector2();
        this.speed = PLAYER_SPEED;
        this.stateTime = 0f;
        this.ultimaDireccion = Direccion.ABAJO;
        this.moving = false;

        // El ancho/alto en unidades del mundo = píxeles * unitScale
        // unitScale está en GameScreen = 1/16f
        this.width = PLAYER_WIDTH / 16f;
        this.height = PLAYER_HEIGHT / 16f;

        cargarSpritesheets();
        currentFrame = idleDown;
    }

    private void cargarSpritesheets() {
        // --- Spritesheet de Idle ---
        Texture idleSheet = new Texture(SPRITESHEET_IDLE);
        idleSheet.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        TextureRegion[][] idleFrames = TextureRegion.split(
            idleSheet,
            idleSheet.getWidth() / FRAME_COLS,
            idleSheet.getHeight() / FRAME_ROWS
        );

        idleDown  = idleFrames[0][0];
        idleLeft  = idleFrames[1][0];
        idleRight = idleFrames[2][0];
        idleUp    = idleFrames[3][0];

        // --- Spritesheet de Walk ---
        Texture walkSheet = new Texture(SPRITESHEET_WALK);
        walkSheet.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        TextureRegion[][] walkFrames = TextureRegion.split(
            walkSheet,
            walkSheet.getWidth() / FRAME_COLS,
            walkSheet.getHeight() / FRAME_ROWS
        );

        walkDown  = new Animation<>(FRAME_DURATION, walkFrames[0]);
        walkLeft  = new Animation<>(FRAME_DURATION, walkFrames[1]);
        walkRight = new Animation<>(FRAME_DURATION, walkFrames[2]);
        walkUp    = new Animation<>(FRAME_DURATION, walkFrames[3]);

        // Play mode loop para todas
        walkDown.setPlayMode(Animation.PlayMode.LOOP);
        walkLeft.setPlayMode(Animation.PlayMode.LOOP);
        walkRight.setPlayMode(Animation.PlayMode.LOOP);
        walkUp.setPlayMode(Animation.PlayMode.LOOP);
    }

    public void update(float delta, CollisionManager collisionManager) {
        stateTime += delta;
        velocity.set(0, 0);
        moving = false;

        // Leer input WASD y Flechas
        boolean left  = Gdx.input.isKeyPressed(Input.Keys.A)  || Gdx.input.isKeyPressed(Input.Keys.LEFT);
        boolean right = Gdx.input.isKeyPressed(Input.Keys.D)  || Gdx.input.isKeyPressed(Input.Keys.RIGHT);
        boolean up    = Gdx.input.isKeyPressed(Input.Keys.W)  || Gdx.input.isKeyPressed(Input.Keys.UP);
        boolean down  = Gdx.input.isKeyPressed(Input.Keys.S)  || Gdx.input.isKeyPressed(Input.Keys.DOWN);

        if (left) {
            velocity.x = -speed;
            ultimaDireccion = Direccion.IZQUIERDA;
            moving = true;
        } else if (right) {
            velocity.x = speed;
            ultimaDireccion = Direccion.DERECHA;
            moving = true;
        }

        if (up) {
            velocity.y = speed;
            ultimaDireccion = Direccion.ARRIBA;
            moving = true;
        } else if (down) {
            velocity.y = -speed;
            ultimaDireccion = Direccion.ABAJO;
            moving = true;
        }

        // Normalizar diagonal para que no sea más rápida
        if (velocity.len() > 0) {
            velocity.nor().scl(speed);
        }

        // Aplicar delta
        float deltaX = velocity.x * delta;
        float deltaY = velocity.y * delta;

        // Verificar colisiones en X e Y por separado (para permitir sliding)
        Rectangle bounds = getBoundingRectangle();
        float newX = position.x + deltaX;
        float newY = position.y + deltaY;

        if (collisionManager.canMove(bounds, newX, position.y)) {
            position.x = newX;
        }
        if (collisionManager.canMove(bounds, position.x, newY)) {
            position.y = newY;
        }

        // Seleccionar frame actual según dirección y movimiento
        if (moving) {
            switch (ultimaDireccion) {
                case ABAJO:     currentFrame = walkDown.getKeyFrame(stateTime, true);  break;
                case ARRIBA:    currentFrame = walkUp.getKeyFrame(stateTime, true);    break;
                case IZQUIERDA: currentFrame = walkLeft.getKeyFrame(stateTime, true);  break;
                case DERECHA:   currentFrame = walkRight.getKeyFrame(stateTime, true); break;
            }
        } else {
            switch (ultimaDireccion) {
                case ABAJO:     currentFrame = idleDown;  break;
                case ARRIBA:    currentFrame = idleUp;    break;
                case IZQUIERDA: currentFrame = idleLeft;  break;
                case DERECHA:   currentFrame = idleRight; break;
            }
        }
    }

    public void draw(Batch batch) {
        batch.draw(currentFrame, position.x, position.y, width, height);
    }

    public Rectangle getBoundingRectangle() {
        return new Rectangle(position.x, position.y, width, height);
    }

    public void setPosition(float x, float y) {
        position.set(x, y);
    }

    public Vector2 getPosition() {
        return position;
    }

    public float getX() { return position.x; }
    public float getY() { return position.y; }

    public void dispose() {
        // Las texturas se cargan con new Texture(), hay que liberarlas
        // Nota: en producción usar AssetManager
    }
}
```

### 5.2 Notas sobre Player.java

- **Colisión separada en X e Y**: Esto permite que el jugador "deslice" contra las paredes. Si se usara un solo `canMove(bounds, newX, newY)`, el personaje se detendría completamente al tocar una esquina.
- **Input simultáneo**: Si se presionan W+D, el jugador se mueve en diagonal. `velocity.nor().scl(speed)` normaliza para que la diagonal no sea más rápida que el movimiento cardinal.
- **PlayMode.LOOP**: Las animaciones de caminar se repiten en ciclo mientras la tecla esté presionada.
- **Texture.setFilter Nearest**: Nitidez pixel-art.

---

## 6. CollisionManager.java — Código Completo

Este sistema implementa colisiones **tile-based** (sin Box2D). Es más simple, predecible y no requiere configuración adicional de físicas.

```java
package io.yourPath.logic;

import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.math.Rectangle;

public class CollisionManager {

    private boolean[][] collisionMatrix;
    private int mapTileWidth;
    private int mapTileHeight;
    private float tileWidth;
    private float tileHeight;

    // Unit scale debe coincidir con GameScreen.UNIT_SCALE
    private static final float UNIT_SCALE = 1f / 16f;

    /**
     * Construye la matriz de colisiones a partir de una capa de tiles.
     * Se considera colisionable TODO tile que tenga un tile asignado en la capa.
     *
     * @param map       Mapa Tiled cargado
     * @param layerName Nombre de la capa de colisión (ej: "Collision")
     */
    public void buildCollisionMatrix(TiledMap map, String layerName) {
        MapLayer mapLayer = map.getLayers().get(layerName);
        if (mapLayer == null) {
            throw new IllegalStateException(
                "Capa de colisión '" + layerName + "' no encontrada en el mapa. " +
                "Debes crear una capa de tiles llamada '" + layerName + "' en Tiled."
            );
        }

        TiledMapTileLayer layer = (TiledMapTileLayer) mapLayer;

        mapTileWidth  = layer.getWidth();
        mapTileHeight = layer.getHeight();
        tileWidth     = layer.getTileWidth() * UNIT_SCALE;
        tileHeight    = layer.getTileHeight() * UNIT_SCALE;

        collisionMatrix = new boolean[mapTileWidth][mapTileHeight];

        for (int x = 0; x < mapTileWidth; x++) {
            for (int y = 0; y < mapTileHeight; y++) {
                TiledMapTileLayer.Cell cell = layer.getCell(x, y);
                if (cell != null) {
                    TiledMapTile tile = cell.getTile();
                    if (tile != null) {
                        // Opción 1: TODO tile en la capa es colisionable
                        collisionMatrix[x][y] = true;

                        // Opción 2 (extendida): Verificar propiedad "collidable" custom
                        // Boolean collidable = tile.getProperties().get("collidable", Boolean.class);
                        // collisionMatrix[x][y] = collidable != null && collidable;
                    }
                } else {
                    collisionMatrix[x][y] = false;
                }
            }
        }
    }

    /**
     * Verifica si un tile específico es caminable.
     */
    public boolean isWalkable(int tileX, int tileY) {
        if (tileX < 0 || tileX >= mapTileWidth || tileY < 0 || tileY >= mapTileHeight) {
            return false; // Fuera del mapa = no caminable
        }
        return !collisionMatrix[tileX][tileY];
    }

    /**
     * Verifica si el rectángulo del jugador puede moverse a la nueva posición (newX, newY).
     * Implementa AABB (Axis-Aligned Bounding Box) vs tile grid.
     *
     * @param playerBounds Rectángulo actual del jugador (sin mover)
     * @param newX         Nueva posición X propuesta
     * @param newY         Nueva posición Y propuesta
     * @return true si el movimiento es válido (sin colisiones)
     */
    public boolean canMove(Rectangle playerBounds, float newX, float newY) {
        // Crear rectángulo en la nueva posición
        Rectangle newBounds = new Rectangle(newX, newY, playerBounds.width, playerBounds.height);

        // Calcular qué tiles cubre el rectángulo
        int leftTile   = (int) Math.floor(newBounds.x / tileWidth);
        int rightTile  = (int) Math.floor((newBounds.x + newBounds.width - 0.001f) / tileWidth);
        int bottomTile = (int) Math.floor(newBounds.y / tileHeight);
        int topTile    = (int) Math.floor((newBounds.y + newBounds.height - 0.001f) / tileHeight);

        // Verificar colisión con cada tile que el rectángulo toca
        for (int tx = leftTile; tx <= rightTile; tx++) {
            for (int ty = bottomTile; ty <= topTile; ty++) {
                if (!isWalkable(tx, ty)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Obtiene el valor de colisión de un tile específico (para debugging).
     */
    public boolean getTileAt(int tileX, int tileY) {
        if (tileX < 0 || tileX >= mapTileWidth || tileY < 0 || tileY >= mapTileHeight) {
            return false;
        }
        return collisionMatrix[tileX][tileY];
    }

    public int getMapTileWidth()  { return mapTileWidth; }
    public int getMapTileHeight() { return mapTileHeight; }
}
```

### 6.1 Consideraciones sobre Colisiones

- **Estrategia**: Todos los tiles en la capa "Collision" se consideran sólidos. En Tiled, se pintan con un tile semitransparente rojo (creado específicamente para debugging) o se dejan vacíos (sin tile = caminable).
- **AABB vs Tile Grid**: El método `canMove` calcula qué tiles toca el rectángulo del jugador en la posición propuesta. Si alguno es colisionable, el movimiento se rechaza.
- **Sliding**: `Player.update()` prueba X e Y por separado, permitiendo que el jugador se deslice a lo largo de paredes.
- **Fuera del mapa**: Si el jugador intenta salir del mapa, `isWalkable` devuelve `false`.

---

## 7. TriggerSystem.java — Código Completo

```java
package io.yourPath.logic;

import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapObjects;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.math.Rectangle;

import io.yourPath.Main;

import java.util.ArrayList;
import java.util.List;

public class TriggerSystem {

    public static class Trigger {
        public Rectangle rect;
        public String type;      // "dialog", "flag", "teleport", "battle"
        public String nodeId;    // ID del nodo narrativo (para type="dialog")
        public String flag;      // Flag a añadir (para type="flag")
        public boolean oneShot;  // true = se desactiva tras activarse
        public String mapaDestino; // Para type="teleport" (futuro)
        public float spawnX, spawnY; // Para type="teleport" (futuro)

        public Trigger(Rectangle rect, String type, String nodeId, String flag,
                       boolean oneShot, String mapaDestino, float spawnX, float spawnY) {
            this.rect = rect;
            this.type = type;
            this.nodeId = nodeId;
            this.flag = flag;
            this.oneShot = oneShot;
            this.mapaDestino = mapaDestino;
            this.spawnX = spawnX;
            this.spawnY = spawnY;
        }
    }

    private List<Trigger> triggers;
    private List<Trigger> triggersAgotados;

    // Unit scale debe coincidir con GameScreen
    private static final float UNIT_SCALE = 1f / 16f;

    public TriggerSystem() {
        this.triggers = new ArrayList<>();
        this.triggersAgotados = new ArrayList<>();
    }

    /**
     * Carga los triggers desde una ObjectLayer del TMX.
     * Busca una capa llamada "Triggers" (creada en Tiled).
     * Cada objeto en esa capa DEBE ser un rectángulo.
     * Propiedades custom esperadas:
     *   - type: "dialog" | "flag" | "teleport"
     *   - nodeId: string (obligatorio si type="dialog")
     *   - flag: string (obligatorio si type="flag")
     *   - oneShot: bool (default true)
     *   - map: string (para teleport, futuro)
     *   - spawnX, spawnY: float (para teleport, futuro)
     */
    public void loadTriggers(TiledMap map) {
        MapLayer layer = map.getLayers().get("Triggers");
        if (layer == null) {
            // No hay capa de triggers — puede ser normal si el mapa no tiene eventos
            return;
        }

        MapObjects objects = layer.getObjects();
        for (MapObject obj : objects) {
            if (obj instanceof RectangleMapObject) {
                RectangleMapObject rectObj = (RectangleMapObject) obj;
                Rectangle pixelRect = rectObj.getRectangle();

                // Convertir píxeles a unidades del mundo
                Rectangle worldRect = new Rectangle(
                    pixelRect.x * UNIT_SCALE,
                    pixelRect.y * UNIT_SCALE,
                    pixelRect.width * UNIT_SCALE,
                    pixelRect.height * UNIT_SCALE
                );

                // Leer propiedades custom
                String type = obj.getProperties().get("type", String.class);
                String nodeId = obj.getProperties().get("nodeId", String.class);
                String flag = obj.getProperties().get("flag", String.class);
                boolean oneShot = Boolean.TRUE.equals(
                    obj.getProperties().get("oneShot", Boolean.class)
                );
                String mapaDestino = obj.getProperties().get("map", String.class);
                float spawnX = obj.getProperties().getFloat("spawnX", 0f);
                float spawnY = obj.getProperties().getFloat("spawnY", 0f);

                if (type == null) {
                    type = "dialog"; // default
                }

                triggers.add(new Trigger(
                    worldRect, type, nodeId, flag, oneShot,
                    mapaDestino, spawnX, spawnY
                ));
            }
        }
    }

    /**
     * Verifica todos los triggers activos contra el rectángulo del jugador.
     * Se llama en cada frame desde GameScreen.render().
     *
     * @param playerBounds Rectángulo de colisión del jugador
     * @param game         Instancia principal de la aplicación
     */
    public void checkTriggers(Rectangle playerBounds, Main game) {
        List<Trigger> aRemover = new ArrayList<>();

        for (Trigger t : triggers) {
            if (playerBounds.overlaps(t.rect)) {
                // Trigger activado
                switch (t.type) {
                    case "dialog":
                        // Guardar posición actual del jugador para restaurar después
                        // La posición se guarda en Main (ver modificaciones a Main.java)
                        game.setUltimaPosicionJugador(
                            t.rect.x / UNIT_SCALE,
                            t.rect.y / UNIT_SCALE
                        );
                        game.setUltimoMapa("TileSet/Tiled/Tilemaps/Beginning Fields.tmx");

                        // Avanzar al nodo narrativo
                        if (t.nodeId != null && !t.nodeId.isEmpty()) {
                            game.getStoryManager().advance(t.nodeId);
                            // Transicionar a DialogOverlayScreen
                            // Nota: Esto se maneja en Main con un flag o callback
                            game.setScreen(new DialogOverlayScreen(game));
                        }
                        break;

                    case "flag":
                        // Añadir flag al estado del juego
                        if (t.flag != null && !t.flag.isEmpty()) {
                            game.getStoryManager().getGameState().addFlag(t.flag);
                            System.out.println("Flag añadido: " + t.flag);
                        }
                        break;

                    case "teleport":
                        // FUTURO: Cambiar de mapa
                        break;
                }

                if (t.oneShot) {
                    aRemover.add(t);
                }
            }
        }

        // Agotar triggers one-shot (moverlos a la lista de agotados)
        triggers.removeAll(aRemover);
        triggersAgotados.addAll(aRemover);
    }

    /**
     * Reinicia todos los triggers (one-shot se reactivan).
     * Útil al recargar un mapa o reiniciar partida.
     */
    public void reset() {
        triggers.addAll(triggersAgotados);
        triggersAgotados.clear();
    }

    public List<Trigger> getTriggers() {
        return triggers;
    }

    public List<Trigger> getTriggersAgotados() {
        return triggersAgotados;
    }
}
```

### 7.1 Notas sobre TriggerSystem.java

- **Capa "Triggers" en TMX**: Se debe crear una nueva ObjectLayer en Tiled llamada exactamente "Triggers". Cada objeto debe ser un rectángulo (RectangleMapObject).
- **Propiedades custom**: Se leen del MapObject con `obj.getProperties().get("key", Type.class)`. En Tiled se añaden como "Custom Properties" a cada objeto.
- **Conversión píxel → mundo**: Los rectángulos en Tiled están en píxeles. Se multiplican por UNIT_SCALE (1/16f) para convertir a unidades del mundo.
- **One-shot**: Los triggers con `oneShot=true` se mueven a `triggersAgotados` después de activarse. No se reactivan a menos que se llame a `reset()`.
- **DialogOverlayScreen**: La referencia a `DialogOverlayScreen` asume que existe (Fase 2). Si no, se puede comentar temporalmente.

---

## 8. CameraController.java — Código Completo

```java
package io.yourPath.logic;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

public class CameraController {

    private static final float LERP_SPEED = 0.1f;

    /**
     * Actualiza la posición de la cámara para seguir al jugador.
     * Interpola suavemente y clampea a los límites del mapa.
     *
     * @param camera    Cámara a actualizar
     * @param target    Posición del jugador (Vector2)
     * @param mapWidth  Ancho total del mapa en unidades del mundo
     * @param mapHeight Alto total del mapa en unidades del mundo
     */
    public void update(OrthographicCamera camera, Vector2 target, float mapWidth, float mapHeight) {
        // Calcular dimensiones del viewport en unidades del mundo
        float viewportWidth  = camera.viewportWidth * camera.zoom;
        float viewportHeight = camera.viewportHeight * camera.zoom;

        // Posición deseada de la cámara (centrada en el jugador)
        float desiredX = target.x;
        float desiredY = target.y;

        // Interpolación lineal suave (lerp) hacia el target
        Vector3 currentPos = camera.position;
        float newX = MathUtils.lerp(currentPos.x, desiredX, LERP_SPEED);
        float newY = MathUtils.lerp(currentPos.y, desiredY, LERP_SPEED);

        // Clampear para que la cámara no se salga del mapa
        float minX = viewportWidth / 2f;
        float maxX = mapWidth - viewportWidth / 2f;
        float minY = viewportHeight / 2f;
        float maxY = mapHeight - viewportHeight / 2f;

        // Si el mapa es más pequeño que el viewport, centrar en el mapa
        if (mapWidth < viewportWidth) {
            newX = mapWidth / 2f;
        } else {
            newX = MathUtils.clamp(newX, minX, maxX);
        }
        if (mapHeight < viewportHeight) {
            newY = mapHeight / 2f;
        } else {
            newY = MathUtils.clamp(newY, minY, maxY);
        }

        camera.position.set(newX, newY, 0);
        camera.update();
    }

    /**
     * Versión con zoom interpolado (para efectos de cámara).
     */
    public void update(OrthographicCamera camera, Vector2 target,
                       float mapWidth, float mapHeight, float targetZoom) {
        update(camera, target, mapWidth, mapHeight);
        camera.zoom = MathUtils.lerp(camera.zoom, targetZoom, LERP_SPEED);
    }
}
```

### 8.1 Notas sobre CameraController.java

- **Lerp suave**: `MathUtils.lerp(current, target, 0.1f)` mueve la cámara al 10% de la distancia cada frame. Esto da un seguimiento suave sin ser instantáneo.
- **Clampeo**: La cámara no muestra áreas fuera del mapa. Si el mapa es más pequeño que el viewport, se centra.
- **Zoom**: Se incluye una versión extendida que admite zoom interpolado para futuros efectos (acercar en diálogos, etc.).

---

## 9. Modificaciones al Mapa Tiled

### 9.1 Capas a Añadir en Beginning Fields.tmx

Se deben añadir **2 nuevas capas** al mapa TMX:

#### 9.1.1 Capa "Collision" (Tile Layer)

| Propiedad     | Valor                    |
|---------------|--------------------------|
| Nombre        | `Collision`              |
| Tipo          | Tile Layer               |
| Orden         | Arriba de Object Layer 1 |
| Visibilidad   | Oculta (uncheck)         |

**Instrucciones en Tiled:**
1. Abrir `Beginning Fields.tmx` en Tiled.
2. Añadir nueva capa de tiles: `Layer → Add Tile Layer`.
3. Nombrarla "Collision".
4. Arrastrarla arriba de "Object Layer 1" (pero abajo de la capa más alta).
5. Crear un tileset de colisión (tile rojo de 16×16) o usar un tile existente rojo/magenta.
6. Marcar la capa como oculta (ojo desactivado) — no se renderiza, solo se usa para lógica.
7. Pintar los tiles de colisión donde el jugador no debe pasar:
   - Bordes del mapa.
   - Alrededor de edificios y árboles.
   - Cercas, rocas grandes, agua (como barrera si no hay puente).
8. Guardar.

#### 9.1.2 Capa "Triggers" (Object Layer)

| Propiedad     | Valor                    |
|---------------|--------------------------|
| Nombre        | `Triggers`               |
| Tipo          | Object Layer              |
| Orden         | Arriba de todo           |
| Visibilidad   | Oculta (uncheck)         |

**Instrucciones en Tiled:**
1. `Layer → Add Object Layer`.
2. Nombrarla "Triggers".
3. Arrastrarla arriba de todo.
4. Crear objetos rectángulo (`Insert Rectangle`) en posiciones de interés:
   - Entrada a una casa → trigger "dialog" que inicia conversación.
   - Área especial → trigger "flag" que marca evento.
5. Para cada objeto, añadir propiedades custom (`Object Properties` → `+`):
   - Para trigger de diálogo:
     - `type` (string): `dialog`
     - `nodeId` (string): `trigger_cabana` (ID del nodo narrativo)
     - `oneShot` (bool): `true`
   - Para trigger de flag:
     - `type` (string): `flag`
     - `flag` (string): `explored_clearing`
     - `oneShot` (bool): `true`
6. Guardar.

### 9.2 Verificación de la Estructura Final del TMX

El mapa final debe tener este orden de capas (de abajo a arriba en Tiled):

```
┌──────────────────────────────────────────────────────┐
│  Triggers (Object Layer, oculta)        ← NUEVA      │
│  Object Layer 1 (objetos decorativos)                 │
│  Collision (Tile Layer, oculta)         ← NUEVA      │
│  Water                                                │
│  Flowers                                              │
│  Road                                                 │
│  RockSlopes_Auto                                      │
│  RockSlopes (Tile Layer, oculta)                      │
│  Ground                                               │
└──────────────────────────────────────────────────────┘
```

### 9.3 Mapa de Posiciones de Spawn (Recomendado)

El jugador aparecerá en tile (20, 20) del mapa (centro aproximado). Se recomienda:

| Ubicación        | Tile X | Tile Y | Propósito                     |
|------------------|--------|--------|-------------------------------|
| Spawn inicial    | 20     | 20     | Zona segura, camino central    |
| Entrada casa 1   | 12     | 35     | Futuro trigger de diálogo     |
| Entrada casa 2   | 32     | 10     | Futuro trigger de diálogo     |
| Claro del bosque | 5      | 15     | Futuro trigger "flag"         |

---

## 10. Integración con el Sistema de Diálogos

### 10.1 Opción Recomendada: Superposición (Overlay)

En lugar de cambiar de Screen completamente (Opción A), se recomienda **Opción B: Superposición**:

```
GameScreen (SIEMPRE activo)
  ├── Renderiza mapa + player
  └── Stage overlay con diálogo (se activa/desactiva)
```

**Ventajas:**
- El mapa se sigue viendo detrás (oscurecido con fade negro semi-transparente).
- No hay que guardar/restaurar estado entre screens.
- Transiciones más suaves.
- El jugador puede ver el entorno mientras dialoga.

### 10.2 Implementación en GameScreen

```java
// En GameScreen.java, añadir:

private DialogOverlay dialogOverlay; // Actor de Scene2D para diálogo
private boolean dialogActivo;

public void iniciarDialogo(String nodeId) {
    dialogActivo = true;
    // storyManager.advance(nodeId) ya fue llamado en TriggerSystem
    dialogOverlay = new DialogOverlay(game, this);
    stage.addActor(dialogOverlay);

    // Opcional: oscurecer fondo
    // Image oscurecer = new Image(new Texture(Gdx.files.internal("...")));
    // oscurecer.setColor(0, 0, 0, 0.5f);
    // stage.addActor(oscurecer);
}

public void finalizarDialogo() {
    dialogActivo = false;
    dialogOverlay.remove();
    dialogOverlay = null;
}

// En render():
// if (dialogActivo) {
//     stage.act(delta);
//     // No procesar input de movimiento mientras se dialoga
// } else {
//     player.update(delta, collisionManager);
// }
```

### 10.3 Guardar/Recuperar Posición (para Opción A)

Si se opta por pantallas separadas, añadir a `Main.java`:

```java
// En Main.java (ver sección 11)
```

---

## 11. Modificaciones a Main.java

Se agregan campos para persistir estado entre screens (necesario para la Opción A o para teleports).

```java
package io.yourPath;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.math.Vector2;

import io.yourPath.logic.StoryManager;
import io.yourPath.models.CharacterProfile;
import io.yourPath.models.GameState;
import io.yourPath.models.NarrativeNode;
import io.yourPath.screens.MainMenuScreen;
import io.yourPath.utils.IntentiaException;
import io.yourPath.utils.NarrativeDAO;
import io.yourPath.utils.NarrativeDAOImplementation;

import java.util.Map;

public class Main extends Game {

    private StoryManager storyManager;
    private Map<String, CharacterProfile> characters;
    private Map<String, NarrativeNode> story;
    private NarrativeDAO narrativeDAO;

    // === NUEVOS CAMPOS PARA NAVEGACIÓN ENTRE SCREENS ===
    private Vector2 ultimaPosicionJugador;
    private String ultimoMapa;

    @Override
    public void create() {
        narrativeDAO = new NarrativeDAOImplementation("database/intentia.db");

        try {
            characters = narrativeDAO.getAllCharacters();
            story = narrativeDAO.getAllDialogNodes();
        } catch (IntentiaException e) {
            System.err.println(e.getMessage());
        }
        storyManager = new StoryManager(story, new GameState());

        setScreen(new MainMenuScreen(this));
    }

    public StoryManager getStoryManager() {
        return storyManager;
    }

    public void setStoryManager(StoryManager storyManager) {
        this.storyManager = storyManager;
    }

    public Map<String, CharacterProfile> getCharacters() {
        return characters;
    }

    public Map<String, NarrativeNode> getStory() {
        return story;
    }

    // === NUEVOS GETTERS/SETTERS ===
    public Vector2 getUltimaPosicionJugador() {
        return ultimaPosicionJugador;
    }

    public void setUltimaPosicionJugador(float x, float y) {
        if (ultimaPosicionJugador == null) {
            ultimaPosicionJugador = new Vector2(x, y);
        } else {
            ultimaPosicionJugador.set(x, y);
        }
    }

    public String getUltimoMapa() {
        return ultimoMapa;
    }

    public void setUltimoMapa(String ultimoMapa) {
        this.ultimoMapa = ultimoMapa;
    }
}
```

---

## 12. LoadingScreen.java — Código Completo

Screen de carga para mostrar mientras se carga el mapa Tiled (que puede tomar varios segundos).

```java
package io.yourPath.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import io.yourPath.Main;

public class LoadingScreen implements Screen {

    private Main game;
    private SpriteBatch batch;
    private OrthographicCamera camera;
    private Viewport viewport;
    private BitmapFont font;
    private float tiempoCargando;

    private static final float TIEMPO_MINIMO_CARGA = 0.5f;
    private boolean cargaCompleta;
    private GameScreen gameScreen;

    public LoadingScreen(Main game) {
        this.game = game;
        this.cargaCompleta = false;
        this.tiempoCargando = 0f;
    }

    @Override
    public void show() {
        camera = new OrthographicCamera();
        viewport = new FitViewport(800, 600, camera);
        batch = new SpriteBatch();

        // Cargar GameScreen en segundo plano
        gameScreen = new GameScreen(game);
    }

    @Override
    public void render(float delta) {
        tiempoCargando += delta;

        // Limpiar pantalla
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Si ya pasó el tiempo mínimo, mostrar GameScreen
        if (!cargaCompleta && tiempoCargando >= TIEMPO_MINIMO_CARGA) {
            cargaCompleta = true;
            gameScreen.show();
            game.setScreen(gameScreen);
            dispose();
        }

        // Mostrar pantalla de carga
        // (En una implementación completa, mostrar barra de progreso)
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void dispose() {
        // No disponer gameScreen aquí porque aún se usa
        batch.dispose();
    }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
}
```

---

## 13. Mejores Prácticas Tiled

### 13.1 Unit Scale

| Tile Size | Unit Scale | Razón                     |
|-----------|------------|---------------------------|
| 16×16     | `1/16f`    | Estándar para pixel art   |
| 32×32     | `1/32f`    | Para mapas más detallados |

**Siempre verificar** con `map.getProperties().get("tilewidth")` en lugar de hardcodear.

### 13.2 Texture Filtering

```java
// Configuración correcta para pixel art
Texture.setEnforcePotImages(false); // Permitir texturas no-POT

TmxMapLoader.Parameters params = new TmxMapLoader.Parameters();
params.textureMinFilter = Texture.TextureFilter.Nearest;
params.textureMagFilter = Texture.TextureFilter.Nearest;
```

### 13.3 Capa de Colisión

- Usar tiles **invisibles** (capa oculta en Tiled).
- NO hardcodear posiciones de colisión en Java.
- Alternativa avanzada: usar ObjectLayer con rectángulos de colisión (más precisos que tiles).

### 13.4 Orden de Renderizado

```
1. Capas de suelo (Ground, Road, Water, Flowers, RockSlopes_Auto)
2. Entidades (Player, NPCs)
3. Capas de objetos (Object Layer 1 — árboles, edificios, props)
4. Capas de foreground (si existen, ej: hojas que caen)
```

**Regla de oro**: El jugador se renderiza ENTRE las capas de suelo y las capas de objetos. Así puede caminar "detrás" de árboles y edificios.

### 13.5 Triggers

- Usar **ObjectLayer con rectángulos**, NO tiles invisibles.
- Los rectángulos en Tiled están en píxeles; convertirlos a unidades del mundo en Java.
- Propiedades custom: `type`, `nodeId`, `flag`, `oneShot`.
- Prefijo de convención: `trigger_` para nodeIds (ej: `trigger_cabana_entrada`).

### 13.6 Pathfinding (Futuro)

gdx-ai ya está en las dependencias. Para pathfinding:
- Crear un `Graph` a partir de la collisionMatrix.
- Usar `IndexedAStarPathFinder` con heurística Manhattan.
- Integrar con NPCs en Fase 4+.

### 13.7 Animaciones del Mapa

El mapa Tiled ya incluye animaciones:
- **Water tiles**: Tileset_Water.tsx tiene `<animation>` con frames.
- **Flores rojas/blancas**: Animation_Flowers_Red/White.tsx.
- **Fogata**: Animation_Campfire.tsx.

El `OrthogonalTiledMapRenderer` de libGDX maneja automáticamente estas animaciones (actualiza `stateTime` internamente). **No requiere código adicional.**

---

## 14. Diagrama de Capas de Renderizado

### 14.1 Orden de Renderizado en GameScreen

```
render(delta) {
    │
    ├── [PASO 1] glClearColor(0.1f, 0.1f, 0.15f, 1f)
    │            glClear(GL_COLOR_BUFFER_BIT)
    │            └── Pantalla limpia, color azul oscuro noche
    │
    ├── [PASO 2] mapRenderer.render(capas de fondo)
    │            └── Ground ─── Hierba, tierra, arena
    │            └── Road ───── Caminos de tierra
    │            └── Water ──── Agua animada
    │            └── Flowers ── Flores decorativas
    │            └── RockSlopes_Auto ── Pendientes
    │
    ├── [PASO 3] spriteBatch.begin()
    │            └── player.draw(batch)
    │            spriteBatch.end()
    │            └── Personaje sobre el suelo
    │
    ├── [PASO 4] mapRenderer.render(capa de objetos)
    │            └── Object Layer 1 ── Árboles, edificios, props
    │            └── (El jugador aparece DETRÁS de objetos altos)
    │
    ├── [PASO 5] triggerSystem.checkTriggers(playerBounds, game)
    │            └── Si colisiona con trigger → activar evento
    │
    └── [PASO 6] stage.act(delta)
                 stage.draw()
                 └── Diálogo overlay (si activo)
                 └── HUD (barra de salud, minimapa, etc.)
}
```

### 14.2 Efecto de capas

```
Vista final del jugador:
┌──────────────────────────────────────────────┐
│  🌳    🏠           🌲          🌳           │  ← Objetos (PASO 4)
│        ┌──────┐                              │
│  🌲    │ 👦   │    🌳                       │  ← Player (PASO 3)
│        └──────┘                              │
│  ░░░░░░▒▒▒▒░░░░░░░▒▒▒▒░░░░░░░░░            │  ← Suelo (PASO 2)
│  ░░camino░░░▒▒hierba▒▒░░░agua≈≈≈            │
└──────────────────────────────────────────────┘
```

---

## 15. Checklist de Verificación

### 15.1 Preparación del Mapa

- [ ] Capa "Collision" creada en Beginning Fields.tmx
- [ ] Tiles de colisión pintados en bordes del mapa, edificios, árboles, agua
- [ ] Capa "Collision" marcada como oculta en Tiled
- [ ] Capa "Triggers" (ObjectLayer) creada en Beginning Fields.tmx
- [ ] Al menos 1 trigger de prueba configurado (type="flag", flag="test_trigger")
- [ ] Propiedades custom añadidas a los objetos trigger
- [ ] Mapa guardado y copiado a `assets/TileSet/Tiled/Tilemaps/` (ruta correcta desde Lwjgl3Launcher)

### 15.2 Código

- [ ] `entities/Player.java` creado con carga de spritesheet y animaciones
- [ ] `logic/CollisionManager.java` creado con buildCollisionMatrix y canMove
- [ ] `logic/TriggerSystem.java` creado con loadTriggers y checkTriggers
- [ ] `logic/CameraController.java` creado con update y clamp
- [ ] `screens/GameScreen.java` creado con show/render/dispose
- [ ] `Main.java` modificado con campos ultimaPosicionJugador y ultimoMapa
- [ ] `screens/LoadingScreen.java` creado (opcional pero recomendado)

### 15.3 Funcionalidad

- [ ] El mapa Tiled se renderiza correctamente (todas las capas visibles)
- [ ] Las animaciones del mapa (agua, flores, fogata) se reproducen
- [ ] La cámara sigue al jugador con suavizado (lerp)
- [ ] La cámara no se sale del mapa (clamp)
- [ ] El jugador se mueve con WASD/Flechas
- [ ] Las animaciones de caminar se reproducen correctamente
- [ ] Las animaciones idle se muestran al soltar teclas
- [ ] Las 4 direcciones funcionan (arriba, abajo, izquierda, derecha)
- [ ] El movimiento diagonal no es más rápido que el cardinal
- [ ] Las colisiones funcionan: el jugador no atraviesa tiles de colisión
- [ ] El jugador "desliza" contra las paredes (colisión X/Y separada)
- [ ] Los triggers se activan al pisar el rectángulo
- [ ] Los triggers one-shot no se reactivan
- [ ] Los triggers de tipo "flag" añaden flags al GameState
- [ ] La integración con el sistema de diálogos funciona
- [ ] `resize()` mantiene el aspect ratio correctamente
- [ ] `dispose()` libera todos los recursos sin errores

### 15.4 Rendimiento

- [ ] FPS estables (60 FPS en desktop)
- [ ] El mapa no parpadea (V-sync activado)
- [ ] La cámara no produce tearing
- [ ] Las texturas se ven nítidas (filter Nearest)
- [ ] No hay memory leaks al cambiar de screen

### 15.5 Código Limpio

- [ ] Sin System.out.println() en producción (solo debugging)
- [ ] Nombres de variables en español (consistente con el proyecto)
- [ ] Imports organizados (sin wildcards `*`)
- [ ] Cada clase tiene package declarado
- [ ] Sin warnings de compilación

---

## Apéndice A: Flujo Completo de Transiciones entre Screens

```
Main.create()
  │
  ├── Cargar datos (DAO → characters, story)
  ├── Crear StoryManager
  └── setScreen(MainMenuScreen)
        │
        ├── Usuario elige "Nueva Partida"
        │   └── storyManager.start("car_awakening")
        │       └── setScreen(GameScreen)  ← AHORA VA DIRECTO A GAMESCREEN
        │             │
        │             ├── JUGADOR EXPLORA MAPA
        │             │   ├── Mueve personaje (WASD)
        │             │   ├── Activa trigger → storyManager.advance("trigger_X")
        │             │   │   └── dialogActivo = true
        │             │   │       └── Se muestra overlay de diálogo sobre el mapa
        │             │   │           ├── Usuario lee diálogo
        │             │   │           ├── Elige opción → storyManager.advance(option)
        │             │   │           └── Finaliza → dialogActivo = false
        │             │   └── Sigue explorando
        │             │
        │             └── ESC → PauseMenu
        │                 ├── "Guardar" → SaveSystem
        │                 ├── "Salir" → setScreen(MainMenuScreen)
        │                 └── "Continuar" → retomar juego
        │
        └── (Futuro: botón "Continuar" carga GameState y va a GameScreen)
```

## Apéndice B: Propiedades Custom de Triggers — Referencia Rápida

| Propiedad | Tipo    | Obligatorio | Valores Ejemplo              | Descripción                              |
|-----------|---------|-------------|------------------------------|------------------------------------------|
| `type`    | string  | Sí          | `dialog`, `flag`, `teleport` | Tipo de trigger                          |
| `nodeId`  | string  | Si dialog   | `trigger_cabana_entrada`     | ID del nodo narrativo a iniciar          |
| `flag`    | string  | Si flag     | `explored_clearing`          | Flag a añadir al GameState               |
| `oneShot` | bool    | No          | `true` (default)             | Si se desactiva tras la primera activación |
| `map`     | string  | Si teleport | `Beginning Fields.tmx`       | Ruta del mapa de destino (futuro)        |
| `spawnX`  | float   | Si teleport | `320`                        | Posición X de spawn en píxeles           |
| `spawnY`  | float   | Si teleport | `320`                        | Posición Y de spawn en píxeles           |

## Apéndice C: Resolución de Problemas Comunes

| Problema                          | Causa Probable                                    | Solución                                              |
|-----------------------------------|---------------------------------------------------|-------------------------------------------------------|
| Mapa no se renderiza (pantalla negra) | Ruta incorrecta del TMX                          | Verificar que `Gdx.files.internal("...")` apunta al archivo correcto |
| Tiles se ven borrosos             | Filter Linear en texturas                         | Usar `Texture.TextureFilter.Nearest` en parámetros del loader |
| Jugador no se mueve               | CollisionManager no inicializado o capa "Collision" no existe | Verificar capa en TMX; temporalmente comentar colisiones |
| Cámara se sale del mapa           | Clampeo incorrecto o mapWidth/mapHeight mal calculados | Verificar `map.getProperties().get("width/height")` y multiplicar por tileSize * unitScale |
| Animaciones no se reproducen      | stateTime no se incrementa en update()             | Asegurar `stateTime += delta` en Player.update()      |
| Trigger no se activa              | Nombre de capa "Triggers" incorrecto o propiedades mal escritas | Verificar mayúsculas/minúsculas; verificar propiedades custom en Tiled |
| El juego se congela al cargar     | Carga sincrónica del mapa (puede tomar ~1s)       | Implementar LoadingScreen con AssetManager            |
| Textura del personaje no aparece  | Ruta del spritesheet incorrecta en Player.java    | Verificar ruta relativa a assets/                     |
