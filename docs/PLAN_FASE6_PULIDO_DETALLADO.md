# PLAN FASE 6: PULIDO, TRANSICIONES Y PARTÍCULAS PARA INTENTIA

> Documento técnico detallado para implementar efectos visuales, transiciones entre pantallas y partículas atmosféricas.
> Fecha: Mayo 2026 — Proyecto INTENTIA (libGDX + Java)

---

## ÍNDICE

1. [PARTE 1: TRANSICIONES ENTRE SCREENS](#parte-1-transiciones-entre-screens)
2. [PARTE 2: EFECTOS DE PARTÍCULAS](#parte-2-efectos-de-partículas)
3. [PARTE 3: EFECTOS DE DIÁLOGO](#parte-3-efectos-de-diálogo)
4. [PARTE 4: EFECTOS DE GAMESCREEN (A futuro)](#parte-4-efectos-de-gamescreen-a-futuro)
5. [PARTE 5: COLOR Y ATMÓSFERA (Identidad Visual)](#parte-5-color-y-atmósfera-identidad-visual)
6. [PARTE 6: MEJORAS DE UX](#parte-6-mejoras-de-ux)
7. [CÓDIGO DE EJEMPLO: ACTIONS PERSONALIZADAS](#código-de-ejemplo-actions-personalizadas-completas)
8. [MEJORES PRÁCTICAS](#mejores-prácticas)
9. [CHECKLIST](#checklist)
10. [REFERENCIAS](#referencias)

---

## PARTE 1: TRANSICIONES ENTRE SCREENS

### 1.1 Arquitectura: Enfoque Simple (Recomendado para empezar)

Existen dos enfoques para transiciones en libGDX:

**Enfoque Simple (Actions sobre el Stage):**
- Consiste en aplicar `Actions.fadeOut()` al `Stage` de la screen actual, cambiar de screen en el callback, y aplicar `Actions.fadeIn()` en la nueva screen.
- Ventajas: extremadamente simple, no requiere FrameBuffer, funciona con cualquier widget de Scene2D.
- Desventaja: no puedes ver ambas screens superpuestas parcialmente durante la transición.

**Enfoque Avanzado (FrameBuffer + textura intermedia):**
- Captura la screen actual como `Texture` usando `FrameBuffer`, renderiza esa textura mientras se anima, luego hace lo mismo con la siguiente.
- Ventaja: control total sobre la animación (slide, zoom con ambas screens visibles).
- Desventaja: más complejo, requiere manejo de FrameBuffer y memoria de textura.

**RECOMENDACIÓN:** Empezar con el enfoque simple (Actions) que es suficiente para un juego narrativo como Intentia. Si se desea slide entre screens más adelante, migrar al avanzado.

### 1.2 Clase: TransicionScreen (Wrapper con Actions)

Clase que envuelve cualquier Screen y añade transiciones fade in/out automáticas.

```java
package io.yourPath.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.utils.viewport.FitViewport;

public abstract class TransicionScreen implements Screen {
    protected Stage stage;
    protected boolean transicionando = false;

    @Override
    public void render(float delta) {
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        Gdx.gl.glClearColor(0.05f, 0.05f, 0.1f, 1f);

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    public void iniciarTransicionFade(final Screen siguienteScreen) {
        if (transicionando) return;
        transicionando = true;

        stage.addAction(Actions.sequence(
            Actions.fadeOut(0.3f),
            Actions.run(new Runnable() {
                @Override
                public void run() {
                    ((com.badlogic.gdx.Game) Gdx.app.getApplicationListener()).setScreen(siguienteScreen);
                }
            })
        ));
    }

    @Override
    public void show() {
        stage.getRoot().getColor().a = 0;
        stage.addAction(Actions.fadeIn(0.3f));
    }

    @Override
    public void hide() {}

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void dispose() {
        stage.dispose();
    }
}
```

### 1.3 Integración en MainMenuScreen (con transición fade)

```java
package io.yourPath.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.FitViewport;
import io.yourPath.Main;
import io.yourPath.models.GameState;
import io.yourPath.utils.SaveSystem;

public class MainMenuScreen extends TransicionScreen {
    private Main game;
    private Skin skin;
    private TextButton btnNuevaPartida;
    private TextButton btnContinuar;
    private TextButton btnSalir;

    public MainMenuScreen(Main game) {
        this.game = game;
        this.stage = new Stage(new FitViewport(800, 600));
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void show() {
        super.show();

        skin = new Skin(Gdx.files.internal("skin/pixel/uiskin.json"));

        Label titulo = new Label("INTENTIA: LEGADO", skin);
        titulo.setFontScale(2f);

        btnNuevaPartida = new TextButton("Nueva Partida", skin);
        btnContinuar = new TextButton("Continuar", skin);
        btnSalir = new TextButton("Salir", skin);

        btnNuevaPartida.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                game.getStoryManager().start("car_awakening");
                iniciarTransicionFade(new StoryScreen(game, game.getStoryManager(), game.getCharacters()));
            }
        });

        btnContinuar.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                GameState guardado = SaveSystem.loadGame();
                if (guardado != null) {
                    game.setStoryManager(new com.badlogic.gdx.utils.Json().fromJson(GameState.class, guardado.toString()));
                    iniciarTransicionFade(new StoryScreen(game, game.getStoryManager(), game.getCharacters()));
                }
            }
        });

        btnSalir.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                Gdx.app.exit();
            }
        });

        Table table = new Table();
        table.setFillParent(true);
        table.center();
        table.add(titulo).padBottom(40).row();
        table.add(btnNuevaPartida).width(250).height(50).padBottom(10).row();
        table.add(btnContinuar).width(250).height(50).padBottom(10).row();
        table.add(btnSalir).width(250).height(50);

        stage.addActor(table);
    }

    @Override
    public void render(float delta) {
        btnContinuar.setVisible(SaveSystem.exists());
        super.render(delta);
    }

    @Override
    public void dispose() {
        super.dispose();
        skin.dispose();
    }
}
```

### 1.4 Transición en StoryScreen

Similar patrón: extender `TransicionScreen`, usar `iniciarTransicionFade()` para ir a `MainMenuScreen` o `DialogOverlayScreen`.

### 1.5 Enfoque Avanzado: Slide (Referencia futura)

Para cuando se quiera una transición más cinematográfica:

```java
// Capturar frame actual como textura usando FrameBuffer
// Luego animar la textura con moveBy() mientras se carga la nueva screen
// Código conceptual (requiere FrameBuffer):
//
// FrameBuffer fbo = new FrameBuffer(Pixmap.Format.RGBA8888, width, height, false);
// fbo.begin();
// screenActual.render(delta); // renderizar screen actual al fbo
// fbo.end();
// Texture texturaActual = fbo.getColorBufferTexture();
// 
// Luego en un nuevo Stage: mostrar texturaActual como Image, animarla
// y cuando termine, mostrar la screen real
```

---

## PARTE 2: EFECTOS DE PARTÍCULAS

### 2.1 ¿Qué es ParticleEffect en libGDX?

`ParticleEffect` es una clase de libGDX que permite crear y renderizar sistemas de partículas 2D. Consiste en uno o más `ParticleEmitter`, cada uno con su propia configuración de: vida, velocidad, tamaño, color, rotación, gravedad, opacidad, etc.

**Formas de crear efectos:**
1. **Desde archivo .p** — creado con gdx-particle-editor (herramienta visual recomendada).
2. **Desde código Java** — configurando `ParticleEmitter` manualmente.

### 2.2 gdx-particle-editor: Instalación y uso

**Descarga:**
- URL: https://github.com/libgdx/gdx-particle-editor/releases
- Descargar el JAR más reciente (ej: `gdx-particle-editor-1.0.1.jar`)

**Ejecución:**
```bash
java -jar gdx-particle-editor-1.0.1.jar
```

**Workflow:**
1. Abrir el editor → elegir imagen de partícula (círculo blanco de 8x8px es lo más común).
2. Configurar emisores:
   - **Tint:** verde agua (#7FFFD4).
   - **Life:** 2000-4000ms (2-4 segundos).
   - **Velocity:** 10-30 píxeles/segundo.
   - **Size:** 2-6 píxeles.
   - **Emission:** 20-40 partículas/segundo.
   - **Gravity:** 0.
   - **Additive:** activado para brillo.
   - **Continuous:** activado para loop infinito.
3. Guardar como `.p` en `assets/particles/menu_bg.p`.
4. En el juego cargar con:
```java
ParticleEffect efecto = new ParticleEffect();
efecto.load(Gdx.files.internal("particles/menu_bg.p"), atlas);
```

### 2.3 Crear partículas desde código (sin archivo .p)

Esta es la opción más portable y no requiere archivos externos:

```java
package io.yourPath.effects;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.ParticleEffect;
import com.badlogic.gdx.graphics.g2d.ParticleEffectPool;
import com.badlogic.gdx.graphics.g2d.ParticleEmitter;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Array;

public class MenuParticleBackground {
    private ParticleEffect efectoBase;
    private ParticleEffectPool pool;
    private Array<ParticleEffectPool.PooledEffect> activos;

    private static final float VERDE_AGUA_R = 0.498f;
    private static final float VERDE_AGUA_G = 1.0f;
    private static final float VERDE_AGUA_B = 0.831f;

    public MenuParticleBackground() {
        activos = new Array<ParticleEffectPool.PooledEffect>();

        Texture texturaParticula = crearTexturaParticula(8, 8);

        efectoBase = new ParticleEffect();
        ParticleEmitter emisor = crearEmisorMenu(texturaParticula);
        efectoBase.getEmitters().clear();
        efectoBase.getEmitters().add(emisor);

        pool = new ParticleEffectPool(efectoBase, 1, 5);

        ParticleEffectPool.PooledEffect efecto = pool.obtain();
        efecto.setPosition(400, 300);
        efecto.start();
        activos.add(efecto);
    }

    private Texture crearTexturaParticula(int ancho, int alto) {
        Pixmap pixmap = new Pixmap(ancho, alto, Pixmap.Format.RGBA8888);
        pixmap.setColor(1, 1, 1, 1);
        pixmap.fillCircle(ancho / 2, alto / 2, ancho / 2);
        Texture textura = new Texture(pixmap);
        pixmap.dispose();
        textura.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        return textura;
    }

    private ParticleEmitter crearEmisorMenu(Texture textura) {
        ParticleEmitter emisor = new ParticleEmitter();

        emisor.setMaxParticleCount(50);
        emisor.setContinuous(true);

        ParticleEmitter.ScalableNumericValue vida = emisor.getLife();
        vida.setLow(2000);
        vida.setHigh(4000);

        ParticleEmitter.ScalableNumericValue velocidad = emisor.getVelocity();
        velocidad.setLow(10);
        velocidad.setHigh(30);

        ParticleEmitter.ScalableNumericValue tamano = emisor.getSize();
        tamano.setLow(2);
        tamano.setHigh(6);

        ParticleEmitter.GradientColorValue color = emisor.getTint();
        color.setColors(new float[] { VERDE_AGUA_R, VERDE_AGUA_G, VERDE_AGUA_B });

        ParticleEmitter.ScalableNumericValue emision = emisor.getEmission();
        emision.setLow(20);
        emision.setHigh(40);

        ParticleEmitter.ScalableNumericValue angulo = emisor.getAngle();
        angulo.setLow(0);
        angulo.setHigh(360);

        emisor.getTransparency().setLow(0.5f);
        emisor.getTransparency().setHigh(1.0f);

        emisor.setAdditive(true);
        emisor.setAttached(false);

        emisor.getSpawnWidth().setLow(600);
        emisor.getSpawnHeight().setLow(400);

        emisor.setSprite(textura);

        return emisor;
    }

    public void update(float delta) {
        for (int i = activos.size - 1; i >= 0; i--) {
            ParticleEffectPool.PooledEffect efecto = activos.get(i);
            efecto.update(delta);
            if (efecto.isComplete()) {
                efecto.free();
                activos.removeIndex(i);
            }
        }
    }

    public void draw(SpriteBatch batch) {
        for (ParticleEffectPool.PooledEffect efecto : activos) {
            efecto.draw(batch, Gdx.graphics.getDeltaTime());
        }
    }

    public void dispose() {
        for (ParticleEffectPool.PooledEffect efecto : activos) {
            efecto.free();
        }
        activos.clear();
        pool.clear();
        efectoBase.dispose();
    }
}
```

### 2.4 Integración en MainMenuScreen

```java
// En MainMenuScreen.java:
private MenuParticleBackground particulas;

@Override
public void show() {
    particulas = new MenuParticleBackground();
    // ... resto del show()
}

@Override
public void render(float delta) {
    // 1. Limpiar pantalla
    Gdx.gl.glClearColor(0.05f, 0.05f, 0.1f, 1f);
    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

    // 2. Dibujar partículas DETRÁS del Stage
    particulas.update(delta);
    particulas.draw(stage.getBatch());

    // 3. Dibujar Stage encima
    stage.act(delta);
    stage.draw();
}

@Override
public void dispose() {
    particulas.dispose();
    super.dispose();
}
```

### 2.5 Efectos recomendados para Intentia

| Ubicación | Tipo | Color | Tamaño | Velocidad |
|-----------|------|-------|--------|-----------|
| Menú principal | Polvo de estrellas flotante | #7FFFD4 | 2-6px | 10-30px/s |
| Diálogos (cambio nodo) | Chispas sutiles | #7FFFD4 → #FFFFFF | 2-4px | 50-100px/s |
| GameScreen (bosque) | Hojas cayendo | #7FFFD4 + verde | 8-16px | 20-50px/s |
| GameScreen (niebla) | Niebla baja | #7FFFD4 alpha 0.1 | 32-64px | 5-15px/s |

---

## PARTE 3: EFECTOS DE DIÁLOGO

### 3.1 Typewriter con sonido Beep retro

Partiendo del `TypewriterAction` de Fase 2, se extiende para añadir sonido por letra:

```java
package io.yourPath.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.utils.TimeUtils;

public class TypewriterConSonido extends TemporalAction {
    private Label label;
    private String textoCompleto;
    private Sound sonidoLetra;
    private int ultimoChars = 0;
    private long ultimoBeep = 0;
    private float volumen = 0.3f;
    private long intervaloMinimoBeep = 50; // ms mínimo entre beeps

    public TypewriterConSonido(Label label, String textoCompleto, float duracion, Sound sonidoLetra) {
        this.label = label;
        this.textoCompleto = textoCompleto;
        this.sonidoLetra = sonidoLetra;
        setDuration(duracion);
        setInterpolation(MathUtils.linear);
    }

    public TypewriterConSonido(Label label, String textoCompleto, float duracion) {
        this(label, textoCompleto, duracion, null);
    }

    @Override
    protected void update(float percent) {
        int chars = (int) (textoCompleto.length() * percent);
        chars = MathUtils.clamp(chars, 0, textoCompleto.length());

        if (chars > ultimoChars) {
            if (sonidoLetra != null) {
                long ahora = TimeUtils.millis();
                if (ahora - ultimoBeep > intervaloMinimoBeep) {
                    sonidoLetra.play(volumen, MathUtils.random(1.0f, 1.5f), 0f);
                    ultimoBeep = ahora;
                }
            }
            ultimoChars = chars;
        }

        label.setText(textoCompleto.substring(0, Math.min(chars, textoCompleto.length())));
    }

    public void setSonidoLetra(Sound sonido) {
        this.sonidoLetra = sonido;
    }

    public void setVolumen(float volumen) {
        this.volumen = volumen;
    }

    public void setIntervaloMinimoBeep(long ms) {
        this.intervaloMinimoBeep = ms;
    }

    public boolean estaCompleto() {
        return ultimoChars >= textoCompleto.length();
    }
}
```

### 3.2 Animación de opciones (aparición secuencial)

Cada opción aparece deslizándose desde la derecha con un pequeño delay escalonado:

```java
private void animarOpcionSecuencial(TextButton boton, int indice) {
    boton.getColor().a = 0f;
    float xOriginal = boton.getX();

    // Iniciar desde la derecha del stage
    boton.setPosition(800, boton.getY());

    boton.addAction(Actions.sequence(
        Actions.delay(0.15f * indice),
        Actions.parallel(
            Actions.moveTo(xOriginal, boton.getY(), 0.3f, Interpolation.exp5Out),
            Actions.fadeIn(0.3f)
        )
    ));
}

// Uso en DialogOverlayScreen:
Table opcionesTable = new Table();
for (int i = 0; i < opciones.size(); i++) {
    TextButton btn = new TextButton(opciones.get(i).getText(), skin);
    final int index = i;
    btn.addListener(new ChangeListener() {
        @Override
        public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
            storyManager.advance(opciones.get(index));
        }
    });
    opcionesTable.add(btn).width(400).height(40).padBottom(5).row();
    animarOpcionSecuencial(btn, i);
}
```

### 3.3 Highlighting de opciones

Efecto hover con cambio de color y escala:

```java
private void configurarHover(TextButton boton, Color colorNormal, Color colorHover) {
    boton.addListener(new com.badlogic.gdx.scenes.scene2d.InputListener() {
        @Override
        public void enter(com.badlogic.gdx.scenes.scene2d.InputEvent event,
                          float x, float y, int pointer, com.badlogic.gdx.scenes.scene2d.Actor fromActor) {
            boton.addAction(Actions.parallel(
                Actions.color(colorHover, 0.15f),
                Actions.scaleTo(1.05f, 1.05f, 0.15f, Interpolation.exp5Out)
            ));
        }

        @Override
        public void exit(com.badlogic.gdx.scenes.scene2d.InputEvent event,
                         float x, float y, int pointer, com.badlogic.gdx.scenes.scene2d.Actor toActor) {
            boton.addAction(Actions.parallel(
                Actions.color(colorNormal, 0.15f),
                Actions.scaleTo(1f, 1f, 0.15f, Interpolation.exp5Out)
            ));
        }
    });
}
```

---

## PARTE 4: EFECTOS DE GAMESCREEN (A futuro)

### 4.1 Parallax simple con TiledMap

Renderizar ciertas capas del mapa con un offset proporcional a la cámara:

```java
// En GameScreen.java:
private OrthographicCamera camaraFondo;
private float factorParallax = 0.3f; // capa de fondo se mueve al 30% de velocidad

public void renderParallax() {
    // Clonar posición de cámara principal pero escalada
    camaraFondo.position.set(
        camaraPrincipal.position.x * factorParallax,
        camaraPrincipal.position.y * factorParallax,
        0
    );
    camaraFondo.update();

    // Renderizar solo las capas de fondo con la cámara parallax
    mapRenderer.getBatch().setProjectionMatrix(camaraFondo.combined);
    for (int i = 0; i < capasFondo; i++) {
        mapRenderer.renderTileLayer((TiledMapTileLayer) map.getLayers().get(i));
    }

    // Restaurar cámara principal para capas superiores
    mapRenderer.getBatch().setProjectionMatrix(camaraPrincipal.combined);
    for (int i = capasFondo; i < map.getLayers().size(); i++) {
        if (map.getLayers().get(i) instanceof TiledMapTileLayer) {
            mapRenderer.renderTileLayer((TiledMapTileLayer) map.getLayers().get(i));
        }
    }
}
```

Alternativa más simple con imágenes de fondo separadas:

```java
// Fondo fijo como imagen en lugar de capa Tiled
private Texture fondoLejano;
private Texture fondoMedio;

public void renderParallaxImagenes(float delta) {
    float offsetX = camaraPrincipal.position.x * 0.2f;
    float offsetY = camaraPrincipal.position.y * 0.1f;

    batch.begin();
    batch.draw(fondoLejano, -offsetX * 0.5f, -offsetY * 0.5f);
    batch.draw(fondoMedio, -offsetX, -offsetY);
    batch.end();
}
```

### 4.2 Niebla / Atmósfera

Overlay con color verde agua tenue sobre toda la pantalla:

```java
// En GameScreen.render(), después del mapa y antes de la UI:
batch.begin();
batch.setColor(ColoresIntentia.VERDE_AGUA.r, ColoresIntentia.VERDE_AGUA.g,
               ColoresIntentia.VERDE_AGUA.b, 0.08f);
batch.draw(pixelBlanco, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
batch.setColor(Color.WHITE);
batch.end();
```

### 4.3 Cámara Shake

Efecto de terremoto al fallar un TrialNode:

```java
package io.yourPath.utils;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;

public class CamaraShake {
    private OrthographicCamera camara;
    private float tiempoRestante = 0;
    private float duracion = 0.3f;
    private float intensidad = 3f;
    private Vector3 posOriginal = new Vector3();

    public CamaraShake(OrthographicCamera camara) {
        this.camara = camara;
    }

    public void iniciar(float duracion, float intensidad) {
        this.duracion = duracion;
        this.intensidad = intensidad;
        this.tiempoRestante = duracion;
        this.posOriginal.set(camara.position);
    }

    public void update(float delta) {
        if (tiempoRestante <= 0) return;

        tiempoRestante -= delta;
        float atenuacion = tiempoRestante / duracion;

        if (tiempoRestante <= 0) {
            camara.position.set(posOriginal);
            camara.update();
            return;
        }

        float offsetX = MathUtils.random(-intensidad, intensidad) * atenuacion;
        float offsetY = MathUtils.random(-intensidad, intensidad) * atenuacion;

        camara.position.set(posOriginal.x + offsetX, posOriginal.y + offsetY, posOriginal.z);
        camara.update();
    }

    public boolean estaActivo() {
        return tiempoRestante > 0;
    }
}
```

---

## PARTE 5: COLOR Y ATMÓSFERA (Identidad Visual)

### 5.1 Paleta de colores oficial de Intentia

| Elemento | Color HEX | Uso |
|----------|-----------|-----|
| Verde agua (ancla) | `#7FFFD4` | Color identitario: títulos, nombre personaje, partículas menú |
| Fondo menú | `#0D0D1A` | Fondo oscuro del menú principal |
| Fondo diálogo | `#0A0A14` alpha 0.85 | Panel semi-transparente de diálogo |
| Texto diálogo | `#FFFFFF` | Texto narrativo |
| Nombre personaje | `#7FFFD4` | Label del hablante |
| Opciones hover | `#AAFFE6` | Verde agua más claro al pasar ratón |
| Opciones bloqueadas | `#666666` alpha 0.4 | Opciones no disponibles por flag |
| Borde ninepatch | `#1A3A3A` | Marco del diálogo |
| Éxito trial | `#7FFFD4` | Feedback positivo |
| Fracaso trial | `#FF6B6B` | Feedback negativo (rojo suave) |

### 5.2 Clase de constantes de color

```java
package io.yourPath.utils;

import com.badlogic.gdx.graphics.Color;

public class ColoresIntentia {
    public static final Color VERDE_AGUA = new Color(0x7F / 255f, 0xFF / 255f, 0xD4 / 255f, 1f);
    public static final Color VERDE_AGUA_CLARO = new Color(0xAA / 255f, 0xFF / 255f, 0xE6 / 255f, 1f);
    public static final Color VERDE_AGUA_OSCURO = new Color(0x1A / 255f, 0x3A / 255f, 0x3A / 255f, 1f);
    public static final Color FONDO_MENU = new Color(0x0D / 255f, 0x0D / 255f, 0x1A / 255f, 1f);
    public static final Color FONDO_DIALOGO = new Color(0x0A / 255f, 0x0A / 255f, 0x14 / 255f, 0.85f);
    public static final Color TEXTO_DIALOGO = Color.WHITE;
    public static final Color NOMBRE_PERSONAJE = VERDE_AGUA;
    public static final Color OPCION_BLOQUEADA = new Color(0.4f, 0.4f, 0.4f, 0.4f);
    public static final Color EXITO_TRIAL = VERDE_AGUA;
    public static final Color FRACASO_TRIAL = new Color(0xFF / 255f, 0x6B / 255f, 0x6B / 255f, 1f);
}
```

### 5.3 Inyectar colores personalizados en Skin

```java
// En la Screen, después de cargar skin:
Skin skin = new Skin(Gdx.files.internal("skin/pixel/uiskin.json"));

// Agregar colores personalizados para usarlos en el JSON o código
skin.add("verdeAgua", ColoresIntentia.VERDE_AGUA, Color.class);
skin.add("verdeAguaClaro", ColoresIntentia.VERDE_AGUA_CLARO, Color.class);
skin.add("verdeAguaOscuro", ColoresIntentia.VERDE_AGUA_OSCURO, Color.class);
skin.add("fondoMenu", ColoresIntentia.FONDO_MENU, Color.class);
skin.add("textoDialogo", ColoresIntentia.TEXTO_DIALOGO, Color.class);

// Crear estilo de Label personalizado
Label.LabelStyle estiloDialogo = new Label.LabelStyle();
estiloDialogo.font = skin.getFont("default-font");
estiloDialogo.fontColor = ColoresIntentia.TEXTO_DIALOGO;
skin.add("dialogo", estiloDialogo, Label.LabelStyle.class);

Label.LabelStyle estiloNombre = new Label.LabelStyle();
estiloNombre.font = skin.getFont("default-font");
estiloNombre.fontColor = ColoresIntentia.VERDE_AGUA;
skin.add("nombrePersonaje", estiloNombre, Label.LabelStyle.class);
```

---

## PARTE 6: MEJORAS DE UX

### 6.1 Indicador de "escribiendo..."

Mientras el typewriter está activo, un triángulo "▼" parpadea al final. Al terminar, se reemplaza por "▼ Pulse para continuar":

```java
// En DialogOverlayScreen:
private Label indicador;
private boolean typewriterCompleto = false;

private void crearIndicador() {
    indicador = new Label("▼", skin);
    indicador.setColor(ColoresIntentia.VERDE_AGUA);

    // Animación de parpadeo infinita
    indicador.addAction(Actions.forever(
        Actions.sequence(
            Actions.fadeOut(0.5f),
            Actions.fadeIn(0.5f)
        )
    ));
}

public void actualizarIndicador() {
    if (typewriterAction != null && typewriterAction.estaCompleto()) {
        if (!typewriterCompleto) {
            typewriterCompleto = true;
            indicador.setText("▼ Pulse para continuar");
        }
    }
}
```

### 6.2 Auto-avance opcional

Si el nodo actual no tiene opciones (solo texto + `nextId`), tras 3 segundos avanzar automáticamente:

```java
private float temporizadorAutoAvance = 0;
private static final float TIEMPO_AUTO_AVANCE = 3f;

@Override
public void render(float delta) {
    super.render(delta);

    if (typewriterCompleto && !tieneOpciones() && !autoAvanceCancelado) {
        temporizadorAutoAvance += delta;
        // Mostrar cuenta regresiva visual
        float restante = TIEMPO_AUTO_AVANCE - temporizadorAutoAvance;
        indicador.setText("▼ Continuando en " + (int) restante + "...");

        if (temporizadorAutoAvance >= TIEMPO_AUTO_AVANCE) {
            avanzarSiguienteNodo();
        }
    }

    // Si el jugador presiona espacio/enter, cancelar auto-avance o avanzar manual
    if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.SPACE) ||
        Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.ENTER)) {
        if (typewriterCompleto) {
            if (!autoAvanceCancelado && temporizadorAutoAvance < TIEMPO_AUTO_AVANCE) {
                autoAvanceCancelado = true;
                indicador.setText("▼ Pulse Enter para continuar");
            } else {
                avanzarSiguienteNodo();
            }
        }
    }
}
```

### 6.3 Feedback háptico visual

Botón presionado → escala 0.95 por 0.1s:

```java
private void configurarFeedbackClick(TextButton boton) {
    boton.addListener(new ChangeListener() {
        @Override
        public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
            boton.addAction(Actions.sequence(
                Actions.scaleTo(0.95f, 0.95f, 0.05f),
                Actions.scaleTo(1f, 1f, 0.1f, Interpolation.exp5Out)
            ));
        }
    });
}

// Destello verde al elegir opción exitosa:
private void feedbackExito(TextButton boton) {
    boton.addAction(Actions.sequence(
        Actions.color(ColoresIntentia.VERDE_AGUA, 0.1f),
        Actions.color(Color.WHITE, 0.2f)
    ));
}
```

---

## CÓDIGO DE EJEMPLO: ACTIONS PERSONALIZADAS COMPLETAS

### ShakeAction.java — Tiembla un actor por duración especificada

```java
package io.yourPath.utils;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.Actor;

public class ShakeAction extends Action {
    private float tiempoRestante;
    private float duracion;
    private float intensidadX = 5f;
    private float intensidadY = 5f;
    private float xOriginal, yOriginal;
    private boolean primeraVez = true;

    public ShakeAction(float duracion, float intensidad) {
        this.duracion = duracion;
        this.intensidadX = intensidad;
        this.intensidadY = intensidad;
        this.tiempoRestante = duracion;
    }

    public ShakeAction(float duracion, float intensidadX, float intensidadY) {
        this.duracion = duracion;
        this.intensidadX = intensidadX;
        this.intensidadY = intensidadY;
        this.tiempoRestante = duracion;
    }

    @Override
    public boolean act(float delta) {
        if (primeraVez) {
            xOriginal = target.getX();
            yOriginal = target.getY();
            primeraVez = false;
        }

        tiempoRestante -= delta;

        if (tiempoRestante <= 0) {
            target.setPosition(xOriginal, yOriginal);
            return true;
        }

        float atenuacion = tiempoRestante / duracion;
        float offsetX = MathUtils.random(-intensidadX, intensidadX) * atenuacion;
        float offsetY = MathUtils.random(-intensidadY, intensidadY) * atenuacion;

        target.setPosition(xOriginal + offsetX, yOriginal + offsetY);
        return false;
    }

    @Override
    public void reset() {
        super.reset();
        primeraVez = true;
        if (target != null) {
            target.setPosition(xOriginal, yOriginal);
        }
    }
}
```

### PulseAction.java — Escala un actor cíclicamente

```java
package io.yourPath.utils;

import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.math.Interpolation;

public class PulseAction extends Action {
    private float duracion;
    private float tiempoTranscurrido = 0;
    private float escalaMin = 0.95f;
    private float escalaMax = 1.05f;

    public PulseAction(float duracion) {
        this.duracion = duracion;
    }

    public PulseAction(float duracion, float escalaMin, float escalaMax) {
        this.duracion = duracion;
        this.escalaMin = escalaMin;
        this.escalaMax = escalaMax;
    }

    @Override
    public boolean act(float delta) {
        tiempoTranscurrido += delta;

        if (tiempoTranscurrido >= duracion) {
            tiempoTranscurrido = 0;
        }

        float medioCiclo = duracion / 2f;
        float progreso;

        if (tiempoTranscurrido < medioCiclo) {
            progreso = tiempoTranscurrido / medioCiclo;
            float escala = Interpolation.exp5Out.apply(escalaMin, escalaMax, progreso);
            target.setScale(escala, escala);
        } else {
            progreso = (tiempoTranscurrido - medioCiclo) / medioCiclo;
            float escala = Interpolation.exp5Out.apply(escalaMax, escalaMin, progreso);
            target.setScale(escala, escala);
        }

        return false; // nunca termina, es cíclico
    }

    @Override
    public void reset() {
        super.reset();
        tiempoTranscurrido = 0;
        if (target != null) {
            target.setScale(1f, 1f);
        }
    }
}
```

### TypewriterWithSoundAction.java — Completo

```java
package io.yourPath.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.utils.TimeUtils;

public class TypewriterWithSoundAction extends TemporalAction {
    private Label label;
    private String textoCompleto;
    private Sound sonidoLetra;
    private int ultimoChars = 0;
    private long ultimoBeep = 0;
    private float volumen = 0.3f;
    private long intervaloMinimoBeep = 50;
    private boolean sonidoHabilitado = true;
    private Runnable alCompletar;

    public TypewriterWithSoundAction(Label label, String textoCompleto, float duracion) {
        this.label = label;
        this.textoCompleto = textoCompleto;
        this.sonidoLetra = null;
        setDuration(duracion);
        setInterpolation(MathUtils.linear);
    }

    public TypewriterWithSoundAction(Label label, String textoCompleto, float duracion, Sound sonidoLetra) {
        this.label = label;
        this.textoCompleto = textoCompleto;
        this.sonidoLetra = sonidoLetra;
        setDuration(duracion);
        setInterpolation(MathUtils.linear);
    }

    @Override
    protected void update(float percent) {
        int chars = (int) (textoCompleto.length() * percent);
        chars = MathUtils.clamp(chars, 0, textoCompleto.length());

        if (chars > ultimoChars && sonidoHabilitado && sonidoLetra != null) {
            long ahora = TimeUtils.millis();
            if (ahora - ultimoBeep > intervaloMinimoBeep) {
                sonidoLetra.play(volumen, MathUtils.random(0.8f, 1.2f), 0f);
                ultimoBeep = ahora;
            }
        }
        ultimoChars = chars;

        label.setText(textoCompleto.substring(0, Math.min(chars, textoCompleto.length())));
    }

    @Override
    protected void end() {
        label.setText(textoCompleto);
        if (alCompletar != null) {
            alCompletar.run();
        }
    }

    public boolean estaCompleto() {
        return getTime() >= getDuration();
    }

    public void setSonido(Sound sonido) {
        this.sonidoLetra = sonido;
    }

    public void setVolumen(float volumen) {
        this.volumen = volumen;
    }

    public void setSonidoHabilitado(boolean habilitado) {
        this.sonidoHabilitado = habilitado;
    }

    public void setAlCompletar(Runnable runnable) {
        this.alCompletar = runnable;
    }

    @Override
    public void reset() {
        super.reset();
        ultimoChars = 0;
        ultimoBeep = 0;
    }
}
```

---

## MEJORES PRÁCTICAS

### Rendimiento
1. **No crear objetos en `render()`**: Reciclar Actions usando `Actions.action()` o pools. Cada `new` en render() produce garbage collection.
2. **Pool de partículas**: Usar `ParticleEffectPool` para evitar crear/destruir efectos constantemente.
3. **SpriteBatch único**: Usar un solo `SpriteBatch.flush()` entre capas para evitar problemas de z-ordering. Llamar `batch.end()` solo una vez por frame si es posible.

### Batch y blending
4. **Usar `batch.setColor()`** en lugar de múltiples draw calls para cambiar colores.
5. **Agrupar partículas por blend mode**: Dibujar primero todas las aditivas, luego las normales. Usar `setEmittersCleanUpBlendFunction(false)` y restaurar manualmente.

### Texturas
6. **Texturas de partículas**: Usar atlas de texturas (`TextureAtlas`) para reducir binds de OpenGL.
7. **Filtro Nearest**: `texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)` para mantener estética pixel art.

### Transiciones
8. **Duración**: No exceder 0.5s para transiciones entre screens. Mantener sensación ágil.
9. **Fade in al entrar**: Siempre empezar con alpha 0 y hacer fadeIn en `show()`.

### Color
10. **Verde agua como hilo conductor**: Usarlo con moderación (títulos, nombre personaje, partículas) para que conserve su peso narrativo. No saturar la pantalla.

### Dispose
11. **Siempre hacer dispose**: `ParticleEffect.dispose()`, `Sound.dispose()`, `Texture.dispose()`, `Stage.dispose()`, `Skin.dispose()`. Cada screen debe limpiar sus recursos.
12. **Verificar 60fps estables**: Monitorear con `Gdx.graphics.getFramesPerSecond()`. Si baja de 55, reducir count de partículas o simplificar transiciones.

---

## CHECKLIST

```
[ ] BaseTransitionScreen (TransicionScreen) implementado con fade in/out
[ ] Fade transition entre MainMenuScreen y StoryScreen
[ ] Fade transition entre StoryScreen y MainMenuScreen
[ ] Menú principal con partículas flotantes (MenuParticleBackground)
[ ] Typewriter con beep retro por letra (TypewriterWithSoundAction)
[ ] Opciones de diálogo aparecen secuencialmente (animación slide)
[ ] Hover highlight en botones (cambio de color + escala 1.05x)
[ ] Colores de la paleta Intentia aplicados (ColoresIntentia.java)
[ ] Indicador de "▼ escribiendo..." en diálogos (parpadeo)
[ ] Auto-avance en nodos sin opciones (3 segundos)
[ ] Shake effect (CamaraShake) para fallos de trial
[ ] Feedback visual de clic en botones (escala 0.95)
[ ] Dispose correcto de partículas, texturas, skins y sounds
[ ] Sin sobrecarga de performance (60fps estable con Gdx.graphics.getFramesPerSecond())
```

---

## REFERENCIAS

### Documentación oficial libGDX
- **Particle Effects 2D API:** https://libgdx.com/wiki/graphics/2d/2d-particleeffects
- **GDX Particle Editor (nuevo):** https://libgdx.com/wiki/tools/2d-particle-editor
- **Scene2D UI:** https://libgdx.com/wiki/graphics/2d/scene2d/scene2d-ui
- **Actions (Scene2D):** https://libgdx.com/wiki/graphics/2d/scene2d/scene2d#actions
- **Interpolation:** https://libgdx.com/wiki/math-utils/interpolation

### Herramientas
- **GDX Particle Editor (JAR descarga):** https://github.com/libgdx/gdx-particle-editor/releases
- **GDX Particle Editor Wiki (guía detallada):** https://github.com/libgdx/gdx-particle-editor/wiki/In%E2%80%90Depth-Guide
- **Particle Park (efectos comunitarios):** https://github.com/raeleus/Particle-Park
- **gdx-skins (Kenney Pixel):** https://github.com/czyzby/gdx-skins

### Videos tutoriales
- **GDX Particle Editor (YouTube):** https://youtu.be/OlPg6C6O-Cg
- **libGDX 2D Particle Effects (YouTube):** https://www.youtube.com/watch?v=LCLa-rgR_MA

### Código de ejemplo
- **ParticleEffect pooling example:** https://libgdxinfo.wordpress.com/particleeffect/
- **Código fuente de ejemplo:** https://hg.sr.ht/~dermetfan/somelibgdxtests/browse/core/src/net/dermetfan/someLibgdxTests/screens/ParticleEffectsTutorial.java

### Estructura de assets sugerida para partículas
```
assets/particles/
├── menu_bg.p           ← Efecto de polvo flotante (creado con editor)
├── sparkle.p           ← Chispas sutiles (cambio de nodo)
├── leaves.p            ← Hojas cayendo (GameScreen futuro)
└── particle.png        ← Textura base (círculo blanco 8x8px)
```

### Nota sobre ParticleEmitter desde código

Para crear un `ParticleEmitter` completamente desde código Java (sin archivo .p), la configuración mínima requerida es:

```java
ParticleEmitter emisor = new ParticleEmitter();
emisor.setMaxParticleCount(100);
emisor.setContinuous(true);

// Valores obligatorios (sin estos no se renderiza nada):
emisor.getLife().setLow(1000);      // vida mínima en ms
emisor.getLife().setHigh(3000);     // vida máxima en ms
emisor.getEmission().setLow(20);    // partículas por segundo
emisor.getVelocity().setLow(10);    // velocidad mínima
emisor.getVelocity().setHigh(30);   // velocidad máxima
emisor.getSize().setLow(4);         // tamaño mínimo
emisor.getSize().setHigh(8);        // tamaño máximo
emisor.getTint().setColors(new float[]{1, 1, 1}); // color blanco
emisor.getAngle().setLow(0);
emisor.getAngle().setHigh(360);     // ángulo completo
emisor.getTransparency().setLow(1); // alpha

// Textura obligatoria
emisor.setSprite(texturaParticula);
```

> **Importante:** La textura debe ser un `Texture` o `TextureRegion`. El tamaño recomendado para partículas pixel art es 8x8 o 16x16 píxeles con filtro Nearest.
