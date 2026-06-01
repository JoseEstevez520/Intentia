# Intentia: El Legado — Rama `dev`

> *Rama de desarrollo activo. Aquí se integran las features antes de pasar a `main`.*

`dev` es la rama donde confluye el trabajo de las ramas satélite (`logic`, `persistence`, etc.). Cada feature se implementa, se prueba y se consolida aquí antes de fusionarse a `main`. El código puede estar en distintas fases de madurez — algunas partes funcionando por consola, otras en transición a la UI gráfica.

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

---

## Flujo de Desarrollo

```
        ramas feature
     ┌─ logic ──────────┐
     │  persistence     │
     │  screens-ui      ├──→ dev ──→ main (lanzamiento)
     │  tiled-maps      │
     └──────────────────┘
```

Cada feature se desarrolla en su propia rama y se fusiona a `dev` cuando está lista. `dev` es el campo de pruebas antes del merge definitivo a `main`.

---

## Historial de Features Incorporadas

| Commit | Feature |
|---|---|
| `af5c47e` | Migración de historia de JSON a SQLite |
| `76ee3e6` | Diálogos e historia completos en SQLite |
| `4e3aa93` | Sistema de excepciones personalizadas |
| `a3c3b30` | Herencia de nodos: `NarrativeNode` → `DialogNode` / `TrialNode` |
| `6ddae3f` | `UIState` enum para menú de pausa |
| `ad31e4e` | Encapsulamiento: atributos privados con getters |

---

## Tecnologías

| Componente | Tecnología |
|---|---|
| Framework base | libGDX (solo `Game`/`Screen` + serialización) |
| Lógica del juego | Java 17 puro |
| Motor narrativo | `StoryManager` |
| Base de datos | SQLite vía JDBC |
| Serialización | libGDX `Json` |
| Build | Gradle |
| UI actual | Consola (`System.out` / `Scanner`) |

---

## Ejecución

```bash
./gradlew lwjgl3:run
```

Requiere `database/intentia.db` en el directorio de trabajo.

---

## Próximas Features (en desarrollo)

- [ ] Reemplazar consola por `Stage` + `Skin` de libGDX
- [ ] Integración con mapas Tiled
- [ ] Reproducción de vídeo WebM con transparencia
- [ ] Pantalla de carga con `AssetManager`
- [ ] Sistema de inventario visual
