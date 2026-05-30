# Documentación: Migración de Datos Narrativos a SQLite

Este documento detalla los recientes cambios arquitectónicos realizados en el proyecto con el objetivo de mejorar la escalabilidad en la gestión de la historia y los personajes. A continuación se documentan todas las modificaciones que se han implementado manualmente en el código.

## Contexto de los Cambios
El sistema de lectura estática mediante archivos JSON (`story.json` y `characters.json`) se estaba volviendo poco escalable para un proyecto narrativo complejo. Por ello, se ha llevado a cabo una migración de los datos estáticos (solo lectura) hacia una base de datos relacional SQLite, manteniendo el guardado de progreso de las partidas en el archivo `save.json`.

## Detalle de Modificaciones en el Código

### 1. Actualización de Dependencias
Se ha modificado el archivo `build.gradle` (dentro del módulo `core`) añadiendo la dependencia JDBC para SQLite:
- **Añadido:** `org.xerial:sqlite-jdbc`
- **Motivo:** Habilitar la conexión local a la base de datos en entornos Desktop.

### 2. Creación de la Base de Datos SQLite
El diseño de la estructura relacional ha sido concebido e integrado por el desarrollador, constando de las siguientes tablas principales:
- `characters` (Personajes)
- `dialog_nodes` (Nodos de diálogo)
- `dialog_options` (Opciones de respuesta)
- `dialog_actions` (Acciones secundarias)
- `trial_evaluations` (Sistema de pruebas y juicios)

*Nota:* Para agilizar la transición y preservar todo el texto ya escrito, la tarea mecánica de parsear los antiguos archivos JSON y volcar su contenido masivamente a las nuevas tablas de la base de datos `assets/database/intentia.db` fue asistida y automatizada mediante IA. El resto de la arquitectura e implementación en código es autoría del desarrollador.

### 3. Implementación del Patrón DAO
Se ha rediseñado la capa de acceso a datos basándose en buenas prácticas de programación (Patrón DAO con recursos auto-gestionables):
- **Eliminado:** Se ha borrado por completo la clase `JsonDataLoader.java`, descartando la antigua lógica de parseo de JSON.
- **Añadido:** Se ha creado la interfaz `NarrativeDAO.java` que define los contratos de obtención de datos (`getAllCharacters` y `getAllDialogNodes`).
- **Añadido:** Se ha implementado la clase `NarrativeDAOImplementation.java`. Esta clase gestiona la conexión a la base de datos a través de `DriverManager`, ejecutando sentencias SQL con `PreparedStatement` y `ResultSet` protegidos en bloques `try-with-resources`.

### 4. Integración Eager Loading en el Motor (`Main.java`)
Se ha modificado el punto de entrada del juego (`Main.java`) para integrar la nueva capa de datos:
- **Modificado:** Se ha sustituido la llamada al antiguo lector JSON por la instanciación de `NarrativeDAOImplementation`.
- **Estrategia:** Se ha optado por mantener una carga *Eager* (en memoria). Al iniciar la aplicación, la base de datos se lee de forma secuencial y los datos se vuelcan directamente en los mapas del `StoryManager`. Gracias a esta decisión de diseño, la lógica interna del juego no sufre tiempos de carga durante la ejecución.

## Conclusión
Con estos cambios, la arquitectura del juego queda preparada para escalar narrativamente. A partir de ahora, la ampliación de la historia de *Intentia* se realizará insertando registros directamente en la base de datos `intentia.db`.
