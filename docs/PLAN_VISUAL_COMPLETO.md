# Plan de Transformación Visual: Intentia

> Documento de planificación arquitectónica para migrar de terminal a interfaz gráfica completa con libGDX.
> Generado tras análisis exhaustivo del código fuente, assets, documentación e investigación existente.

---

## Tabla de Contenidos

1. [Estado Actual vs. Visión Final](#1-estado-actual-vs-visión-final)
2. [Arquitectura Objetivo](#2-arquitectura-objetivo)
3. [Fases de Migración Detalladas](#3-fases-de-migración-detalladas)
4. [Decisiones Técnicas y Mejores Prácticas](#4-decisiones-técnicas-y-mejores-prácticas)
5. [Estructura de Paquetes Final](#5-estructura-de-paquetes-final)
6. [Gestión de Assets](#6-gestión-de-assets)
7. [Escalabilidad y Extensibilidad](#7-escalabilidad-y-extensibilidad)
8. [Riesgos y Mitigaciones](#8-riesgos-y-mitigaciones)

---

## 1. Estado Actual vs. Visión Final

### 1.1 Diagnóstico Actual

| Aspecto | Estado Actual |
|---------|---------------|
| UI | `System.out.println()` + `Scanner(System.in)` |
| Renderizado | No hay renderizado gráfico (solo consola) |
| Mapas | Tiled maps (`.tmx`) y tilesets (`.tsx`) listos en `TileSet/Tiled/` pero no se renderizan |
| Diálogos | Texto plano en terminal con opciones numéricas |
| Personajes | `CharacterProfile` con `portraitPath` pero sin imagen cargada |
| Música | Campo `musicTrack` en NarrativeNode pero sin implementación de audio |
| Cámaras | Sin cámara (no hay escena visual) |
| Animaciones | Spritesheets en `TileSet/Art/` pero sin usar |
| Skins/Pieles | Sin Scene2D skin |
| Partículas | No implementadas |
| Transiciones | No hay transiciones entre pantallas |
| Input | `Scanner` bloqueante (incompatible con el game loop de libGDX) |
| Dependencias ociosas | Ashley ECS, Box2D, FreeType, gdx-ai incluidos pero sin uso |

### 1.2 Visión Final

| Aspecto | Estado Deseado |
|---------|----------------|
| UI | Scene2D con `Stage`, `Skin`, `Table` y widgets |
| Renderizado | `SpriteBatch` con cámara `OrthographicCamera` |
| Mapas | `OrthogonalTiledMapRenderer` renderizando mapas existentes |
| Diálogos | `DialogOverlay` con panel semi-transparente, retrato, texto typewriter, opciones cliqueables |
| Personajes | Retratos cargados como `Texture` -> `Image` en Scene2D |
| Música | `Music` de libGDX reproduciendo pistas según `musicTrack` |
| Cámaras | Cámara que sigue al jugador con `FitViewport` 800x600 |
| Animaciones | `Animation<TextureRegion>` para walk cycle idle/caminar |
| Skins | Kenney Pixel o Craftacular de gdx-skins |
| Partículas | `ParticleEffect` para efectos atmosféricos |
| Transiciones | `Actions.fadeIn/fadeOut` entre screens |
| Input | `Gdx.input.isKeyJustPressed()` + `InputProcessor` en Stage |
| Dependencias | FreeType (fuentes), Box2D (colisiones), Ashley (opcional ECS) |

---

## 2. Arquitectura Objetivo

### 2.1 Diagrama de Capas

```
┌─────────────────────────────────────────────────────────────────────┐
│                        SCREENS (Capa de Presentación)                │
│                                                                      │
│  ┌──────────────┐  ┌──────────────┐  ┌─────────────┐  ┌──────────┐  │
│  │ MainMenu     │  │ GameScreen   │  │ Dialog      │  │Cinematic │  │
│  │ Screen       │  │ (Tiled+Ent.) │  │ OverlayScr. │  │ Screen   │  │
│  └──────┬───────┘  └──────┬───────┘  └──────┬──────┘  └────┬─────┘  │
│         │                 │                 │              │         │
│  ┌──────┴─────────────────┴─────────────────┴──────────────┴──────┐  │
│  │              STAGE / VIEWPORT / SCENE2D                        │  │
│  │         (InputProcessor, Skin, Table, Widgets)                 │  │
│  └────────────────────────────────────────────────────────────────┘  │
├─────────────────────────────────────────────────────────────────────┤
│                    CORE / CONTROLLER (Capa de Lógica)                │
│                                                                      │
│  ┌────────────────────────────────────────────────────────────────┐  │
│  │                    StoryManager                                 │  │
│  │  (advance, checkTrialEvaluation, processActions, getCurrentNode) │  │
│  └───────────┬────────────────────────────────────────┬───────────┘  │
│              │                                        │              │
│         ┌────┴────┐                              ┌───┴────┐        │
│         │GameState│                              │ UIState │        │
│         └────┬────┘                              └───┬────┘        │
│              │                                        │              │
├──────────────┴────────────────────────────────────────┴──────────────┤
│                    DATA (Capa de Modelos y Persistencia)              │
│                                                                      │
│  ┌─────────────┐  ┌──────────────┐  ┌────────────┐  ┌────────────┐  │
│  │ NarrativeNode│  │ DialogNode   │  │ TrialNode  │  │Character   │  │
│  │ (abstract)   │  │ (+TrialNode) │  │ (+TrialEva)│  │ Profile    │  │
│  └─────────────┘  └──────────────┘  └────────────┘  └────────────┘  │
│  ┌────────────────────────────────────────────────────────────────┐  │
│  │                    DATA ACCESS (DAO)                           │  │
│  │  NarrativeDAO (interface) ← NarrativeDAOImplementation (SQLite)  │
│  │  SaveSystem (JSON persist)                                      │
│  └────────────────────────────────────────────────────────────────┘  │
│  ┌────────────────────────────────────────────────────────────────┐  │
│  │                    ASSET MANAGER                               │  │
│  │  Textures, Skins, Maps, Music, Sounds, Videos                 │  │
│  └────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────┘
```

### 2.2 Flujo de Screens

```
┌─────────────┐     ┌──────────────┐     ┌──────────────────┐
│ MainMenu    │────>│ Dialog       │────>│ GameScreen       │
│ Screen      │     │ OverlayScr.  │     │ (Tiled + Player) │
│ (Scene2D)   │     │ (Scene2D)    │     │                  │
└─────────────┘     └──────┬───────┘     └────────┬─────────┘
       ^                   │                      │
       │            ┌──────┴──────┐        ┌──────┴──────┐
       │            │ PauseMenu   │        │ Dialog      │
       │            │ (Window)    │        │ Overlay     │
       │            └─────────────┘        │ (Scene2D)   │
       │                                   └──────┬──────┘
       │                                          │
       └──────────────────────────────────────────┘
                          │
                    ┌─────┴─────┐
                    │ Cinematic │
                    │ Screen    │
                    └───────────┘
```

### 2.3 Estados de UI (UIState expandido)

```java
public enum UIState {
    EXPLORANDO,      // Caminando por el mapa Tiled (futuro)
    DIALOGANDO,      // Leyendo diálogo con opciones
    MENU_PAUSA,      // Menú de pausa
    CINEMATICA,      // Reproduciendo video (futuro)
    TRANSICION       // Entre screens (fade in/out)
}
```

---

## 3. Fases de Migración Detalladas

### ⚙️ FASE 0: Infraestructura y Preparación (2-3 días)

**Objetivo:** Dejar el proyecto listo para recibir la UI gráfica sin romper la lógica existente.

#### 0.1 Refactor de Input (CRÍTICO)
- **Problema:** `Scanner(System.in)` es bloqueante y NO funciona con el game loop de libGDX.
- **Solución:** Reemplazar `Scanner` en `MainMenuScreen` y `StoryScreen` con el callback `render(float delta)` usando teclas.
- **Tareas:**
  - [ ] `MainMenuScreen.render()`: usar `Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)` etc. o un menú con opción escaneada por teclado.
  - [ ] `StoryScreen`: cambiar de `scanner.nextInt()` a lógica basada en `Gdx.input.isKeyJustPressed()`.
  - [ ] Eliminar `Scanner scanner` de ambos screens.
  - [ ] **Verificar:** `./gradlew lwjgl3:run` funciona sin Scanner.

#### 0.2 Setup del Skin Scene2D
- [ ] Descargar `kenney-pixel` de [gdx-skins](https://github.com/czyzby/gdx-skins).
- [ ] Copiar `skin/` a `assets/skin/` con estructura:
  ```
  assets/skin/
    pixel/
      uiskin.json
      uiskin.atlas
      uiskin.png
      default.fnt / default.png
      (o fuentes .fnt personalizadas)
  ```
- [ ] Probar carga: `new Skin(Gdx.files.internal("skin/pixel/uiskin.json"))`.

#### 0.3 Setup de FreeTypeFont
- [ ] Dado que `gdx-freetype` ya está en dependencias, usarlo para generar fuentes bitmap.
- [ ] Crear `utils/Assets.java` con un `FreeTypeFontGenerator`:
  ```java
  FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/pixel.ttf"));
  FreeTypeFontGenerator.FreeTypeFontParameter params = new FreeTypeFontGenerator.FreeTypeFontParameter();
  params.size = 16;
  params.minFilter = Texture.TextureFilter.Nearest;
  params.magFilter = Texture.TextureFilter.Nearest;
  BitmapFont pixelFont = generator.generateFont(params);
  ```
- [ ] Inyectar esta fuente en el Skin.

#### 0.4 Crear AssetManager wrapper
- [ ] Crear `utils/AssetManagerProvider.java` singleton que gestione carga progresiva.
- [ ] Agregar interface `LoadableScreen` con `getAssetsToLoad(): List<String>` para que cada screen pueda declarar sus assets.

#### 0.5 Eliminar dependencias ociosas (opcional pero recomendado)
- [ ] Evaluar si `gdx-ai`, `ashley` y `gdx-box2d` son necesarios.
- [ ] Si no se usarán en esta migración, comentarlas del `build.gradle` para reducir tiempos de compilación.
- [ ] Dejarlas si se planea usar Box2D para colisiones en Fase 4.

---

### 🎨 FASE 1: MainMenuScreen Visual (2-3 días)

**Objetivo:** Reemplazar el menú de terminal por uno gráfico con Scene2D.

#### 1.1 Reestructurar MainMenuScreen
- [ ] Eliminar `Scanner` y toda lógica de consola.
- [ ] Implementar `Stage` + `Skin` + `Table` en `show()`.
- [ ] Layout:
  ```
  ┌──────────────────────┐
  │                      │
  │   INTENTIA: LEGADO   │  ← Label con fontScale grande
  │                      │
  │   [ Nueva Partida ]  │  ← TextButton
  │   [  Continuar    ]  │  ← TextButton (visible solo si save existe)
  │   [    Salir      ]  │  ← TextButton
  │                      │
  └──────────────────────┘
  ```
- [ ] Listeners: `ChangeListener` en cada botón.
- [ ] Fondo: Color sólido (#0D0D1A) o imagen de fondo parallax.

#### 1.2 Persistencia del botón "Continuar"
- [ ] `btnContinuar.setVisible(SaveSystem.exists())` en cada `show()`.
- [ ] Refrescar visibilidad al regresar del juego (si se guardó).

#### 1.3 Transición a StoryScreen
- [ ] `Nueva Partida` → `game.setScreen(new StoryScreen(game))` (nueva versión visual).
- [ ] `Continuar` → cargar `GameState` → crear nuevo `StoryManager` → `StoryScreen`.

#### 1.4 Efectos visuales
- [ ] Fade in al aparecer: `stage.addAction(Actions.sequence(Actions.fadeOut(0), Actions.fadeIn(0.5f)))`.
- [ ] Efecto hover en botones (cambio de color o escala).
- [ ] Opcional: partículas de fondo o título animado.

---

### 💬 FASE 2: StoryScreen → DialogOverlayScreen Visual (4-5 días)

**Objetivo:** Transformar la pantalla de diálogo de terminal a una superposición visual con Scene2D.

#### 2.1 Nueva clase: `DialogOverlayScreen`
- [ ] Crear `screens/DialogOverlayScreen.java`.
- [ ] `Stage` con `FitViewport(800, 600)`.
- [ ] Layout del diálogo:
  ```
  ┌────────────────────────────────────────────┐
  │   [MARCO DE DIÁLOGO - NinePatch]           │
  │   ┌─────────┬──────────────────────────┐   │
  │   │ RETRATO │  NOMBRE (cyan)           │   │
  │   │ (Image) │                           │   │
  │   │         │  Texto del diálogo...     │   │
  │   │         │  (con typewriter effect)  │   │
  │   │         │                           │   │
  │   │         │  > Opción 1               │   │
  │   │         │  > Opción 2 (si aplica)   │   │
  │   └─────────┴──────────────────────────┘   │
  └────────────────────────────────────────────┘
  ```
- [ ] Panel inferior: `Stack` con `Image` (fondo semi-transparente negro 70%) + `Table` (contenido).
- [ ] Retrato: `Image` con `Texture` cargada de `CharacterProfile.portraitPath`.
- [ ] Nombre: `Label` con `fontColor = Color.CYAN`.
- [ ] Texto: `Label` con `setWrap(true)`.
- [ ] Opciones: Botones `TextButton` apilados verticalmente.

#### 2.2 Typewriter Effect
- [ ] Crear `utils/TypewriterAction.java` que herede de `TemporalAction`.
- [ ] Revelar letras progresivamente en el Label.
- [ ] Duración configurable (ej: 1.5s para texto normal).
- [ ] Al terminar, mostrar opciones.

#### 2.3 Conectar con StoryManager
- [ ] En `show()` llamar a `mostrarNodoActual()`.
- [ ] `mostrarNodoActual()`:
  - Limpiar `opcionesTable`.
  - Obtener `NarrativeNode` de `storyManager.getCurrentNode()`.
  - Mostrar nombre del personaje desde `characters.get(node.getSpeakerId())`.
  - Si hay opciones: crear `TextButton` por cada `DialogOption`.
  - Filtrar opciones bloqueadas por `requiredFlag` (deshabilitar + opacity 0.4f).
  - Si no hay opciones y hay `nextId`: botón "Continuar".
- [ ] Al hacer clic en opción: `storyManager.advance(opt)`, reguardar, refrescar UI.
- [ ] Animar transición: `fadeOut → mostrarNodoActual() → fadeIn`.

#### 2.4 Manejo de estados: PauseMenu
- [ ] Tecla `ESCAPE`: `UIState.MENU_PAUSA`.
- [ ] Mostrar `Window` modal con opciones:
  - "Volver al juego" → retomar.
  - "Guardar partida" → `SaveSystem.saveGame()` + retomar.
  - "Salir al menú" → `game.setScreen(new MainMenuScreen(game))`.

#### 2.5 Manejo de TrialNode
- [ ] Cuando `storyManager.getCurrentNode()` es `TrialNode`:
  - Mostrar texto del juicio/prueba.
  - Evaluación ocurre automáticamente en `StoryManager.checkTrialEvaluation()`.
  - Mostrar resultado (éxito/fallo) visualmente con color y emoción.
- [ ] Botón "Continuar" para avanzar al siguiente nodo.

#### 2.6 Carga de retratos
- [ ] Usar `AssetManager` o carga directa con `new Texture(Gdx.files.internal(path))`.
- [ ] Escalar con `setSize()` manteniendo aspecto.
- [ ] Manejar `null` (portraitPath ausente): mostrar silueta genérica o ícono por defecto.

#### 2.7 Reproducción de música
- [ ] Cuando `node.getMusicTrack()` no es null, detener música anterior y reproducir nueva.
- [ ] Usar `Gdx.audio.newMusic(Gdx.files.internal("music/" + track))`.
- [ ] `Music.setLooping(true)` para música ambiental.

---

### 🗺️ FASE 3: GameScreen con Tiled (5-7 días)

**Objetivo:** Renderizar el mapa Tiled, control de cámara, personaje jugable y triggers narrativos.

#### 3.1 Nueva clase: `GameScreen`
- [ ] Crear `screens/GameScreen.java`.
- [ ] `OrthographicCamera` con `FitViewport(800, 600)`.
- [ ] `OrthogonalTiledMapRenderer` renderizando el mapa `Beginning Fields.tmx`.

#### 3.2 Configuración del Mapa
- [ ] Cargar `Gdx.files.internal("TileSet/Tiled/Tilemaps/Beginning Fields.tmx")`.
- [ ] Escalar: `unitScale = 1 / 16f` (si tiles son 16x16 píxeles).
- [ ] Determinar capas del mapa: "Ground", "Objects", "Collision", "Triggers".

#### 3.3 Sistema de Cámara
- [ ] Cámara sigue al jugador con interpolación suave (`lerp`).
- [ ] Clampear a límites del mapa:
  ```java
  camera.position.x = MathUtils.clamp(player.x, viewportWidth/2, mapWidth - viewportWidth/2);
  ```
- [ ] Zoom fijo (1.0) o ajustable.

#### 3.4 Personaje Jugable
- [ ] Crear `Player.java` en nuevo paquete `entities/`.
- [ ] Atributos: `Vector2 position`, `float speed`, `Animation<TextureRegion> currentAnim`, `stateTime`.
- [ ] Spritesheet de personaje (idle + walk en 4 direcciones).
- [ ] Input WASD/Flechas: `Gdx.input.isKeyPressed(Input.Keys.W)`.
- [ ] Animación: `stateTime += delta`, `currentFrame = currentAnim.getKeyFrame(stateTime, true)`.
- [ ] Dirección: 8 direcciones o 4 (dependiendo del spritesheet).
- [ ] Dibujar: `spriteBatch.begin()`, `spriteBatch.draw(currentFrame, player.x, player.y)`, `spriteBatch.end()`.

#### 3.5 Colisiones
- [ ] **Opción recomendada:** Tile-based simple (sin Box2D por ahora).
  - Leer capa de colisión del TMX.
  - Antes de mover, verificar si el tile destino es walkable.
  - Matriz booleana `boolean[][] collisionLayer` cargada al iniciar.
- [ ] **Opción avanzada:** Box2D (ya incluido en dependencias).
  - Crear `Body` para jugador con `BodyDef.BodyType.DynamicBody`.
  - Crear `Body` estáticos para tiles de colisión.
  - Sincronizar posición Box2D con sprite.
- [ ] Implementar detección de colisión simple: si tile colisionable, no mover.

#### 3.6 Triggers Narrativos
- [ ] Leer capa de objetos (`MapObjects`) del TMX.
- [ ] Detectar rectángulos trigger: si `player.boundingBox.overlaps(triggerRectangle)`.
- [ ] Propiedades custom del trigger:
  - `type: "dialog"` → iniciar diálogo con `storyManager.advance(nodeId)`.
  - `nodeId: "trigger_cabaña"` → ID del nodo a mostrar.
  - `oneShot: true` → marcar flag para no repetir.
- [ ] Al activar: `game.setScreen(new DialogOverlayScreen(game, storyManager))`.
  - Cuando el diálogo termine, volver a `GameScreen`.
- [ ] Transferir estado: guardar `player.position` antes de entrar al diálogo.

#### 3.7 Integración DialogOverlay + GameScreen
- [ ] Decisión arquitectónica importante:
  - **Opción A:** Pantallas separadas (`GameScreen` → `DialogOverlayScreen` y vuelta).
    - Más limpio, independencia total.
    - Requiere guardar/restaurar posición del jugador.
  - **Opción B:** Un solo screen que renderiza mapa + overlay de diálogo.
    - Más complejo pero permite ver el mapa detrás del diálogo.
    - GameScreen renderiza el mapa, superpone un `Stage` de diálogo.
- **Recomendación: Opción B** para el caso de uso de Intentia.
  - El diálogo cubre solo la mitad inferior, dejando ver el mapa atrás.

#### 3.8 Capas de Renderizado
```
1. Clear screen (color de fondo)
2. Renderizar mapa Tiled (capas de suelo abajo, objetos arriba)
3. Renderizar player sprite
4. Renderizar Stage de UI (diálogo/pausa si está activo)
```

---

### 🎬 FASE 4: Cinemáticas y Video (2-3 días)

**Objetivo:** Integrar reproducción de video WebM con gdx-video.

#### 4.1 Setup de gdx-video
- [ ] Agregar dependencia `gdx-video` a los `build.gradle`.
- [ ] Configurar para LWJGL3 desktop.
- [ ] Probar con un video WebM de muestra.

#### 4.2 CinematicScreen
- [ ] Crear `screens/CinematicScreen.java`.
- [ ] Reproducir video con `GdxVideo.createVideoPlayer()`.
- [ ] Botón "Skip" detectable con tecla ENTER/ESCAPE.
- [ ] Al terminar, transicionar a `DialogOverlayScreen` o `GameScreen`.

#### 4.3 Integración narrativa
- [ ] `NarrativeNode.videoPath` (a futuro: nuevo campo en `NarrativeNode`).
- [ ] Flag "visto_X" para no repetir cinemáticas.

---

### 🔊 FASE 5: Audio y Sonido (1-2 días)

**Objetivo:** Sistema completo de audio.

#### 5.1 Música
- [ ] `MusicManager` singleton: `play(String trackId)`, `stop()`, `setVolume(float)`.
- [ ] Transiciones suaves entre pistas (fade out/in).
- [ ] Música por defecto para menú principal.

#### 5.2 SFX
- [ ] Sonidos para: clic de botón, hover, cambio de nodo, error/éxito en TrialNode.
- [ ] `Sound` de libGDX: `Gdx.audio.newSound(Gdx.files.internal("sfx/click.wav"))`.

---

### ✨ FASE 6: Pulido y Efectos (2-3 días)

**Objetivo:** Transiciones, partículas, efectos visuales.

#### 6.1 Transiciones entre Screens
- [ ] Clase base `TransitionScreen` con fade in/out.
- [ ] Usar `Actions.sequence(fadeOut, run -> setScreen(), fadeIn)`.

#### 6.2 Partículas
- [ ] Efectos atmosféricos: polvo, hojas, niebla.
- [ ] `ParticleEffect` de libGDX cargado desde archivos `.p`.

#### 6.3 Efectos de Diálogo
- [ ] Highlight en opción hovereada.
- [ ] Fade entre nodos de diálogo.
- [ ] Indicador de "escribiendo..." con animación de puntos.

---

### 🧪 FASE 7: Testing y Calidad (2-3 días)

**Objetivo:** Garantizar que la migración no rompió la lógica narrativa.

#### 7.1 Tests Unitarios
- [ ] Agregar JUnit 5 al `build.gradle`.
- [ ] Tests para `StoryManager.advance()`, `checkTrialEvaluation()`, `processActions()`.
- [ ] Tests para `GameState`: flags, trial scoring, reset.
- [ ] Mock de `NarrativeDAO` para pruebas.

#### 7.2 Tests de Integración
- [ ] Verificar carga de SQLite con datos de prueba.
- [ ] Verificar persistencia (guardado/carga) de `GameState`.

#### 7.3 Tests Visuales
- [ ] Verificar que todos los skins cargan correctamente.
- [ ] Verificar que los mapas Tiled renderizan sin errores.
- [ ] Verificar que los retratos de personajes se muestran.

---

## 4. Decisiones Técnicas y Mejores Prácticas

### 4.1 Viewport Strategy

| Viewport | Uso | Razón |
|----------|-----|-------|
| `FitViewport(800, 600)` | GameScreen, Dialogs | Mantiene aspect ratio 4:3, agrega letterboxing si es necesario |
| `ScreenViewport` | Solo para debugging | No escalar, cada píxel = 1 unidad |

### 4.2 Skin Strategy
- **Fase 1-2:** Usar skin pre-hecho de gdx-skins (`kenney-pixel`).
- **Fase 6:** Personalizar con Skin Composer para identidad visual única.
- **Fuentes:** Usar FreeTypeFontGenerator con fuente pixel-art (ej: "Press Start 2P" o "PixelFont").

### 4.3 Patrón de Screens
```java
public abstract class BaseScreen implements Screen {
    protected Main game;
    protected Stage stage;
    protected Skin skin;

    public BaseScreen(Main game) {
        this.game = game;
    }

    protected abstract void buildUI();

    @Override
    public void render(float delta) {
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int w, int h) {
        stage.getViewport().update(w, h, true);
    }
}
```

### 4.4 Gestión de Estado entre Screens
```java
// GameScreen → DialogOverlay → GameScreen
// Al salir de GameScreen para diálogo:
//   1. Guardar playerPosition en Main (o GameState extendido)
//   2. Al volver: restaurar playerPosition

// En Main.java:
private Vector2 lastPlayerPosition;
private String lastMapPath;
```

### 4.5 Manejo de Resources
- **Siempre** llamar `dispose()` en `Screen` (Stage, Skin, Textures, Music).
- **Nunca** cargar texturas en el constructor de Screen (hacerlo en `show()`).
- Usar `AssetManager` para carga asíncrona con barra de progreso.

### 4.6 Estructura de Paquetes Final
```
io.yourPath/
├── Main.java
├── screens/
│   ├── MainMenuScreen.java
│   ├── GameScreen.java
│   ├── DialogOverlayScreen.java
│   ├── CinematicScreen.java
│   └── BaseScreen.java (abstract)
├── entities/
│   └── Player.java
├── logic/
│   └── StoryManager.java
├── models/
│   ├── NarrativeNode.java
│   ├── DialogNode.java
│   ├── TrialNode.java
│   ├── DialogOption.java
│   ├── TrialEvaluation.java
│   ├── CharacterProfile.java
│   ├── GameState.java
│   └── UIState.java (expandido)
├── utils/
│   ├── NarrativeDAO.java
│   ├── NarrativeDAOImplementation.java
│   ├── SaveSystem.java
│   ├── Assets.java (AssetManager wrapper)
│   ├── TypewriterAction.java
│   └── IntentiaException.java
└── managers/
    ├── MusicManager.java
    └── InputManager.java (opcional)
```

### 4.7 Convenciones de Código
- Nombres en español (como ya se usa en el código actual) para consistencia.
- `private` por defecto, getters/setters públicos.
- `final` en parámetros de métodos donde aplique.
- JavaDoc en todas las clases públicas y métodos críticos.
- Uso de `StringBuilder` para concatenaciones (ya implementado).

---

## 5. Gestión de Assets

### 5.1 Assets existentes que se usarán
```
assets/
├── database/intentia.db        ← Ya en uso (SQLite)
├── story.json / characters.json ← Fallback, mantener
├── skin/pixel/                  ← NUEVO: Skin Scene2D
│   ├── uiskin.json
│   ├── uiskin.atlas
│   └── uiskin.png
├── fonts/                       ← NUEVO: Fuentes FreeType
│   └── pixel.ttf
├── portraits/                   ← NUEVO: Retratos de personajes
│   ├── abuelo.png
│   ├── nino.png
│   └── narrador.png (o icono genérico)
├── music/                       ← NUEVO: Pistas musicales
│   ├── menu.ogg
│   ├── dialogo.ogg
│   └── bosque.ogg
├── sfx/                         ← NUEVO: Efectos de sonido
│   ├── click.wav
│   ├── hover.wav
│   └── decision.wav
├── maps/                        ← NUEVO: Mapas Tiled (copiar de TileSet/)
│   └── Beginning Fields.tmx
├── sprites/                     ← NUEVO: Spritesheets del jugador
│   ├── player_idle.png
│   └── player_walk.png
├── videos/                      ← A FUTURO: Cinemáticas WebM
│   └── intro.webm
└── particles/                   ← OPCIONAL: Efectos de partículas
    └── menu_bg.p
```

### 5.2 Estrategia de carga
1. **Inicio:** Cargar Skin, fuentes y música del menú.
2. **Menú principal:** Cargar retratos y datos narrativos (ya eager-loaded).
3. **GameScreen:** Cargar mapa Tiled y spritesheet del jugador.
4. **Bajo demanda:** Videos y efectos de partículas.

---

## 6. Escalabilidad y Extensibilidad

### 6.1 Para añadir nuevos tipos de nodo
Extender `NarrativeNode`:
```java
public class ExplorationNode extends NarrativeNode {
    private String mapId;
    private float playerX, playerY;
    @Override
    public String getNextTargetId() { return null; }
}
```

### 6.2 Para añadir nuevos estados de UI
Agregar al enum `UIState`:
```java
EXPLORANDO,    // caminando por el mapa
INVENTARIO,    // viendo items (futuro)
MINIJUEGO,     // en un minijuego (futuro)
CONFIGURACION  // ajustes
```

### 6.3 Para soporte multi-idioma
- Los textos ya están externalizados en SQLite/JSON.
- Agregar columna `locale` a las tablas o archivos separados.
- Pasar locale al DAO.

### 6.4 Para futuro móvil (Android/iOS)
- La arquitectura libGDX ya es multiplataforma.
- Solo cambiar backend: `gdx-backend-android` + `gdx-backend-lwjgl3`.
- Scene2D maneja táctil automáticamente.

---

## 7. Riesgos y Mitigaciones

| Riesgo | Probabilidad | Impacto | Mitigación |
|--------|-------------|---------|------------|
| `Scanner` no funciona en game loop | Alta (ya ocurre) | Alto | Refactor urgente a input por teclas en `render()` (Fase 0.1) |
| gdx-video no compila | Media | Alto | Probar con WebM básico; tener fallback con imágenes estáticas |
| Transición abrupta entre screens | Media | Medio | Usar `BaseScreen` con transiciones fade |
| Skins no se ven bien con pixel art | Baja | Medio | Usar kenney-pixel, probar temprano |
| Colisiones Tiled sin Box2D es limitado | Media | Medio | Implementar tile-based primero, migrar a Box2D si necesario |
| Pérdida de estado entre screens | Baja | Alto | Guardar posición y estado en `Main.java` antes de cambiar screen |
| Carga sincrónica de texturas congela el juego | Alta | Medio | Usar `AssetManager` con load screen intermedio |
| La música no se detiene al cambiar de screen | Media | Bajo | `MusicManager` singleton con `stopAll()` en cada cambio |
| Typewriter effect bloquea input | Baja | Medio | Solo aplicar si `!isFinished()`; opciones aparecen al terminar |
| Regresión en lógica narrativa | Alta | Crítico | Tests unitarios en StoryManager antes de tocar screens |

---

## 8. Cronograma Estimado

| Fase | Descripción | Días (parcial) | Depende de |
|------|-------------|----------------|------------|
| 0 | Infraestructura | 2-3 | - |
| 1 | MainMenu visual | 2-3 | Fase 0.1, 0.2 |
| 2 | DialogOverlay | 4-5 | Fase 1 |
| 3 | GameScreen + Tiled | 5-7 | Fase 2 |
| 4 | Cinemáticas | 2-3 | Fase 2 |
| 5 | Audio | 1-2 | Fase 2 |
| 6 | Pulido | 2-3 | Fase 3, 4, 5 |
| 7 | Testing | 2-3 | Fase 0-6 |

**Total estimado: 4-6 semanas (trabajo parcial).**

---

## 9. Checklist Final de Entregables

- [ ] FASE 0: Input refactorizado a teclas, sin Scanner
- [ ] FASE 0: Skin Scene2D instalado y probado
- [ ] FASE 0: FreeType fonts configurados
- [ ] FASE 1: MainMenu con botones, fondo, fade in
- [ ] FASE 1: Botón Continuar visible solo si hay save
- [ ] FASE 2: Diálogo con retrato, nombre, texto, opciones
- [ ] FASE 2: Typewriter effect
- [ ] FASE 2: Filtro de opciones por flags
- [ ] FASE 2: PauseMenu con guardar/cargar/salir
- [ ] FASE 2: Música por nodo
- [ ] FASE 3: Mapa Tiled renderizado
- [ ] FASE 3: Cámara sigue al jugador
- [ ] FASE 3: Movimiento WASD con animación
- [ ] FASE 3: Colisiones tile-based
- [ ] FASE 3: Triggers narrativos del mapa
- [ ] FASE 3: Diálogo superpuesto sobre mapa
- [ ] FASE 4: Reproducción de video WebM
- [ ] FASE 5: Música ambiental y SFX
- [ ] FASE 6: Transiciones fade entre screens
- [ ] FASE 6: Efectos visuales (partículas, etc.)
- [ ] FASE 7: Tests unitarios de StoryManager
- [ ] FASE 7: Tests de persistencia
- [ ] Sin regresión en la lógica narrativa original
- [ ] `./gradlew lwjgl3:run` funciona en cada fase
