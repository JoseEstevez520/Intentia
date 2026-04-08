# 📘 Game Design Document (GDD): Intentia

## 📖 1. Historia y Estructura Narrativa

### 🎮 Introducción
Los padres llevan al hijo a pasar las vacaciones con el abuelo. Tras la despedida, el abuelo recibe al niño y le anuncia que la aventura está por comenzar.

### 🌲 El Bosque de las Nubes
El abuelo advierte al niño: el bosque está lleno de dragones. Sin embargo, al salir del bosque, el abuelo le dice al niño: "Los hemos dejado atrás". Le señala el cielo y el niño descubre que los "dragones" que temían eran simplemente nubes con esa forma. Esto establece la idea de que todo lo que van a jugar será una "fantasía" sobre la realidad.

----

## 🎬 2. Escena Prólogo: La Profecía de Pitof

**Acto 1: El Presagio Milenario**
Un mago llamado Pitof lanzó un presagio: *"Cuando el milenio termine, el manto de plata cubrirá la luna y anunciará el fin del mundo conocido"*.

**Acto 2: El Caos y el Bocadillo**
La humanidad está paralizada frente a los telescopios. Un técnico de TV recibe la llamada de que su mujer está de parto, sale disparado y olvida el papel de plata de su bocadillo sobre la lente. La luna "se cubre de plata". Pánico mundial.

**Acto 3: El Veterinario**
En el hospital vacío, el técnico y su hija sacan a un hombre de un ascensor y lo meten al paritorio a la fuerza. El hombre resulta ser un veterinario, pero la niña, experta por ver Anatomía de Grey, toma el mando.

**Acto 4: La Realidad**
El nieto interrumpe: *"¡Venga ya, abuelo! Eso es un chiste..."*. 
El abuelo sonríe: *"Tenía que engancharte a la historia, hijo. Pero ahora vamos a empezar en serio... Yo también fui el cometa que lo cambió todo"*. Ese es el inicio formal del juego jugable.

---

## ⚙️ 3. Motor de Mecánicas: Exploración y Decisiones

El juego abandona la idea de volver a la casa como "Nexo" y propone una aventura continua donde el 100% de la base es caminar estilo RPG clásico, hablar con NPCs y reaccionar a situaciones.

### 🏛️ Arquitectura de Código: Pruebas y Preguntas
Para mantener el proyecto fácil de programar y muy centrado en la narrativa, cualquier situación de "Habilidad o Acción" se resolverá mediante un sistema de decisiones directas sin temporizador.

*   **Clase `Pregunta` (Unidad de acción):**
    *   Maneja una única decisión contextual (Ej. *¡El dragón ataca por la derecha!*).
    *   Muestra 2 o 3 botones en pantalla (`[Esquivar izquierda] / [Cubrirse]`).
    *   (No hay temporizador por defecto; el jugador tiene tiempo para elegir su ruta o respuesta).
*   **Clase `Prueba` (Agrupador de nivel):**
    *   Reúne varias `Preguntas` seguidas o en un mismo mapa.
    *   **El Peso Narrativo (Intento Único):** No se pueden repetir las pruebas. Las decisiones se toman una sola vez, y dependiendo de los aciertos o elecciones del jugador, el motor bifurca la narrativa a un desenlace de éxito (épico) o de fallo (cómico). Esta resolución se convierte en el transcurso oficial de la partida y avanza la historia en lugar de lanzar una pantalla de muerte.

### 🎙️ La Voz en Off Reactiva
Las mecánicas y los fallos alimentan el diálogo. Constantemente, las acciones del jugador son comentadas por el niño ("¿En serio te caíste de la bici ahí?") y justificadas por el abuelo.
