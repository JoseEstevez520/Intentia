# 🚀 Posibles Mejoras y Expansiones Futuras (Intentia)

Este documento recopila ideas de diseño, mecánicas experimentales e integraciones tecnológicas avanzadas que podrían añadirse al juego en el futuro para llevar la inmersión al siguiente nivel.

## 🎵 1. Integración de IA para Música y Sonido Generativo
La banda sonora es crucial en un RPG. Integrar IAs musicales puede crear una narrativa inolvidable:
*   **APIs GenAI de Música (Suno AI, Udio, etc.):** En lugar de tener música estática, el juego podría tomar el archivo de guardado (`Preferences` en JSON) con las decisiones recientes del jugador, pasarlas por un LLM rápido para crear una letra, y enviarla a una API musical.
    *   *Ejemplo Narrativo:* Llegas a una taberna y el bardo NPC canta una balada con voces reales donde la letra satiriza específicamente cómo fracasaste intentando esquivar el ataque del dragón en el nivel anterior o cómo te asustaron unas nubes.
*   **Sistema de Bandas Sonoras Adaptativas:** Implementar en LibGDX música multipista (stems). Si el jugador está tardando mucho en responder una `Pregunta` (el tiempo de resolución baja), la pista de percusión sube de volumen progresivamente para generar estrés real.

## ⚙️ 2. Evolución del "Motor de Mecánicas"
Se pueden añadir capas sobre el actual sistema de `Pregunta`/`Prueba` para hacerlo más de "rol":
*   **Inventario Narrativo Acumulativo:** Los éxitos o fallos cómicos te dan ítems (ej. si fallas mucho en el bosque, obtienes "Tiritas Sucias"). Estos objetos no se usan en combate clásico, sino que desbloquean un 3º o 4º botón especial en decisiones de *Preguntas* futuras.
*   **Mecánica de Clima Activo:** Eventos en el mapa (declarados en Tiled) que inician condiciones climáticas (lluvia, nieve, niebla). 
    *   *Impacto Mecánico:* La lluvia provoca que el temporizador (`delta time`) de las `Preguntas` corra un 20% más rápido, simulando que el jugador "patina" o tiene prisa.
*   **Sistema de Confianza del Abuelo:** Sustituir la "barra de vida" por una "Barra de Credibilidad". Si tomas decisiones muy ilógicas o fallas mucho, el abuelo interrumpe más la historia con la voz en off para "corregir" la situación, haciendo la narrativa más caótica y graciosa.

## 🧠 3. Diálogos IA Dinámicos para NPCs "De Relleno"
*   El GDD restringe la IA a las opciones de las pruebas para evitar el "truco del dragón" y descontrol del estado. Sin embargo, los **NPCs irrelevantes** del mapa (ciudadanos, guardias) podrían usar un LLM local barato. 
*   Estos NPCs leerían tus últimas acciones fallidas almacenadas y harían comentarios *ad-lib* al pasar cerca, como: *"Veo que aún tienes barro en la túnica de tu última caída"*, haciendo el mundo mucho más vivo sin romper la historia principal.

## 🎨 4. Generación Dinámica del "Álbum de Recuerdos" (Final del Juego)
*   Como la historia transcurre entre una "fantasía épica" (el nieto jugando) y la "realidad" (lo que de verdad pasó, narrado por el abuelo), se podría conectar una IA generadora de imágenes para el final del juego.
*   Al acabar, la IA toma las resoluciones del usuario y genera un "Álbum de fotos Polaroid" estilo vida real. Si elegiste salvar a un NPC vestido de caballero plateado, la foto final te muestra a un veterinario en un paritorio (haciendo eco del Acto 3 de la historia del meteorito). Cada partida tendría un desenlace visual único.
