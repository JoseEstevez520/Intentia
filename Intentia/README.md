# Intentia: El Legado

> *"Juzgar si la vida vale o no vale la pena vivir es responder a la pregunta fundamental de la filosofía."* — Albert Camus

Una aventura narrativa interactiva construida con [libGDX](https://libgdx.com/), donde las decisiones del jugador moldean la historia a través de un sistema de flags, evaluaciones y ramificaciones.

---

## Arquitectura del Código

```
io.yourPath/
├── Main.java                        # Punto de entrada (Game). Inicializa DAO, StoryManager y Screen.
├── screens/
│   ├── MainMenuScreen.java          # Menú principal: nueva partida, continuar, salir.
│   └── StoryScreen.java             # Presentación de diálogo y menú de pausa.
├── models/
│   ├── UIState.java                 # Enum: DIALOGANDO, MENU_PAUSA
│   ├── NarrativeNode.java           # Clase abstracta base para nodos narrativos.
│   ├── DialogNode.java              # Nodo con opciones de diálogo seleccionables.
│   ├── TrialNode.java               # Nodo de evaluación (prueba/juicio).
│   ├── DialogOption.java            # Opción individual: texto, destino, flag requerido, puntuación.
│   ├── TrialEvaluation.java         # Criterios de éxito/fallo para TrialNode.
│   ├── CharacterProfile.java        # Datos de personaje: id, nombre, retrato.
│   └── GameState.java               # Estado persistente: flags, nodo actual, puntuación de prueba.
├── logic/
│   └── StoryManager.java            # Motor narrativo: avance, evaluación de pruebas y procesamiento de acciones.
└── utils/
    ├── NarrativeDAO.java            # Interfaz DAO para datos narrativos.
    ├── NarrativeDAOImplementation.java # Implementación SQLite del DAO.
    ├── IntentiaException.java       # Excepción personalizada del proyecto.
    └── SaveSystem.java              # Guardado/carga JSON del estado de juego.
```

### Capa de Datos (DAO + SQLite)

La persistencia se maneja mediante JDBC con SQLite en `database/intentia.db`. El `NarrativeDAOImplementation` lee cinco tablas:

| Tabla | Propósito |
|---|---|
| `characters` | Perfiles de personajes (id, nombre, ruta de retrato) |
| `dialog_nodes` | Nodos narrativos (texto, orador, tipo, música) |
| `dialog_options` | Opciones de diálogo por nodo (texto, destino, flags, puntuación) |
| `dialog_actions` | Acciones/efectos secundarios al entrar a un nodo |
| `trial_evaluations` | Evaluaciones de prueba (umbral, rutas de éxito/fallo) |

### Motor Narrativo (StoryManager)

El `StoryManager` es el cerebro del juego. Procesa la navegación entre nodos:

1. **Entrada a nodo:** Ejecuta las `actions` del nodo como flags en `GameState`.
2. **Opciones:** Filtra `DialogOption`s según `requiredFlag` del estado actual.
3. **Evaluación (Trials):** Al llegar a un `TrialNode`, compara el porcentaje de acierto contra el umbral y bifurca la historia.
4. **Persistencia:** El `SaveSystem` serializa el `GameState` a `save.json` usando el serializador JSON de libGDX.

### Ciclo de Vida de una Decisión

```
Input Jugador → StoryManager.advance(option)
  → Acumula puntuación en GameState
  → Navega al nodo destino
  → Si es TrialNode: evalúa porcentaje vs umbral
    → Éxito: activa flag, bifurca a successTargetId
    → Fallo: bifurca a failTargetId
  → Renderiza nuevo estado en pantalla
```

---

## Tecnologías

| Componente | Tecnología |
|---|---|
| Framework | libGDX |
| Lenguaje | Java 17 |
| Build | Gradle (multi-proyecto: `core` + `lwjgl3`) |
| Base de datos | SQLite via JDBC |
| Serialización | libGDX `Json` |
| Estado actual | Prototipo funcional por consola |

---

## Ejecución

```bash
# Clonar y ejecutar
./gradlew lwjgl3:run
```

El juego requiere la base de datos `database/intentia.db` en el directorio de trabajo.

---

## Proyecto Generado con gdx-liftoff

Este proyecto fue generado con [gdx-liftoff](https://github.com/libgdx/gdx-liftoff) e incluye lanzadores para escritorio (LWJGL3). La clase principal `Main` extiende `Game` y establece la primera pantalla.
