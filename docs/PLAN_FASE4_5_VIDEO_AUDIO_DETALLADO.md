# Plan de Implementación: FASE 4 (Video y Cinemáticas) + FASE 5 (Sistema de Audio)
## Proyecto: INTENTIA — libGDX 1.13.1 / LWJGL3 Desktop

---

## Tabla de Contenidos

1. [PARTE 1: VIDEO Y CINEMÁTICAS](#parte-1-video-y-cinemáticas)
   - 1.1 [Investigación de gdx-video](#11-investigación-de-gdx-video)
   - 1.2 [Dependencias en build.gradle](#12-dependencias-en-buildgradle)
   - 1.3 [Alternativas si gdx-video falla](#13-alternativas-si-gdx-video-falla)
   - 1.4 [CinematicScreen — Código completo](#14-cinematicscreen--código-completo)
   - 1.5 [Video con Transparencia (Canal Alpha)](#15-video-con-transparencia-canal-alpha)
   - 1.6 [Integración Narrativa](#16-integración-narrativa)
   - 1.7 [Codificación de videos con FFmpeg](#17-codificación-de-videos-con-ffmpeg)
2. [PARTE 2: SISTEMA DE AUDIO](#parte-2-sistema-de-audio)
   - 2.1 [MusicManager — Código completo](#21-musicmanager--código-completo)
   - 2.2 [FadeMusicAction — Código completo](#22-fademusicaction--código-completo)
   - 2.3 [SoundManager — Código completo](#23-soundmanager--código-completo)
   - 2.4 [Integración con diálogos](#24-integración-con-diálogos)
   - 2.5 [Assets de audio necesarios](#25-assets-de-audio-necesarios)
3. [MEJORES PRÁCTICAS](#mejores-prácticas)
4. [CHECKLIST DE VERIFICACIÓN](#checklist-de-verificación)
5. [REFERENCIAS](#referencias)

---

# PARTE 1: VIDEO Y CINEMÁTICAS

---

## 1.1 Investigación de gdx-video

### Estado actual

| Aspecto | Dato |
|---------|------|
| Última versión | **1.3.4** (publicada 30 Dic 2025) |
| Compatibilidad con libGDX | **1.13.0 y 1.13.1** (fix: Compatibility with libGDX 1.13.0 and RawMusic en PR #107) |
| Estado del proyecto | **Activo** — mantenido por el equipo oficial de libGDX (SimonIT, Tom-Ski) |
| Licencia | Apache 2.0 |
| Maven Central | Disponible en `com.badlogicgames.gdx-video:gdx-video:1.3.4` |

### Plataformas soportadas

| Plataforma | Artifact | Estado |
|------------|----------|--------|
| Desktop LWJGL3 | `gdx-video-lwjgl3` | ✅ |
| Desktop LWJGL2 (legacy) | `gdx-video-lwjgl` | ✅ |
| Android | `gdx-video-android` | ✅ |
| iOS (RoboVM) | `gdx-video-robovm` | ✅ |
| HTML (GWT) | `gdx-video-gwt` | ⚠️ Limitado |
| TeaVM | `gdx-video-teavm` | Nuevo en 1.3.4 |

### Formatos de video soportados (Desktop)

| Formato | Codec | Compatibilidad | Notas |
|---------|-------|----------------|-------|
| WebM | VP8 + Vorbis | ✅ Excelente | **RECOMENDADO** para Intentia |
| WebM | VP9 + Opus | ✅ Excelente | Mejor calidad, más CPU |
| WebM | VP8 + alpha (yuva420p) | ✅ Buena | **Soporte nativo de transparencia** |
| WebM | VP9 + alpha | ✅ Buena | Con `-pix_fmt yuva420p` |
| MKV | AV1 + Opus | ✅ | Requiere dav1d (incluido en 1.3.3+) |
| MP4 | H.264 | ❌ No nativo | Requiere recompilar FFmpeg |
| MP4 | H.265/HEVC | ❌ No nativo | Requiere recompilar FFmpeg |

### API pública principal

```java
// Paquete: com.badlogic.gdx.video

// Creación
VideoPlayer player = VideoPlayerCreator.createVideoPlayer();

// Ciclo de vida
player.load(FileHandle file);                // Cargar video
player.play();                                // Reproducir
player.pause();                               // Pausar
player.resume();                              // Reanudar
player.stop();                                // Detener
player.update();                              // Llamar cada frame
player.dispose();                             // Liberar recursos

// Consultas
player.isBuffered();                          // ¿Video cargado completamente?
player.getTexture();                          // Obtener frame actual como Texture
player.getVideoWidth();                       // Ancho del video
player.getVideoHeight();                      // Alto del video
player.isFinished();                          // ¿Terminó?
player.getCurrentTime();                      // Tiempo actual (ms)
player.getDuration();                         // Duración total (ms)

// Control
player.setOnCompletionCallback(Consumer<VideoPlayer> callback);
                                             // Callback al terminar
player.setLooping(boolean);                   // Loop infinito
player.setVolume(float);                      // Volumen 0.0-1.0
player.setFilter(Texture.TextureFilter);      // Filtro de textura

// Scene2D
VideoActor actor = new VideoActor();
actor.setVideoPlayer(player);
actor.setFillType(VideoActor.FillType.FIT);   // FIT, FILL, STRETCH, NONE
```

### VideoActor para Scene2D

La clase `VideoActor` (Scene2D) permite incrustar el video dentro de la jerarquía visual del Stage. Soporta:

- `FillType.FIT` — mantiene aspect ratio, agrega letterboxing
- `FillType.FILL` — llena el área, recorta
- `FillType.STRETCH` — distorsiona para llenar
- `FillType.NONE` — tamaño original

---

## 1.2 Dependencias en build.gradle

### gradle.properties — Agregar:

```properties
gdxVideoVersion=1.3.4
```

### core/build.gradle — Agregar:

```groovy
dependencies {
  // ... existentes
  api "com.badlogicgames.gdx-video:gdx-video:$gdxVideoVersion"
}
```

### lwjgl3/build.gradle — Agregar:

```groovy
dependencies {
  // ... existentes
  implementation "com.badlogicgames.gdx-video:gdx-video-lwjgl3:$gdxVideoVersion"
}
```

### Verificar que el proxy Maven (si existe) tenga acceso a:

```
https://oss.sonatype.org/content/repositories/releases
https://oss.sonatype.org/content/repositories/snapshots
```

---

## 1.3 Alternativas si gdx-video falla

### Estrategia de degradación (fallback chain):

```
gdx-video → ImageSequence (Animation<TextureRegion>) → StillImage estática
```

### Alternativa 1: ImageSequence (Animation<TextureRegion>)

Para clips cortos (<10s). Se pre-renderizan frames del video como PNGs y se cargan como animación.

```java
public class ImageSequenceCinematicScreen implements Screen {
    private Animation<TextureRegion> animacion;
    private float stateTime;
    private Stage stage;
    private Image imageActor;
    private Main game;
    private String videoId;
    private Screen siguienteScreen;

    public ImageSequenceCinematicScreen(Main game, String videoId, Screen siguiente,
                                         String atlasPath, float frameDuration) {
        this.game = game;
        this.videoId = videoId;
        this.siguienteScreen = siguiente;
        TextureAtlas atlas = new TextureAtlas(Gdx.files.internal(atlasPath));
        animacion = new Animation<>(frameDuration, atlas.getRegions());
    }

    @Override
    public void show() {
        stage = new Stage(new FitViewport(1280, 720));
        imageActor = new Image(animacion.getKeyFrame(0));
        imageActor.setFillParent(true);
        imageActor.setScaling(Scaling.fit);
        stage.addActor(imageActor);
        stateTime = 0;
    }

    @Override
    public void render(float delta) {
        stateTime += delta;
        if (animacion.isAnimationFinished(stateTime)) {
            game.setScreen(siguienteScreen);
            return;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)
            || Gdx.input.isKeyJustPressed(Input.Keys.ENTER)
            || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            game.setScreen(siguienteScreen);
            return;
        }
        imageActor.setDrawable(new TextureRegionDrawable(
            animacion.getKeyFrame(stateTime)));
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.act(delta);
        stage.draw();
    }

    @Override public void resize(int w, int h) { stage.getViewport().update(w, h, true); }
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
    @Override public void dispose() { stage.dispose(); }
}
```

**Pros:** No requiere gdx-video, funciona en todas las plataformas.
**Contras:** Ocupa mucho espacio en disco. Solo viable para clips <5s.

### Alternativa 2: StillImage estática (fallback último)

```java
public class StaticCinematicScreen implements Screen {
    private Stage stage;
    private Main game;
    private Screen siguienteScreen;

    public StaticCinematicScreen(Main game, Screen siguiente, String imagePath) {
        this.game = game;
        this.siguienteScreen = siguiente;
        Image img = new Image(new Texture(Gdx.files.internal(imagePath)));
        img.setFillParent(true);
        img.setScaling(Scaling.fit);
        stage = new Stage(new FitViewport(1280, 720));
        stage.addActor(img);
    }

    @Override
    public void render(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)
            || Gdx.input.isKeyJustPressed(Input.Keys.ENTER)
            || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            game.setScreen(siguienteScreen);
            return;
        }
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.act(delta);
        stage.draw();
    }

    @Override public void show() {}
    @Override public void resize(int w, int h) { stage.getViewport().update(w, h, true); }
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
    @Override public void dispose() { stage.dispose(); }
}
```

### Alternativa 3: vlcj (VLC Java bindings) — NO RECOMENDADO

```java
// Requiere: uk.co.caprica:vlcj:4.x
// Requiere: VLC instalado en el sistema
// Problemas: No es multiplataforma puro, dependencia externa pesada,
//            conflicto con el game loop de libGDX (ventana separada o embedding nativo)
```

**Veredicto:** No usar vlcj. Usar gdx-video como solución primaria.

---

## 1.4 CinematicScreen — Código completo

```java
package io.intentia.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.video.VideoPlayer;
import com.badlogic.gdx.video.VideoPlayerCreator;

import io.intentia.Main;

public class CinematicScreen implements Screen {

    private Main game;
    private VideoPlayer videoPlayer;
    private Texture currentFrame;
    private Stage stage;
    private Skin skin;
    private SpriteBatch batch;

    private String videoPath;
    private Screen siguienteScreen;

    private boolean skipAvailable = true;
    private boolean finished = false;
    private boolean transicionando = false;

    private float tiempoMinimoSkip = 1.5f;
    private float timer = 0f;

    private static final float FADE_DURATION = 0.5f;

    public CinematicScreen(Main game, String videoPath, Screen siguienteScreen, Skin skin) {
        this.game = game;
        this.videoPath = videoPath;
        this.siguienteScreen = siguienteScreen;
        this.skin = skin;
    }

    @Override
    public void show() {
        batch = new SpriteBatch();
        stage = new Stage(new FitViewport(1280, 720));
        Gdx.input.setInputProcessor(stage);

        videoPlayer = VideoPlayerCreator.createVideoPlayer();

        FileHandle videoFile = Gdx.files.internal("videos/" + videoPath);
        if (!videoFile.exists()) {
            Gdx.app.error("CinematicScreen", "Video no encontrado: " + videoFile.path());
            game.setScreen(siguienteScreen);
            return;
        }

        videoPlayer.setOnCompletionCallback(vp -> {
            finished = true;
        });

        videoPlayer.load(videoFile);
        videoPlayer.play();

        if (skipAvailable) {
            TextButton btnSkip = new TextButton("SALTAR [ESC]", skin);
            btnSkip.setPosition(stage.getWidth() - btnSkip.getWidth() - 20,
                                stage.getHeight() - btnSkip.getHeight() - 20);
            btnSkip.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                    skip();
                }
            });
            stage.addActor(btnSkip);
        }
    }

    @Override
    public void render(float delta) {
        if (transicionando) return;

        timer += delta;

        if (videoPlayer != null) {
            videoPlayer.update();
            currentFrame = videoPlayer.getTexture();
        }

        if (!finished && videoPlayer != null && videoPlayer.isFinished()) {
            finished = true;
        }

        if (finished && timer >= FADE_DURATION) {
            iniciarTransicion();
            return;
        }

        if (skipAvailable && timer >= tiempoMinimoSkip) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)
                || Gdx.input.isKeyJustPressed(Input.Keys.ENTER)
                || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
                skip();
                return;
            }
        }

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        if (currentFrame != null) {
            batch.begin();
            float scaleX = (float) Gdx.graphics.getWidth() / currentFrame.getWidth();
            float scaleY = (float) Gdx.graphics.getHeight() / currentFrame.getHeight();
            float scale = Math.max(scaleX, scaleY);
            float w = currentFrame.getWidth() * scale;
            float h = currentFrame.getHeight() * scale;
            float x = (Gdx.graphics.getWidth() - w) / 2f;
            float y = (Gdx.graphics.getHeight() - h) / 2f;
            batch.draw(currentFrame, x, y, w, h);
            batch.end();
        }

        stage.act(delta);
        stage.draw();
    }

    private void skip() {
        if (transicionando) return;
        if (videoPlayer != null) {
            videoPlayer.stop();
        }
        finished = true;
        iniciarTransicion();
    }

    private void iniciarTransicion() {
        if (transicionando) return;
        transicionando = true;

        stage.addAction(Actions.sequence(
            Actions.fadeOut(FADE_DURATION),
            Actions.run(() -> {
                game.setScreen(siguienteScreen);
            })
        ));
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void pause() {
        if (videoPlayer != null) videoPlayer.pause();
    }

    @Override
    public void resume() {
        if (videoPlayer != null) videoPlayer.resume();
    }

    @Override
    public void hide() {
        dispose();
    }

    @Override
    public void dispose() {
        if (videoPlayer != null) {
            videoPlayer.stop();
            videoPlayer.dispose();
            videoPlayer = null;
        }
        if (stage != null) {
            stage.dispose();
            stage = null;
        }
        if (batch != null) {
            batch.dispose();
            batch = null;
        }
        currentFrame = null;
    }
}
```

### Uso desde Main.java (ejemplo):

```java
Screen intro = new CinematicScreen(
    this,
    "intro_premonicion.webm",
    new DialogOverlayScreen(this, storyManager, skin),
    skin
);
setScreen(intro);
```

---

## 1.5 Video con Transparencia (Canal Alpha)

### El "Truco del Dragón" — Técnica completa

La técnica consiste en reproducir un video WebM con canal alpha (yuva420p) superpuesto sobre el mapa Tiled y el jugador, creando la ilusión de un elemento 3D volumétrico (dragón, magia, flash) dentro del mundo 2D pixel art.

### Requisitos técnicos

1. **Video:** WebM con pix_fmt `yuva420p` (VP8 o VP9)
2. **gdx-video:** Devuelve la textura en formato RGBA (el canal A contiene la transparencia)
3. **OpenGL:** Blending activado (`GL_BLEND`)
4. **Orden de renderizado:** Fondo → Jugador → Video Alpha → UI

### GameScreen con superposición de video alpha

```java
package io.intentia.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.badlogic.gdx.video.VideoPlayer;
import com.badlogic.gdx.video.VideoPlayerCreator;

public class GameScreenConVideoAlpha implements Screen {

    private OrthographicCamera camara;
    private Viewport viewport;
    private SpriteBatch batch;
    private TiledMap mapa;
    private OrthogonalTiledMapRenderer rendererMapa;
    private ShapeRenderer shapeRenderer;

    private VideoPlayer videoAlpha;
    private Texture frameVideoAlpha;

    private float playerX = 100, playerY = 100;
    private Texture texturaJugador;

    private boolean videoActivo = false;
    private float duracionVideo = 5f;
    private float temporizadorVideo = 0f;

    private static final float UNIT_SCALE = 1f / 16f;

    @Override
    public void show() {
        camara = new OrthographicCamera();
        viewport = new FitViewport(800, 600, camara);
        batch = new SpriteBatch();

        mapa = new TmxMapLoader().load("TileSet/Tiled/Tilemaps/Beginning Fields.tmx");
        rendererMapa = new OrthogonalTiledMapRenderer(mapa, UNIT_SCALE);

        shapeRenderer = new ShapeRenderer();

        texturaJugador = new Texture(Gdx.files.internal("sprites/player_idle.png"));

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    }

    public void activarVideoAlpha(String rutaVideo) {
        if (videoAlpha != null) {
            videoAlpha.stop();
            videoAlpha.dispose();
        }
        videoAlpha = VideoPlayerCreator.createVideoPlayer();
        FileHandle videoFile = Gdx.files.internal(rutaVideo);
        if (videoFile.exists()) {
            videoAlpha.load(videoFile);
            videoAlpha.play();
            videoAlpha.setLooping(false);
            videoActivo = true;
            temporizadorVideo = 0f;
        }
    }

    @Override
    public void render(float delta) {
        if (videoActivo && videoAlpha != null) {
            videoAlpha.update();
            frameVideoAlpha = videoAlpha.getTexture();
            temporizadorVideo += delta;
            if (videoAlpha.isFinished() || temporizadorVideo >= duracionVideo) {
                videoActivo = false;
                if (videoAlpha != null) {
                    videoAlpha.dispose();
                    videoAlpha = null;
                }
                frameVideoAlpha = null;
            }
        }

        camara.position.set(playerX, playerY, 0);
        camara.update();

        Gdx.gl.glClearColor(0.05f, 0.05f, 0.1f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // CAPA 1: Fondo y mapa Tiled
        rendererMapa.setView(camara);
        rendererMapa.render();

        // CAPA 2: Jugador
        batch.setProjectionMatrix(camara.combined);
        batch.begin();
        batch.draw(texturaJugador, playerX, playerY, 32, 32);
        batch.end();

        // CAPA 3: Video con canal alpha (El "Truco del Dragón")
        if (frameVideoAlpha != null) {
            batch.begin();
            Gdx.gl.glEnable(GL20.GL_BLEND);
            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

            float videoX = playerX - 64;
            float videoY = playerY + 32;
            float videoW = 256;
            float videoH = 256;

            batch.draw(frameVideoAlpha, videoX, videoY, videoW, videoH);
            batch.end();
        }

        // CAPA 4: UI (Stage de diálogos, etc.) se dibujaría aquí
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void dispose() {
        rendererMapa.dispose();
        mapa.dispose();
        batch.dispose();
        shapeRenderer.dispose();
        texturaJugador.dispose();
        if (videoAlpha != null) videoAlpha.dispose();
    }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
}
```

### Shader GLSL para videos con chroma key (fallback si alpha no funciona)

Si el video no tiene canal alpha nativo pero usa chroma key (verde), se puede usar este fragment shader:

```glsl
// assets/shaders/chroma_key.frag
#ifdef GL_ES
precision mediump float;
#endif

varying vec2 v_texCoord;
uniform sampler2D u_texture;

void main() {
    vec4 color = texture2D(u_texture, v_texCoord);

    // Detectar verde chroma: (g > 0.7 && r < 0.3 && b < 0.3)
    float umbral = 0.7;
    if (color.g > umbral && color.r < 0.3 && color.b < 0.3) {
        discard; // Hacer transparente
    }

    gl_FragColor = color;
}
```

```java
ShaderProgram chromaShader = new ShaderProgram(
    Gdx.files.internal("shaders/default.vert"),
    Gdx.files.internal("shaders/chroma_key.frag")
);
batch.setShader(chromaShader);
// ... dibujar video ...
batch.setShader(null); // restaurar
```

### FBO (FrameBufferObject) para efectos avanzados

Para composiciones más complejas (ej: video alpha recortado con forma circular para "talking head"):

```java
FrameBuffer fbo = new FrameBuffer(Pixmap.Format.RGBA8888, 256, 256, false);
fbo.begin();
// Renderizar video alpha en el FBO
batch.draw(frameVideoAlpha, 0, 0, 256, 256);
fbo.end();

Texture texturaFBO = fbo.getColorBufferTexture();
// Ahora texturaFBO se puede usar con cualquier shape/máscara
fbo.dispose();
```

---

## 1.6 Integración Narrativa

### Extensión de NarrativeNode

```java
package io.intentia.models;

public abstract class NarrativeNode {
    private String id;
    private String speakerId;
    private String text;
    private boolean esRaiz;
    private String musicTrack;

    // NUEVOS CAMPOS PARA VIDEO
    private String videoPath;      // "intro_premonicion.webm" o null
    private String videoType;      // "cutscene" | "transition" | "flash" | "alpha_overlay"
    private String videoNextNodeId; // Nodo al que avanzar después del video (opcional)

    // Getters y setters
    public String getVideoPath() { return videoPath; }
    public void setVideoPath(String videoPath) { this.videoPath = videoPath; }

    public String getVideoType() { return videoType; }
    public void setVideoType(String videoType) { this.videoType = videoType; }

    public String getVideoNextNodeId() { return videoNextNodeId; }
    public void setVideoNextNodeId(String videoNextNodeId) { this.videoNextNodeId = videoNextNodeId; }

    public boolean tieneVideo() { return videoPath != null && !videoPath.isEmpty(); }

    public String getMusicTrack() { return musicTrack; }
    public void setMusicTrack(String musicTrack) { this.musicTrack = musicTrack; }
}
```

### StoryManager — disparo de cinemáticas

```java
package io.intentia.logic;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import io.intentia.Main;
import io.intentia.models.GameState;
import io.intentia.models.NarrativeNode;
import io.intentia.screens.CinematicScreen;

public class StoryManager {

    private Main game;
    private GameState gameState;
    private NarrativeNode currentNode;
    private Screen dialogScreen;

    public StoryManager(Main game, GameState gameState) {
        this.game = game;
        this.gameState = gameState;
    }

    public NarrativeNode getCurrentNode() {
        return currentNode;
    }

    /**
     * Avanza al siguiente nodo. Si el nodo actual tiene video y no se ha visto,
     * redirige a la CinematicScreen primero.
     */
    public void advance(String optionId) {
        NarrativeNode nextNode = obtenerNodoSiguiente(currentNode, optionId);

        if (nextNode == null) {
            Gdx.app.log("StoryManager", "No hay siguiente nodo, fin de la historia.");
            return;
        }

        // Verificar si hay video pendiente
        if (nextNode.tieneVideo() && !gameState.hasFlag("visto_" + nextNode.getId())) {
            gameState.addFlag("visto_" + nextNode.getId());
            String videoId = nextNode.getId();

            Screen cinematicScreen = new CinematicScreen(
                game,
                nextNode.getVideoPath(),
                dialogScreen,
                null // skin se obtendría del GameState o Main
            );
            game.setScreen(cinematicScreen);
            return;
        }

        // Avance normal
        currentNode = nextNode;
        notificarCambioNodo();
    }

    /**
     * Verifica si el nodo actual requiere mostrar un video transparente
     * mientras se muestra el diálogo (videoType = "alpha_overlay").
     */
    public boolean requiereVideoAlpha() {
        return currentNode != null
            && "alpha_overlay".equals(currentNode.getVideoType())
            && !gameState.hasFlag("visto_" + currentNode.getId());
    }

    public String getVideoAlphaPath() {
        if (requiereVideoAlpha()) {
            return currentNode.getVideoPath();
        }
        return null;
    }

    private NarrativeNode obtenerNodoSiguiente(NarrativeNode actual, String optionId) {
        // Lógica real: consultar DAO
        return null;
    }

    private void notificarCambioNodo() {
        // Actualizar UI, música, etc.
    }

    public void marcarVideoVisto(String nodeId) {
        gameState.addFlag("visto_" + nodeId);
    }
}
```

### Trigger desde mapa Tiled

```java
// En GameScreen, detectar colisión con trigger
public void checkTriggers() {
    for (MapObject objeto : capaTriggers.getObjects()) {
        if (objeto instanceof RectangleMapObject) {
            Rectangle rect = ((RectangleMapObject) objeto).getRectangle();
            Rectangle playerRect = new Rectangle(playerX, playerY, 32, 32);

            if (playerRect.overlaps(rect)) {
                String type = objeto.getProperties().get("type", String.class);
                String videoPath = objeto.getProperties().get("videoPath", String.class);
                String oneShot = objeto.getProperties().get("oneShot", String.class);

                if ("cinematic".equals(type) && videoPath != null) {
                    if ("true".equals(oneShot)) {
                        String flagId = "trigger_" + objeto.getName();
                        if (gameState.hasFlag(flagId)) continue;
                        gameState.addFlag(flagId);
                    }

                    game.setScreen(new CinematicScreen(
                        game, videoPath, this, skin
                    ));
                }
            }
        }
    }
}
```

---

## 1.7 Codificación de videos con FFmpeg

### WebM con canal alpha (VP8)

```bash
ffmpeg -i input.mov -c:v libvpx -pix_fmt yuva420p -b:v 2M \
  -auto-alt-ref 0 -vf "scale=1920:1080:flags=neighbor" \
  -r 24 -an output_alpha.webm
```

### WebM sin alpha (VP8 + Vorbis)

```bash
ffmpeg -i input.mov -c:v libvpx -c:a libvorbis -b:v 2M \
  -b:a 128k -r 24 -vf "scale=1920:1080:flags=neighbor" \
  output.webm
```

### WebM con alpha (VP9 — mejor calidad)

```bash
ffmpeg -i input.mov -c:v libvpx-vp9 -pix_fmt yuva420p -b:v 1M \
  -cpu-used 0 -r 24 -vf "scale=1920:1080:flags=neighbor" \
  output_alpha_vp9.webm
```

### Extraer frames para ImageSequence fallback

```bash
ffmpeg -i input.webm -vf "fps=24" frames/frame_%04d.png
# Luego crear TextureAtlas con gdx-texturepacker o similar
```

### Especificaciones recomendadas para Intentia

| Uso | Resolución | FPS | Bitrate | Alpha | Formato |
|-----|-----------|-----|---------|-------|---------|
| Cinemática inicial (Profecía Pitof) | 1920×1080 | 24 | 2Mbps | No | WebM VP8+Vorbis |
| Flash Verde (transición) | 512×288 | 24 | 500Kbps | Sí | WebM VP8 yuva420p |
| Dragón alpha overlay | 256×256 | 24 | 500Kbps | Sí | WebM VP8 yuva420p |
| Cinemáticas menores | 1280×720 | 24 | 1Mbps | No | WebM VP8+Vorbis |

---

# PARTE 2: SISTEMA DE AUDIO

---

## 2.1 MusicManager — Código completo

```java
package io.intentia.managers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;

public class MusicManager {

    private static MusicManager instancia;

    private Music currentMusic;
    private String currentTrackId;
    private float volume = 0.7f;
    private boolean transitioning = false;

    private float fadeDuration = 1.0f;
    private float fadeTimer = 0f;
    private float fadeFrom = 0f;
    private float fadeTo = 0f;
    private boolean fadingIn = false;
    private boolean fadingOut = false;

    private Music pendingMusic;
    private String pendingTrackId;

    private MusicManager() {}

    public static MusicManager getInstance() {
        if (instancia == null) {
            instancia = new MusicManager();
        }
        return instancia;
    }

    /**
     * Reproduce una pista musical. Si ya está sonando la misma, no hace nada.
     * Si hay otra pista, hace fade out de la actual y luego fade in de la nueva.
     */
    public void play(String trackId) {
        if (trackId == null || trackId.isEmpty()) {
            stop();
            return;
        }

        if (currentTrackId != null && currentTrackId.equals(trackId)
            && currentMusic != null && currentMusic.isPlaying()) {
            return;
        }

        Music nuevaMusica = Gdx.audio.newMusic(
            Gdx.files.internal("music/" + trackId));

        if (nuevaMusica == null) {
            Gdx.app.error("MusicManager", "No se pudo cargar: music/" + trackId);
            return;
        }

        nuevaMusica.setLooping(true);
        nuevaMusica.setVolume(0f);

        if (currentMusic != null && currentMusic.isPlaying()) {
            pendingMusic = nuevaMusica;
            pendingTrackId = trackId;
            iniciarFadeOut();
        } else {
            if (currentMusic != null) {
                currentMusic.stop();
                currentMusic.dispose();
            }
            currentMusic = nuevaMusica;
            currentTrackId = trackId;
            iniciarFadeIn();
        }
    }

    /**
     * Detiene la música actual con fade out.
     */
    public void stop() {
        if (currentMusic != null && currentMusic.isPlaying()) {
            iniciarFadeOut();
            pendingMusic = null;
            pendingTrackId = null;
        }
    }

    /**
     * Establece el volumen global (0.0 a 1.0).
     */
    public void setVolume(float vol) {
        this.volume = MathUtils.clamp(vol, 0f, 1f);
        if (currentMusic != null && !transitioning) {
            currentMusic.setVolume(this.volume);
        }
    }

    public float getVolume() {
        return volume;
    }

    public String getCurrentTrackId() {
        return currentTrackId;
    }

    public boolean isPlaying() {
        return currentMusic != null && currentMusic.isPlaying();
    }

    /**
     * Actualiza las transiciones de fade in/out. Llamar desde el game loop.
     */
    public void update(float delta) {
        if (!transitioning) return;

        fadeTimer += delta;
        float progress = MathUtils.clamp(fadeTimer / fadeDuration, 0f, 1f);

        if (fadingOut && currentMusic != null) {
            float targetVol = MathUtils.lerp(fadeFrom, 0f, progress);
            currentMusic.setVolume(targetVol);

            if (progress >= 1f) {
                currentMusic.stop();
                currentMusic.dispose();
                currentMusic = null;
                currentTrackId = null;
                fadingOut = false;

                if (pendingMusic != null) {
                    currentMusic = pendingMusic;
                    currentTrackId = pendingTrackId;
                    pendingMusic = null;
                    pendingTrackId = null;
                    iniciarFadeIn();
                } else {
                    transitioning = false;
                }
            }
        }

        if (fadingIn && currentMusic != null) {
            float targetVol = MathUtils.lerp(0f, volume, progress);
            currentMusic.setVolume(targetVol);
            currentMusic.play();

            if (progress >= 1f) {
                currentMusic.setVolume(volume);
                fadingIn = false;
                transitioning = false;
            }
        }
    }

    private void iniciarFadeOut() {
        if (currentMusic == null) return;
        transitioning = true;
        fadingOut = true;
        fadingIn = false;
        fadeTimer = 0f;
        fadeFrom = currentMusic.getVolume();
    }

    private void iniciarFadeIn() {
        if (currentMusic == null) return;
        transitioning = true;
        fadingIn = true;
        fadingOut = false;
        fadeTimer = 0f;
        currentMusic.setVolume(0f);
        currentMusic.play();
    }

    public void dispose() {
        stop();
        if (currentMusic != null) {
            currentMusic.dispose();
            currentMusic = null;
        }
        currentTrackId = null;
        instancia = null;
    }

    /**
     * Reinicia la instancia (útil al cambiar de partida).
     */
    public static void reiniciar() {
        if (instancia != null) {
            instancia.dispose();
        }
        instancia = new MusicManager();
    }

    // MathUtils no está disponible en el core sin dependencia.
    // Usamos un clamp simple inline.
    private static class MathUtils {
        static float clamp(float value, float min, float max) {
            return Math.max(min, Math.min(max, value));
        }
        static float lerp(float a, float b, float t) {
            return a + (b - a) * t;
        }
    }
}
```

### Uso en Screens:

```java
// En MainMenuScreen.show():
MusicManager.getInstance().play("menu.ogg");

// En DialogOverlayScreen.mostrarNodoActual():
String track = currentNode.getMusicTrack();
MusicManager.getInstance().play(track);

// En el game loop (por ejemplo en Main.render()):
MusicManager.getInstance().update(delta);
```

---

## 2.2 FadeMusicAction — Código completo

```java
package io.intentia.managers;

import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction;

/**
 * TemporalAction de Scene2D que fadea el volumen de un Music
 * desde un valor inicial a un valor final durante una duración.
 */
public class FadeMusicAction extends TemporalAction {

    private Music music;
    private float startVolume;
    private float endVolume;

    public FadeMusicAction(Music music, float endVolume, float duration) {
        super(duration);
        this.music = music;
        this.endVolume = endVolume;
    }

    @Override
    protected void begin() {
        this.startVolume = music.getVolume();
    }

    @Override
    protected void update(float percent) {
        float vol = startVolume + (endVolume - startVolume) * percent;
        music.setVolume(Math.max(0f, Math.min(1f, vol)));
    }

    @Override
    protected void end() {
        music.setVolume(endVolume);
        if (endVolume <= 0f) {
            music.stop();
        }
    }

    public static FadeMusicAction fadeOut(Music music, float duration) {
        return new FadeMusicAction(music, 0f, duration);
    }

    public static FadeMusicAction fadeIn(Music music, float duration) {
        return new FadeMusicAction(music, music.getVolume(), duration);
    }
}
```

### Uso con Scene2D:

```java
// Fade out de 1 segundo
music.addAction(FadeMusicAction.fadeOut(music, 1f));

// Fade in de 0.5 segundos
music.setVolume(0f);
music.play();
music.addAction(FadeMusicAction.fadeIn(music, 0.5f));
```

---

## 2.3 SoundManager — Código completo

```java
package io.intentia.managers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.utils.Disposable;

import java.util.HashMap;
import java.util.Map;

public class SoundManager implements Disposable {

    private static SoundManager instancia;

    private Map<String, Sound> cache;
    private float volume = 0.8f;

    private SoundManager() {
        cache = new HashMap<>();
    }

    public static SoundManager getInstance() {
        if (instancia == null) {
            instancia = new SoundManager();
        }
        return instancia;
    }

    /**
     * Reproduce un efecto de sonido. Si no está en caché, lo carga y cachea.
     */
    public void play(String soundId) {
        Sound sound = cache.get(soundId);

        if (sound == null) {
            String path = "sfx/" + soundId;
            if (!path.endsWith(".wav") && !path.endsWith(".ogg")
                && !path.endsWith(".mp3")) {
                path += ".wav";
            }
            try {
                sound = Gdx.audio.newSound(Gdx.files.internal(path));
                cache.put(soundId, sound);
            } catch (Exception e) {
                Gdx.app.error("SoundManager", "No se pudo cargar sfx: " + path, e);
                return;
            }
        }

        sound.play(volume);
    }

    /**
     * Reproduce con control del identificador de instancia (útil para loops).
     * @return long El ID de la instancia de sonido.
     */
    public long playConControl(String soundId) {
        Sound sound = cache.get(soundId);
        if (sound == null) {
            String path = "sfx/" + soundId;
            if (!path.endsWith(".wav") && !path.endsWith(".ogg")
                && !path.endsWith(".mp3")) {
                path += ".wav";
            }
            try {
                sound = Gdx.audio.newSound(Gdx.files.internal(path));
                cache.put(soundId, sound);
            } catch (Exception e) {
                Gdx.app.error("SoundManager", "No se pudo cargar sfx: " + path, e);
                return -1;
            }
        }
        return sound.play(volume);
    }

    /**
     * Precarga sonidos que se usan frecuentemente.
     */
    public void preload(String... soundIds) {
        for (String id : soundIds) {
            if (!cache.containsKey(id)) {
                String path = "sfx/" + id;
                if (!path.endsWith(".wav")) path += ".wav";
                try {
                    Sound sound = Gdx.audio.newSound(Gdx.files.internal(path));
                    cache.put(id, sound);
                    Gdx.app.log("SoundManager", "Precargado: " + id);
                } catch (Exception e) {
                    Gdx.app.error("SoundManager", "Error precargando: " + id, e);
                }
            }
        }
    }

    public void stop(long soundInstanceId) {
        for (Sound sound : cache.values()) {
            sound.stop(soundInstanceId);
        }
    }

    public void stopAll() {
        for (Sound sound : cache.values()) {
            sound.stop();
        }
    }

    public void setVolume(float vol) {
        this.volume = Math.max(0f, Math.min(1f, vol));
    }

    public float getVolume() {
        return volume;
    }

    @Override
    public void dispose() {
        stopAll();
        for (Sound sound : cache.values()) {
            sound.dispose();
        }
        cache.clear();
        instancia = null;
    }
}
```

---

## 2.4 Integración con diálogos

### En DialogOverlayScreen.mostrarNodoActual():

```java
private void mostrarNodoActual() {
    NarrativeNode node = storyManager.getCurrentNode();
    if (node == null) return;

    // Cambiar música si el nodo especifica una pista
    String track = node.getMusicTrack();
    if (track != null && !track.isEmpty()) {
        MusicManager.getInstance().play(track);
    }

    // SFX: cambio de nodo
    SoundManager.getInstance().play("decision");

    // ... resto de la lógica de UI (texto, opciones, etc.)
}
```

### En botones de opciones:

```java
private TextButton crearBotonOpcion(DialogOption opcion) {
    TextButton btn = new TextButton(opcion.getText(), skin);

    btn.addListener(new ChangeListener() {
        @Override
        public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
            SoundManager.getInstance().play("click");
            storyManager.advance(opcion.getId());
            fadeOutYActualizar();
        }
    });

    btn.addListener(new com.badlogic.gdx.scenes.scene2d.InputListener() {
        @Override
        public void enter(com.badlogic.gdx.scenes.scene2d.InputEvent event,
                          float x, float y, int pointer, com.badlogic.gdx.scenes.scene2d.Actor fromActor) {
            SoundManager.getInstance().play("hover");
        }
    });

    return btn;
}
```

### En TrialNode (evaluación):

```java
private void mostrarResultadoTrial(boolean exito) {
    if (exito) {
        SoundManager.getInstance().play("success");
        labelResultado.setText("¡Prueba superada!");
        labelResultado.setColor(Color.GREEN);
    } else {
        SoundManager.getInstance().play("failure");
        labelResultado.setText("Prueba fallida...");
        labelResultado.setColor(Color.RED);
    }
}
```

### Typewriter effect con SFX:

```java
public class TypewriterAction extends TemporalAction {
    private Label label;
    private CharSequence textoCompleto;
    private int charIndex = 0;
    private float charDelay;
    private float charTimer = 0;

    public TypewriterAction(Label label, CharSequence textoCompleto,
                            float duration) {
        super(duration);
        this.label = label;
        this.textoCompleto = textoCompleto;
        this.charDelay = duration / textoCompleto.length();
    }

    @Override
    protected void update(float percent) {
        int targetIndex = (int) (percent * textoCompleto.length());
        while (charIndex < targetIndex) {
            charIndex++;
            if (charIndex % 3 == 0) { // beep cada 3 letras para no saturar
                SoundManager.getInstance().play("typewriter");
            }
        }
        label.setText(textoCompleto.subSequence(0, charIndex));
    }
}
```

---

## 2.5 Assets de audio necesarios

### Estructura de directorios

```
assets/
  music/
    menu.ogg              → Música del menú principal (loop)
    dialogo.ogg           → Música por defecto para diálogos (loop)
    bosque.ogg            → Música para exploración en el bosque (loop)
    misterio.ogg          → Música para momentos de tensión (loop)
    abuelo_taller.ogg     → Música del taller del abuelo (loop)
    cabana_lago.ogg       → Música de la cabaña junto al lago (loop)
    flashback.ogg         → Música para escenas de recuerdo (loop)

  sfx/
    click.wav             → Click de botón (corto, ~0.1s)
    hover.wav             → Hover sobre botón (corto, ~0.05s)
    typewriter.wav        → Sonido de letra apareciendo (beep retro, ~0.02s)
    decision.wav          → Al elegir opción de diálogo (corto, ~0.2s)
    success.wav           → Trial exitoso (subidón, ~1s)
    failure.wav           → Trial fallido (bajón, ~1s)
    page_flip.wav         → Cambio de nodo narrativo (~0.15s)
    magic_sparkle.wav     → Efecto mágico/transición (~0.5s)
    dragón_roar.wav       → Rugido de dragón (para overlay, ~2s)
    rain_ambient.wav      → Lluvia de fondo (loop opcional)
```

### Especificaciones técnicas

| Tipo | Formato | Sample Rate | Bits | Canales | Razón |
|------|---------|-------------|------|---------|-------|
| Música | **OGG** (Vorbis) | 44100 Hz | - | Estéreo | Compresión con calidad, streaming |
| SFX | **WAV** (PCM) | 44100 Hz | 16 | Mono | Baja latencia, sin compresión |
| SFX largos | **OGG** | 44100 Hz | - | Mono | SFX > 1s pueden comprimirse |

### Conversión desde el original

```bash
# Música: cualquier formato → OGG (calidad 5, ~128kbps)
ffmpeg -i input.wav -c:a libvorbis -q:a 5 -ar 44100 music/track.ogg

# SFX: cualquier formato → WAV 44100Hz 16bit mono
ffmpeg -i input.mp3 -acodec pcm_s16le -ar 44100 -ac 1 sfx/sonido.wav
```

---

# MEJORES PRÁCTICAS

### 1. Formatos de audio
- **Música:** Siempre OGG Vorbis (compresión ~90% vs WAV, calidad transparente a 128kbps)
- **SFX:** Siempre WAV 44100Hz 16bit mono (latencia cero, sin sobrecarga de decodificación)
- **Excepción:** SFX > 3 segundos pueden ser OGG para ahorrar espacio

### 2. Memoria y ciclo de vida
- Siempre `dispose()` de `Music` y `Sound` cuando no se usen
- MusicManager y SoundManager son singletons con `dispose()` global
- En cada `Screen.hide()` llamar a `MusicManager.getInstance().update(0)` para no dejar huerfanos
- Los `Sound` cacheados en SoundManager se liberan con `SoundManager.getInstance().dispose()`

### 3. Pooling de SFX
- SoundManager cachea los sonidos en un `HashMap<String, Sound>`
- Llamar `preload("click", "hover", "decision")` al inicio del juego
- Usar `play()` sin crear nuevos objetos Sound cada vez

### 4. Video fallback
```
gdx-video → ImageSequence → StillImage (en orden de prioridad)
```
- Detectar disponibilidad de gdx-video con try-catch en tiempo de construcción
- Si `VideoPlayerCreator.createVideoPlayer()` lanza excepción, degradar a ImageSequence

### 5. Sincronización audio en video
- El audio DENTRO del WebM (Vorbis/Opus) es más confiable que pistas separadas
- gdx-video sincroniza automáticamente audio y video por timestamps
- No usar pistas de música separadas para cinemáticas; codificar el audio en el WebM

### 6. Skip de cinemáticas
- Siemper esperar **1.5 segundos** mínimo antes de permitir skip
- Botón visible con texto "SALTAR [ESC]"
- Confirmación implícita: no preguntar "¿estás seguro?" (en cinemáticas iniciales sí podría)
- Marcar el video como "visto" aunque se salte

### 7. Control de volumen
- Volumen música: 0.7 por defecto (0.0 - 1.0)
- Volumen SFX: 0.8 por defecto
- Permitir configuración desde menú de pausa (guardar en preferencias)
- No mezclar tipos: MusicManager.setVolume() y SoundManager.setVolume() independientes

### 8. Prevención de solapamiento musical
- MusicManager.play() detecta si el mismo track ya está sonando → no reinicia
- Las transiciones fade in/out duran 1 segundo
- No pueden coexistir dos Music reproduciendo a la vez

---

# CHECKLIST DE VERIFICACIÓN

### FASE 4 — Video y Cinemáticas

- [ ] `gdxVideoVersion=1.3.4` agregado en `gradle.properties`
- [ ] dependencia `gdx-video:$gdxVideoVersion` en `core/build.gradle`
- [ ] dependencia `gdx-video-lwjgl3:$gdxVideoVersion` en `lwjgl3/build.gradle`
- [ ] `./gradlew lwjgl3:run` compila sin errores tras agregar dependencias
- [ ] `CinematicScreen.java` creado en `screens/`
- [ ] Reproduce video WebM correctamente (probar con video de prueba)
- [ ] Audio del video se escucha sincronizado
- [ ] Tecla ESC/ENTER/SPACE salta el video después de 1.5 segundos
- [ ] Transición fade out al terminar/saltear
- [ ] Skip button visible en pantalla
- [ ] Callback `setOnCompletionCallback` funciona y cambia de screen
- [ ] `dispose()` libera VideoPlayer, Stage, Batch
- [ ] Video con canal alpha se renderiza con blending correcto
- [ ] Orden de renderizado: fondo → jugador → video alpha → UI
- [ ] `NarrativeNode` extendido con campos `videoPath`, `videoType`, `videoNextNodeId`
- [ ] `StoryManager.advance()` redirige a CinematicScreen si hay video no visto
- [ ] Flag `"visto_<id>"` persiste en GameState
- [ ] Trigger desde mapa Tiled dispara cinemática
- [ ] Fallback ImageSequence funcional si gdx-video no está disponible
- [ ] Fallback StillImage funcional como último recurso
- [ ] Chroma key shader listo para videos sin alpha nativo

### FASE 5 — Audio

- [ ] `MusicManager.java` creado en `managers/`
- [ ] `play(trackId)` reproduce música desde `assets/music/`
- [ ] `play(trackId)` con mismo track no reinicia la música
- [ ] `stop()` detiene con fade out gradual
- [ ] `setVolume(float)` funciona en rango 0.0-1.0
- [ ] `update(delta)` maneja transiciones fade in/out
- [ ] `dispose()` libera recursos
- [ ] `FadeMusicAction.java` creado para transiciones Scene2D
- [ ] `SoundManager.java` creado en `managers/`
- [ ] `play(soundId)` reproduce SFX con carga lazy y cacheo
- [ ] `preload()` carga sonidos comunes al inicio
- [ ] Sonido de click en botones de diálogo
- [ ] Sonido de hover en botones de diálogo
- [ ] Sonido de typewriter en efecto de texto
- [ ] Sonido de decisión al cambiar de nodo
- [ ] Sonido de éxito/fallo en TrialNode
- [ ] Música cambia según `node.getMusicTrack()`
- [ ] `dispose()` en SoundManager libera todos los sonidos cacheados
- [ ] Assets de audio listos en `assets/music/` y `assets/sfx/`

### GENERAL

- [ ] MusicManager.update() llamado en el game loop
- [ ] Sin fugas de memoria (Audio dispose verificado)
- [ ] Volumen configurable desde menú de pausa
- [ ] `./gradlew lwjgl3:run` funciona con video y audio

---

# REFERENCIAS

### gdx-video (oficial)
- **Repositorio GitHub:** https://github.com/libgdx/gdx-video
- **Última versión:** https://github.com/libgdx/gdx-video/releases/tag/1.3.4
- **Maven Central:** https://search.maven.org/artifact/com.badlogicgames.gdx-video/gdx-video
- **Documentación README:** https://github.com/libgdx/gdx-video#readme

### libGDX
- **Sitio oficial:** https://libgdx.com
- **API de Audio (Music):** https://libgdx.badlogicgames.com/ci/nightlies/docs/api/com/badlogic/gdx/audio/Music.html
- **API de Audio (Sound):** https://libgdx.badlogicgames.com/ci/nightlies/docs/api/com/badlogic/gdx/audio/Sound.html
- **Escene2D Actions:** https://libgdx.com/wiki/graphics/2d/scene2d/scene2d-actions

### Codificación de video
- **FFmpeg VP8 encoding guide:** https://trac.ffmpeg.org/wiki/Encode/VP8
- **FFmpeg VP9 encoding guide:** https://trac.ffmpeg.org/wiki/Encode/VP9
- **WebM con canal alpha:** https://trac.ffmpeg.org/wiki/Encode/VP8#Alpha

### Skins para Scene2D
- **gdx-skins (czyzby):** https://github.com/czyzby/gdx-skins
- **Skin Composer:** https://github.com/raeleus/skin-composer

### Alternativas
- **vlcj (VLC Java Bindings):** https://github.com/caprica/vlcj
- **gdx-texturepacker:** https://github.com/libgdx/libgdx/wiki/Texture-packer
