# 📘 Game Design Document (GDD): Intentia

## 📖 1. Historia y Estructura Narrativa

### 🎮 Introducción
Los padres dejan al hijo a pasar las vacaciones en el pueblo. La despedida es cálida y alegre; le piden que se lo pase en grande. El niño es la viva imagen de la inocencia, la curiosidad y la felicidad. El abuelo, por su parte, es un personaje estilo "Don Quijote": un bromista empedernido, teatral y con muchísima energía. Nada más perderse el coche de los padres de vista, el abuelo se pone muy serio y le anuncia que la gran aventura acaba de comenzar.

> **Nota de Dirección (El Efecto Espejo):** Aunque dentro del universo del juego son literalmente un abuelo y su nieto que ignoran cualquier trasfondo, a nivel de subtexto representan al autor en dos etapas de su vida. Para reforzar esta conexión de forma invisible al jugador, se incluirá un detalle muy sutil (un tic, un gesto idéntico en sus *sprites* o un objeto paralelo) que compartan sin darse cuenta, reforzando su vínculo más allá de las palabras.

### 🌲 El Bosque de las Nubes (La Broma)
De camino a la casa, cruzan un bosque. El niño empieza a ver carteles físicos de advertencia: "¡PELIGRO: ZONA DE DRAGONES!". El abuelo, metidísimo en su papel, le mete prisa y le hace correr esquivando supuestos peligros. Al salir por fin al claro, el abuelo empieza a reírse a carcajadas (*jajaja*). Le señala el cielo y le confiesa que esa mañana vio unas nubes con forma de dragón y decidió que "tenía que vallar la zona por seguridad escolar". El niño, dándose cuenta de que ha caído en la trampa total, suspira sonriendo: *"¿En serio, abuelo?"*. Esto establece el tono de todo el juego: jugar a seguirle las bromas teatrales a este genio loco.

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
    *   Muestra 2 o 3 opciones visuales acompañadas de iconos de botones (`[<-] Esquivar izquierda` / `[->] Cubrirse`).
    *   **Controles Integrados (Teclado/Pad):** Se elimina el uso del ratón. El jugador resuelve la situación usando los mismos botones direccionales con los que camina, asegurando un *game-feel* inmersivo estilo consola, sin romper el ritmo del juego.
    *   (No hay temporizador por defecto; el jugador tiene tiempo para elegir su ruta o respuesta).
*   **Clase `Prueba` (Agrupador de nivel):**
    *   Reúne varias `Preguntas` seguidas o en un mismo mapa.
    *   **El Peso Narrativo (Intento Único):** No se pueden repetir las pruebas. Las decisiones se toman una sola vez, y dependiendo de los aciertos o elecciones del jugador, el motor bifurca la narrativa a un desenlace de éxito (épico) o de fallo (cómico). Esta resolución se convierte en el transcurso oficial de la partida y avanza la historia en lugar de lanzar una pantalla de muerte.

### 📂 Persistencia, Idiomas y Consecuencias Reales
*   **Memoria de Decisiones (Flags):** Las decisiones de una *Prueba* se almacenan en el estado del juego (ej: `estado.tiene_pegamento == true`). Esto altera diálogos futuros o "desbloquea" la aparición dinámica de botones especiales sin necesidad de menús complejos visuales.
*   **Bases de Datos y Múltiples Idiomas:** Los diálogos se estructuran en archivos `.json` separados (ej. `español.json`, `english.json`). El código es universal, lo que permitirá escalar el juego globalmente sin dolor de cabeza en el futuro.
*   **Configuración de Guardado:** Se aplicará un modelo de **Guardado Manual Regular** para el desarrollo rápido del prototipo actual. Para el despliegue final, existirá un conmutador de configuración opcional para el "Auto-Save" tras cada evento.

### 🎙️ La Voz en Off Reactiva
Las mecánicas y los fallos alimentan el diálogo. Constantemente, las acciones del jugador son comentadas por el niño ("¿En serio te caíste de la bici ahí?") y justificadas por el abuelo.
