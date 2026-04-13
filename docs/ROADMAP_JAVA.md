# Guía de Desarrollo Manual: Pasos y Clases a Crear en Java

Esta es tu bitácora de programación personal. Aquí se especifica *qué clases crear, dónde ubicarlas y qué código base hacer* para que puedas darle vida al motor narrativo de Intentia sin perderte. 

La estructura sugerida es organizar el paquete `core/src/main/java/io/yourPath/` en sub-paquetes como `models`, `logic`, `ui` y `utils`.

---

## PARTE 1: La Base de Datos (Models)
*Objetivo: Estas clases son "tontas", solo sirven para guardar texto y datos valiosos.*
*Ruta recomendada:* `core/.../yourPath/models/`

### 1. `Opcion.java`
*   **Propósito:** Representa una de las opciones que el jugador puede hacer click en medio del texto.
*   **Qué agregar:**
    *   `private String texto;` (Ej: "Abofetear al Rey")
    *   `private String idDestino;` (Hacia qué nodo saltará el texto)
    *   `private String requisito;` (Ej: "flag_valor_alto" -> Opcional, solo si el jugador tiene este flag podrá ver la opción).

### 2. `NodoDialogo.java`
*   **Propósito:** La "pantalla" actual de diálogo. Todo lo necesario para un instante de la historia.
*   **Qué agregar:**
    *   `private String id;`
    *   `private String texto;` (Lo que dice el narrador/personaje)
    *   `private List<Opcion> opciones;` (Lista con las distintas opciones posibles)
    *   `private List<String> acciones;` (Ej: "dar_oro". Efectos posteriores al leer el nodo)

### 3. `GameState.java`
*   **Propósito:** La memoria ram de la partida de tu jugador. Todo lo que el usuario hace termina aquí.
*   **Qué agregar:**
    *   `private Set<String> flags;` (Una colección de etiquetas que el jugador va ganando "espada", "ha_llorado", etc)
    *   `private String nodoActualId;` (Para saber dónde se quedó leyendo).

---

## PARTE 2: El Cerebro Lógico (Logic)
*Objetivo: Hacer que se apliquen las reglas del juego a los datos de la Parte 1.*
*Ruta recomendada:* `core/.../yourPath/logic/`

### 4. `StoryManager.java`
*   **Propósito:** Es el controlador principal. Gestiona qué se debe mostrar en pantalla y lee el progreso de la partida.
*   **Qué agregar:**
    *   `private Map<String, NodoDialogo> nodos;` (Mapa completo de toda tu historia).
    *   `private GameState gameState;` (El estado del jugador).
    *   **Método:** `avanzar(String opcionElegidaId)` -> Busca la consecuencia, reparte banderas y actualiza al UI con el siguiente Nodo.

---

## PARTE 3: Herramientas (Utils)
*Objetivo: Cargar nuestro trabajo de disco hacia las clases de arriba.*
*Ruta recomendada:* `core/.../yourPath/utils/`

### 5. `JSONFileLoader.java`
*   **Propósito:** Cargar `historia.json` desde tus assets y convertir cada bloque del JSON a la clase `NodoDialogo`.
*   **Recomendación:** Utilizar el serializador de la librería integrada: `new com.badlogic.gdx.utils.Json()`.

### 6. `SaveSystem.java`
*   **Propósito:** Guardar el progreso.
*   **Qué agregar:** Un método `guardarPartida()` que tome el objeto `GameState`, lo convierta en Texto JSON, y haga un `Gdx.files.local("save.json").writeString(texto, false);`. Y otro `cargarPartida()`.

---

## PARTE 4: La Capa Gráfica (UI)
*Objetivo: Mostrar la pantalla y leer al usuario. LibGDX nativo.*
*Ruta recomendada:* `core/.../yourPath/screens/` y `.../ui/`

### 7. `MenuScreen.java`
*   **Propósito:** La pantalla de inicio con los clásicos "Juego Nuevo", "Cargar" o "Salir".
*   **Qué construir:** Un `Stage`, agregar botones interactivos que llamen y construyan el GameManager si pulsamos "Empezar".

### 8. `GameScreen.java`
*   **Propósito:** Tu motor Tiled en la práctica.
*   **Qué construir:**
    *   Tu cámara interactuando con tu TileMapRenderer.
    *   La lectura del imput del teclado local (Si se pulsa W,A,S,D mueves el sprite).

### 9. `DialogOverlay.java` (Opcional pero recomendado)
*   **Propósito:** Un marco de lectura de cuentos tipo GBA o Pokémon encima del juego.
*   **Qué construir:** Una clase que pinta en la pantalla una caja `NinePatch` negra semitransparente que oculta parte del Tiled para hacer de lienzo. Sobre ella, se inyecta siempre el texto dictaminado por `StoryManager.getNodoActual()`.
