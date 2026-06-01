# PLAN DE MIGRACIÓN: FASE 0 y FASE 1 — DETALLADO

> **Proyecto:** INTENTIA: EL LEGADO
> **Objetivo:** Migrar menú principal y sistema de input de consola a Scene2D visual
> **Autor:** Arquitecto de Software — libGDX
> **Fecha:** 2026-05-31

---

## TABLA DE CONTENIDOS

1. [FASE 0: Refactor de Input (Crítico)](#fase-0-refactor-de-input-crítico)
   - [1.1 El Problema del Scanner](#11-el-problema-del-scanner)
   - [1.2 Estrategia de Reemplazo](#12-estrategia-de-reemplazo)
   - [1.3 Código: MainMenuScreen sin Scanner](#13-código-mainmenuscreen-sin-scanner)
   - [1.4 Código: StoryScreen sin Scanner](#14-código-storyscreen-sin-scanner)
   - [1.5 Verificación](#15-verificación)
2. [FASE 1: MainMenu Visual con Scene2D](#fase-1-mainmenu-visual-con-scene2d)
   - [2.1 Arquitectura Scene2D](#21-arquitectura-scene2d)
   - [2.2 Skin Setup](#22-skin-setup)
   - [2.3 Assets.java — Utilidad de Fuentes y Skin](#23-assetsjava--utilidad-de-fuentes-y-skin)
   - [2.4 Código: MainMenuScreen con Scene2D](#24-código-mainmenuscreen-con-scene2d)
   - [2.5 Mejores Prácticas](#25-mejores-prácticas)
3. [Checklist de Verificación](#3-checklist-de-verificación)

---

# FASE 0: REFACTOR DE INPUT (CRÍTICO)

## 1.1 El Problema del Scanner

### ¿Qué está pasando hoy?

`MainMenuScreen` y `StoryScreen` usan `Scanner(System.in)` para leer opciones del usuario:

```java
// MainMenuScreen.java:17
this.scanner = new Scanner(System.in);

// MainMenuScreen.java:36-37 (dentro de render)
if (scanner.hasNextInt()) {
    int opcion = scanner.nextInt();
```

### ¿Por qué esto es catastrófico en libGDX?

El game loop de libGDX funciona así (en el backend LWJGL3):

```
while (!window.shouldClose()) {
    game.render(deltaTime);  // ← se llama 60+ veces/segundo
    updateAudio();
    pollInput();
    swapBuffers();
}
```

El `Scanner(System.in)` es una operación **bloqueante de E/S**. Cuando `scanner.hasNextInt()` o `scanner.nextInt()` se ejecuta:

1. El thread principal de renderizado se **congela** esperando entrada del usuario.
2. Si el usuario no escribe nada, el juego deja de renderizar **completamente**.
3. `delta` se acumula, causando saltos enormes cuando se reanuda.
4. La ventana deja de responder al sistema operativo (aparece "No responde" en Windows).
5. En algunos backends (Android, GWT), `System.in` ni siquiera existe.

### Consecuencias concretas en el código actual

```java
// MainMenuScreen.java:36-52
if (scanner.hasNextInt()) {                     // ← BLOQUEA hasta que haya input
    int opcion = scanner.nextInt();              // ← BLOQUEA hasta que el usuario presione Enter
    scanner.nextLine();                          // ← consume el newline
    // ... lógica de negocio ...
}
```

Cada llamada a `render()` **se detiene** hasta que el usuario escribe un número y presiona Enter. El juego no pinta nada, no reproduce audio, no actualiza animaciones. Es como si el juego estuviera congelado.

## 1.2 Estrategia de Reemplazo

### Principio: Input NO bloqueante

libGDX expone el estado del teclado en tiempo real a través de `Gdx.input`:

| Método | Comportamiento |
|--------|----------------|
| `Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)` | `true` solo el frame en que se presionó la tecla |
| `Gdx.input.isKeyPressed(Input.Keys.NUM_1)` | `true` mientras la tecla esté presionada |
| `Gdx.input.isKeyJustPressed(Input.Keys.ENTER)` | Útil para "continuar" |

`isKeyJustPressed()` **nunca bloquea**. Retorna `boolean` inmediatamente. Esto permite que `render()` se ejecute 60 FPS sin pausas.

### ¿Mantener System.out o eliminarlo?

En Fase 0, **mantenemos `System.out`** para mostrar texto. Solo cambiamos el input. Esto permite verificar que la lógica de negocio funciona correctamente antes de introducir Scene2D.

### Convención a seguir

Cuando `isKeyJustPressed` detecta una tecla, el game loop debe asegurarse de **no procesar la misma tecla múltiples veces**. `isKeyJustPressed()` ya garantiza esto: solo retorna `true` un frame.

### Patrón general

```java
@Override
public void render(float delta) {
    pintarMenu();  // System.out (solo texto)

    if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) {
        // opción 1
    } else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) {
        // opción 2
    }
}
```

## 1.3 Código: MainMenuScreen sin Scanner

> **Archivo destino:** `core/src/main/java/io/yourPath/screens/MainMenuScreen.java`
> **Cambio:** Eliminar `Scanner`, usar `Gdx.input.isKeyJustPressed()`.

```java
package io.yourPath.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import io.yourPath.Main;
import io.yourPath.models.GameState;
import io.yourPath.logic.StoryManager;
import io.yourPath.utils.SaveSystem;

public class MainMenuScreen implements Screen {
    private Main juego;
    private boolean partidaGuardada;

    public MainMenuScreen(Main juego) {
        this.juego = juego;
    }

    @Override
    public void show() {
        partidaGuardada = SaveSystem.exists();
    }

    @Override
    public void render(float delta) {
        partidaGuardada = SaveSystem.exists();

        // ── Pintar menú en consola (temporal) ──
        for (int i = 0; i < 50; i++) System.out.println();
        System.out.println("╔═══════════════════════════════════╗");
        System.out.println("║       INTENTIA: EL LEGADO        ║");
        System.out.println("╚═══════════════════════════════════╝");
        System.out.println("  1. Nueva Partida");
        if (partidaGuardada) {
            System.out.println("  2. Continuar Partida");
            System.out.println("  3. Salir");
        } else {
            System.out.println("  2. Salir");
        }
        System.out.print("\n  Presiona 1, 2 o 3: ");

        // ── Input NO bloqueante ──
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) {
            juego.getStoryManager().start("car_awakening");
            juego.setScreen(new StoryScreen(juego, juego.getStoryManager(), juego.getCharacters()));
            return;
        }

        if (partidaGuardada) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) {
                GameState guardado = SaveSystem.loadGame();
                if (guardado != null) {
                    juego.setStoryManager(new StoryManager(juego.getStory(), guardado));
                    juego.setScreen(new StoryScreen(juego, juego.getStoryManager(), juego.getCharacters()));
                }
                return;
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_3)) {
                Gdx.app.exit();
                return;
            }
        } else {
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) {
                Gdx.app.exit();
                return;
            }
        }
    }

    @Override public void show() {}
    @Override public void resize(int ancho, int alto) {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
    @Override public void dispose() {}
}
```

### Puntos clave

1. **Scanner eliminado completamente** — no hay import de `java.util.Scanner`.
2. **`render()` ya no bloquea** — vuelve inmediatamente si no hay tecla presionada.
3. **`partidaGuardada` se recalcula** cada frame — así refleja cambios inmediatos.
4. **`return` después de cada acción** — evita que se ejecuten múltiples acciones en un mismo frame.
5. **Misma lógica de negocio** — `start("car_awakening")`, `SaveSystem.loadGame()`, `Gdx.app.exit()` exactamente igual.

## 1.4 Código: StoryScreen sin Scanner

> **Archivo destino:** `core/src/main/java/io/yourPath/screens/StoryScreen.java`
> **Cambio:** Eliminar `Scanner`, mantener state machine y `System.out`, usar input por teclas.

```java
package io.yourPath.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import io.yourPath.Main;
import io.yourPath.logic.StoryManager;
import io.yourPath.models.CharacterProfile;
import io.yourPath.models.DialogNode;
import io.yourPath.models.DialogOption;
import io.yourPath.models.NarrativeNode;
import io.yourPath.models.UIState;
import io.yourPath.utils.SaveSystem;

import java.util.List;
import java.util.Map;

public class StoryScreen implements Screen {
    private Main juego;
    private StoryManager gestorHistoria;
    private Map<String, CharacterProfile> personajes;
    private UIState estadoActual = UIState.DIALOGANDO;

    // Control para evitar doble-avance
    private boolean esperandoContinuar = false;

    public StoryScreen(Main juego, StoryManager gestorHistoria, Map<String, CharacterProfile> personajes) {
        this.juego = juego;
        this.gestorHistoria = gestorHistoria;
        this.personajes = personajes;
    }

    @Override
    public void render(float delta) {
        switch (estadoActual) {
            case DIALOGANDO:
                procesarDialogo();
                break;
            case MENU_PAUSA:
                procesarMenuPausa();
                break;
        }
    }

    // ──────────────────────────────────────────────
    //  ESTADO: DIALOGANDO
    // ──────────────────────────────────────────────

    private void procesarDialogo() {
        NarrativeNode nodo = gestorHistoria.getCurrentNode();
        if (nodo == null) return;

        for (int i = 0; i < 50; i++) System.out.println();

        CharacterProfile hablante = personajes.get(nodo.getSpeakerId());
        String nombre = (hablante != null) ? hablante.getName() : "Narrador";

        System.out.println("\n────────────────────────────────────────────────");
        System.out.println("[ " + nombre.toUpperCase() + " ]");
        System.out.println(nodo.getText());

        if (nodo instanceof DialogNode) {
            DialogNode nodoDialogo = (DialogNode) nodo;
            List<DialogOption> opciones = nodoDialogo.getOptions();
            String siguienteId = nodoDialogo.getNextId();

            // ── Caso: nodo con opciones ──
            if (opciones != null && !opciones.isEmpty()) {
                System.out.println("  ─────────────────────────────");
                System.out.println("  0. [ MENÚ DE PAUSA ]");
                for (int i = 0; i < opciones.size(); i++) {
                    String prefijo = (opciones.get(i).getRequiredFlag() != null
                        && !gestorHistoria.getGameState().hasFlag(opciones.get(i).getRequiredFlag()))
                        ? "  [BLOQUEADA] "
                        : "  ";
                    System.out.println(prefijo + (i + 1) + ". " + opciones.get(i).getText());
                }
                System.out.print("\n  Presiona 0-" + opciones.size() + ": ");

                // Input: tecla 0 = pausa
                if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_0)) {
                    estadoActual = UIState.MENU_PAUSA;
                    return;
                }

                // Input: teclas 1..N para opciones
                for (int i = 0; i < opciones.size(); i++) {
                    Input.Keys[] teclas = {
                        Input.Keys.NUM_1, Input.Keys.NUM_2, Input.Keys.NUM_3,
                        Input.Keys.NUM_4, Input.Keys.NUM_5, Input.Keys.NUM_6,
                        Input.Keys.NUM_7, Input.Keys.NUM_8, Input.Keys.NUM_9
                    };
                    if (i < teclas.length && Gdx.input.isKeyJustPressed(teclas[i])) {
                        DialogOption opcion = opciones.get(i);
                        // Verificar flag requerido
                        if (opcion.getRequiredFlag() != null
                            && !gestorHistoria.getGameState().hasFlag(opcion.getRequiredFlag())) {
                            // Opción bloqueada, ignorar
                            return;
                        }
                        gestorHistoria.advance(opcion);
                        return;
                    }
                }
            }

            // ── Caso: nodo sin opciones, con nextId (continuar) ──
            else if (siguienteId != null) {
                System.out.println("\n  (Presiona ENTER para continuar...)");
                if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
                    gestorHistoria.advance(siguienteId);
                }
            }

            // ── Caso: sin opciones y sin nextId (FIN) ──
            else {
                System.out.println("\n  ─── FIN DEL PRÓLOGO ───");
                System.out.println("  Presiona 1 para volver al menú: ");
                if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) {
                    juego.setScreen(new MainMenuScreen(juego));
                }
            }
        }
    }

    // ──────────────────────────────────────────────
    //  ESTADO: MENU_PAUSA
    // ──────────────────────────────────────────────

    private void procesarMenuPausa() {
        System.out.println("\n  ═══════════════════════════════");
        System.out.println("  │       MENÚ DE PAUSA         │");
        System.out.println("  ═══════════════════════════════");
        System.out.println("  1. Volver al juego");
        System.out.println("  2. Guardar partida");
        System.out.println("  3. Salir del juego");
        System.out.print("\n  Presiona 1, 2 o 3: ");

        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) {
            estadoActual = UIState.DIALOGANDO;
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) {
            SaveSystem.saveGame(gestorHistoria.getGameState());
            System.out.println("\n  ✓ Partida guardada.");
            estadoActual = UIState.DIALOGANDO;
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_3)) {
            Gdx.app.exit();
        }
    }

    @Override public void show() {}
    @Override public void resize(int ancho, int alto) {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
    @Override public void dispose() {}
}
```

### Cambios críticos respecto al original

1. **Scanner eliminado.** Sin `import java.util.Scanner`, sin campo `scanner`, sin `scanner.nextInt()`.
2. **`render()` ya no bloquea.** Vuelve en milisegundos si no hay tecla presionada.
3. **State machine intacta.** `UIState.DIALOGANDO` y `UIState.MENU_PAUSA` funcionan igual.
4. **Mapeo de teclas:** Teclas numéricas `1`-`9` para opciones, `0` para pausa, `ENTER` para continuar.
5. **Opciones bloqueadas:** Se muestran con `[BLOQUEADA]` y el input se ignora — misma semántica que el original pero sin llegar siquiera a llamar a `advance()`.
6. **Control de opciones:** Limitado a 9 opciones (teclas `NUM_1`..`NUM_9`). Si hay más, se necesitaría navegación por páginas.

### Nota sobre opciones bloqueadas

En el código original, las opciones bloqueadas no tenían verificación en `MainMenuScreen`. En `StoryScreen` original se llamaba a `advance()` sin verificar flags en el screen (la verificación estaba en el nodo destino). En esta versión refactorizada **sí verificamos** antes de avanzar porque el usuario podría presionar la tecla de una opción bloqueada. Esto es más robusto.

## 1.5 Verificación

Para probar que Fase 0 funciona:

```bash
cd Intentia
./gradlew lwjgl3:run
```

**Comportamiento esperado:**
- El menú principal imprime opciones en la terminal.
- Presionar `1` inicia nueva partida y cambia a StoryScreen.
- Presionar `2` (o `3` si hay guardado) ejecuta la acción correspondiente.
- El juego corre a 60 FPS sin congelarse.
- La ventana de la aplicación responde al SO (se puede mover, redimensionar, cerrar).

---

# FASE 1: MAINMENU VISUAL CON SCENE2D

## 2.1 Arquitectura Scene2D

### ¿Qué es Scene2D?

Scene2D es el framework de UI gráfica de libGDX. Está compuesto por:

| Componente | Rol |
|------------|-----|
| **Stage** | Contenedor raíz. Maneja el input y dibuja todos los actores. |
| **Actor** | Clase base de todo elemento visual (Label, TextButton, Image). |
| **Group** | Actor que contiene otros actores (Table, Stack, Window). |
| **Table** | Layout manager con filas/columnas (como HTML `<table>`). |
| **Skin** | Contenedor de estilos (colores, fuentes, drawables, 9-patches). |
| **Viewport** | Define cómo se proyectan las coordenadas virtuales a la pantalla. |

### Ciclo de vida de un Stage

```
show():
  stage = new Stage(new FitViewport(800, 600));
  Gdx.input.setInputProcessor(stage);  // ← el Stage recibe todo el input
  skin = new Skin(Gdx.files.internal("skin/pixel/uiskin.json"));
  table = new Table();
  table.setFillParent(true);
  stage.addActor(table);
  // agregar widgets a table

render(float delta):
  Gdx.gl.glClearColor(0.05f, 0.05f, 0.1f, 1);
  Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
  stage.act(delta);   // ← actualiza animaciones, acciones, input
  stage.draw();        // ← renderiza todos los actores

resize(int w, int h):
  stage.getViewport().update(w, h, true);  // ← recalcula proyección

dispose():
  stage.dispose();
  skin.dispose();
```

### ¿Por qué FitViewport(800, 600)?

`FitViewport` mantiene la relación de aspecto 4:3 y agrega barras negras (letterboxing) si la ventana no coincide con la proporción. Es la opción correcta porque:

1. **El arte pixel-art se ve igual en cualquier resolución** — los píxeles se escalan uniformemente.
2. **Sin deformación** — a diferencia de `ScreenViewport` o `StretchViewport`, no estira la imagen.
3. **Coordenadas predecibles** — (0,0) es abajo-izquierda, (800,600) es arriba-derecha.
4. **Fácil de prototipar** — diseñas para una resolución fija y funciona en todas.

Alternativas consideradas:

| Viewport | Comportamiento | ¿Recomendado? |
|----------|----------------|:---:|
| `FitViewport(800, 600)` | Escala manteniendo 4:3, letterboxing | ✅ |
| `ExtendViewport(800, 600)` | Escala manteniendo, extiende si sobra | ⚠️ (más área de juego) |
| `ScreenViewport` | Sin escalar (1 píxel = 1 unidad) | ❌ (menú se ve diminuto en HD) |
| `StretchViewport(800, 600)` | Deforma para llenar | ❌ (píxeles no cuadrados) |

### InputProcessor del Stage

Cuando haces `Gdx.input.setInputProcessor(stage)`, el Stage registra listeners internos que convierten eventos de teclado/mouse/touch en eventos de Scene2D:

```
Usuario hace clic en botón
  → Gdx.input detecta clic
  → Stage.inputProcessor.touchDown(x, y, pointer, button)
  → Stage.hit(x, y) → encuentra el Actor en esas coordenadas
  → Actor.fire(new ClickEvent())
  → ChangeListener.changed(event, actor)
```

**Importante:** Cuando el Stage es el InputProcessor, los eventos `isKeyJustPressed()` siguen funcionando en `render()` si se necesita input directo de teclado (e.g., tecla ESC para pausa).

## 2.2 Skin Setup

### Obteniendo la skin

La skin recomendada es **Kenney Pixel** del repositorio [gdx-skins](https://github.com/czyzby/gdx-skins).

1. Ir a: `https://github.com/czyzby/gdx-skins/tree/master/skins/kenney-pixel`
2. Copiar los archivos a `assets/skin/pixel/`:

```
assets/
  skin/
    pixel/
      uiskin.json       ← Definición de estilos
      uiskin.atlas      ← Texture atlas (empaqueta las imágenes)
      uiskin.png        ← Imagen del atlas (botones, paneles, etc.)
      default.fnt       ← Fuente bitmap por defecto
      default.png       ← Textura de la fuente bitmap
```

### Estructura del uiskin.json

```json
{
    com.badlogic.gdx.graphics.Color: {
        white: { r: 1, g: 1, b: 1, a: 1 },
        cyan: { r: 0, g: 1, b: 1, a: 1 },
        darkBg: { r: 0.05, g: 0.05, b: 0.1, a: 1 },
        gris: { r: 0.4, g: 0.4, b: 0.4, a: 1 },
        hoverAzul: { r: 0.3, g: 0.6, b: 1, a: 1 },
        tituloOro: { r: 1, g: 0.84, b: 0, a: 1 }
    },

    com.badlogic.gdx.graphics.g2d.BitmapFont: {
        pixel: { file: default.fnt },
        pixelGrande: { file: default.fnt }
    },

    com.badlogic.gdx.scenes.scene2d.ui.Label$LabelStyle: {
        default: {
            font: pixel,
            fontColor: white
        },
        titulo: {
            font: pixelGrande,
            fontColor: tituloOro
        }
    },

    com.badlogic.gdx.scenes.scene2d.ui.TextButton$TextButtonStyle: {
        default: {
            up: button-up,
            down: button-down,
            over: button-hover,
            font: pixel,
            fontColor: white,
            overFontColor: hoverAzul,
            disabledFontColor: gris
        }
    },

    com.badlogic.gdx.scenes.scene2d.ui.Window$WindowStyle: {
        default: {
            titleFont: pixel,
            titleFontColor: white,
            stageBackground: darkBg
        }
    }
}
```

### Integrando FreeTypeFontGenerator

`gdx-freetype` ya está en `build.gradle:8`:

```groovy
api "com.badlogicgames.gdx:gdx-freetype:$gdxVersion"
```

Usaremos `FreeTypeFontGenerator` para cargar una fuente `.ttf` pixel-art y generar `BitmapFont` con filtro `Nearest` para mantener el aspecto pixelado nítido.

## 2.3 Assets.java — Utilidad de Fuentes y Skin

> **Archivo destino:** `core/src/main/java/io/yourPath/utils/Assets.java`

Clase utilitaria que centraliza la creación de fuentes y carga de skin. Evita duplicación de código entre screens.

```java
package io.yourPath.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;

public class Assets {

    private static final String RUTA_SKIN = "skin/pixel/uiskin.json";
    private static final String RUTA_FUENTE = "fonts/pixel.ttf";

    private static Skin piel;
    private static BitmapFont fuentePorDefecto;
    private static BitmapFont fuenteTitulo;

    /**
     * Carga la skin desde el archivo JSON.
     * La skin incluye todos los estilos de botones, labels, ventanas, etc.
     * @return Skin lista para usar
     */
    public static Skin cargarPiel() {
        if (piel == null) {
            piel = new Skin(Gdx.files.internal(RUTA_SKIN));
        }
        return piel;
    }

    /**
     * Genera una fuente pixel-art usando FreeTypeFontGenerator.
     * Usa filtro Nearest para mantener bordes nítidos al escalar.
     * @param tamano tamaño en píxeles (ej: 16 para texto normal, 32 para títulos)
     * @return BitmapFont con filtro Nearest
     */
    public static BitmapFont generarFuentePixel(int tamano) {
        FileHandle archivoFuente = Gdx.files.internal(RUTA_FUENTE);
        if (!archivoFuente.exists()) {
            // Fallback: si no hay TTF, devolver la fuente por defecto del skin
            if (piel != null) {
                return piel.getFont("pixel");
            }
            return new BitmapFont();
        }

        FreeTypeFontGenerator generador = new FreeTypeFontGenerator(archivoFuente);
        FreeTypeFontGenerator.FreeTypeFontParameter parametros = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parametros.size = tamano;
        parametros.minFilter = Texture.TextureFilter.Nearest;
        parametros.magFilter = Texture.TextureFilter.Nearest;
        parametros.genMipMaps = false;

        BitmapFont fuente = generador.generateFont(parametros);
        generador.dispose();  // liberar el generador inmediatamente
        return fuente;
    }

    /**
     * Inyecta una fuente personalizada en la skin.
     * Reemplaza la fuente con nombre "pixel" por la generada con FreeType.
     * @param tamanoTexto tamaño para texto normal (ej: 16)
     * @param tamanoTitulo tamaño para títulos (ej: 32)
     */
    public static void inyectarFuentesPersonalizadas(int tamanoTexto, int tamanoTitulo) {
        if (piel == null) {
            cargarPiel();
        }

        fuentePorDefecto = generarFuentePixel(tamanoTexto);
        fuenteTitulo = generarFuentePixel(tamanoTitulo);

        if (fuentePorDefecto != null) {
            piel.add("pixel", fuentePorDefecto, BitmapFont.class);
        }
        if (fuenteTitulo != null) {
            piel.add("pixelGrande", fuenteTitulo, BitmapFont.class);
        }
    }

    /**
     * Libera todos los recursos cargados por Assets.
     */
    public static void dispose() {
        if (fuentePorDefecto != null) fuentePorDefecto.dispose();
        if (fuenteTitulo != null) fuenteTitulo.dispose();
        if (piel != null) piel.dispose();
        fuentePorDefecto = null;
        fuenteTitulo = null;
        piel = null;
    }

    // ── Getters ──

    public static Skin getPiel() {
        return piel;
    }

    public static BitmapFont getFuentePorDefecto() {
        return fuentePorDefecto;
    }

    public static BitmapFont getFuenteTitulo() {
        return fuenteTitulo;
    }
}
```

### Notas sobre Assets.java

- **Singleton perezoso:** La skin se carga una sola vez y se reusa. Pero **cuidado**: si se comparte entre screens, no hay que hacer `dispose()` hasta que todos los screens terminen. Alternativa segura: cada screen carga su propia skin.
- **FreeTypeFontGenerator debe hacerse dispose()** inmediatamente después de generar la fuente. El `BitmapFont` generado es independiente y no necesita el generador abierto.
- **Filtro Nearest** es esencial para pixel-art. Sin él, los bordes se ven borrosos (`Linear` filter).
- **Manejo de errores:** Si el archivo `.ttf` no existe, usa la fuente por defecto del skin o `new BitmapFont()`.

### Estructura de assets necesaria

```
assets/
  fonts/
    pixel.ttf                    ← Fuente TTF pixel-art (Press Start 2P o similar)
  skin/
    pixel/
      uiskin.json                ← Definición de estilos
      uiskin.atlas               ← Atlas de texturas
      uiskin.png                 ← Imagen del atlas
      default.fnt                ← Fuente bitmap (fallback)
      default.png                ← Textura de fuente (fallback)
```

**Dónde conseguir el TTF:** Descargar "Press Start 2P" de Google Fonts: https://fonts.google.com/specimen/Press+Start+2P

## 2.4 Código: MainMenuScreen con Scene2D

> **Archivo destino:** `core/src/main/java/io/yourPath/screens/MainMenuScreen.java`
> **Cambio:** Reemplazar completamente la implementación de consola por Scene2D.

```java
package io.yourPath.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.FitViewport;

import io.yourPath.Main;
import io.yourPath.logic.StoryManager;
import io.yourPath.models.GameState;
import io.yourPath.utils.Assets;
import io.yourPath.utils.SaveSystem;

public class MainMenuScreen implements Screen {

    private static final int ANCHO_VIRTUAL = 800;
    private static final int ALTO_VIRTUAL = 600;

    private Main juego;
    private Stage escenario;
    private Skin piel;
    private Table tabla;
    private TextButton botonNuevo;
    private TextButton botonContinuar;
    private TextButton botonSalir;

    public MainMenuScreen(Main juego) {
        this.juego = juego;
    }

    @Override
    public void show() {
        // ── 1. Crear Stage con FitViewport 800x600 ──
        escenario = new Stage(new FitViewport(ANCHO_VIRTUAL, ALTO_VIRTUAL));
        Gdx.input.setInputProcessor(escenario);

        // ── 2. Cargar Skin y fuentes personalizadas ──
        Assets.cargarPiel();
        Assets.inyectarFuentesPersonalizadas(16, 32);
        piel = Assets.getPiel();

        // ── 3. Layout principal con Table ──
        tabla = new Table();
        tabla.setFillParent(true);
        tabla.center();
        escenario.addActor(tabla);

        // ── 4. Título ──
        Label etiquetaTitulo = new Label("INTENTIA: EL LEGADO", piel, "titulo");
        etiquetaTitulo.setFontScale(1.8f);

        // ── 5. Botones ──
        botonNuevo = new TextButton("Nueva Partida", piel);
        botonContinuar = new TextButton("Continuar", piel);
        botonSalir = new TextButton("Salir", piel);

        // ── 6. Visibilidad del botón Continuar ──
        botonContinuar.setVisible(SaveSystem.exists());

        // ── 7. Listeners ──

        botonNuevo.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent evento, Actor actor) {
                juego.getStoryManager().start("car_awakening");
                juego.setScreen(new StoryScreen(juego, juego.getStoryManager(), juego.getCharacters()));
            }
        });

        botonContinuar.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent evento, Actor actor) {
                GameState guardado = SaveSystem.loadGame();
                if (guardado != null) {
                    juego.setStoryManager(new StoryManager(juego.getStory(), guardado));
                    juego.setScreen(new StoryScreen(juego, juego.getStoryManager(), juego.getCharacters()));
                }
            }
        });

        botonSalir.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent evento, Actor actor) {
                Gdx.app.exit();
            }
        });

        // ── 8. Armar layout ──
        tabla.add(etiquetaTitulo)
            .colspan(3)
            .padBottom(60)
            .center();
        tabla.row();

        tabla.add(botonNuevo)
            .width(260)
            .height(50)
            .padBottom(12);
        tabla.row();

        tabla.add(botonContinuar)
            .width(260)
            .height(50)
            .padBottom(12);
        tabla.row();

        tabla.add(botonSalir)
            .width(260)
            .height(50);

        // ── 9. Efecto fade in ──
        tabla.getColor().a = 0f;
        tabla.addAction(Actions.sequence(
            Actions.fadeIn(0.6f)
        ));
    }

    @Override
    public void render(float delta) {
        // ── Fondo oscuro #0D0D1A ──
        Gdx.gl.glClearColor(0.05f, 0.05f, 0.1f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Actualizar visibilidad por si se guardó mientras estábamos aquí
        botonContinuar.setVisible(SaveSystem.exists());

        escenario.act(delta);
        escenario.draw();

        // ── Input directo de teclado (opcional: ESC para salir rápido) ──
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            Gdx.app.exit();
        }
    }

    @Override
    public void resize(int ancho, int alto) {
        escenario.getViewport().update(ancho, alto, true);
    }

    @Override
    public void dispose() {
        escenario.dispose();
        // Nota: No hacer dispose() de skin aquí si se comparte via Assets.
        // Si cada screen carga su propia Skin, hacer skin.dispose().
        // Assets.dispose() se llama cuando la aplicación termina.
    }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
}
```

### Explicación línea por línea

| Sección | Explicación |
|---------|-------------|
| **`show()` — Stage** | `new Stage(new FitViewport(800, 600))` crea el escenario con coordenadas virtuales. `setInputProcessor(escenario)` entrega todo el input táctil/mouse al Stage. |
| **`show()` — Skin** | `Assets.cargarPiel()` carga el JSON del skin. `inyectarFuentesPersonalizadas(16, 32)` genera fuentes de 16px (texto) y 32px (título) con filtro Nearest. |
| **`show()` — Table** | `table.setFillParent(true)` hace que la tabla ocupe todo el Stage. `center()` centra el contenido. |
| **`show()` — Título** | `Label(..., "titulo")` usa el estilo `titulo` del skin (fuente pixelGrande + color oro). `setFontScale(1.8f)` agranda aún más. |
| **`show()` — Botón Continuar** | `setVisible(SaveSystem.exists())` oculta el botón si no hay partida guardada. |
| **`show()` — Listeners** | `ChangeListener` se dispara cuando el usuario suelta el clic sobre el botón. Es preferible a `ClickListener` porque es más semántico (cambio de estado vs. clic físico). |
| **`show()` — Layout** | `colspan(3)` hace que el título ocupe toda la fila. `padBottom(60)` separa el título de los botones. Los botones tienen `width(260)` y `height(50)` fijos. |
| **`show()` — Fade in** | `tabla.getColor().a = 0f` hace invisible la tabla. `Actions.sequence(fadeIn(0.6f))` la desvanece en 0.6 segundos. |
| **`render()`** | `glClearColor(0.05, 0.05, 0.1)` = #0D0D1A (azul oscuro casi negro). `stage.act(delta)` actualiza animaciones y acciones. `stage.draw()` renderiza todo. |
| **`resize()`** | `viewport.update(w, h, true)` recalcula la proyección del FitViewport. El `true` centra la cámara. |

### Efecto hover en botones

El efecto hover se maneja automáticamente por el skin a través de los drawables `over` y `overFontColor` en el `TextButtonStyle`. Cuando el mouse pasa sobre el botón, Scene2D cambia automáticamente al drawable `over` y al color `overFontColor`.

En el `uiskin.json`:
```json
"com.badlogic.gdx.scenes.scene2d.ui.TextButton$TextButtonStyle": {
    "default": {
        "up": "button-up",
        "down": "button-down",
        "over": "button-hover",
        "font": "pixel",
        "fontColor": "white",
        "overFontColor": "hoverAzul",
        "disabledFontColor": "gris"
    }
}
```

Si el skin de Kenney Pixel no incluye `button-hover`, se puede duplicar el drawable `button-up` con un tinte azul en el JSON:

```json
"com.badlogic.gdx.scenes.scene2d.utils.TintedDrawable": {
    "button-hover": {
        "name": "button-up",
        "color": { "r": 0.2, "g": 0.4, "b": 0.8, "a": 1 }
    }
}
```

### Diseño visual final del menú

```
┌────────────────────────────────────────────────┐
│                                                │
│                                                │
│                                                │
│            INTENTIA: EL LEGADO                 │  ← Label, fontScale 1.8, color oro
│                                                │
│                                                │
│            ┌──────────────────────┐            │
│            │    Nueva Partida     │            │  ← TextButton, 260x50
│            └──────────────────────┘            │
│                                                │
│            ┌──────────────────────┐            │
│            │      Continuar       │            │  ← TextButton, visible solo si save existe
│            └──────────────────────┘            │
│                                                │
│            ┌──────────────────────┐            │
│            │        Salir         │            │  ← TextButton
│            └──────────────────────┘            │
│                                                │
│                                                │
│  Fondo: #0D0D1A (RGB 0.05, 0.05, 0.10)       │
└────────────────────────────────────────────────┘
```

### Manejo de la escena: fade in completo

```java
// En show():
tabla.getColor().a = 0f;
tabla.addAction(Actions.sequence(
    Actions.fadeIn(0.6f)
));
```

Esto aplica un fade in a TODA la tabla (título y botones aparecen suavemente). Para un efecto más dramático, se podría hacer que los botones aparezcan escalonadamente:

```java
// Opción premium: fade in con escalón
etiquetaTitulo.getColor().a = 0f;
botonNuevo.getColor().a = 0f;
botonContinuar.getColor().a = 0f;
botonSalir.getColor().a = 0f;

etiquetaTitulo.addAction(Actions.fadeIn(0.4f));
botonNuevo.addAction(Actions.sequence(
    Actions.delay(0.15f),
    Actions.fadeIn(0.4f)
));
botonContinuar.addAction(Actions.sequence(
    Actions.delay(0.30f),
    Actions.fadeIn(0.4f)
));
botonSalir.addAction(Actions.sequence(
    Actions.delay(0.45f),
    Actions.fadeIn(0.4f)
));
```

## 2.5 Mejores Prácticas

### 1. Stage se crea en `show()`, se destruye en `dispose()`

**Correcto:**
```java
public void show() {
    escenario = new Stage(new FitViewport(800, 600));
    Gdx.input.setInputProcessor(escenario);
    // ... construir UI ...
}

public void dispose() {
    escenario.dispose();  // ← libera todos los actores
}

public void hide() {
    // No hacer dispose() aquí — la Screen puede reutilizarse
}
```

**Incorrecto:**
```java
// ❌ No crear Stage en el constructor
public MainMenuScreen(Main juego) {
    escenario = new Stage(...);  // ¡MAL! Gdx aún no está inicializado
}
```

### 2. `resize()` debe actualizar el viewport

```java
@Override
public void resize(int ancho, int alto) {
    escenario.getViewport().update(ancho, alto, true);  // ← OBLIGATORIO
}
```

Sin esto, el viewport mantiene el tamaño original y la UI se ve mal al redimensionar.

### 3. No crear listeners anónimos en cada render

**Mal:**
```java
public void render(float delta) {
    botonNuevo.addListener(new ChangeListener() { ... });  // ← NUEVO LISTENER CADA FRAME
}
```

**Bien:**
```java
// Los listeners se crean UNA VEZ en show()
public void show() {
    botonNuevo.addListener(new ChangeListener() { ... });
}
```

### 4. Usar ChangeListener en lugar de ClickListener

- `ChangeListener`: Se dispara cuando el valor del widget cambia (ej: botón presionado y soltado). Es semánticamente correcto para botones.
- `ClickListener`: Se dispara en cualquier clic, incluso si el usuario hace clic fuera del botón y arrastra. Puede causar activaciones accidentales.

```java
// ✅ Correcto para botones
botonNuevo.addListener(new ChangeListener() {
    public void changed(ChangeEvent e, Actor a) {
        // acción
    }
});

// ❌ Demasiado genérico
botonNuevo.addListener(new ClickListener() {
    public void clicked(InputEvent e, float x, float y) {
        // esto se dispara incluso si arrastras fuera del botón
    }
});
```

### 5. Compartir Skin entre screens vs. independiente

| Enfoque | Ventajas | Desventajas |
|---------|----------|-------------|
| Skin compartida (Assets.java) | Una sola carga en memoria | Cuidado con dispose(): no liberar hasta que todos los screens terminen |
| Skin independiente por screen | Aislación total, dispose() simple | Múltiples cargas de la misma textura |

**Recomendación para Fase 1:** Compartir via `Assets.java`. Cuando la app tenga más screens (Fase 2+), migrar a `AssetManager`.

### 6. No olvidar el InputProcessor

Cada vez que se cambia de screen, el nuevo screen debe registrar su Stage como InputProcessor:

```java
// En el screen nuevo:
public void show() {
    escenario = new Stage(...);
    Gdx.input.setInputProcessor(escenario);  // ← IMPRESCINDIBLE
}
```

Si no se hace, el Stage anterior sigue recibiendo input, o ningún input funciona.

### 7. Clear color por frame

Siempre limpiar el buffer de color al inicio de `render()`:

```java
public void render(float delta) {
    Gdx.gl.glClearColor(r, g, b, 1);
    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
    // ...
}
```

Sin esto, se ven "rastros" del frame anterior (efecto ghosting).

### 8. Disposición final en Main.java

Cuando la aplicación se cierra, `Main.dispose()` debe liberar recursos globales:

```java
// En Main.java:
@Override
public void dispose() {
    if (getScreen() != null) getScreen().dispose();
    Assets.dispose();
}
```

### 9. Separación de responsabilidades (Skin JSON)

Mantener los estilos en el JSON de la skin, no en código:

```java
// ✅ EN JSON:
// "titulo": { "font": "pixelGrande", "fontColor": "tituloOro" }

// ✅ EN CÓDIGO:
Label titulo = new Label("TEXT", skin, "titulo");  // ← limpio, reutilizable

// ❌ EN CÓDIGO:
Label.LabelStyle estilo = new Label.LabelStyle();
estilo.font = assets.getFont("pixelGrande");
estilo.fontColor = Color.GOLD;
Label titulo = new Label("TEXT", estilo);  // ← hardcoded, difícil de cambiar
```

---

# 3. CHECKLIST DE VERIFICACIÓN

## FASE 0: Refactor de Input

### MainMenuScreen
- [ ] `import java.util.Scanner` eliminado
- [ ] Campo `Scanner scanner` eliminado
- [ ] `Scanner(System.in)` eliminado del constructor
- [ ] `Gdx.input.isKeyJustPressed()` usado en `render()` para todas las opciones
- [ ] `NUM_1` → `start("car_awakening")` + `setScreen(new StoryScreen(...))`
- [ ] `NUM_2` (con save) → `SaveSystem.loadGame()` + `setScreen(new StoryScreen(...))`
- [ ] `NUM_2` (sin save) → `Gdx.app.exit()`
- [ ] `NUM_3` (con save) → `Gdx.app.exit()`
- [ ] `partidaGuardada` se recalcula cada render
- [ ] `return` después de cada acción para evitar múltiples activaciones
- [ ] `show()` recalcula `partidaGuardada`
- [ ] Sin `System.out` bloqueante (seguimos imprimiendo, pero el input no bloquea)

### StoryScreen
- [ ] `import java.util.Scanner` eliminado
- [ ] Campo `Scanner scanner` eliminado
- [ ] Constructor sin `new Scanner(System.in)`
- [ ] `Gdx.input.isKeyJustPressed()` para opciones numéricas (1-9, 0 para pausa)
- [ ] `ENTER` para continuar (cuando no hay opciones)
- [ ] Opciones bloqueadas verifican `requiredFlag` antes de avanzar
- [ ] State machine `UIState.DIALOGANDO` / `MENU_PAUSA` intacta
- [ ] `procesarMenuPausa()` usa teclas 1, 2, 3
- [ ] Guardado funciona en pausa
- [ ] Vuelta al menú funciona al final del prólogo
- [ ] `render()` retorna inmediatamente si no hay tecla presionada

### Verificación Técnica
- [ ] `./gradlew lwjgl3:run` compila sin errores
- [ ] El juego no se congela al mostrar menú
- [ ] Las opciones responden inmediatamente al presionar teclas
- [ ] La ventana se puede mover y redimensionar sin que el juego se cuelgue
- [ ] 60 FPS estables (monitorear con `Gdx.graphics.getFramesPerSecond()`)

---

## FASE 1: MainMenu Visual con Scene2D

### Arquitectura
- [ ] `Stage` creado en `show()` con `FitViewport(800, 600)`
- [ ] `Gdx.input.setInputProcessor(escenario)` en `show()`
- [ ] `resize()` actualiza viewport: `escenario.getViewport().update(w, h, true)`
- [ ] `dispose()` libera escenario (y skin si es independiente)

### Skin y Fuentes
- [ ] Archivos de skin copiados a `assets/skin/pixel/`
- [ ] `uiskin.json` define colores, fuentes, estilos de Label y TextButton
- [ ] `uiskin.atlas` y `uiskin.png` presentes
- [ ] `default.fnt` y `default.png` presentes (fallback)
- [ ] Fuente TTF pixel-art en `assets/fonts/pixel.ttf`
- [ ] `FreeTypeFontGenerator` configurado con filtro `Nearest`
- [ ] `Assets.java` creado con métodos `cargarPiel()`, `generarFuentePixel()`, `inyectarFuentesPersonalizadas()`
- [ ] Fuente personalizada inyectada en la skin correctamente

### UI Completa
- [ ] Título "INTENTIA: EL LEGADO" visible con estilo "titulo" (fuente grande, color oro)
- [ ] Botón "Nueva Partida" funciona → `start("car_awakening")` → cambia a `StoryScreen`
- [ ] Botón "Continuar" visible solo si `SaveSystem.exists()`
- [ ] Botón "Continuar" carga `GameState` y cambia a `StoryScreen`
- [ ] Botón "Salir" → `Gdx.app.exit()`

### Efectos Visuales
- [ ] Fade in de la tabla al entrar (0.6s)
- [ ] Hover en botones cambia de color (drawable `over` + `overFontColor` en skin)
- [ ] Fondo `glClearColor(0.05, 0.05, 0.1)` = #0D0D1A

### Mejores Prácticas
- [ ] Listeners creados en `show()`, no en `render()`
- [ ] `ChangeListener` usado en lugar de `ClickListener`
- [ ] Sin referencias a `Scanner` o `System.out` en la versión visual
- [ ] `dispose()` de Stage correcto
- [ ] Sin fuga de recursos (skin, fuentes)

### Verificación Técnica
- [ ] `./gradlew lwjgl3:run` compila sin errores
- [ ] Menú se ve con fondo oscuro y botones centrados
- [ ] Botones responden al clic del mouse
- [ ] Botón Continuar aparece/desaparece según exista save
- [ ] Fade in se ve suave, no abrupto
- [ ] Hover resalta botones
- [ ] Redimensionar ventana mantiene proporción (letterboxing)
- [ ] ESC cierra la aplicación
- [ ] `dispose()` se llama correctamente al salir (sin warnings en consola)

---

## RESUMEN DE ARCHIVOS CREADOS/MODIFICADOS

| Archivo | Acción | Fase |
|---------|--------|:----:|
| `core/.../screens/MainMenuScreen.java` | Modificar (Fase 0 → Fase 1) | 0 y 1 |
| `core/.../screens/StoryScreen.java` | Modificar (solo Fase 0) | 0 |
| `core/.../utils/Assets.java` | **NUEVO** | 1 |
| `assets/fonts/pixel.ttf` | **NUEVO** | 1 |
| `assets/skin/pixel/uiskin.json` | **NUEVO** | 1 |
| `assets/skin/pixel/uiskin.atlas` | **NUEVO** | 1 |
| `assets/skin/pixel/uiskin.png` | **NUEVO** | 1 |
| `assets/skin/pixel/default.fnt` | **NUEVO** | 1 |
| `assets/skin/pixel/default.png` | **NUEVO** | 1 |

## DIAGRAMA DE FLUJO COMPLETO

```
                    ┌──────────────────┐
                    │   Main.java      │
                    │   create()       │
                    │                  │
                    │  setScreen(      │
                    │   MainMenuScreen)│
                    └────────┬─────────┘
                             │
                    ┌────────▼─────────┐
                    │  MainMenuScreen  │
                    │                  │
                    │  ┌────────────┐  │
                    │  │"Nueva      │  │
                    │  │ Partida"   │──┼──→ storyManager.start("car_awakening")
                    │  └────────────┘  │      → new StoryScreen(game, sm, chars)
                    │  ┌────────────┐  │
                    │  │"Continuar" │──┼──→ SaveSystem.loadGame()
                    │  └────────────┘  │      → new StoryManager(story, saved)
                    │  ┌────────────┐  │      → new StoryScreen(game, sm2, chars)
                    │  │  "Salir"   │──┼──→ Gdx.app.exit()
                    │  └────────────┘  │
                    └──────────────────┘
                             │
                    ┌────────▼─────────┐
                    │   StoryScreen    │
                    │  (consola aún)   │
                    │                  │
                    │  DIALOGANDO      │
                    │  MENU_PAUSA      │
                    └──────────────────┘
```
