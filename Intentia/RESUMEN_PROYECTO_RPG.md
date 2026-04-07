# 🎮 Resumen de Diseño: RPG Pixel Art con Vídeo en Java

Este documento resume las decisiones técnicas y estrategias de diseño acordadas para el proyecto de clase **Intentia**, utilizando **Java** y **LibGDX**.

---

## 🚀 Arquitectura Técnica

### 1. Motor de Juego: LibGDX
*   **Base:** Java 17+ con Gradle.
*   **Gestión de Escenas:** Uso de la interfaz `Screen` para separar el juego (`GameScreen`) de las cinemáticas (`VideoScreen`).

### 2. Integración de Vídeo
*   **Librería Recomendada:** `gdx-video` (Extensión oficial de LibGDX).
*   **Formato de Vídeo:** **WebM** con códec **VP8**.
    *   *¿Por qué?* Es gratuito (sin licencias), ligero y soporta **transparencia (canal alfa)**.
*   **Alternativa (VLCj):** Solo si se requiere compatibilidad total con formatos como MP4/MKV (opción "tanque").

---

## 🎭 Estrategias de Narrativa Visual

### A. Cinemáticas a Pantalla Completa
*   Se activa al llegar a eventos específicos del mapa (Tiled).
*   Se detiene el renderizado del RPG para dar el 100% de potencia al vídeo.
*   Ideal para introducciones, finales de nivel o diálogos importantes.

### B. Overlays Transparentes (Efectos Complejos)
*   Uso de vídeos WebM con fondo transparente sobre el mapa de Tiled.
*   **Ejemplo del Dragón:** Un dragón gigante vuela sobre el mapa pixel art. Al ser un vídeo, puede tener un nivel de detalle altísimo (fuego, humo, luces) sin sobrecargar al procesador.
*   **Interacción Vídeo-Código (Lógica de Daño):** 
    *   El código de Java puede consultar el tiempo de reproducción del vídeo. 
    *   *Ejemplo:* "Si el vídeo del dragón está en el segundo 2.5 (cuando lanza el fuego), comprueba si el jugador está en el área de impacto y réstale vida".
    *   Esto permite que elementos visuales complejos dicten eventos reales en la jugabilidad.

---

## 🛠️ Herramientas y Flujo de Trabajo

| Tarea | Herramienta Recomendada |
|---|---|
| **Programación** | Java 17 + IntelliJ IDEA / VS Code |
| **Diseño de Mapas** | [Tiled Map Editor](https://www.mapeditor.org/) (Formato `.tmx`) |
| **Arte Pixel Art** | Aseprite / Piskel |
| **Creación de Vídeos** | Blender / After Effects / Herramientas de IA |
| **Conversión a WebM** | FFmpeg o conversores online gratuitos |

---

## 📝 Notas de Implementación (Tips)

1.  **Filtro Nearest:** En LibGDX, configurar las texturas con filtros `Nearest` para que el Pixel Art se vea nítido y no borroso.
2.  **Eventos en Tiled (Capas de Objetos):** 
    *   **Capa Visual (Tiles):** Donde dibujas el arte (suelo, paredes). Estas no deben tener los nombres de los eventos.
    *   **Capa Lógica (Objetos):** Creas una "Object Layer" y dibujas rectángulos invisibles sobre las casillas donde quieras un evento.
    *   **Ventaja:** Estos rectángulos son invisibles en el juego pero en Java detectamos cuando el jugador los "pisa" para lanzar un vídeo, cambiar de mapa o activar un diálogo.
3.  **Resolución:** Mantener los vídeos a 720p o 1080p para asegurar fluidez total en cualquier ordenador.

---
*Documento generado para el proyecto de Programación - 2º Trimestre.*
