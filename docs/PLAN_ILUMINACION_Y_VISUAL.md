# Plan de Iluminación y Mejora Visual — Intentia

> Documento de planificación para añadir iluminación 2D, sombras y efectos visuales al motor gráfico existente (libGDX + LWJGL3).
> Basado en el estado actual del proyecto tras la Fase de Encarnación (GameScreen con Tiled, sprites, diálogos Scene2D).

---

## 1. Estado Visual Actual

| Aspecto | Estado |
|---------|--------|
| Renderizado | SpriteBatch + OrthogonalTiledMapRenderer |
| Resolución | 640x360 (FitViewport) |
| Filtrado | Nearest (pixel-art nítido) |
| Luces | Ninguna |
| Sombras | 41 PNGs de sombras en assets sin usar |
| Shaders | Ninguno (OpenGL ES 2.0 vanilla) |
| Post-processing | Ninguno |
| Atmósfera | Color sólido de fondo (0.1, 0.1, 0.15) |

---

## 2. Filosofía Visual

El juego es pixel-art 2D (estilo SNES/GBA) con temática filosófica/narrativa. La iluminación debe:

- Reforzar la atmósfera sin romper la estética retro
- Ser sutil — luces de ventanas, hogueras, linternas, amaneceres
- Guiar la atención del jugador en el mapa (zonas iluminadas = importantes)
- Soportar el tono narrativo (momentos oscuros, revelaciones, calidez)

**Referencias visuales:**
- *Hyper Light Drifter* — pixel-art + iluminación dinámica 2D
- *Blasphemous* — atmósfera oscura con focos de luz
- *Dead Cells* — iluminación 2D con ambientación
- *Nocturnal* — linterna y oscuridad como mecánica
- *Eastward* — pixel-art cálido con luces y niebla

---

## 3. Enfoques de Implementación

### 3.1 Box2DLights (Recomendado como primera fase)

Dependencia oficial de libGDX que añade luces 2D con sombras dinámicas sobre Box2D.

**Ventajas:**
- Ya tenéis Box2D como dependencia en build.gradle
- API simple — ~30 líneas para tener luz funcionando
- Sombras dinámicas contra cuerpos de Box2D
- Soporta: PointLight, ConeLight, DirectionalLight, ambient light

**Desventajas:**
- Requiere cuerpos Box2D en el mapa (collision bodies)
- Rendimiento: bien para 640x360, puede escalar mal con >50 luces

**Dependencia a añadir:**
```gradle
// core/build.gradle
implementation "com.badlogicgames.gdx:gdx-box2dlights:$gdxVersion"
```

### 3.2 Sombras con Sprites (Mínimo esfuerzo)

Usar los 41 PNGs de sombras ya existentes en `assets/TileSet/Art/Shadows/`.

**Ventajas:**
- Cero nuevas dependencias
- Assets ya listos
- Mejora visual inmediata

**Desventajas:**
- Sombras estáticas (no reaccionan a la luz)
- Solo profundidad básica
- Más trabajo manual al añadir entidades nuevas

### 3.3 Shaders GLSL Personalizados (Fase avanzada)

Shaders fragment + vertex para iluminación por píxel, normal maps y post-processing.

**Ventajas:**
- Control total sobre el look
- Efectos imposibles con Box2DLights (bloom, night vision, color grading)
- Más eficiente que Box2DLights en ciertos casos

**Desventajas:**
- Mayor complejidad técnica
- Requiere generar normal maps para tiles y sprites
- Curva de aprendizaje alta

---

## 4. Roadmap de Implementación

### ⚪ FASE L1: Sombras estáticas (1-2 días)

Usar los assets de sombra ya existentes para dar profundidad inmediata.

**Tareas:**
- [ ] Cargar los PNGs de sombra relevantes (round, square, character shadow)
- [ ] En `GameScreen.render()`, dibujar sombra debajo de cada entidad:

```java
// Ejemplo conceptual:
spriteBatch.setColor(0, 0, 0, 0.3f); // negra al 30%
spriteBatch.draw(shadowTexture, player.x - 8, player.y - 4, 32, 16);
spriteBatch.setColor(Color.WHITE);    // restaurar
spriteBatch.draw(playerFrame, player.x, player.y, 32, 48);
```

- [ ] Asignar sombras a NPCs según su tamaño
- [ ] Sombras para objetos del mapa (árboles, bancos, faroles)
- [ ] **Verificar:** rendimiento sin drops de frames

### 🟡 FASE L2: Box2DLights (1-2 semanas)

Integrar iluminación dinámica real con Box2DLights.

**Tareas previas:**
- [ ] Añadir `gdx-box2dlights` a `core/build.gradle`
- [ ] Sincronizar proyecto (`./gradlew --refresh-dependencies`)

**Implementación:**

#### 4.2.1 Configurar RayHandler en GameScreen

```java
// En GameScreen.java
import com.badlogic.gdx.physics.box2d.Box2D;
import com.badlogic.gdx.physics.box2d.World;
import box2dLight.RayHandler;
import box2dLight.PointLight;

public class GameScreen {
    private World box2dWorld;
    private RayHandler rayHandler;
    private float[] ambientColor = {0.03f, 0.03f, 0.05f, 1f}; // noche

    void initLighting() {
        Box2D.init();
        box2dWorld = new World(Vector2.Zero, true);
        rayHandler = new RayHandler(box2dWorld);
        rayHandler.setAmbientLight(ambientColor[0], ambientColor[1], ambientColor[2], ambientColor[3]);
        rayHandler.setBlurNum(3); // suavizado de sombras

        // Luz ambiental variable según zona
        // Día:   (0.4, 0.4, 0.5, 1)
        // Tarde: (0.2, 0.2, 0.3, 1)
        // Noche: (0.03, 0.03, 0.05, 1)
    }
}
```

#### 4.2.2 Añadir cuerpos Box2D al mapa

```java
void buildCollisionBodies() {
    MapLayer layer = tiledMap.getLayers().get("Collision");
    if (layer instanceof MapObjectsLayer) {
        for (MapObject obj : ((MapObjectsLayer) layer).getObjects()) {
            if (obj instanceof RectangleMapObject rect) {
                BodyDef def = new BodyDef();
                def.type = BodyDef.BodyType.StaticBody;
                def.position.set(rect.getRectangle().x * unitScale,
                                 rect.getRectangle().y * unitScale);
                Body body = box2dWorld.createBody(def);
                PolygonShape shape = new PolygonShape();
                shape.setAsBox(rect.getRectangle().width / 2 * unitScale,
                               rect.getRectangle().height / 2 * unitScale);
                body.createFixture(shape, 1f);
                shape.dispose();
            }
        }
    }
}
```

#### 4.2.3 Colocar puntos de luz en el mapa

```java
void placeLights() {
    // Farola
    PointLight farola = new PointLight(rayHandler, 64);
    farola.setColor(1f, 0.9f, 0.6f, 1f); // amarillo cálido
    farola.setDistance(80f);
    farola.setPosition(15 * 16, 20 * 16);

    // Ventana de casa
    PointLight ventana = new PointLight(rayHandler, 32);
    ventana.setColor(0.8f, 0.6f, 0.3f, 1f);
    ventana.setDistance(40f);
    ventana.setPosition(25 * 16, 30 * 16);

    // Hoguera (con parpadeo)
    PointLight hoguera = new PointLight(rayHandler, 48);
    hoguera.setColor(1f, 0.5f, 0.1f, 1f); // naranja
    hoguera.setDistance(100f);
    hoguera.setPosition(10 * 16, 35 * 16);
}
```

#### 4.2.4 Linterna del jugador

```java
public class Player {
    private PointLight linterna;

    void initLight(World world, RayHandler rayHandler) {
        linterna = new PointLight(rayHandler, 32);
        linterna.setColor(0.9f, 0.95f, 1f, 1f); // blanco suave
        linterna.setDistance(60f);
        linterna.attachToBody(playerBody, 0, 0);
    }

    void toggleLinterna() {
        linterna.setActive(!linterna.isActive());
    }
}
```

#### 4.2.5 Modificar el render loop

```java
@Override
public void render(float delta) {
    // 1. Actualizar física y luces
    box2dWorld.step(delta, 6, 2);
    rayHandler.update();

    // 2. Renderizar mapa (capas base)
    mapRenderer.render(backgroundLayers);

    // 3. Renderizar luces sobre el mapa pero bajo entidades
    rayHandler.setCombinedMatrix(camera.combined);
    rayHandler.renderOnly();

    // 4. Renderizar entidades (jugador, NPCs) — con iluminación
    spriteBatch.setProjectionMatrix(camera.combined);
    spriteBatch.begin();
    renderEntities(spriteBatch);
    spriteBatch.end();

    // 5. Renderizar capas de foreground (techos, árboles)
    mapRenderer.render(foregroundLayers);

    // 6. Renderizar UI (Stage)
    stage.act(delta);
    stage.draw();
}
```

**Consideraciones:**
- [ ] Las entidades (player, NPCs) se renderizan con la luz aplicada automáticamente por RayHandler
- [ ] Capas de foreground (techos, árboles) se renderizan *después* de las luces para que tengan sombra natural
- [ ] Ajustar `setBlurNum` para controlar suavizado (3 = suave, 1 = duro tipo pixel)

**Luces recomendadas por zona:**
| Zona | Luz ambiental | Luces puntuales | Tono |
|------|---------------|-----------------|------|
| Exterior día | 0.5, 0.5, 0.6 | Farolas apagadas | Neutro |
| Exterior tarde | 0.3, 0.3, 0.4 | Farolas empiezan a brillar | Cálido |
| Exterior noche | 0.03, 0.03, 0.08 | Farolas, ventanas, linterna | Azulado |
| Interior | 0.15, 0.15, 0.2 | Velas, chimenea | Ámbar |
| Bosque | 0.1, 0.12, 0.08 | Hojas que filtran luz solar | Verdoso |

### 🔵 FASE L3: Efectos Shader (2-4 semanas)

Para cuando Box2DLights se quede corto, añadir GLSL.

**Tareas:**
- [ ] Crear `shaders/` en assets con archivos `.vert` y `.frag`
- [ ] Implementar `ShaderProgram` en GameScreen
- [ ] Efectos candidatos:

#### 4.3.1 Bloom (resplandor en luces brillantes)

Fragment shader que identifica píxeles brillantes y los extiende:

```glsl
// bloom_fragment.glsl (conceptual)
void main() {
    vec4 color = texture2D(u_texture, v_texCoord);
    float brightness = dot(color.rgb, vec3(0.2126, 0.7152, 0.0722));
    if (brightness > 0.8) {
        gl_FragColor = color * 1.5;
    } else {
        gl_FragColor = color;
    }
}
```

#### 4.3.2 Normal Maps con iluminación por píxel

Requiere generar normal maps para tiles y sprites (herramientas: Laigter, Sprite Lamp, GIMP normal map plugin).

```glsl
// normal_lighting.frag (conceptual)
uniform vec2 u_lightPos;
uniform vec3 u_lightColor;

void main() {
    vec4 color = texture2D(u_texture, v_texCoord);
    vec3 normal = texture2D(u_normalMap, v_texCoord).rgb;
    normal = normalize(normal * 2.0 - 1.0);

    vec3 lightDir = normalize(vec3(u_lightPos - v_position, 1.0));
    float diff = max(dot(lightDir, normal), 0.0);

    gl_FragColor = vec4(color.rgb * (diff * u_lightColor), color.a);
}
```

#### 4.3.3 Post-processing (FrameBuffer + Shader)

Para efectos de pantalla completa (niebla, underwater, transiciones estilizadas):

```java
void renderWithPostProcess() {
    // Renderizar a framebuffer
    fbo.begin();
    renderGame(delta);
    fbo.end();

    // Aplicar shader de post-procesado
    postProcessShader.bind();
    postProcessShader.setUniformf("u_time", totalTime);
    postProcessShader.setUniformf("u_vignette", 0.3f);

    // Dibujar FBO con shader
    spriteBatch.setShader(postProcessShader);
    spriteBatch.begin();
    spriteBatch.draw(fboColorBuffer, 0, 0, viewportWidth, viewportHeight);
    spriteBatch.end();
    spriteBatch.setShader(null);
}
```

### 🟣 FASE L4: Atmósfera y Efectos Ambientales (1 semana)

Una vez la iluminación base funciona, añadir capa atmosférica.

**Tareas:**
- [ ] **Niebla:** Capa de color semitransparente que se mueve lentamente

```java
void renderFog() {
    spriteBatch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    spriteBatch.setColor(0.8f, 0.85f, 0.9f, 0.15f); // niebla blanquecina al 15%
    spriteBatch.draw(fogTexture, fogOffsetX, fogOffsetY, mapWidth, mapHeight);
    spriteBatch.setColor(Color.WHITE);
    spriteBatch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
}
```

- [ ] **Partículas:** `ParticleEffect` de libGDX para:
  - Hojas cayendo (bosque)
  - Polvo/tierra (camino seco)
  - Chispas (hoguera)
  - Lluvia (tormenta narrativa)
- [ ] **Ciclo día/noche:** Cambiar luz ambiental gradualmente con el tiempo

```java
void updateTimeOfDay(float delta) {
    timeOfDay += delta * 0.01f; // ciclo lento
    if (timeOfDay > 1f) timeOfDay = 0f;

    // 0.0 = medianoche, 0.5 = mediodía
    float dayFactor = (float) Math.sin(timeOfDay * Math.PI * 2);
    float ambient = 0.03f + (dayFactor * 0.47f); // 0.03 -> 0.5

    rayHandler.setAmbientLight(ambient * 0.5f, ambient * 0.5f, ambient * 0.7f, 1f);
}
```

- [ ] **Lluvia:** Sistema simple de partículas o sprites cayendo con sonido ambiental

---

## 5. Integración con la Narrativa

El sistema de iluminación debe conectarse con `StoryManager` y `GameState` para cambios atmosféricos dirigidos por la historia.

```java
// En GameState.java o nuevo AmbientState.java
public class AmbientState {
    private float ambientR, ambientG, ambientB;
    private float fogDensity;
    private boolean isRaining;
    private String timeOfDay; // "day", "dusk", "night"

    // Métodos para cambiar ambiente desde la narrativa
    public void setStorm() { /* oscurecer, activar lluvia, relámpagos */ }
    public void setSunrise() { /* transición suave a día */ }
    public void setDreamSequence() { /* niebla densa, luces distorsionadas */ }
}
```

**Disparadores desde StoryManager:**
```java
// En un NodeAction:
{
    "type": "set_ambient",
    "params": {
        "ambientColor": [0.8, 0.7, 0.5],
        "fog": 0.0,
        "rain": false
    }
}
```

---

## 6. Optimización y Rendimiento

| Técnica | Impacto visual | Coste de rendimiento | Prioridad |
|---------|---------------|---------------------|-----------|
| Sombras sprite | Bajo | Muy bajo | Alta |
| Box2DLights (16-32 luces) | Alto | Bajo-Medio | Alta |
| Box2DLights (50+ luces) | Alto | Medio | Bajo |
| Blur en luces | Medio | Mínimo | Alta |
| Normal maps + pixel light | Muy alto | Medio-Alto | Baja |
| Post-processing full screen | Alto | Alto | Baja |
| Partículas (50-100) | Medio | Bajo | Media |
| FrameBuffer effects | Alto | Medio | Media |

**Target:** 60fps estables en 640x360.

---

## 7. Dependencias a Añadir

```gradle
// core/build.gradle
dependencies {
    implementation "com.badlogicgames.gdx:gdx-box2d:$gdxVersion"     // ya incluida
    implementation "com.badlogicgames.gdx:gdx-box2dlights:$gdxVersion" // NUEVA
}

// lwjgl3/build.gradle
dependencies {
    implementation "com.badlogicgames.gdx:gdx-box2d-platform:$gdxVersion:natives-desktop"     // ya incluida
    // Box2DLights no necesita natives específicos (es puro Java)
}
```

**Nota:** Box2DLights no tiene versión por separado; se incluye con `gdx-box2dlights` usando la misma versión de libGDX.

---

## 8. Cronograma Estimado

| Fase | Descripción | Días (parcial) |
|------|-------------|----------------|
| L1 | Sombras estáticas (assets existentes) | 1-2 |
| L2 | Box2DLights + cuerpos Box2D en mapa | 5-10 |
| L3 | Shaders GLSL (bloom, normal maps, post) | 10-20 |
| L4 | Partículas, niebla, ciclo día/noche, lluvia | 3-5 |

**Total:** ~3-6 semanas en paralelo al desarrollo normal.

---

## 9. Assets Necesarios

### Existentes (sin usar):
- `assets/TileSet/Art/Shadows/*.png` (41 sombras)
- Sprites de hoguera (`fireplace`, `campfire`)
- Sprites de faroles (`LampPost`)

### Por crear:
- Normal maps para tilesets y sprites (herramienta recomendada: Laigter)
- Textura de niebla (512x512 gradiente suave)
- Spritesheet de partículas (hojas, lluvia, chispas)
- Textura de linterna (glow radial)

---

## 10. Arquitectura Final (con iluminación)

```
screens/
├── GameScreen.java
│   ├── OrthogonalTiledMapRenderer
│   ├── World (Box2D)
│   ├── RayHandler (Box2DLights)
│   ├── SpriteBatch (entidades)
│   ├── Stage (UI)
│   └── ShaderProgram (opcional post-processing)
├── DialogOverlayScreen.java
├── MainMenuScreen.java
└── CinematicScreen.java

ambient/
├── AmbientState.java         // estado atmosférico actual
├── DayNightCycle.java        // ciclo día/noche automático
├── WeatherSystem.java        // lluvia, niebla, viento
└── LightingManager.java      // gestión de luces por zona

shaders/
├── bloom.vert / bloom.frag
├── lighting.vert / lighting.frag
└── post_process.vert / post_process.frag
```

---

## 11. Prioridades Recomendadas

1. **FASE L1 — Inmediata (1-2 días):** Dibujar sombras estáticas de los assets existentes. Código mínimo, mejora visible.
2. **FASE L2 — Corto plazo (1-2 semanas):** Box2DLights con mínimas luces (5-10). Iluminar hogueras, farolas, linterna del jugador.
3. **FASE L4 — Medio plazo (1 semana):** Niebla y partículas básicas para atmósfera.
4. **FASE L3 — Largo plazo (2-4 semanas):** Shaders GLSL cuando el resto funcione sólido.

> **Regla de oro:** No implementar shaders hasta que Box2DLights esté estable y bien integrado. El salto de "sin luces" a "luces dinámicas" es mucho mayor que el salto de "Box2DLights" a "shaders".
