# 📘 Game Design Document (GDD): Intentia

## 👁️ Visión Central y Filosofía del Juego (El Core)
*Intentia* es un viaje autobiográfico e introspectivo basado sutilmente en el **Absurdismo de Albert Camus**. El mundo y la realidad carecen de un "sentido" o destino predefinido, y el juego trata sobre cómo decidimos enfrentarnos a ese vacío.
*   **El Espejo Temporal:** El Niño y el Abuelo no son dos personajes separados; son el propio autor (el jugador) dialogando consigo mismo desde dos etapas de la vida.
*   **La Evolución:** El juego explora el viaje humano. Empieza con la inocencia plena (niño), pasará a través de los recuerdos a la crisis existencial (adolescente/adulto joven) y culmina en la sabiduría del Abuelo.
*   **El Sísifo Feliz:** El Abuelo ya ha comprendido que el mundo no tiene magia inherente. En lugar de deprimirse, su rebelión es *inventarse la magia*. Crea carteles de dragones y bromea porque ha elegido divertirse con la vida. Es Sísifo empujando la piedra con una sonrisa.
*   **Mecánica Absurdista:** Por eso no hay "Game Over". En este universo, equivocarse en un QTE no te castiga, porque los errores carecen de importancia universal. Solo importan las risas y la anécdota que dejan atrás.

---

## 🎨 Dirección de Arte y Color (Leitmotiv)
Para asegurar la cohesión visual entre los vídeos 3D (cinemáticas) y el mundo jugable (Pixel Art con *assets* genéricos), se utilizará un **Color Ancla**: el **Verde Agua (Teal/Aqua)** mezclado con tonos Plata/Ámbar.
*   **Significado:** Es el color universal de la nostalgia, los recuerdos y la magia en la obra, y sirve como ancla autobiográfica del autor.
*   **Aplicación en Vídeos:** La iluminación atmosférica, constelaciones y nieblas oníricas tendrán destellos de verde agua. Los ojos de los personajes serán el punto focal de la conexión.
*   **Aplicación en Gameplay:** 
    *   **El Niño:** 9 años, delgado y ágil. Camiseta lisa Aquamarine (#7FFFD4). Ojos aquamarine brillantes.
    *   **El Abuelo (@anciano):** Aprox. 70 años, alto y atlético. Camisa marrón remangada y reloj de plata. Ojos marrones cálidos y amables.
    *   **Coherencia:** El diseño muestra la evolución biológica y el cambio de paleta (de la vibrante infancia al tono tierra de la madurez), manteniendo el vínculo a través del colgante de plata.

---

## 📖 1. Historia y Estructura Narrativa

### 💭 El Sueño Premonitorio (Cinemática Inicial)
El juego arranca directamente con un potente *vídeo WebM* de 15 segundos. Tras mostrar la inmensidad de una Luna rodeada de nubes Verde Agua, aparece la figura mística del **Mago Pitof**, señalando al cielo y recitando su profecía con una voz profunda. De pronto, la música y la imagen se rompen en un corte acústico seco y un **fundido a negro total**.

### 🎮 La Llegada (Mundo Real — Presente)
La apertura del juego es silenciosa. **Una furgo** se detiene frente a una casa aislada. **Leo** (el padre) conduce. **Ray** (8-12) mira por la ventana. No hay diálogo — solo el ruido del motor que se apaga.

Leo baja, saca la mochila del maletero, la deja en el suelo. Mira la casa un momento. Mira a Ray. No hay abrazo. No hay escena. Dice algo breve — *"Venga, baja"* o *"Pórtate bien"* — y se mete de vuelta al coche.

Ray se queda solo en la acera con la mochila. La furgo arranca. Se aleja. Las luces traseras se pierden en la carretera.

**Elio** (el abuelo) está en el umbral. No ha salido a recibirle. Solo espera.

Ray se da la vuelta. La casa es vieja. Ventanas con polvo. Un taller en la planta baja. Una luz encendida arriba. Primer encuentro: sin palabras. Elio le señala la puerta con la cabeza. Ray entra.

> **Nota de Dirección (Paralelismo):** Esta misma composición — un coche que se va, un niño con una mochila, un adulto que espera en silencio — se repite dentro de la consola cuando Elio llega al taller de Cierzo. El jugador lo reconocerá sin que nadie se lo señale.

### 🎮 El Inicio: El Flash Verde y el Porche
Tras la cinemática, el paso al juego no es inmediato.
1.  **La Transición:** La pantalla estalla en un **Flash Verde Agua (#7FFFD4)**. El sonido 3D se disuelve y emerge la melodía Pixel Art.
2.  **El Diálogo (Tutorial de Decisión):** El niño y el abuelo están en el porche. El abuelo promete aventuras y lanza la primera pregunta: *"¿Te gustan los dragones?"*.
    *   Esta es la primera interacción del jugador con la **Clase Pregunta** (decisión dialógica sin presión).
3.  **El Bosque de la Niebla:** Solo tras la charla, comienza el desafío a pie por un sendero de 1.5 km (15 min de caminata) profundamente adentrado en el bosque, donde se introduce el movimiento y la primera "Prueba" de acción.
4.  **La Revelación:** Al final del camino, se llega al claro donde están la cabaña y el taller de piedra y madera oscura, situados literalmente junto a un lago de montaña. El scroll ascendente muestra que los dragones eran nubes.

> **Consulta Detallada:** Para ver la disposición exacta de los mapas, distancias y localizaciones (Muelle, Taller, Cabaña), consulta el documento **[WORLDBUILDING_Y_MAPA.md](file:///c:/Users/sonde/Downloads/Intentia-main/Intentia-main/docs/WORLDBUILDING_Y_MAPA.md)**.

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
