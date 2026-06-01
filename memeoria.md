# Base de datos en Intentia — Dónde encaja y cómo se implementa

## Qué es el juego

Un RPG top-down con mapas de tiles (estilo Pokémon). El jugador se mueve por el mapa e interactúa con NPCs y objetos, lo que desencadena diálogos, pruebas y eventos. Los diálogos son una capa del juego, no el juego en sí.

---

## Dónde encaja la BD

Cuando el jugador interactúa con algo, el juego carga un **nodo de diálogo**: un texto, opciones de respuesta y consecuencias. Esos datos están ahora en ficheros JSON pero tienen estructura relacional natural — son tablas.

**La BD sustituye a los ficheros JSON de historia.** El guardado de partida (save.json) no cambia, es demasiado simple para necesitar una BD.

---

## Tablas de la BD (`intentia_story.db`)

| Tabla | Qué guarda |
|---|---|
| `dialog_nodes` | Cada mensaje que aparece al interactuar con el mundo |
| `dialog_options` | Las opciones de respuesta del jugador en cada nodo |
| `node_actions` | Efectos automáticos al entrar en un nodo (dar item, activar flag...) |
| `trial_evaluations` | Pruebas con umbral de puntuación que ramifican la historia |
| `character_profiles` | Los NPCs: nombre y ruta del retrato |

---

## Patrón de acceso: DAO

Se usa el patrón **DAO (Data Access Object)**: una clase por tabla. Cada DAO agrupa todas las operaciones de base de datos para esa tabla.

```
utils/
└── dao/
    ├── DialogNodeDao.java        → SELECT/INSERT sobre dialog_nodes
    ├── DialogOptionDao.java      → SELECT/INSERT sobre dialog_options
    ├── CharacterProfileDao.java  → SELECT/INSERT sobre character_profiles
    ├── NodeActionDao.java        → SELECT/INSERT sobre node_actions
    └── TrialEvaluationDao.java   → SELECT/INSERT sobre trial_evaluations
```

Cada DAO recibe la conexión a la BD y expone métodos como `findAll()`, `findById(id)` o `insert(objeto)`.

---

## Cómo encaja con el código actual

```
Al arrancar el juego:

DatabaseManager.connect()          → abre intentia_story.db
    │
    ├── DialogNodeDao.findAll()        ┐
    ├── DialogOptionDao.findByNode()   │ → construyen el Map<String, DialogNode>
    ├── NodeActionDao.findByNode()     │   igual que hacía JsonDataLoader
    └── TrialEvaluationDao.find()      ┘

StoryManager recibe el mismo Map de siempre → cero cambios en la lógica
SaveSystem sigue usando save.json → cero cambios en el guardado
```

---

## Por qué el patrón DAO

- Es lo estándar en DAW para acceso a datos
- Cada clase tiene una responsabilidad clara (una tabla)
- Se puede usar cada DAO de forma independiente
- Separa la lógica del juego del acceso a la BD
