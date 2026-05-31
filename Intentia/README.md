# Intentia: El Legado — Rama `logic`

> *Branch del motor narrativo. libGDX apenas asoma; esto es Java puro modelando la lógica del juego.*

En esta rama, libGDX se usa únicamente como esqueleto de proyecto (la clase `Main` extiende `Game`, y `SaveSystem` usa su serializador `Json`). Todo lo demás — modelos, DAO, motor narrativo, evaluación de trials, sistema de flags — es Java estándar, independiente del framework.

La idea es que el **motor de juego** esté completamente desacoplado de la capa gráfica. Cuando llegue la fase visual, libGDX consumirá esta lógica sin tocar una línea del núcleo.

---

## Arquitectura

```
io.yourPath/
├── Main.java                        # Punto de entrada. Inicializa DAO + StoryManager.
├── screens/                (delgada)
│   ├── MainMenuScreen.java          # Menú por consola (Scanner).
│   └── StoryScreen.java             # Diálogo por consola.
├── models/                 (núcleo)
│   ├── NarrativeNode.java           # Clase abstracta: nodo narrativo base.
│   ├── DialogNode.java              # Nodo con opciones seleccionables.
│   ├── TrialNode.java               # Nodo de juicio/evaluación.
│   ├── DialogOption.java            # Opción: texto, destino, flag, puntuación.
│   ├── TrialEvaluation.java         # Umbral, rutas de éxito/fallo.
│   ├── CharacterProfile.java        # id, nombre, retrato.
│   ├── GameState.java               # Flags, nodo actual, puntuación de prueba.
│   └── UIState.java                 # Enum: DIALOGANDO / MENU_PAUSA.
├── logic/                  (cerebro)
│   └── StoryManager.java            # Motor narrativo: avance, trials, acciones.
└── utils/                 (infraestructura)
    ├── NarrativeDAO.java            # Interfaz DAO.
    ├── NarrativeDAOImplementation.java # SQLite vía JDBC.
    ├── IntentiaException.java       # Excepción personalizada.
    └── SaveSystem.java              # Persistencia JSON (único con libGDX aquí).
```

### Lo que NO usa libGDX

| Componente | Dependencia |
|---|---|
| `NarrativeNode`, `DialogNode`, `TrialNode`, `DialogOption`, `TrialEvaluation`, `CharacterProfile`, `UIState` | 100% Java |
| `GameState` (flags, scores) | 100% Java |
| `StoryManager` (navegación, trials, acciones) | 100% Java |
| `NarrativeDAO` + `NarrativeDAOImplementation` | Solo JDBC (SQLite) |
| `IntentiaException` | 100% Java |

### Lo que SÍ usa libGDX

| Componente | Uso de libGDX |
|---|---|
| `Main` | Extiende `com.badlogic.gdx.Game` |
| `MainMenuScreen`, `StoryScreen` | Implementan `Screen` (pero su IO es `Scanner`/`System.out`, no Stage) |
| `SaveSystem` | Usa `Gdx.files.local()` y `com.badlogic.gdx.utils.Json` |

---

## Motor Narrativo (StoryManager)

El `StoryManager` maneja el flujo sin depender de libGDX:

1. **Entrada a nodo** → ejecuta `actions` como flags en `GameState`.
2. **Opciones filtradas** → solo muestra `DialogOption`s cuyo `requiredFlag` esté presente.
3. **Evaluación de Trial** → al llegar a un `TrialNode`, calcula porcentaje vs umbral y bifurca.
4. **Persistencia** → `SaveSystem` serializa `GameState` a `save.json`.

```
Jugador elige → StoryManager.advance(option)
  → suma puntuación
  → navega al nodo destino
  → si es TrialNode: evalúa % vs threshold
    → éxito: flag + successTargetId
    → fallo: failTargetId
  → render en consola
```

---

## Tecnologías (reales)

| Componente | Tecnología |
|---|---|
| Lógica del juego | Java 17 puro |
| Motor narrativo | `StoryManager` (0 dependencias externas) |
| Base de datos | SQLite vía JDBC |
| Serialización | libGDX `Json` (única dependencia real) |
| Build | Gradle |
| UI actual | Consola (`System.out` / `Scanner`) |

---

## Ejecución

```bash
./gradlew lwjgl3:run
```

Requiere `database/intentia.db` en el directorio de trabajo.

---

## Estado de la Rama

```
logic ────●──→ master (cuando llegue la UI visual)
           │
           └── Aquí se solidifica el motor.
               libGDX es solo el andamio.
```

El siguiente paso será reemplazar las pantallas de consola por `Stage`+`Skin` de libGDX, manteniendo intacto todo lo que está en `models/`, `logic/` y `utils/`.
