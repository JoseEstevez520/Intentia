# 🎮 Guía Técnica: RPG Pixel Art con Integración HD (Java + LibGDX)

Este documento es el "blueprint" final para la implementación del proyecto **Intentia**.

---

## 🚀 Arquitectura del Sistema

### 1. Gestión de Pantallas (Screen API)
LibGDX utiliza una clase principal que hereda de `Game`. La lógica se divide en objetos `Screen`:
*   **`IntroScreen`:** Pantalla inicial dedicada exclusivamente a la cinemática de apertura.
*   **`MenuScreen`:** Interfaz de inicio con `Stage` y `Table`.
*   **`GameScreen`:** Contiene el bucle principal: `update()`, `physics()` y `render()`.
*   **`VideoScreen`:** Pantalla "puente" que detiene la música del juego y lanza un vídeo a pantalla completa.

### 2. Gestión de Recursos (AssetManager)
Para evitar que el juego "se congele" cuando aparece un dragón o una cinemática:
*   **Clase `AssetManager`:** Se usa para cargar mapas de Tiled, texturas y sonidos en segundo plano.
*   **Pantalla de Carga:** Se recomienda una pequeña barra de carga al inicio del juego que precargue todos los vídeos WebM pesados para que la transición sea instantánea.

---

## 🎭 Narrativa y Efectos Visuales

### A. Intro estilo "Pokemon Zafiro/GBA" (Cinemática Técnica)
*   **Técnica:** No es necesario animar sprites uno a uno. El vídeo WebM ya contiene el logo moviéndose, las nubes pasando y los créditos.
*   **Sincronización:** Una vez que `introVideo.play()` comienza, se debe desactivar cualquier entrada del teclado (excepto Skip).
*   **Uso de Cámaras:** Aunque sea un vídeo, se debe usar un `FitViewport` para asegurar que el vídeo mantenga su relación de aspecto (16:9) independientemente de si la pantalla es cuadrada o panorámica.

### B. Overlays Transparentes (El "Truco del Dragón")
Esta es la técnica clave para darle un aspecto "triple A" a un juego Pixel Art sin esfuerzo masivo de animación manual.

*   **¿Qué es la Transparencia (Canal Alfa)?**
    Al exportar los vídeos en formato WebM (VP8/VP9), podemos incluir un "canal alfa". Esto significa que el reproductor de vídeo de LibGDX sabe qué píxeles son parte del dibujo (el dragón) y qué píxeles son totalmente invisibles. Para el motor de juego, el vídeo no es un "recuadro", sino una entidad con forma propia que se funde con el escenario.

*   **El Orden de Renderizado (Las "Capas" de Cebolla):**
    Para que el efecto sea real, el código debe dibujar las cosas en este orden exacto:
    1.  **Fondo:** El mapa de Tiled (suelo, casas).
    2.  **Jugador:** Tu personaje moviéndose.
    3.  **VÍDEO:** Aquí dibujamos el vídeo transparente del dragón. Al estar "encima" del jugador, parecerá que el dragón vuela sobre su cabeza.
    4.  **UI (Interfaz):** Los menús y diálogos siempre se dibujan al final para que nunca queden tapados.

*   **Sincronización Interactiva (Vídeo-Mundo):**
    La parte más potente es que el vídeo no es solo "decoración". El código de Java puede "escuchar" al vídeo:
    *   **Detección de Tiempo:** Si el vídeo del dragón dura 5 segundos y sabemos que en el segundo 3.2 lanza el fuego, programamos un evento en Java que diga: `if (videoTime >= 3.2) { player.takeDamage(); }`.
    *   **Efecto Visual Real:** Aunque el fuego sea un vídeo, el jugador verá cómo su barra de vida baja justo cuando las llamas le tocan. Esto crea la ilusión de que el vídeo es una parte física del juego.

*   **Ventaja Creativa:** Permite meter efectos de partículas, fluidos y luces cinemáticas que serían imposibles de programar directamente en Java para un estudiante, pero que son muy fáciles de exportar desde un programa de vídeo.

### C. Sistema de Diálogos y Retratos Animados
*   **NinePatch (Marcos):** Técnica de subdivisión de imagen en 9 zonas para permitir el escalado dinámico del cuadro de texto sin distorsión.
*   **Voz del Personaje:** Se puede sincronizar un efecto de sonido (`beep.wav`) con la aparición de cada letra para dar esa sensación retro.
*   **Retratos de Vídeo:** El "talking head" es un `FBO` (Frame Buffer Object) donde el vídeo se dibuja en pequeño dentro de un círculo en la caja de texto.

---

## 🗺️ Integración con Tiled (Lógica de Mapa)

### Los "Event Triggers" Invisibles
*   **Capa de Objetos:** Se crean rectángulos con nombres como `VideoStart_01`.
*   **Máquina de Estados del Jugador:** 
    *   **ESTADO_CAMINANDO:** El jugador se mueve normal.
    *   **ESTADO_EVENTO:** Cuando toca el rectángulo, el jugador se queda quieto (no puede caminar) mientras el vídeo se reproduce.
*   **Propiedades Custom:** En Tiled, en la pestaña de propiedades del objeto, añadimos:
    *   `source`: "cinematica_final.webm"
    *   `nextMap`: "level2.tmx"

---

## 📝 Notas de Optimización y Entrega

*   **Sincronización Audio-Vídeo:** Asegurarse de que el audio esté codificado dentro del WebM para evitar problemas de desfase en Java.
*   **Escalado Nearest:** `Texture.setFilter(TextureFilter.Nearest, TextureFilter.Nearest)` para mantener la estética Pixel Art pura.
*   **Memoria RAM:** Llamar a `dispose()` en cada cambio de pantalla para limpiar la memoria de vídeo. Es vital para que el juego sea estable durante la presentación.

## 💾 Persistencia y Jugabilidad

### 1. Sistema de Guardado (Save/Load)
*   **Técnica:** Uso de la clase `Preferences` de LibGDX o archivos **JSON**.
*   **Lógica:** Al guardar, convertimos la posición del jugador (`x`, `y`), su salud y el nombre del mapa actual en un string JSON y lo guardamos en el disco.
*   **Carga:** Al iniciar, leemos el archivo y usamos `game.setScreen(new GameScreen(mapName, posX, posY))`.

### 2. Animación de Sprites (Walk Cycle)
*   **TextureAtlas:** En lugar de cargar muchas imágenes sueltas, usamos una única "hoja de sprites" (Sprite Sheet).
*   **Clase `Animation`:** Java calcula qué frame mostrar basándose en el tiempo: `stateTime += Gdx.graphics.getDeltaTime()`.
*   **Estados:** El personaje cambia entre animaciones de "Quieto", "Caminando" o "Atacando" según las teclas pulsadas.

---

## 📦 Distribución y Entrega Final

*   **Exportación:** Uso de la tarea de Gradle `lwjgl3:jar`.
*   **Resultado:** Se genera un archivo **`.jar` ejecutable** en la carpeta `build/libs`.
*   **Ventaja:** El profesor puede ejecutar tu juego en cualquier ordenador con Java instalado sin necesidad de abrir el código fuente ni configurar el entorno de desarrollo.

---
*Documento final revisado y completado para el proyecto - 2º Trimestre.*
