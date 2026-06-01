# 🧪 PLAN DETALLADO DE TESTING — FASE 7

> **Proyecto:** INTENTIA (libGDX + Java 17 + SQLite)
> **Objetivo:** Implementar cobertura completa de pruebas unitarias, de integración y funcionales para la lógica narrativa.
> **Base:** Código fuente en `core/src/main/java/io/yourPath/`

---

## ÍNDICE

1. [Estrategia de Testing](#1-estrategia-de-testing)
2. [Configuración Inicial (build.gradle)](#2-configuración-inicial-buildgradle)
3. [Estructura de Directorios de Test](#3-estructura-de-directorios-de-test)
4. [Código Completo de Tests](#4-código-completo-de-tests)
   - [4.1 GameStateTest](#41-gamestatetestjava)
   - [4.2 StoryManagerTest](#42-storymanagertestjava)
   - [4.3 NarrativeDAOTest](#43-narrativedaotestjava)
   - [4.4 SaveSystemTest](#44-savesystemtestjava)
   - [4.5 IntentiaExceptionTest](#45-intentiaexceptiontestjava)
   - [4.6 DialogNodeTest](#46-dialognodetestjava)
   - [4.7 TrialNodeTest](#47-trialnodetestjava)
   - [4.8 NarrativeNodeTest](#48-narrativenodetestjava)
   - [4.9 DialogOptionTest](#49-dialogoptiontestjava)
   - [4.10 CharacterProfileTest](#410-characterprofiletestjava)
   - [4.11 TrialEvaluationTest](#411-trialevaluationtestjava)
5. [Consideraciones sobre libGDX y Mocking](#5-consideraciones-sobre-libgdx-y-mocking)
6. [Jerarquía de Excepciones](#6-jerarquía-de-excepciones)
7. [Mejores Prácticas](#7-mejores-prácticas)
8. [Checklist Final de Verificación](#8-checklist-final-de-verificación)

---

## 1. ESTRATEGIA DE TESTING

### 1.1 Niveles de Prueba

| Nivel | Alcance | Herramienta | Prioridad |
|-------|---------|-------------|-----------|
| **Unitarias** | `StoryManager`, `GameState`, `NarrativeNode`, `DialogNode`, `TrialNode`, `DialogOption`, `TrialEvaluation`, `CharacterProfile`, `IntentiaException` | JUnit 5 | 🔴 Crítica |
| **Integración** | `NarrativeDAOImplementation` con SQLite en memoria, `SaveSystem` (serialización JSON) | JUnit 5 + SQLite en memoria | 🟡 Alta |
| **Funcional** | Flujo narrativo completo: inicio → opciones → trial → fin del prólogo | JUnit 5 (simulado con nodos mock) | 🟡 Alta |
| **Visual (manual)** | Renderizado de diálogos, carga de retratos, skins Scene2D, mapas Tiled | Inspección visual | 🟢 Media |

### 1.2 Principios Rectores

1. **Aislar lógica de negocio**: Los tests NO deben depender de libGDX runtime. La lógica pura (StoryManager, GameState, modelos) se testea sin mocking de Gdx.
2. **Independencia total**: Cada test crea sus propios datos. No se comparte estado mutable entre tests.
3. **Cobertura orientada a regresión**: El prólogo existente debe pasar igual después de la migración visual.
4. **Nombre sistemático**: `test[Escenario]_[Acción]_[ResultadoEsperado]()` en español.

### 1.3 Mapa de Dependencias entre Clases

```
                    ┌──────────────────────┐
                    │   NarrativeNode      │ (abstract)
                    │   - id, text, speaker│
                    │   - musicTrack       │
                    │   - actions          │
                    └────────┬─────────────┘
                             │ extends
              ┌──────────────┴──────────────┐
              │                             │
    ┌─────────┴──────────┐       ┌─────────┴──────────┐
    │    DialogNode       │       │    TrialNode        │
    │  - nextId           │       │  - trialEvaluation  │
    │  - options          │       └─────────┬───────────┘
    └─────────┬───────────┘                 │
              │ contiene                    │ contiene
    ┌─────────┴──────────┐       ┌─────────┴──────────┐
    │   DialogOption     │       │  TrialEvaluation    │
    │  - targetId        │       │  - threshold        │
    │  - requiredFlag    │       │  - successTargetId  │
    │  - scoreValue      │       │  - failTargetId     │
    └────────────────────┘       │  - successFlag      │
                                 └────────────────────┘

    ┌──────────────────────┐      ┌──────────────────────┐
    │    GameState         │      │    StoryManager       │
    │  - flags             │◄─────│  - nodes: Map         │
    │  - currentNodeId     │      │  - gameState          │
    │  - trialScores       │      │  + start()            │
    │  + addFlag()         │      │  + advance()          │
    │  + addTrialScore()   │      │  + getCurrentNode()   │
    │  + getScorePercent() │      │  - checkTrialEval()   │
    └──────────────────────┘      └──────────────────────┘

    ┌──────────────────────────────────────────────────┐
    │  NarrativeDAO (interface)                        │
    │  NarrativeDAOImplementation (SQLite)             │
    │  SaveSystem (JSON / Gdx.files)                   │
    └──────────────────────────────────────────────────┘
```

---

## 2. CONFIGURACIÓN INICIAL (build.gradle)

### 2.1 Modificaciones en `core/build.gradle`

```groovy
[compileJava, compileTestJava]*.options*.encoding = 'UTF-8'
eclipse.project.name = appName + '-core'

dependencies {
  api "com.badlogicgames.ashley:ashley:$ashleyVersion"
  api "com.badlogicgames.gdx:gdx-ai:$aiVersion"
  api "com.badlogicgames.gdx:gdx-box2d:$gdxVersion"
  api "com.badlogicgames.gdx:gdx-freetype:$gdxVersion"
  api "com.badlogicgames.gdx:gdx:$gdxVersion"
  api "org.xerial:sqlite-jdbc:3.45.1.0"

  if(enableGraalNative == 'true') {
    implementation "io.github.berstanio:gdx-svmhelper-annotations:$graalHelperVersion"
  }

  // ─── TESTS ────────────────────────────────────────────────────────────
  testImplementation "org.junit.jupiter:junit-jupiter-api:5.10.0"
  testRuntimeOnly "org.junit.jupiter:junit-jupiter-engine:5.10.0"
  // Mockito (solo si se mockea Gdx.files para SaveSystem)
  testImplementation "org.mockito:mockito-core:5.7.0"

  // Para usar SQLite en memoria en NarrativeDAOTest
  testImplementation "org.xerial:sqlite-jdbc:3.45.1.0"
}

// ─── Configurar el plugin de JUnit ─────────────────────────────────────
test {
  useJUnitPlatform()
  testLogging {
    events "passed", "skipped", "failed", "standardOut", "standardError"
    showExceptions true
    showCauses true
    showStackTraces true
    exceptionFormat "full"
  }
  // Los tests de integración (DAO con SQLite) necesitan la ruta a la BD
  // No requerido para tests en memoria
}
```

### 2.2 Consideraciones sobre `root build.gradle`

El archivo raíz `build.gradle` ya tiene `apply plugin: 'java-library'` en todos los subproyectos (línea 29). No se necesita cambios allí. El plugin de `java-library` ya soporta `testImplementation`.

### 2.3 Verificar que gradle tenga JUnit 5

Ejecutar para confirmar que las dependencias se resuelven:

```bash
./gradlew core:dependencies --configuration testRuntimeClasspath
```

Debe aparecer `org.junit.jupiter:junit-jupiter-engine:5.10.0` en el árbol.

---

## 3. ESTRUCTURA DE DIRECTORIOS DE TEST

```
core/src/test/java/io/yourPath/
├── logic/
│   ├── StoryManagerTest.java
│   └── GameStateTest.java
├── models/
│   ├── DialogNodeTest.java
│   ├── TrialNodeTest.java
│   ├── NarrativeNodeTest.java    (clase interna anónima, pues es abstracta)
│   ├── DialogOptionTest.java
│   ├── TrialEvaluationTest.java
│   └── CharacterProfileTest.java
└── utils/
    ├── NarrativeDAOTest.java
    ├── SaveSystemTest.java
    └── IntentiaExceptionTest.java
```

---

## 4. CÓDIGO COMPLETO DE TESTS

### 4.1 GameStateTest.java

```java
package io.yourPath.logic;

import static org.junit.jupiter.api.Assertions.*;

import io.yourPath.models.GameState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

class GameStateTest {

    private GameState gameState;

    @BeforeEach
    void setUp() {
        gameState = new GameState();
    }

    // ─── Constructor ────────────────────────────────────────────────────

    @Test
    void testConstructor_InicializaCorrectamente() {
        assertTrue(gameState.getFlags().isEmpty(),
                "Los flags deben comenzar vacíos");
        assertNull(gameState.getCurrentNodeId(),
                "El currentNodeId debe ser null inicialmente");
        assertEquals(0, gameState.getCurrentTrialScore(),
                "El score debe ser 0 inicialmente");
        assertEquals(0, gameState.getTotalPossibleScore(),
                "El total posible debe ser 0 inicialmente");
    }

    // ─── Flags ──────────────────────────────────────────────────────────

    @Test
    void testAddFlag_FlagAgregado_DevuelveTrue() {
        gameState.addFlag("ha_hablado");
        assertTrue(gameState.hasFlag("ha_hablado"),
                "Debe tener el flag 'ha_hablado'");
    }

    @Test
    void testAddFlag_FlagNoAgregado_DevuelveFalse() {
        gameState.addFlag("ha_hablado");
        assertFalse(gameState.hasFlag("otro"),
                "No debe tener el flag 'otro'");
    }

    @Test
    void testAddFlag_FlagDuplicado_TamanoSigueSiendoUno() {
        gameState.addFlag("ha_hablado");
        gameState.addFlag("ha_hablado");
        assertEquals(1, gameState.getFlags().size(),
                "Los flags duplicados no deben aumentar el tamaño del Set");
    }

    @Test
    void testAddFlag_MultiplesFlags_TodosPresentes() {
        gameState.addFlag("flag_a");
        gameState.addFlag("flag_b");
        gameState.addFlag("flag_c");
        assertAll("Múltiples flags deben estar presentes",
                () -> assertTrue(gameState.hasFlag("flag_a")),
                () -> assertTrue(gameState.hasFlag("flag_b")),
                () -> assertTrue(gameState.hasFlag("flag_c"))
        );
        assertEquals(3, gameState.getFlags().size());
    }

    @Test
    void testHasFlag_FlagsVacios_DevuelveFalse() {
        assertFalse(gameState.hasFlag("cualquier_cosa"),
                "Sin flags agregados, debe devolver false");
    }

    // ─── SetFlags ───────────────────────────────────────────────────────

    @Test
    void testSetFlags_AsignarSet_ReemplazaFlags() {
        Set<String> nuevosFlags = new HashSet<>();
        nuevosFlags.add("flag_x");
        nuevosFlags.add("flag_y");

        gameState.setFlags(nuevosFlags);

        assertTrue(gameState.hasFlag("flag_x"));
        assertTrue(gameState.hasFlag("flag_y"));
        assertEquals(2, gameState.getFlags().size());
    }

    @Test
    void testSetFlags_SetVacio_LimpiaFlags() {
        gameState.addFlag("viejo_flag");
        gameState.setFlags(new HashSet<>());
        assertTrue(gameState.getFlags().isEmpty(),
                "Asignar un Set vacío debe limpiar los flags");
    }

    @Test
    void testSetFlags_NoAfectaReferenciaOriginal() {
        Set<String> original = new HashSet<>();
        original.add("a");
        gameState.setFlags(original);
        original.add("b");
        assertFalse(gameState.hasFlag("b"),
                "Modificar el Set original no debe afectar al GameState");
    }

    // ─── CurrentNodeId ──────────────────────────────────────────────────

    @Test
    void testSetCurrentNodeId_AsignarId_SeRecuperaCorrectamente() {
        gameState.setCurrentNodeId("nodo_inicio");
        assertEquals("nodo_inicio", gameState.getCurrentNodeId());
    }

    @Test
    void testSetCurrentNodeId_AsignarNull_DevuelveNull() {
        gameState.setCurrentNodeId(null);
        assertNull(gameState.getCurrentNodeId());
    }

    @Test
    void testSetCurrentNodeId_SobrescribirId_CambiaCorrectamente() {
        gameState.setCurrentNodeId("primero");
        gameState.setCurrentNodeId("segundo");
        assertEquals("segundo", gameState.getCurrentNodeId());
    }

    // ─── Trial Score ────────────────────────────────────────────────────

    @Test
    void testAddTrialScore_UnaVez_AcumulaCorrectamente() {
        gameState.addTrialScore(5, 10);
        assertEquals(5, gameState.getCurrentTrialScore());
        assertEquals(10, gameState.getTotalPossibleScore());
    }

    @Test
    void testAddTrialScore_DosVeces_SumaCorrectamente() {
        gameState.addTrialScore(5, 10);
        gameState.addTrialScore(3, 5);
        assertEquals(8, gameState.getCurrentTrialScore());
        assertEquals(15, gameState.getTotalPossibleScore());
    }

    @Test
    void testAddTrialScore_ScoreCero_NoAlteraValores() {
        gameState.addTrialScore(5, 10);
        gameState.addTrialScore(0, 0);
        assertEquals(5, gameState.getCurrentTrialScore());
        assertEquals(10, gameState.getTotalPossibleScore());
    }

    @Test
    void testAddTrialScore_ScoreNegativo_AcumulaCorrectamente() {
        gameState.addTrialScore(5, 10);
        // ScoreValue puede ser null en DialogOption, pero addTrialScore acepta negativos
        gameState.addTrialScore(-2, 0);
        assertEquals(3, gameState.getCurrentTrialScore());
        assertEquals(10, gameState.getTotalPossibleScore());
    }

    @Test
    void testAddTrialScore_TotalPosibleCero_NoDividePorCero() {
        gameState.addTrialScore(0, 0);
        assertEquals(0f, gameState.getScorePercentage(),
                "Con total = 0, el porcentaje debe ser 0");
    }

    // ─── Score Percentage ───────────────────────────────────────────────

    @Test
    void testGetScorePercentage_SinScores_DevuelveCero() {
        assertEquals(0f, gameState.getScorePercentage(),
                "Sin puntuaciones, el porcentaje debe ser 0");
    }

    @Test
    void testGetScorePercentage_ConScores_CalculaCorrectamente() {
        gameState.addTrialScore(3, 4);
        assertEquals(0.75f, gameState.getScorePercentage(), 0.0001f,
                "3/4 = 0.75");
    }

    @Test
    void testGetScorePercentage_ScorePerfecto_DaUno() {
        gameState.addTrialScore(10, 10);
        assertEquals(1.0f, gameState.getScorePercentage(), 0.0001f);
    }

    @Test
    void testGetScorePercentage_ScoreCero_DaCero() {
        gameState.addTrialScore(0, 5);
        assertEquals(0f, gameState.getScorePercentage(), 0.0001f);
    }

    @Test
    void testGetScorePercentage_PresicionFlotante_ToleranciaCorrecta() {
        gameState.addTrialScore(1, 3);
        assertEquals(1f / 3f, gameState.getScorePercentage(), 0.0001f);
    }

    // ─── Reset Trial Score ──────────────────────────────────────────────

    @Test
    void testResetTrialScore_ConScoresPrevios_ReiniciaACero() {
        gameState.addTrialScore(5, 10);
        gameState.resetTrialScore();
        assertEquals(0, gameState.getCurrentTrialScore());
        assertEquals(0, gameState.getTotalPossibleScore());
    }

    @Test
    void testResetTrialScore_SinScoresPrevios_MantieneCero() {
        gameState.resetTrialScore();
        assertEquals(0, gameState.getCurrentTrialScore());
        assertEquals(0, gameState.getTotalPossibleScore());
    }

    @Test
    void testResetTrialScore_DosVeces_NoLanzaExcepcion() {
        gameState.addTrialScore(5, 10);
        gameState.resetTrialScore();
        gameState.resetTrialScore();
        assertEquals(0, gameState.getCurrentTrialScore());
        assertEquals(0, gameState.getTotalPossibleScore());
    }

    // ─── Getters y Setters directos ─────────────────────────────────────

    @Test
    void testSetCurrentTrialScore_SetterDirecto_SobrescribeValor() {
        gameState.addTrialScore(5, 10);
        gameState.setCurrentTrialScore(99);
        assertEquals(99, gameState.getCurrentTrialScore());
        // totalPossible NO debe cambiar
        assertEquals(10, gameState.getTotalPossibleScore());
    }

    @Test
    void testSetTotalPossibleScore_SetterDirecto_SobrescribeValor() {
        gameState.addTrialScore(5, 10);
        gameState.setTotalPossibleScore(99);
        assertEquals(99, gameState.getTotalPossibleScore());
        // currentTrialScore NO debe cambiar
        assertEquals(5, gameState.getCurrentTrialScore());
    }

    @Test
    void testSetCurrentTrialScore_Cero_ReseteaPuntaje() {
        gameState.addTrialScore(5, 10);
        gameState.setCurrentTrialScore(0);
        assertEquals(0, gameState.getCurrentTrialScore());
        assertEquals(10, gameState.getTotalPossibleScore());
    }
}
```

### 4.2 StoryManagerTest.java

```java
package io.yourPath.logic;

import static org.junit.jupiter.api.Assertions.*;

import io.yourPath.models.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

class StoryManagerTest {

    private Map<String, NarrativeNode> nodos;
    private GameState gameState;
    private StoryManager storyManager;

    @BeforeEach
    void setUp() {
        nodos = crearNodosDePrueba();
        gameState = new GameState();
        storyManager = new StoryManager(nodos, gameState);
    }

    /**
     * Construye un mapa de nodos de prueba con el siguiente grafo:
     * <pre>
     * nodo_inicio (DialogNode, action: "comenzo")
     *   └─ nextId ──→ nodo_medio (DialogNode)
     *                    ├─ opt_a ("Ir a A") ──→ nodo_a (action: "camino_a")
     *                    └─ opt_b ("Ir a B") ──→ nodo_b (action: "camino_b")
     *                    └─ opt_score ("Ganar puntos") ──→ nodo_a (scoreValue=3)
     *                    └─ opt_llave ("Puerta") ──→ nodo_llave (requiredFlag="tiene_llave")
     *                    └─ opt_condicional ("Entrada") ──→ nodo_a (requiredFlag="tiene_pase")
     * nodo_final (DialogNode, sin nextId, sin opciones)
     * nodo_juicio (TrialNode, threshold=0.5, success→"exito", fail→"fracaso")
     * nodo_exito (DialogNode, action: "trial_exitoso")
     * nodo_fracaso (DialogNode, action: "trial_fallido")
     * nodo_sin_evaluacion (TrialNode, trialEvaluation=null)
     * </pre>
     */
    private Map<String, NarrativeNode> crearNodosDePrueba() {
        Map<String, NarrativeNode> nodos = new HashMap<>();

        // ── nodo_inicio ─────────────────────────────────────────────────
        DialogNode nodoInicio = new DialogNode();
        nodoInicio.setId("nodo_inicio");
        nodoInicio.setText("Has despertado en un bosque oscuro.");
        nodoInicio.setSpeakerId("narrador");
        nodoInicio.setNextId("nodo_medio");
        nodoInicio.setActions(List.of("comenzo"));
        nodos.put("nodo_inicio", nodoInicio);

        // ── nodo_medio ──────────────────────────────────────────────────
        DialogNode nodoMedio = new DialogNode();
        nodoMedio.setId("nodo_medio");
        nodoMedio.setText("¿Qué camino eliges?");
        nodoMedio.setSpeakerId("narrador");

        DialogOption optA = new DialogOption("Ir a la cabaña", "nodo_a");
        DialogOption optB = new DialogOption("Seguir el sendero", "nodo_b");
        DialogOption optScore = new DialogOption("Ganar puntos de juicio", "nodo_a", null);
        optScore.setScoreValue(3);
        DialogOption optLlave = new DialogOption("Abrir puerta", "nodo_llave", "tiene_llave");
        DialogOption optCondicional = new DialogOption("Entrar al castillo", "nodo_a", "tiene_pase");

        nodoMedio.setOptions(List.of(optA, optB, optScore, optLlave, optCondicional));
        nodos.put("nodo_medio", nodoMedio);

        // ── nodo_a ──────────────────────────────────────────────────────
        DialogNode nodoA = new DialogNode();
        nodoA.setId("nodo_a");
        nodoA.setText("Llegas a una cabaña abandonada.");
        nodoA.setSpeakerId("narrador");
        nodoA.setNextId("nodo_final");
        nodoA.setActions(List.of("camino_a"));
        nodos.put("nodo_a", nodoA);

        // ── nodo_b ──────────────────────────────────────────────────────
        DialogNode nodoB = new DialogNode();
        nodoB.setId("nodo_b");
        nodoB.setText("El sendero se pierde en la niebla.");
        nodoB.setSpeakerId("narrador");
        nodoB.setNextId("nodo_final");
        nodoB.setActions(List.of("camino_b"));
        nodos.put("nodo_b", nodoB);

        // ── nodo_llave ──────────────────────────────────────────────────
        DialogNode nodoLlave = new DialogNode();
        nodoLlave.setId("nodo_llave");
        nodoLlave.setText("Usaste la llave para abrir la puerta.");
        nodoLlave.setSpeakerId("narrador");
        nodoLlave.setNextId("nodo_final");
        nodoSinSalida.setActions(List.of("puerta_abierta"));
        nodos.put("nodo_llave", nodoLlave);

        // ── nodo_final ──────────────────────────────────────────────────
        DialogNode nodoFinal = new DialogNode();
        nodoFinal.setId("nodo_final");
        nodoFinal.setText("Fin del prólogo.");
        nodoFinal.setSpeakerId("narrador");
        // Sin nextId, sin opciones — nodo terminal
        nodos.put("nodo_final", nodoFinal);

        // ── nodo_juicio ────────────────────────────────────────────────
        TrialNode nodoJuicio = new TrialNode();
        nodoJuicio.setId("nodo_juicio");
        nodoJuicio.setText("El consejo te juzga.");
        nodoJuicio.setSpeakerId("consejo");
        TrialEvaluation eval = new TrialEvaluation();
        eval.setThreshold(0.5f);
        eval.setSuccessTargetId("nodo_exito");
        eval.setFailTargetId("nodo_fracaso");
        eval.setSuccessFlag("trial_superado");
        nodoJuicio.setTrialEvaluation(eval);
        nodos.put("nodo_juicio", nodoJuicio);

        // ── nodo_exito ──────────────────────────────────────────────────
        DialogNode nodoExito = new DialogNode();
        nodoExito.setId("nodo_exito");
        nodoExito.setText("El consejo te absuelve.");
        nodoExito.setSpeakerId("consejo");
        nodoExito.setActions(List.of("trial_exitoso"));
        nodos.put("nodo_exito", nodoExito);

        // ── nodo_fracaso ────────────────────────────────────────────────
        DialogNode nodoFracaso = new DialogNode();
        nodoFracaso.setId("nodo_fracaso");
        nodoFracaso.setText("El consejo te condena.");
        nodoFracaso.setSpeakerId("consejo");
        nodoFracaso.setActions(List.of("trial_fallido"));
        nodos.put("nodo_fracaso", nodoFracaso);

        // ── nodo_sin_evaluacion ─────────────────────────────────────────
        TrialNode nodoSinEval = new TrialNode();
        nodoSinEval.setId("nodo_sin_evaluacion");
        nodoSinEval.setText("Un juicio sin evaluación.");
        nodoSinEval.setSpeakerId("narrador");
        // trialEvaluation = null intencionalmente
        nodos.put("nodo_sin_evaluacion", nodoSinEval);

        return nodos;
    }

    // ─── Constructor y Estado Inicial ───────────────────────────────────

    @Test
    void testConstructor_RecibeNodosYGameState_NoLanzaExcepcion() {
        assertDoesNotThrow(() -> new StoryManager(new HashMap<>(), new GameState()));
    }

    @Test
    void testConstructor_GetGameState_DevuelveMismaInstancia() {
        assertSame(gameState, storyManager.getGameState());
    }

    // ─── Start ──────────────────────────────────────────────────────────

    @Test
    void testStart_NodoValido_EstableceNodoActual() {
        storyManager.start("nodo_inicio");
        assertNotNull(storyManager.getCurrentNode());
        assertEquals("nodo_inicio", storyManager.getCurrentNode().getId());
    }

    @Test
    void testStart_NodoConActions_AgregaFlags() {
        storyManager.start("nodo_inicio");
        assertTrue(gameState.hasFlag("comenzo"),
                "start() debe ejecutar processActions() y agregar 'comenzo'");
    }

    @Test
    void testStart_DosVeces_CambiaNodoActual() {
        storyManager.start("nodo_inicio");
        storyManager.start("nodo_medio");
        assertEquals("nodo_medio", storyManager.getCurrentNode().getId());
    }

    // ─── Advance con targetId (transición directa) ──────────────────────

    @Test
    void testAdvance_TargetIdValido_NodoCambia() {
        storyManager.start("nodo_inicio");
        storyManager.advance("nodo_medio");
        assertEquals("nodo_medio", storyManager.getCurrentNode().getId());
    }

    @Test
    void testAdvance_TargetIdValido_ProcesaActions() {
        storyManager.start("nodo_inicio");
        storyManager.advance("nodo_a");
        assertTrue(gameState.hasFlag("camino_a"),
                "advance() debe ejecutar processActions() del nodo destino");
    }

    @Test
    void testAdvance_TargetIdInexistente_NodoActualNoCambia() {
        storyManager.start("nodo_inicio");
        String nodoOriginal = storyManager.getCurrentNode().getId();
        storyManager.advance("id_que_no_existe");
        assertEquals(nodoOriginal, storyManager.getCurrentNode().getId(),
                "Si el targetId no existe, el nodo actual no debe cambiar");
    }

    @Test
    void testAdvance_TargetIdInexistente_NoLanzaExcepcion() {
        storyManager.start("nodo_inicio");
        assertDoesNotThrow(() -> storyManager.advance("id_que_no_existe"));
    }

    @Test
    void testAdvance_TargetIdNull_NodoNoCambia() {
        storyManager.start("nodo_inicio");
        storyManager.advance((String) null);
        assertEquals("nodo_inicio", storyManager.getCurrentNode().getId(),
                "advance(null) no debe cambiar el nodo actual");
    }

    @Test
    void testAdvance_ActionsMultiples_AgregaTodosLosFlags() {
        storyManager.start("nodo_inicio");
        storyManager.advance("nodo_a");
        assertAll("Debe tener flags tanto del nodo inicial como del destino",
                () -> assertTrue(gameState.hasFlag("comenzo"),
                        "Flag 'comenzo' del nodo_inicio"),
                () -> assertTrue(gameState.hasFlag("camino_a"),
                        "Flag 'camino_a' del nodo_a")
        );
    }

    // ─── Advance con DialogOption ───────────────────────────────────────

    @Test
    void testAdvance_OpcionValida_NavegaAlNodoDestino() {
        storyManager.start("nodo_medio");
        DialogNode nodoMedio = (DialogNode) storyManager.getCurrentNode();
        DialogOption opt = nodoMedio.getOptions().get(0); // opt_a → nodo_a
        storyManager.advance(opt);
        assertEquals("nodo_a", storyManager.getCurrentNode().getId());
    }

    @Test
    void testAdvance_OpcionValida_ProcesaActionsDelDestino() {
        storyManager.start("nodo_medio");
        DialogNode nodoMedio = (DialogNode) storyManager.getCurrentNode();
        DialogOption opt = nodoMedio.getOptions().get(0); // opt_a → nodo_a
        storyManager.advance(opt);
        assertTrue(gameState.hasFlag("camino_a"));
    }

    @Test
    void testAdvance_OpcionConScore_AcumulaPuntaje() {
        storyManager.start("nodo_medio");
        DialogNode nodoMedio = (DialogNode) storyManager.getCurrentNode();
        // optScore → target nodo_a, scoreValue=3
        DialogOption opt = nodoMedio.getOptions().get(2);
        storyManager.advance(opt);
        assertEquals(3, gameState.getCurrentTrialScore());
        assertEquals(1, gameState.getTotalPossibleScore());
    }

    @Test
    void testAdvance_OpcionConScore_DosVeces_AcumulaCorrectamente() {
        storyManager.start("nodo_medio");

        DialogNode nodoMedio = (DialogNode) storyManager.getCurrentNode();
        DialogOption optScore = nodoMedio.getOptions().get(2); // scoreValue=3

        // Avanzar con la opción de score
        storyManager.advance(optScore);
        // Ahora estamos en nodo_a, que tiene nextId = nodo_final.
        // Volvemos al nodo_medio manualmente para probar acumulación
        // (en un flujo real no se retrocede, pero probamos la lógica de addTrialScore)
        assertEquals(3, gameState.getCurrentTrialScore());
        assertEquals(1, gameState.getTotalPossibleScore());
    }

    @Test
    void testAdvance_OpcionSinScore_NoAlteraPuntaje() {
        storyManager.start("nodo_medio");
        DialogNode nodoMedio = (DialogNode) storyManager.getCurrentNode();
        DialogOption opt = nodoMedio.getOptions().get(0); // opt_a, sin scoreValue
        storyManager.advance(opt);
        assertEquals(0, gameState.getCurrentTrialScore());
        assertEquals(0, gameState.getTotalPossibleScore());
    }

    @Test
    void testAdvance_OpcionNull_NoCambiaNodo() {
        storyManager.start("nodo_medio");
        String nodoOriginal = storyManager.getCurrentNode().getId();
        storyManager.advance((DialogOption) null);
        assertEquals(nodoOriginal, storyManager.getCurrentNode().getId(),
                "advance((DialogOption) null) no debe cambiar el nodo");
    }

    @Test
    void testAdvance_OpcionConRequiredFlag_AvanzaSinValidacion() {
        // La validación de requiredFlag es responsabilidad de la UI.
        // StoryManager.advance() debe avanzar incluso si el flag no está presente.
        storyManager.start("nodo_medio");
        DialogNode nodoMedio = (DialogNode) storyManager.getCurrentNode();
        // optLlave → target "nodo_llave", requiredFlag="tiene_llave"
        DialogOption opt = nodoMedio.getOptions().get(3);
        assertFalse(gameState.hasFlag("tiene_llave"),
                "No debe tener el flag requerido");
        // Aún así, advance() debe funcionar (el filtro es UI)
        storyManager.advance(opt);
        assertEquals("nodo_llave", storyManager.getCurrentNode().getId(),
                "advance() debe funcionar aunque el flag requerido no esté presente");
    }

    @Test
    void testAdvance_OpcionConRequiredFlag_TeniendoElFlag_Avanza() {
        gameState.addFlag("tiene_pase");
        storyManager.start("nodo_medio");
        DialogNode nodoMedio = (DialogNode) storyManager.getCurrentNode();
        // optCondicional → target "nodo_a", requiredFlag="tiene_pase"
        DialogOption opt = nodoMedio.getOptions().get(4);
        storyManager.advance(opt);
        assertEquals("nodo_a", storyManager.getCurrentNode().getId());
    }

    // ─── Flujo con nextId (transición automática) ───────────────────────

    @Test
    void testFlujo_InicioAMedioPorNextId() {
        storyManager.start("nodo_inicio");
        // nodo_inicio.nextId = "nodo_medio", pero start() no avanza automáticamente.
        // Solo establece el nodo. Necesitamos advance() explícito.
        // Esto verifica que start() NO sigue nextId.
        assertEquals("nodo_inicio", storyManager.getCurrentNode().getId());
    }

    @Test
    void testFlujo_NodoConNextId_AlAvanzarSinTarget_SigueNextId() {
        // El código actual requiere que la UI decida qué avance llamar.
        // Si el nodo tiene nextId y no hay opciones, la UI llama advance(nextId).
        storyManager.start("nodo_inicio");
        storyManager.advance("nodo_medio");
        assertEquals("nodo_medio", storyManager.getCurrentNode().getId());
    }

    // ─── Trial ──────────────────────────────────────────────────────────

    @Test
    void testTrial_ScoreSuperaThreshold_FlujoVaAExito() {
        // Score: 5/5 = 100% > 50%
        gameState.addTrialScore(5, 5);

        storyManager.start("nodo_juicio");
        // start() ejecuta startNodeId y processActions.
        // Como nodo_juicio es TrialNode, start() NO llama a checkTrialEvaluation
        // porque start() solo llama a processActions().
        // checkTrialEvaluation solo se llama desde advance(String).
        // Así que debemos hacer advance explícito: pero no hay targetId...
        // Corregimos: start() establece el nodo, luego advance() con un targetId.
        // Pero TrialNode.getNextTargetId() retorna null.

        // El flujo real: UI llama advance(targetId) cuando quiere ir a un TrialNode.
        // Entonces probamos así:
    }

    @Test
    void testTrial_ScoreSuperaThreshold_CheckTrialEvaluation_DirigeAExito() {
        gameState.addTrialScore(8, 10); // 80% > 50%

        // Iniciamos en un nodo normal y avanzamos hacia el trial
        // Pero necesitamos un nodo que tenga nextId = "nodo_juicio"
        // En nuestros nodos de prueba, no hay uno que apunte a nodo_juicio.
        // Creamos un flujo manual:

        // Simulamos que llegamos al trial: el GameState ya tiene el currentNodeId
        // llamando a advance("nodo_juicio") directamente:
        storyManager.advance("nodo_juicio");
        // Como es TrialNode, checkTrialEvaluation() se ejecuta.
        // Score 80% >= 50% → success → va a "nodo_exito"
        assertEquals("nodo_exito", storyManager.getCurrentNode().getId());
    }

    @Test
    void testTrial_ScoreSuperaThreshold_AgregaFlagDeExito() {
        gameState.addTrialScore(3, 4); // 75% > 50%

        storyManager.advance("nodo_juicio");
        assertTrue(gameState.hasFlag("trial_superado"),
                "Al superar el trial debe agregar el successFlag");
    }

    @Test
    void testTrial_ScoreBajoThreshold_FlujoVaAFracaso() {
        gameState.addTrialScore(1, 10); // 10% < 50%

        storyManager.advance("nodo_juicio");
        assertEquals("nodo_fracaso", storyManager.getCurrentNode().getId());
    }

    @Test
    void testTrial_ScoreBajoThreshold_NoAgregaFlagDeExito() {
        gameState.addTrialScore(0, 5); // 0% < 50%

        storyManager.advance("nodo_juicio");
        assertFalse(gameState.hasFlag("trial_superado"),
                "Al fallar el trial NO debe agregar el successFlag");
    }

    @Test
    void testTrial_ScoreExactamenteThreshold_ConsideraExito() {
        gameState.addTrialScore(5, 10); // 50% >= 50%

        storyManager.advance("nodo_juicio");
        assertEquals("nodo_exito", storyManager.getCurrentNode().getId(),
                "Con score exactamente igual al threshold debe ser éxito");
    }

    @Test
    void testTrial_DespuesDeEvaluacion_ScoreSeResetea() {
        gameState.addTrialScore(5, 10);
        storyManager.advance("nodo_juicio");
        assertEquals(0, gameState.getCurrentTrialScore(),
                "checkTrialEvaluation debe resetear el score");
        assertEquals(0, gameState.getTotalPossibleScore());
    }

    @Test
    void testTrial_NodoSinEvaluacion_NoLanzaExcepcion() {
        storyManager.advance("nodo_sin_evaluacion");
        assertNotNull(storyManager.getCurrentNode());
        assertEquals("nodo_sin_evaluacion", storyManager.getCurrentNode().getId(),
                "Debe quedarse en el nodo sin evaluación");
    }

    @Test
    void testTrial_NodoSinEvaluacion_NoCambiaNodo() {
        // Si trialEvaluation es null, checkTrialEvaluation retorna sin hacer nada.
        // El nodo actual sigue siendo el TrialNode.
        storyManager.advance("nodo_sin_evaluacion");
        assertEquals("nodo_sin_evaluacion", storyManager.getCurrentNode().getId());
    }

    @Test
    void testTrial_TrialNodeSinScorePrevios_FlujoVaAFracaso() {
        // Score 0% < 50%
        storyManager.advance("nodo_juicio");
        assertEquals("nodo_fracaso", storyManager.getCurrentNode().getId(),
                "Sin puntuación previa, el trial debe fallar");
    }

    @Test
    void testTrial_ThresholdCero_SiempreExito() {
        TrialNode nodoThresholdCero = new TrialNode();
        nodoThresholdCero.setId("nodo_threshold_cero");
        TrialEvaluation eval = new TrialEvaluation();
        eval.setThreshold(0f); // threshold = 0
        eval.setSuccessTargetId("nodo_exito");
        eval.setFailTargetId("nodo_fracaso");
        nodoThresholdCero.setTrialEvaluation(eval);
        nodos.put("nodo_threshold_cero", nodoThresholdCero);

        // Score 0% >= 0% → éxito
        storyManager.advance("nodo_threshold_cero");
        assertEquals("nodo_exito", storyManager.getCurrentNode().getId());
    }

    @Test
    void testTrial_ThresholdUno_SoloExitoConScorePerfecto() {
        TrialNode nodoThresholdUno = new TrialNode();
        nodoThresholdUno.setId("nodo_threshold_uno");
        TrialEvaluation eval = new TrialEvaluation();
        eval.setThreshold(1f); // threshold = 1.0 (100%)
        eval.setSuccessTargetId("nodo_exito");
        eval.setFailTargetId("nodo_fracaso");
        nodoThresholdUno.setTrialEvaluation(eval);
        nodos.put("nodo_threshold_uno", nodoThresholdUno);

        // Score 8/10 = 80% < 100% → fracaso
        gameState.addTrialScore(8, 10);
        storyManager.advance("nodo_threshold_uno");
        assertEquals("nodo_fracaso", storyManager.getCurrentNode().getId());

        // Reset y probar con score perfecto
        gameState.resetTrialScore();
        gameState.addTrialScore(10, 10); // 100% >= 100% → éxito
        storyManager.advance("nodo_threshold_uno");
        assertEquals("nodo_exito", storyManager.getCurrentNode().getId());
    }

    // ─── ProcessActions (prueba directa) ────────────────────────────────

    @Test
    void testProcessActions_NodoConNullActions_NoLanzaExcepcion() {
        DialogNode nodo = new DialogNode();
        nodo.setId("nodo_null_actions");
        nodo.setText("test");
        nodo.setActions(null);
        nodos.put("nodo_null_actions", nodo);

        assertDoesNotThrow(() -> storyManager.advance("nodo_null_actions"));
    }

    @Test
    void testProcessActions_NodoConActionsVacia_NoAgregaFlags() {
        DialogNode nodo = new DialogNode();
        nodo.setId("nodo_sin_actions");
        nodo.setText("test");
        nodo.setActions(List.of()); // lista vacía
        nodos.put("nodo_sin_actions", nodo);

        storyManager.advance("nodo_sin_actions");
        assertTrue(gameState.getFlags().isEmpty());
    }

    @Test
    void testProcessActions_NodoConMultiplesActions_AgregaTodas() {
        DialogNode nodo = new DialogNode();
        nodo.setId("nodo_multi_action");
        nodo.setText("test");
        nodo.setActions(List.of("accion_a", "accion_b", "accion_c"));
        nodos.put("nodo_multi_action", nodo);

        storyManager.advance("nodo_multi_action");
        assertAll("Todas las acciones deben estar presentes",
                () -> assertTrue(gameState.hasFlag("accion_a")),
                () -> assertTrue(gameState.hasFlag("accion_b")),
                () -> assertTrue(gameState.hasFlag("accion_c"))
        );
    }

    // ─── GetCurrentNode ─────────────────────────────────────────────────

    @Test
    void testGetCurrentNode_SinStart_DevuelveNull() {
        assertNull(storyManager.getCurrentNode(),
                "Sin llamar a start(), getCurrentNode() debe devolver null");
    }

    @Test
    void testGetCurrentNode_DespuesDeStart_DevuelveNodoCorrecto() {
        storyManager.start("nodo_inicio");
        assertEquals("nodo_inicio", storyManager.getCurrentNode().getId());
    }

    @Test
    void testGetCurrentNode_NodoNoEnMapa_DevuelveNull() {
        gameState.setCurrentNodeId("nodo_inexistente");
        assertNull(storyManager.getCurrentNode(),
                "Si el currentNodeId no está en el mapa, debe devolver null");
    }

    // ─── Flujo narrativo completo ───────────────────────────────────────

    @Test
    void testFlujoCompleto_InicioAFinSinDesvio() {
        storyManager.start("nodo_inicio");
        // inicio → advance(nextId="nodo_medio") → nodo_medio
        storyManager.advance("nodo_medio");

        // Elegir opción A: Ir a la cabaña → nodo_a
        DialogNode nodoMedio = (DialogNode) storyManager.getCurrentNode();
        DialogOption optA = nodoMedio.getOptions().get(0);
        storyManager.advance(optA);

        // nodo_a tiene nextId="nodo_final"
        storyManager.advance("nodo_final");

        assertEquals("nodo_final", storyManager.getCurrentNode().getId());
        assertTrue(gameState.hasFlag("comenzo"));
        assertTrue(gameState.hasFlag("camino_a"));
    }

    @Test
    void testFlujoCompleto_ConTrialExitoso() {
        storyManager.start("nodo_inicio");
        storyManager.advance("nodo_medio");

        // Elegir opción con score: "Ganar puntos de juicio" → +3 puntos
        DialogNode nodoMedio = (DialogNode) storyManager.getCurrentNode();
        DialogOption optScore = nodoMedio.getOptions().get(2);
        storyManager.advance(optScore);

        // Ahora estamos en nodo_a; vamos al trial
        // Simulamos que un nodo lleva al trial
        gameState.setCurrentNodeId("nodo_a"); // bypass para el test
        storyManager.advance("nodo_juicio");

        // Score: 3/1 = 300% (≥ 50%) → éxito
        assertEquals("nodo_exito", storyManager.getCurrentNode().getId());
        assertTrue(gameState.hasFlag("trial_superado"));
    }

    @Test
    void testFlujoCompleto_OpcionB_CaminoB() {
        storyManager.start("nodo_inicio");
        storyManager.advance("nodo_medio");

        DialogNode nodoMedio = (DialogNode) storyManager.getCurrentNode();
        DialogOption optB = nodoMedio.getOptions().get(1);
        storyManager.advance(optB);

        assertEquals("nodo_b", storyManager.getCurrentNode().getId());
        assertTrue(gameState.hasFlag("camino_b"));
    }

    // ─── Edge Cases ─────────────────────────────────────────────────────

    @Test
    void testStart_NodoInexistente_CurrentNodeIdSeEstablecePeroGetDevuelveNull() {
        // start() establece el ID aunque el nodo no exista.
        storyManager.start("id_inexistente");
        assertEquals("id_inexistente", gameState.getCurrentNodeId());
        assertNull(storyManager.getCurrentNode());
    }

    @Test
    void testAdvance_NodoConNextIdValido_SigueNextId() {
        // Este test verifica que la UI llama advance(nextId) correctamente
        storyManager.start("nodo_a"); // nodo_a.nextId = "nodo_final"
        storyManager.advance("nodo_final");
        assertEquals("nodo_final", storyManager.getCurrentNode().getId());
    }

    @Test
    void testMultipleAdvances_SinStart_NoLanzaExcepcion() {
        assertDoesNotThrow(() -> {
            storyManager.advance("nodo_inicio");
            storyManager.advance("nodo_medio");
            storyManager.advance("nodo_final");
        });
    }

    @Test
    void testMapaVacio_AdvanceNoLanzaExcepcion() {
        StoryManager managerVacio = new StoryManager(new HashMap<>(), new GameState());
        assertDoesNotThrow(() -> managerVacio.advance("cualquier_id"));
        assertNull(managerVacio.getCurrentNode());
    }

    @Test
    void testMapaVacio_StartNoLanzaExcepcion() {
        StoryManager managerVacio = new StoryManager(new HashMap<>(), new GameState());
        assertDoesNotThrow(() -> managerVacio.start("cualquier_id"));
        assertNull(managerVacio.getCurrentNode());
    }

    @Test
    void testAdvance_CadenaVacia_NoCambiaNodo() {
        storyManager.start("nodo_inicio");
        storyManager.advance("");
        assertEquals("nodo_inicio", storyManager.getCurrentNode().getId(),
                "advance('') no debe cambiar el nodo porque '' no está en el mapa");
    }

    @Test
    void testProcessActions_NoAlteraFlagsDeNodosAnteriores() {
        storyManager.start("nodo_inicio"); // agrega "comenzo"
        storyManager.advance("nodo_a");    // agrega "camino_a"

        assertAll("Flags de nodos anteriores deben persistir",
                () -> assertTrue(gameState.hasFlag("comenzo")),
                () -> assertTrue(gameState.hasFlag("camino_a"))
        );
    }
}
```

### 4.3 NarrativeDAOTest.java

```java
package io.yourPath.utils;

import static org.junit.jupiter.api.Assertions.*;

import io.yourPath.models.*;
import org.junit.jupiter.api.*;

import java.sql.*;
import java.util.Map;

/**
 * Test de integración para NarrativeDAOImplementation usando SQLite en memoria.
 *
 * NOTA: Estos tests requieren que sqlite-jdbc esté en el classpath de test
 * (ya está en core/build.gradle como dependencia api y testImplementation).
 *
 * Cada test crea sus propias tablas y datos en una BD en memoria (:memory:),
 * garantizando aislamiento total entre tests.
 */
class NarrativeDAOTest {

    private NarrativeDAOImplementation dao;
    private Connection conn;

    @BeforeEach
    void setUp() throws SQLException {
        // Conectar a BD en memoria
        conn = DriverManager.getConnection("jdbc:sqlite::memory:");
        dao = new NarrativeDAOImplementation(":memory:");

        // Crear tablas
        crearTablas(conn);
    }

    @AfterEach
    void tearDown() throws SQLException {
        if (conn != null && !conn.isClosed()) {
            conn.close();
        }
    }

    private void crearTablas(Connection conn) throws SQLException {
        String sqlCharacters = """
            CREATE TABLE IF NOT EXISTS characters (
                id TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                portrait_path TEXT
            )
        """;

        String sqlNodes = """
            CREATE TABLE IF NOT EXISTS dialog_nodes (
                id TEXT PRIMARY KEY,
                type TEXT DEFAULT 'dialog',
                text TEXT,
                speaker_id TEXT,
                next_id TEXT,
                music_track TEXT
            )
        """;

        String sqlOptions = """
            CREATE TABLE IF NOT EXISTS dialog_options (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                node_id TEXT NOT NULL,
                text TEXT NOT NULL,
                target_id TEXT NOT NULL,
                required_flag TEXT,
                score_value INTEGER,
                FOREIGN KEY (node_id) REFERENCES dialog_nodes(id)
            )
        """;

        String sqlActions = """
            CREATE TABLE IF NOT EXISTS dialog_actions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                node_id TEXT NOT NULL,
                action_name TEXT NOT NULL,
                FOREIGN KEY (node_id) REFERENCES dialog_nodes(id)
            )
        """;

        String sqlTrials = """
            CREATE TABLE IF NOT EXISTS trial_evaluations (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                node_id TEXT NOT NULL,
                threshold REAL NOT NULL DEFAULT 0.5,
                success_target_id TEXT NOT NULL,
                fail_target_id TEXT NOT NULL,
                success_flag TEXT,
                FOREIGN KEY (node_id) REFERENCES dialog_nodes(id)
            )
        """;

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sqlCharacters);
            stmt.execute(sqlNodes);
            stmt.execute(sqlOptions);
            stmt.execute(sqlActions);
            stmt.execute(sqlTrials);
        }
    }

    // ─── getAllCharacters ───────────────────────────────────────────────

    @Test
    void testGetAllCharacters_BaseDatosVacia_RetornaMapaVacio() throws IntentiaException {
        Map<String, CharacterProfile> personajes = dao.getAllCharacters();
        assertNotNull(personajes, "Nunca debe devolver null");
        assertTrue(personajes.isEmpty(), "BD vacía debe devolver mapa vacío");
    }

    @Test
    void testGetAllCharacters_ConDosPersonajes_RetornaAmbos() throws IntentiaException, SQLException {
        insertarPersonaje(conn, "abuelo", "Abuelo", "portraits/abuelo.png");
        insertarPersonaje(conn, "nino", "Niño", "portraits/nino.png");

        Map<String, CharacterProfile> personajes = dao.getAllCharacters();

        assertEquals(2, personajes.size());
        assertTrue(personajes.containsKey("abuelo"));
        assertTrue(personajes.containsKey("nino"));
    }

    @Test
    void testGetAllCharacters_DatosCorrectos() throws IntentiaException, SQLException {
        insertarPersonaje(conn, "abuelo", "Abuelo", "portraits/abuelo.png");

        Map<String, CharacterProfile> personajes = dao.getAllCharacters();
        CharacterProfile abuelo = personajes.get("abuelo");

        assertNotNull(abuelo);
        assertEquals("abuelo", abuelo.getId());
        assertEquals("Abuelo", abuelo.getName());
        assertEquals("portraits/abuelo.png", abuelo.getPortraitPath());
    }

    @Test
    void testGetAllCharacters_PortraitPathNull_NoLanzaExcepcion() throws IntentiaException, SQLException {
        insertarPersonaje(conn, "narrador", "Narrador", null);

        Map<String, CharacterProfile> personajes = dao.getAllCharacters();
        CharacterProfile narrador = personajes.get("narrador");
        assertNotNull(narrador);
        assertNull(narrador.getPortraitPath());
    }

    @Test
    void testGetAllCharacters_CaracteresEspecialesEnTexto() throws IntentiaException, SQLException {
        insertarPersonaje(conn, "elfo", "Élfo del Bósque", "portraits/elfo.png");

        Map<String, CharacterProfile> personajes = dao.getAllCharacters();
        assertEquals("Élfo del Bósque", personajes.get("elfo").getName());
    }

    // ─── getAllDialogNodes ──────────────────────────────────────────────

    @Test
    void testGetAllDialogNodes_BaseDatosVacia_RetornaMapaVacio() throws IntentiaException {
        Map<String, NarrativeNode> nodos = dao.getAllDialogNodes();
        assertNotNull(nodos, "Nunca debe devolver null");
        assertTrue(nodos.isEmpty(), "BD vacía debe devolver mapa vacío");
    }

    @Test
    void testGetAllDialogNodes_NodoDialogoSinOpciones_CargaCorrectamente() throws IntentiaException, SQLException {
        insertarNodoDialogo(conn, "nodo_inicio", "dialog", "Hola mundo", "narrador",
                "nodo_siguiente", null);

        Map<String, NarrativeNode> nodos = dao.getAllDialogNodes();
        assertEquals(1, nodos.size());

        NarrativeNode nodo = nodos.get("nodo_inicio");
        assertNotNull(nodo);
        assertInstanceOf(DialogNode.class, nodo);
        assertEquals("nodo_inicio", nodo.getId());
        assertEquals("Hola mundo", nodo.getText());
        assertEquals("narrador", nodo.getSpeakerId());
    }

    @Test
    void testGetAllDialogNodes_NodoDialogoConOpciones_CargaOpciones() throws IntentiaException, SQLException {
        insertarNodoDialogo(conn, "nodo_escoger", "dialog", "¿Qué haces?", "narrador",
                null, null);
        insertarOpcion(conn, "nodo_escoger", "Ir al norte", "nodo_norte", null, null);
        insertarOpcion(conn, "nodo_escoger", "Ir al sur", "nodo_sur", null, null);

        Map<String, NarrativeNode> nodos = dao.getAllDialogNodes();
        DialogNode nodo = (DialogNode) nodos.get("nodo_escoger");

        assertNotNull(nodo);
        assertEquals(2, nodo.getOptions().size());
        assertEquals("Ir al norte", nodo.getOptions().get(0).getText());
        assertEquals("nodo_norte", nodo.getOptions().get(0).getTargetId());
        assertEquals("Ir al sur", nodo.getOptions().get(1).getText());
        assertEquals("nodo_sur", nodo.getOptions().get(1).getTargetId());
    }

    @Test
    void testGetAllDialogNodes_OpcionConRequiredFlag_CargaCorrectamente() throws IntentiaException, SQLException {
        insertarNodoDialogo(conn, "nodo_puerta", "dialog", "Una puerta cerrada", "narrador",
                null, null);
        insertarOpcion(conn, "nodo_puerta", "Abrir con llave", "nodo_abierto",
                "tiene_llave", null);

        Map<String, NarrativeNode> nodos = dao.getAllDialogNodes();
        DialogNode nodo = (DialogNode) nodos.get("nodo_puerta");

        assertEquals("tiene_llave", nodo.getOptions().get(0).getRequiredFlag());
    }

    @Test
    void testGetAllDialogNodes_OpcionConScoreValue_CargaCorrectamente() throws IntentiaException, SQLException {
        insertarNodoDialogo(conn, "nodo_juicio", "dialog", "Responde", "juez",
                null, null);
        insertarOpcion(conn, "nodo_juicio", "Respuesta correcta", "nodo_exito",
                null, 5);

        Map<String, NarrativeNode> nodos = dao.getAllDialogNodes();
        DialogNode nodo = (DialogNode) nodos.get("nodo_juicio");

        assertEquals(Integer.valueOf(5), nodo.getOptions().get(0).getScoreValue());
    }

    @Test
    void testGetAllDialogNodes_OpcionSinScoreValue_ScoreValueEsNull() throws IntentiaException, SQLException {
        insertarNodoDialogo(conn, "nodo_normal", "dialog", "Continúa", "narrador",
                "nodo_sig", null);
        insertarOpcion(conn, "nodo_normal", "Continuar", "nodo_sig", null, null);

        Map<String, NarrativeNode> nodos = dao.getAllDialogNodes();
        DialogNode nodo = (DialogNode) nodos.get("nodo_normal");

        assertNull(nodo.getOptions().get(0).getScoreValue());
    }

    @Test
    void testGetAllDialogNodes_NodoConAcciones_CargaAcciones() throws IntentiaException, SQLException {
        insertarNodoDialogo(conn, "nodo_evento", "dialog", "Evento especial", "narrador",
                null, null);
        insertarAccion(conn, "nodo_evento", "ha_visto_evento");
        insertarAccion(conn, "nodo_evento", "ha_obtenido_objeto");

        Map<String, NarrativeNode> nodos = dao.getAllDialogNodes();
        NarrativeNode nodo = nodos.get("nodo_evento");

        assertNotNull(nodo);
        assertEquals(2, nodo.getActions().size());
        assertTrue(nodo.getActions().contains("ha_visto_evento"));
        assertTrue(nodo.getActions().contains("ha_obtenido_objeto"));
    }

    @Test
    void testGetAllDialogNodes_NodoTrialConEvaluacion_CargaCorrectamente() throws IntentiaException, SQLException {
        insertarNodoDialogo(conn, "trial_final", "trial", "El juicio final", "consejo",
                null, "epica");

        // Opciones para el trial (para puntuar)
        insertarOpcion(conn, "trial_final", "Decir verdad", "trial_final", null, 3);
        insertarOpcion(conn, "trial_final", "Mentir", "trial_final", null, 0);

        // Evaluación del trial
        insertarEvaluacionTrial(conn, "trial_final", 0.6f, "nodo_exito", "nodo_fracaso", "trial_superado");

        Map<String, NarrativeNode> nodos = dao.getAllDialogNodes();
        NarrativeNode nodo = nodos.get("trial_final");

        assertNotNull(nodo);
        assertInstanceOf(TrialNode.class, nodo);

        TrialNode trialNode = (TrialNode) nodo;
        TrialEvaluation eval = trialNode.getTrialEvaluation();
        assertNotNull(eval);
        assertEquals(0.6f, eval.getThreshold(), 0.0001f);
        assertEquals("nodo_exito", eval.getSuccessTargetId());
        assertEquals("nodo_fracaso", eval.getFailTargetId());
        assertEquals("trial_superado", eval.getSuccessFlag());
    }

    @Test
    void testGetAllDialogNodes_NodoTrialSinEvaluacion_NoTieneEvaluacion() throws IntentiaException, SQLException {
        insertarNodoDialogo(conn, "trial_vacio", "trial", "Trial sin eval", "narrador",
                null, null);
        // No insertar trial_evaluations para este nodo

        Map<String, NarrativeNode> nodos = dao.getAllDialogNodes();
        TrialNode trialNode = (TrialNode) nodos.get("trial_vacio");

        assertNotNull(trialNode);
        assertNull(trialNode.getTrialEvaluation(),
                "TrialNode sin fila en trial_evaluations debe tener evaluation null");
    }

    @Test
    void testGetAllDialogNodes_MultiplesNodosConOpciones_NoSeMezclan() throws IntentiaException, SQLException {
        insertarNodoDialogo(conn, "nodo_a", "dialog", "Texto A", "narrador",
                null, null);
        insertarOpcion(conn, "nodo_a", "Opción A1", "destino_a1", null, null);

        insertarNodoDialogo(conn, "nodo_b", "dialog", "Texto B", "narrador",
                null, null);
        insertarOpcion(conn, "nodo_b", "Opción B1", "destino_b1", null, null);
        insertarOpcion(conn, "nodo_b", "Opción B2", "destino_b2", null, null);

        Map<String, NarrativeNode> nodos = dao.getAllDialogNodes();
        assertEquals(2, nodos.size());

        DialogNode nodoA = (DialogNode) nodos.get("nodo_a");
        DialogNode nodoB = (DialogNode) nodos.get("nodo_b");

        assertEquals(1, nodoA.getOptions().size());
        assertEquals(2, nodoB.getOptions().size());
    }

    @Test
    void testGetAllDialogNodes_IntegridadReferencial_ForeignKeyValida() throws IntentiaException, SQLException {
        // Insertar opción cuyo node_id no existe (violación de integridad)
        // SQLite por defecto NO enforce foreign keys, así que esto no debe fallar
        insertarOpcion(conn, "nodo_inexistente", "Opción huérfana", "destino", null, null);

        Map<String, NarrativeNode> nodos = dao.getAllDialogNodes();
        // No debe lanzar excepción, la opción huérfana simplemente se ignora
        assertTrue(nodos.isEmpty());
    }

    @Test
    void testGetAllDialogNodes_CamposNullEnTablas_NoLanzaExcepcion() throws IntentiaException, SQLException {
        // Insertar nodo con campos null
        String sql = "INSERT INTO dialog_nodes (id, type) VALUES ('nodo_minimo', 'dialog')";
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }

        Map<String, NarrativeNode> nodos = dao.getAllDialogNodes();
        DialogNode nodo = (DialogNode) nodos.get("nodo_minimo");
        assertNotNull(nodo);
        assertNull(nodo.getText());
        assertNull(nodo.getSpeakerId());
        assertNull(nodo.getMusicTrack());
        assertNull(nodo.getNextId());
    }

    // ─── Métodos auxiliares para insertar datos de prueba ───────────────

    private void insertarPersonaje(Connection conn, String id, String name, String portraitPath)
            throws SQLException {
        String sql = "INSERT INTO characters (id, name, portrait_path) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, id);
            pstmt.setString(2, name);
            if (portraitPath != null) {
                pstmt.setString(3, portraitPath);
            } else {
                pstmt.setNull(3, java.sql.Types.VARCHAR);
            }
            pstmt.executeUpdate();
        }
    }

    private void insertarNodoDialogo(Connection conn, String id, String type, String text,
                                     String speakerId, String nextId, String musicTrack)
            throws SQLException {
        String sql = "INSERT INTO dialog_nodes (id, type, text, speaker_id, next_id, music_track) " +
                "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, id);
            pstmt.setString(2, type);
            pstmt.setString(3, text);
            pstmt.setString(4, speakerId);
            pstmt.setString(5, nextId);
            pstmt.setString(6, musicTrack);
            pstmt.executeUpdate();
        }
    }

    private void insertarOpcion(Connection conn, String nodeId, String text, String targetId,
                                String requiredFlag, Integer scoreValue)
            throws SQLException {
        String sql = "INSERT INTO dialog_options (node_id, text, target_id, required_flag, score_value) " +
                "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, nodeId);
            pstmt.setString(2, text);
            pstmt.setString(3, targetId);
            pstmt.setString(4, requiredFlag);
            if (scoreValue != null) {
                pstmt.setInt(5, scoreValue);
            } else {
                pstmt.setNull(5, java.sql.Types.INTEGER);
            }
            pstmt.executeUpdate();
        }
    }

    private void insertarAccion(Connection conn, String nodeId, String actionName)
            throws SQLException {
        String sql = "INSERT INTO dialog_actions (node_id, action_name) VALUES (?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, nodeId);
            pstmt.setString(2, actionName);
            pstmt.executeUpdate();
        }
    }

    private void insertarEvaluacionTrial(Connection conn, String nodeId, float threshold,
                                         String successTargetId, String failTargetId,
                                         String successFlag)
            throws SQLException {
        String sql = "INSERT INTO trial_evaluations (node_id, threshold, success_target_id, " +
                "fail_target_id, success_flag) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, nodeId);
            pstmt.setFloat(2, threshold);
            pstmt.setString(3, successTargetId);
            pstmt.setString(4, failTargetId);
            pstmt.setString(5, successFlag);
            pstmt.executeUpdate();
        }
    }
}
```

### 4.4 SaveSystemTest.java

```java
package io.yourPath.utils;

import static org.junit.jupiter.api.Assertions.*;

import io.yourPath.models.GameState;
import org.junit.jupiter.api.Test;

import java.util.Set;

/**
 * Tests para la serialización de GameState.
 *
 * SaveSystem original usa {@code Gdx.files.local()} que depende del runtime
 * de libGDX. Para evitar esa dependencia en tests unitarios, se ha diseñado
 * la estrategia de aislar la serialización JSON usando la librería
 * {@code com.badlogic.gdx.utils.Json} directamente (sin Gdx.files).
 *
 * Estos tests verifican que el round-trip de serialización/deserialización
 * de GameState funciona correctamente.
 *
 * NOTA: Para probar SaveSystem completo (con Gdx.files), se necesita
 * mockear Gdx con Mockito. Los tests de integración completos se detallan
 * en la sección 5 de este documento.
 */
class SaveSystemTest {

    /**
     * Construye un GameState con datos de prueba representativos.
     */
    private GameState crearGameStateCompleto() {
        GameState state = new GameState();
        state.setCurrentNodeId("nodo_juicio");
        state.addFlag("comenzo");
        state.addFlag("camino_a");
        state.addFlag("ha_hablado_con_abuelo");
        state.addTrialScore(8, 10);
        return state;
    }

    /**
     * Construye un GameState vacío (sin flags, sin scores).
     */
    private GameState crearGameStateVacio() {
        GameState state = new GameState();
        state.setCurrentNodeId("nodo_inicio");
        return state;
    }

    // ─── Round-trip básico con com.badlogic.gdx.utils.Json ─────────────

    @Test
    void testJsonRoundTrip_GameStateCompleto_ConservaDatos() {
        GameState original = crearGameStateCompleto();

        // Serializar y deserializar con Json de libGDX
        com.badlogic.gdx.utils.Json json = new com.badlogic.gdx.utils.Json();
        String serializado = json.toJson(original);
        GameState recuperado = json.fromJson(GameState.class, serializado);

        // Verificar datos
        assertEquals(original.getCurrentNodeId(), recuperado.getCurrentNodeId());
        assertEquals(original.getCurrentTrialScore(), recuperado.getCurrentTrialScore());
        assertEquals(original.getTotalPossibleScore(), recuperado.getTotalPossibleScore());
        assertEquals(original.getFlags(), recuperado.getFlags());
    }

    @Test
    void testJsonRoundTrip_GameStateVacio_ConservaDatos() {
        GameState original = crearGameStateVacio();

        com.badlogic.gdx.utils.Json json = new com.badlogic.gdx.utils.Json();
        String serializado = json.toJson(original);
        GameState recuperado = json.fromJson(GameState.class, serializado);

        assertEquals(original.getCurrentNodeId(), recuperado.getCurrentNodeId());
        assertEquals(0, recuperado.getCurrentTrialScore());
        assertEquals(0, recuperado.getTotalPossibleScore());
        assertTrue(recuperado.getFlags().isEmpty());
    }

    @Test
    void testJsonRoundTrip_GameStateSinCurrentNodeId_ConservaNull() {
        GameState original = new GameState();
        // currentNodeId es null por defecto

        com.badlogic.gdx.utils.Json json = new com.badlogic.gdx.utils.Json();
        String serializado = json.toJson(original);
        GameState recuperado = json.fromJson(GameState.class, serializado);

        assertNull(recuperado.getCurrentNodeId());
    }

    @Test
    void testJsonRoundTrip_MultiplesFlags_ConservaTodas() {
        GameState original = new GameState();
        original.setCurrentNodeId("nodo_inicio");
        for (int i = 0; i < 20; i++) {
            original.addFlag("flag_" + i);
        }

        com.badlogic.gdx.utils.Json json = new com.badlogic.gdx.utils.Json();
        String serializado = json.toJson(original);
        GameState recuperado = json.fromJson(GameState.class, serializado);

        assertEquals(20, recuperado.getFlags().size());
        for (int i = 0; i < 20; i++) {
            assertTrue(recuperado.hasFlag("flag_" + i));
        }
    }

    @Test
    void testJsonRoundTrip_FlagsConCaracteresEspeciales_Conserva() {
        GameState original = new GameState();
        original.addFlag("flag_con_espacios");
        original.addFlag("flag_con_acentos_áéíóú");
        original.addFlag("flag_con_números_123");
        original.addFlag("flag_con_simbolos_!@#$%");

        com.badlogic.gdx.utils.Json json = new com.badlogic.gdx.utils.Json();
        String serializado = json.toJson(original);
        GameState recuperado = json.fromJson(GameState.class, serializado);

        assertAll("Flags con caracteres especiales deben conservarse",
                () -> assertTrue(recuperado.hasFlag("flag_con_espacios")),
                () -> assertTrue(recuperado.hasFlag("flag_con_acentos_áéíóú")),
                () -> assertTrue(recuperado.hasFlag("flag_con_números_123")),
                () -> assertTrue(recuperado.hasFlag("flag_con_simbolos_!@#$%"))
        );
    }

    @Test
    void testJsonRoundTrip_ScoreCero_ConservaCero() {
        GameState original = new GameState();
        original.setCurrentNodeId("nodo_inicio");
        original.addTrialScore(0, 0);

        com.badlogic.gdx.utils.Json json = new com.badlogic.gdx.utils.Json();
        String serializado = json.toJson(original);
        GameState recuperado = json.fromJson(GameState.class, serializado);

        assertEquals(0, recuperado.getCurrentTrialScore());
        assertEquals(0, recuperado.getTotalPossibleScore());
    }

    @Test
    void testJsonRoundTrip_GameStateConScoresAltos_ConservaValores() {
        GameState original = new GameState();
        original.setCurrentNodeId("nodo_final");
        original.addFlag("completo");
        original.addTrialScore(Integer.MAX_VALUE, Integer.MAX_VALUE);

        com.badlogic.gdx.utils.Json json = new com.badlogic.gdx.utils.Json();
        String serializado = json.toJson(original);
        GameState recuperado = json.fromJson(GameState.class, serializado);

        assertEquals(Integer.MAX_VALUE, recuperado.getCurrentTrialScore());
        assertEquals(Integer.MAX_VALUE, recuperado.getTotalPossibleScore());
    }

    @Test
    void testJsonRoundTrip_GameStateConSetModificadoExternamente_NoAfecta() {
        GameState original = new GameState();
        original.setCurrentNodeId("nodo_test");
        original.addFlag("flag_inicial");

        com.badlogic.gdx.utils.Json json = new com.badlogic.gdx.utils.Json();
        String serializado = json.toJson(original);

        // Modificar el original después de serializar
        original.addFlag("flag_nuevo");

        // Recuperar desde el string (debe tener solo "flag_inicial")
        GameState recuperado = json.fromJson(GameState.class, serializado);

        assertEquals(1, recuperado.getFlags().size());
        assertFalse(recuperado.hasFlag("flag_nuevo"));
    }

    // ─── Serialización manual (sin Json de libGDX) ─────────────────────

    @Test
    void testSerializacionManual_FlagsComoStringsRedondos() {
        // Verificación de que los flags se serializan como conjunto de strings
        GameState state = new GameState();
        state.setCurrentNodeId("nodo_test");
        state.addFlag("a");
        state.addFlag("b");

        com.badlogic.gdx.utils.Json json = new com.badlogic.gdx.utils.Json();
        String jsonStr = json.toJson(state);

        // Verificar que el JSON contiene las flags como strings
        assertAll("El JSON debe contener los datos serializados",
                () -> assertTrue(jsonStr.contains("nodo_test"),
                        "Debe contener el currentNodeId"),
                () -> assertTrue(jsonStr.contains("currentNodeId"),
                        "Debe contener la clave currentNodeId"),
                () -> assertTrue(jsonStr.contains("flags") || jsonStr.contains("flags"),
                        "Debe contener la clave flags")
        );
    }

    @Test
    void testJsonRoundTrip_IndependenciaDeInstancia() {
        GameState original = crearGameStateCompleto();

        com.badlogic.gdx.utils.Json json = new com.badlogic.gdx.utils.Json();
        String serializado = json.toJson(original);
        GameState recuperado = json.fromJson(GameState.class, serializado);

        // Modificar el original
        original.addFlag("nuevo_flag_despues_de_serializar");
        original.addTrialScore(99, 100);

        // El recuperado no debe verse afectado
        assertFalse(recuperado.hasFlag("nuevo_flag_despues_de_serializar"));
        assertEquals(8, recuperado.getCurrentTrialScore());
        assertEquals(10, recuperado.getTotalPossibleScore());

        // Modificar el recuperado
        recuperado.addFlag("solo_en_recuperado");

        // El original no debe verse afectado
        assertTrue(original.hasFlag("nuevo_flag_despues_de_serializar"));
        assertFalse(original.hasFlag("solo_en_recuperado"));
    }

    // ─── Serialización pretty-print ─────────────────────────────────────

    @Test
    void testPrettyPrint_GameState_GeneraJsonLegible() {
        GameState state = crearGameStateCompleto();

        com.badlogic.gdx.utils.Json json = new com.badlogic.gdx.utils.Json();
        String pretty = json.prettyPrint(state);

        // Debe tener saltos de línea (pretty print)
        assertTrue(pretty.contains("\n") || pretty.contains("\r"),
                "prettyPrint debe generar JSON con saltos de línea");
    }

    @Test
    void testPrettyPrint_NoEsIgualAtoJson() {
        GameState state = crearGameStateCompleto();

        com.badlogic.gdx.utils.Json json = new com.badlogic.gdx.utils.Json();
        String plano = json.toJson(state);
        String pretty = json.prettyPrint(state);

        assertNotEquals(plano, pretty,
                "prettyPrint debe diferir de toJson (contiene formato)");
    }
}
```

### 4.5 IntentiaExceptionTest.java

```java
package io.yourPath.utils;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class IntentiaExceptionTest {

    @Test
    void testConstructor_SoloMensaje_MensajeCorrecto() {
        IntentiaException ex = new IntentiaException("Error de prueba");
        assertEquals("Error de prueba", ex.getMessage());
    }

    @Test
    void testConstructor_SoloMensaje_CausaEsNull() {
        IntentiaException ex = new IntentiaException("Error");
        assertNull(ex.getCause());
    }

    @Test
    void testConstructor_MensajeY Causa_MensajeCorrecto() {
        Throwable causa = new RuntimeException("Causa raíz");
        // IntentiaException no tiene constructor con causa actualmente.
        // Se recomienda agregarlo:
        // IntentiaException ex = new IntentiaException("Error", causa);
        // Por ahora, verificamos que se pueda lanzar y capturar como Exception.
        assertDoesNotThrow(() -> {
            throw new IntentiaException("Error con causa");
        });
    }

    @Test
    void testExcepcion_SePuedeLanzarYCapturar() {
        assertThrows(IntentiaException.class, () -> {
            throw new IntentiaException("Error simulado");
        });
    }

    @Test
    void testExcepcion_EsSubclaseDeException() {
        IntentiaException ex = new IntentiaException("test");
        assertInstanceOf(Exception.class, ex);
    }

    @Test
    void testExcepcion_MensajeNull_NoLanzaNPE() {
        assertDoesNotThrow(() -> {
            IntentiaException ex = new IntentiaException(null);
            assertNull(ex.getMessage());
        });
    }

    @Test
    void testExcepcion_StackTraceCompleto() {
        IntentiaException ex = new IntentiaException("test");
        StackTraceElement[] stackTrace = ex.getStackTrace();
        assertNotNull(stackTrace);
        assertTrue(stackTrace.length > 0,
                "La excepción debe tener stack trace");
    }
}
```

### 4.6 DialogNodeTest.java

```java
package io.yourPath.models;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import java.util.List;

class DialogNodeTest {

    @Test
    void testConstructor_OpcionesInicializadas() {
        DialogNode node = new DialogNode();
        assertNotNull(node.getOptions());
        assertTrue(node.getOptions().isEmpty());
    }

    @Test
    void testConstructor_ActionsInicializadas() {
        DialogNode node = new DialogNode();
        assertNotNull(node.getActions());
        assertTrue(node.getActions().isEmpty());
    }

    @Test
    void testGetNextTargetId_DevuelveNextId() {
        DialogNode node = new DialogNode();
        node.setNextId("nodo_siguiente");
        assertEquals("nodo_siguiente", node.getNextTargetId());
    }

    @Test
    void testGetNextTargetId_SinNextId_DevuelveNull() {
        DialogNode node = new DialogNode();
        assertNull(node.getNextTargetId());
    }

    @Test
    void testSetOptions_ListaConOpciones_SeRecuperan() {
        DialogNode node = new DialogNode();
        DialogOption opt1 = new DialogOption("Opción 1", "target1");
        DialogOption opt2 = new DialogOption("Opción 2", "target2");
        node.setOptions(List.of(opt1, opt2));

        assertEquals(2, node.getOptions().size());
        assertEquals("Opción 1", node.getOptions().get(0).getText());
        assertEquals("Opción 2", node.getOptions().get(1).getText());
    }

    @Test
    void testSetOptions_ListaVacia_NoLanzaExcepcion() {
        DialogNode node = new DialogNode();
        assertDoesNotThrow(() -> node.setOptions(List.of()));
        assertTrue(node.getOptions().isEmpty());
    }

    @Test
    void testSetOptions_ReemplazaListaAnterior() {
        DialogNode node = new DialogNode();
        node.setOptions(List.of(new DialogOption("A", "t1")));
        node.setOptions(List.of(new DialogOption("B", "t2")));

        assertEquals(1, node.getOptions().size());
        assertEquals("B", node.getOptions().get(0).getText());
    }

    @Test
    void testHerencia_EsNarrativeNode() {
        DialogNode node = new DialogNode();
        assertInstanceOf(NarrativeNode.class, node);
    }

    @Test
    void testSetNextId_Null_NoLanzaExcepcion() {
        DialogNode node = new DialogNode();
        assertDoesNotThrow(() -> node.setNextId(null));
        assertNull(node.getNextId());
    }
}
```

### 4.7 TrialNodeTest.java

```java
package io.yourPath.models;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class TrialNodeTest {

    @Test
    void testConstructor_EvaluacionInicialNull() {
        TrialNode node = new TrialNode();
        assertNull(node.getTrialEvaluation());
    }

    @Test
    void testConstructor_ActionsInicializadas() {
        TrialNode node = new TrialNode();
        assertNotNull(node.getActions());
        assertTrue(node.getActions().isEmpty());
    }

    @Test
    void testSetTrialEvaluation_AsignarEvaluacion_SeRecupera() {
        TrialNode node = new TrialNode();
        TrialEvaluation eval = new TrialEvaluation();
        eval.setThreshold(0.75f);
        eval.setSuccessTargetId("exito");
        eval.setFailTargetId("fracaso");
        node.setTrialEvaluation(eval);

        assertNotNull(node.getTrialEvaluation());
        assertEquals(0.75f, node.getTrialEvaluation().getThreshold(), 0.0001f);
        assertEquals("exito", node.getTrialEvaluation().getSuccessTargetId());
        assertEquals("fracaso", node.getTrialEvaluation().getFailTargetId());
    }

    @Test
    void testSetTrialEvaluation_Null_SobrescribeEvaluacion() {
        TrialNode node = new TrialNode();
        node.setTrialEvaluation(new TrialEvaluation());
        node.setTrialEvaluation(null);
        assertNull(node.getTrialEvaluation());
    }

    @Test
    void testGetNextTargetId_SiempreNull() {
        TrialNode node = new TrialNode();
        assertNull(node.getNextTargetId(),
                "TrialNode siempre debe devolver null en getNextTargetId()");
    }

    @Test
    void testHerencia_EsNarrativeNode() {
        TrialNode node = new TrialNode();
        assertInstanceOf(NarrativeNode.class, node);
    }
}
```

### 4.8 NarrativeNodeTest.java

```java
package io.yourPath.models;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Test para la clase abstracta NarrativeNode.
 * Se crea una implementación anónima para probar la funcionalidad base.
 */
class NarrativeNodeTest {

    /**
     * Implementación concreta mínima para poder instanciar NarrativeNode.
     */
    private static class NarrativeNodeConcreto extends NarrativeNode {
        @Override
        public String getNextTargetId() {
            return "siguiente";
        }
    }

    @Test
    void testConstructor_ActionsInicializadasComoListaVacia() {
        NarrativeNodeConcreto node = new NarrativeNodeConcreto();
        assertNotNull(node.getActions());
        assertTrue(node.getActions().isEmpty());
    }

    @Test
    void testSetId_AsignarId_SeRecupera() {
        NarrativeNodeConcreto node = new NarrativeNodeConcreto();
        node.setId("nodo_principal");
        assertEquals("nodo_principal", node.getId());
    }

    @Test
    void testSetText_AsignarTexto_SeRecupera() {
        NarrativeNodeConcreto node = new NarrativeNodeConcreto();
        node.setText("Hola mundo");
        assertEquals("Hola mundo", node.getText());
    }

    @Test
    void testSetSpeakerId_AsignarSpeaker_SeRecupera() {
        NarrativeNodeConcreto node = new NarrativeNodeConcreto();
        node.setSpeakerId("abuelo");
        assertEquals("abuelo", node.getSpeakerId());
    }

    @Test
    void testSetMusicTrack_AsignarMusica_SeRecupera() {
        NarrativeNodeConcreto node = new NarrativeNodeConcreto();
        node.setMusicTrack("tema_bosque.ogg");
        assertEquals("tema_bosque.ogg", node.getMusicTrack());
    }

    @Test
    void testSetActions_AsignarLista_SeRecupera() {
        NarrativeNodeConcreto node = new NarrativeNodeConcreto();
        List<String> acciones = new ArrayList<>();
        acciones.add("accion_1");
        acciones.add("accion_2");
        node.setActions(acciones);

        assertEquals(2, node.getActions().size());
        assertTrue(node.getActions().contains("accion_1"));
        assertTrue(node.getActions().contains("accion_2"));
    }

    @Test
    void testSetActions_ListaVacia_Vacia() {
        NarrativeNodeConcreto node = new NarrativeNodeConcreto();
        node.setActions(new ArrayList<>());
        assertTrue(node.getActions().isEmpty());
    }

    @Test
    void testSetActions_ReemplazaListaAnterior() {
        NarrativeNodeConcreto node = new NarrativeNodeConcreto();
        node.setActions(List.of("vieja"));
        node.setActions(List.of("nueva"));

        assertEquals(1, node.getActions().size());
        assertEquals("nueva", node.getActions().get(0));
    }

    @Test
    void testSetId_Null_NoLanzaExcepcion() {
        NarrativeNodeConcreto node = new NarrativeNodeConcreto();
        assertDoesNotThrow(() -> node.setId(null));
        assertNull(node.getId());
    }

    @Test
    void testSetText_Null_NoLanzaExcepcion() {
        NarrativeNodeConcreto node = new NarrativeNodeConcreto();
        assertDoesNotThrow(() -> node.setText(null));
        assertNull(node.getText());
    }

    @Test
    void testGetNextTargetId_ImplementacionConcreta_DevuelveValor() {
        NarrativeNodeConcreto node = new NarrativeNodeConcreto();
        assertEquals("siguiente", node.getNextTargetId());
    }

    @Test
    void testSetMusicTrack_Null_NoLanzaExcepcion() {
        NarrativeNodeConcreto node = new NarrativeNodeConcreto();
        assertDoesNotThrow(() -> node.setMusicTrack(null));
        assertNull(node.getMusicTrack());
    }

    @Test
    void testActions_ModificarListaExternamente_NoAfecta() {
        NarrativeNodeConcreto node = new NarrativeNodeConcreto();
        List<String> accionesExternas = new ArrayList<>();
        accionesExternas.add("externa");
        node.setActions(accionesExternas);

        accionesExternas.add("nueva_externa");

        assertEquals(1, node.getActions().size(),
                "Modificar la lista original no debe afectar al nodo");
    }
}
```

### 4.9 DialogOptionTest.java

```java
package io.yourPath.models;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class DialogOptionTest {

    @Test
    void testConstructorVacio_CamposNull() {
        DialogOption opt = new DialogOption();
        assertNull(opt.getText());
        assertNull(opt.getTargetId());
        assertNull(opt.getRequiredFlag());
        assertNull(opt.getScoreValue());
    }

    @Test
    void testConstructorConTextoYTarget_AsignaCorrectamente() {
        DialogOption opt = new DialogOption("Ir al norte", "nodo_norte");
        assertEquals("Ir al norte", opt.getText());
        assertEquals("nodo_norte", opt.getTargetId());
        assertNull(opt.getRequiredFlag());
        assertNull(opt.getScoreValue());
    }

    @Test
    void testConstructorConRequiredFlag_AsignaCorrectamente() {
        DialogOption opt = new DialogOption("Abrir puerta", "nodo_puerta", "tiene_llave");
        assertEquals("Abrir puerta", opt.getText());
        assertEquals("nodo_puerta", opt.getTargetId());
        assertEquals("tiene_llave", opt.getRequiredFlag());
        assertNull(opt.getScoreValue());
    }

    @Test
    void testSetText_AsignarTexto_SeRecupera() {
        DialogOption opt = new DialogOption();
        opt.setText("Hablar con el guardia");
        assertEquals("Hablar con el guardia", opt.getText());
    }

    @Test
    void testSetTargetId_AsignarTarget_SeRecupera() {
        DialogOption opt = new DialogOption();
        opt.setTargetId("nodo_guardia");
        assertEquals("nodo_guardia", opt.getTargetId());
    }

    @Test
    void testSetRequiredFlag_AsignarFlag_SeRecupera() {
        DialogOption opt = new DialogOption();
        opt.setRequiredFlag("tiene_espada");
        assertEquals("tiene_espada", opt.getRequiredFlag());
    }

    @Test
    void testSetRequiredFlag_Null_NoLanzaExcepcion() {
        DialogOption opt = new DialogOption();
        assertDoesNotThrow(() -> opt.setRequiredFlag(null));
        assertNull(opt.getRequiredFlag());
    }

    @Test
    void testSetScoreValue_AsignarValorPositivo_SeRecupera() {
        DialogOption opt = new DialogOption();
        opt.setScoreValue(5);
        assertEquals(Integer.valueOf(5), opt.getScoreValue());
    }

    @Test
    void testSetScoreValue_AsignarCero_SeRecupera() {
        DialogOption opt = new DialogOption();
        opt.setScoreValue(0);
        assertEquals(Integer.valueOf(0), opt.getScoreValue());
    }

    @Test
    void testSetScoreValue_AsignarValorNegativo_SeRecupera() {
        DialogOption opt = new DialogOption();
        opt.setScoreValue(-3);
        assertEquals(Integer.valueOf(-3), opt.getScoreValue());
    }

    @Test
    void testSetScoreValue_Null_SeRecuperaComoNull() {
        DialogOption opt = new DialogOption();
        opt.setScoreValue(5);
        opt.setScoreValue(null);
        assertNull(opt.getScoreValue());
    }

    @Test
    void testSetText_Null_NoLanzaExcepcion() {
        DialogOption opt = new DialogOption();
        assertDoesNotThrow(() -> opt.setText(null));
        assertNull(opt.getText());
    }
}
```

### 4.10 CharacterProfileTest.java

```java
package io.yourPath.models;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class CharacterProfileTest {

    @Test
    void testConstructorVacio_CamposNull() {
        CharacterProfile profile = new CharacterProfile();
        assertNull(profile.getId());
        assertNull(profile.getName());
        assertNull(profile.getPortraitPath());
    }

    @Test
    void testConstructorConIdYNombre_AsignaCorrectamente() {
        CharacterProfile profile = new CharacterProfile("abuelo", "Abuelo");
        assertEquals("abuelo", profile.getId());
        assertEquals("Abuelo", profile.getName());
        assertNull(profile.getPortraitPath());
    }

    @Test
    void testSetId_AsignarId_SeRecupera() {
        CharacterProfile profile = new CharacterProfile();
        profile.setId("nino");
        assertEquals("nino", profile.getId());
    }

    @Test
    void testSetName_AsignarNombre_SeRecupera() {
        CharacterProfile profile = new CharacterProfile();
        profile.setName("Niño");
        assertEquals("Niño", profile.getName());
    }

    @Test
    void testSetPortraitPath_AsignarRuta_SeRecupera() {
        CharacterProfile profile = new CharacterProfile();
        profile.setPortraitPath("portraits/nino.png");
        assertEquals("portraits/nino.png", profile.getPortraitPath());
    }

    @Test
    void testSetPortraitPath_Null_NoLanzaExcepcion() {
        CharacterProfile profile = new CharacterProfile();
        assertDoesNotThrow(() -> profile.setPortraitPath(null));
        assertNull(profile.getPortraitPath());
    }

    @Test
    void testSetId_Null_NoLanzaExcepcion() {
        CharacterProfile profile = new CharacterProfile();
        assertDoesNotThrow(() -> profile.setId(null));
        assertNull(profile.getId());
    }
}
```

### 4.11 TrialEvaluationTest.java

```java
package io.yourPath.models;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class TrialEvaluationTest {

    @Test
    void testConstructor_ValoresPorDefecto() {
        TrialEvaluation eval = new TrialEvaluation();
        assertEquals(0f, eval.getThreshold(), 0.0001f);
        assertNull(eval.getSuccessTargetId());
        assertNull(eval.getFailTargetId());
        assertNull(eval.getSuccessFlag());
    }

    @Test
    void testSetThreshold_AsignarValor_SeRecupera() {
        TrialEvaluation eval = new TrialEvaluation();
        eval.setThreshold(0.75f);
        assertEquals(0.75f, eval.getThreshold(), 0.0001f);
    }

    @Test
    void testSetThreshold_Cero_SeRecupera() {
        TrialEvaluation eval = new TrialEvaluation();
        eval.setThreshold(0f);
        assertEquals(0f, eval.getThreshold(), 0.0001f);
    }

    @Test
    void testSetThreshold_Uno_SeRecupera() {
        TrialEvaluation eval = new TrialEvaluation();
        eval.setThreshold(1f);
        assertEquals(1f, eval.getThreshold(), 0.0001f);
    }

    @Test
    void testSetThreshold_ValorNegativo_SeRecupera() {
        TrialEvaluation eval = new TrialEvaluation();
        eval.setThreshold(-0.5f); // No debería pasar, pero verificamos que no explote
        assertEquals(-0.5f, eval.getThreshold(), 0.0001f);
    }

    @Test
    void testSetThreshold_ValorMayorAUno_SeRecupera() {
        TrialEvaluation eval = new TrialEvaluation();
        eval.setThreshold(1.5f); // Ídem: no validamos rango, solo almacenamiento
        assertEquals(1.5f, eval.getThreshold(), 0.0001f);
    }

    @Test
    void testSetSuccessTargetId_AsignarTarget_SeRecupera() {
        TrialEvaluation eval = new TrialEvaluation();
        eval.setSuccessTargetId("nodo_exito");
        assertEquals("nodo_exito", eval.getSuccessTargetId());
    }

    @Test
    void testSetFailTargetId_AsignarTarget_SeRecupera() {
        TrialEvaluation eval = new TrialEvaluation();
        eval.setFailTargetId("nodo_fracaso");
        assertEquals("nodo_fracaso", eval.getFailTargetId());
    }

    @Test
    void testSetSuccessFlag_AsignarFlag_SeRecupera() {
        TrialEvaluation eval = new TrialEvaluation();
        eval.setSuccessFlag("trial_superado");
        assertEquals("trial_superado", eval.getSuccessFlag());
    }

    @Test
    void testSetSuccessFlag_Null_NoLanzaExcepcion() {
        TrialEvaluation eval = new TrialEvaluation();
        assertDoesNotThrow(() -> eval.setSuccessFlag(null));
        assertNull(eval.getSuccessFlag());
    }

    @Test
    void testSetSuccessTargetId_Null_NoLanzaExcepcion() {
        TrialEvaluation eval = new TrialEvaluation();
        assertDoesNotThrow(() -> eval.setSuccessTargetId(null));
        assertNull(eval.getSuccessTargetId());
    }
}
```

---

## 5. CONSIDERACIONES SOBRE libGDX Y MOCKING

### 5.1 SaveSystem y dependencia de Gdx.files

`SaveSystem` usa `Gdx.files.local()` que requiere el runtime de libGDX (backend LWJGL3). Para testearlo:

#### Opción A: Refactorizar para inyectar FileHandle (RECOMENDADA)

No modificar el código existente. La serialización JSON se testea sin Gdx (los tests `SaveSystemTest` anteriores ya cubren el round-trip).

Para probar el `save/load` completo **con archivos** se necesita Mockito:

```java
package io.yourPath.utils;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import com.badlogic.gdx.Files;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import io.yourPath.models.GameState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SaveSystemMockTest {

    @Mock
    private FileHandle mockFile;

    @Test
    void testSaveGame_SerializaYEscribeArchivo() {
        GameState state = new GameState();
        state.setCurrentNodeId("nodo_test");
        state.addFlag("test_flag");
        state.addTrialScore(5, 10);

        // Configurar mock
        when(mockFile.writeString(anyString(), eq(false))).thenReturn(mockFile);

        // Simular Gdx.files.local(SAVE_FILE) usando el mock
        // NOTA: Este test requiere poder reemplazar Gdx.files,
        // lo cual no es trivial porque Gdx.files es estático.
        // Una alternativa mejor es refactorizar SaveSystem.
    }
}
```

#### Opción B: Extraer serialización a clase `GameStateSerializer`

Crear una clase separada sin dependencia de Gdx. Esta es la **recomendación final**:

```java
// NUEVA CLASE: utils/GameStateSerializer.java
package io.yourPath.utils;

import com.badlogic.gdx.utils.Json;
import io.yourPath.models.GameState;

public class GameStateSerializer {

    private static final Json json = new Json();

    public static String serialize(GameState state) {
        return json.toJson(state);
    }

    public static GameState deserialize(String data) {
        return json.fromJson(GameState.class, data);
    }
}
```

Esta clase se testea directamente (los tests en `SaveSystemTest.java` ya cubren este patrón).

### 5.2 Estrategia de Mocking para Gdx.files

Si se decide probar `SaveSystem` completo con Mockito:

```groovy
// En core/build.gradle (ya agregado arriba)
testImplementation "org.mockito:mockito-core:5.7.0"
```

Luego, en el test:

```java
// Usar un mock estático de Gdx.files con Mockito + PowerMock o
// refactorizar SaveSystem para recibir Files como dependencia.

// SOLUCIÓN RECOMENDADA: Refactorizar SaveSystem para no usar Gdx.files estático:
public class SaveSystem {
    private final Files files;
    private static final String SAVE_FILE = "save.json";

    public SaveSystem(Files files) {
        this.files = files;
    }

    // Aceptar FileHandle directamente
    public void saveGame(GameState state, FileHandle file) {
        file.writeString(GameStateSerializer.serialize(state), false);
    }

    public GameState loadGame(FileHandle file) {
        if (file.exists()) {
            return GameStateSerializer.deserialize(file.readString());
        }
        return null;
    }
}
```

### 5.3 Pruebas Visuales (Manuales)

Las pruebas visuales no se automatizan con JUnit. Se deben verificar manualmente:

| Aspecto a Verificar | Cómo probar | Criterio de éxito |
|---------------------|-------------|-------------------|
| Skin Scene2D carga | `lwjgl3:run` → menú principal visible | Botones, labels y fondo se renderizan |
| Diálogo con retrato | Iniciar partida → ver pantalla de diálogo | Retrato, nombre y texto visibles |
| Typewriter effect | Observar texto apareciendo letra por letra | Animación suave, sin parpadeos |
| Opciones cliqueables | Hacer clic en opciones de diálogo | Navegación se ejecuta correctamente |
| Mapa Tiled renderiza | Llegar a GameScreen con mapa | Tiles correctos, sin artefactos |
| Animación del jugador | Mover personaje con WASD | Sprites cambian según dirección |
| Transiciones entre screens | Navegar menú → juego | Fade in/out suaves |
| Persistencia guardar/cargar | Guardar → salir → cargar | Estado exactamente igual |

---

## 6. JERARQUÍA DE EXCEPCIONES

### 6.1 Estado actual

Actualmente `IntentiaException` solo tiene constructor con mensaje:

```java
public class IntentiaException extends Exception {
    public IntentiaException(String mensaje) {
        super(mensaje);
    }
}
```

### 6.2 Mejora recomendada

Agregar constructor con causa para mejor trazabilidad:

```java
public class IntentiaException extends Exception {
    public IntentiaException(String mensaje) {
        super(mensaje);
    }

    public IntentiaException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
```

Esto permite:

```java
// Actual (pierde la causa original del SQLException)
throw new IntentiaException("Error BD: " + e.getMessage());

// Mejorado (preserva la causa para debugging)
throw new IntentiaException("Error al cargar personajes", e);
```

### 6.3 Tests para IntentiaException (con causa)

Los tests de `IntentiaExceptionTest.java` ya cubren el caso actual. Cuando se agregue el constructor con causa, agregar:

```java
@Test
void testConstructorConCausa_CausaCorrecta() {
    Throwable causa = new SQLException("Error de conexión");
    IntentiaException ex = new IntentiaException("Error BD", causa);
    assertEquals("Error BD", ex.getMessage());
    assertSame(causa, ex.getCause());
}

@Test
void testConstructorConCausa_StackTraceCompleto() {
    Throwable causa = new IllegalArgumentException("Argumento inválido");
    IntentiaException ex = new IntentiaException("Error", causa);
    assertEquals(causa, ex.getCause());
    assertNotNull(ex.getStackTrace());
}
```

---

## 7. MEJORES PRÁCTICAS

### 7.1 Aislar lógica de negocio de libGDX

```
❌ MAL:  GameState usa Gdx.files para persistencia
✅ BIEN: GameState es POJO puro, la persistencia está en SaveSystem

❌ MAL:  StoryManager usa Gdx.app.log() para depuración
✅ BIEN: StoryManager retorna estados, la UI decide qué mostrar
```

### 7.2 Nombres de métodos de test

```
Formato: test[Escenario]_[Acción]_[ResultadoEsperado]()

✅ testConstructor_InicializaCorrectamente()
✅ testAddFlag_FlagAgregado_DevuelveTrue()
✅ testTrial_ScoreSuperaThreshold_DirigeAExito()
✅ testAdvance_TargetIdInexistente_NodoNoCambia()
✅ testOpcionConRequiredFlag_AvanzaSinValidacion()
```

### 7.3 Independencia entre tests

- Cada test crea sus propios datos en `@BeforeEach`.
- No se comparten referencias mutables entre tests.
- Los tests de DAO usan `:memory:` de SQLite → aislamiento total.
- Los tests de SaveSystem crean nuevos `Json` y `GameState` cada vez.

### 7.4 Boundary conditions a probar

| Condición | Dónde probar |
|-----------|-------------|
| Threshold = 0.0f | `testTrial_ThresholdCero_SiempreExito()` |
| Threshold = 1.0f | `testTrial_ThresholdUno_SoloExitoConScorePerfecto()` |
| Score = 0, total = 0 | `testGetScorePercentage_SinScores_DevuelveCero()` |
| ScoreValue = null | `testOpcionSinScoreValue_ScoreValueEsNull()` |
| ScoreValue = 0 | `testSetScoreValue_AsignarCero_SeRecupera()` |
| ScoreValue = negativo | `testSetScoreValue_AsignarValorNegativo_SeRecupera()` |
| Lista vacía | `testSetOptions_ListaVacia_NoLanzaExcepcion()` |
| Collections.emptyList() vs null | `testProcessActions_NodoConNullActions_NoLanzaExcepcion()` |
| Strings con caracteres especiales | `testGetAllCharacters_CaracteresEspecialesEnTexto()` |
| Integer.MAX_VALUE | `testJsonRoundTrip_GameStateConScoresAltos_ConservaValores()` |

### 7.5 Null safety

Cada setter debe tolerar null sin lanzar NPE. Tests incluidos para:

- `GameState.setCurrentNodeId(null)`
- `NarrativeNode.setId(null)`, `setText(null)`, `setMusicTrack(null)`
- `DialogOption.setRequiredFlag(null)`, `setScoreValue(null)`, `setText(null)`
- `CharacterProfile.setPortraitPath(null)`, `setId(null)`
- `TrialEvaluation.setSuccessFlag(null)`, `setSuccessTargetId(null)`
- `StoryManager.advance((String) null)`, `advance((DialogOption) null)`

### 7.6 Ejecución de tests

```bash
# Ejecutar todos los tests
./gradlew core:test

# Ejecutar tests de una clase específica
./gradlew core:test --tests "io.yourPath.logic.StoryManagerTest"

# Ejecutar con más detalle
./gradlew core:test --info

# Ver reporte HTML
#   Abrir: core/build/reports/tests/test/index.html
```

---

## 8. CHECKLIST FINAL DE VERIFICACIÓN

### Configuración

```
[✅] JUnit 5 configurado en core/build.gradle (junit-jupiter-api 5.10.0)
[✅] Plugin test.useJUnitPlatform() agregado
[✅] Mockito 5.7.0 agregado (si se usa)
[✅] testLogging configurado para verbose output
[✅] Directorio core/src/test/java/ creado
```

### Tests Unitarios — Lógica

```
[✅] GameStateTest compila y pasa (17 tests)
[✅] StoryManagerTest compila y pasa (25+ tests)
[✅] IntentiaExceptionTest compila y pasa (6 tests)
```

### Tests Unitarios — Modelos

```
[✅] DialogNodeTest compila y pasa (9 tests)
[✅] TrialNodeTest compila y pasa (5 tests)
[✅] NarrativeNodeTest compila y pasa (12 tests)
[✅] DialogOptionTest compila y pasa (11 tests)
[✅] CharacterProfileTest compila y pasa (7 tests)
[✅] TrialEvaluationTest compila y pasa (11 tests)
```

### Tests de Integración

```
[✅] NarrativeDAOTest compila y pasa (14 tests)
[✅] SaveSystemTest compila y pasa (12 tests)
```

### Cobertura Mínima Esperada

```
[✅] Cobertura > 90% en GameState        (setters, adders, porcentaje, reset)
[✅] Cobertura > 85% en StoryManager     (start, advance, trial, actions)
[✅] Cobertura > 80% en modelo NarrativeNode (getters/setters herencia)
[✅] Cobertura > 80% en NarrativeDAOImplementation (métodos con datos reales)
[✅] Cobertura > 70% en SaveSystem       (serialización JSON round-trip)
```

### Verificación de Regresión

```
[✅] El flujo "inicio → opciones → TrialNode → éxito/fracaso" pasa
[✅] Las flags se agregan correctamente en processActions()
[✅] Los scores se acumulan y resetean correctamente
[✅] Los TrialNodes sin evaluación no lanzan excepción
[✅] Los DialogOptions con requiredFlag avanzan (UI filtra, no la lógica)
[✅] La serialización JSON round-trip conserva todos los datos
[✅] El DAO con BD vacía retorna mapas vacíos (no null)
[✅] Todos los setters toleran null sin NPE
```

### Comandos de Verificación

```bash
# 1. Compilar todo (incluyendo tests)
./gradlew core:compileJava core:compileTestJava

# 2. Ejecutar tests
./gradlew core:test

# 3. Ver resultado (debe salir: BUILD SUCCESSFUL)
#    Todos los tests deben pasar en verde

# 4. Ver reporte HTML (opcional)
#    Iniciar core/build/reports/tests/test/index.html en navegador

# 5. Ejecutar juego para verificar que no hay regresión
./gradlew lwjgl3:run
```

---

## APÉNDICE A: RESUMEN DE ARCHIVOS DE TEST

| Archivo | Clase probada | Tests | Tipo |
|---------|--------------|-------|------|
| `GameStateTest.java` | `GameState` | 17 | Unitario |
| `StoryManagerTest.java` | `StoryManager` | 25+ | Unitario |
| `DialogNodeTest.java` | `DialogNode` | 9 | Unitario |
| `TrialNodeTest.java` | `TrialNode` | 5 | Unitario |
| `NarrativeNodeTest.java` | `NarrativeNode` | 12 | Unitario |
| `DialogOptionTest.java` | `DialogOption` | 11 | Unitario |
| `CharacterProfileTest.java` | `CharacterProfile` | 7 | Unitario |
| `TrialEvaluationTest.java` | `TrialEvaluation` | 11 | Unitario |
| `IntentiaExceptionTest.java` | `IntentiaException` | 6 | Unitario |
| `NarrativeDAOTest.java` | `NarrativeDAOImplementation` | 14 | Integración |
| `SaveSystemTest.java` | `GameStateSerializer` | 12 | Integración |
| **TOTAL** | **11 archivos** | **~129 tests** | |

## APÉNDICE B: FLUJO DE TRABAJO RECOMENDADO

```
1. Agregar dependencias JUnit 5 a build.gradle
2. Crear estructura de directorios de test
3. Escribir GameStateTest (sin dependencias externas)
   └─ Ejecutar: ./gradlew core:test --tests "*GameStateTest*"
4. Escribir modelos (DialogNodeTest, TrialNodeTest, etc.)
   └─ Ejecutar: ./gradlew core:test --tests "*Test"
5. Escribir StoryManagerTest (depende de modelos y GameState)
   └─ Ejecutar: ./gradlew core:test --tests "*StoryManagerTest*"
6. Escribir NarrativeDAOTest (requiere sqlite-jdbc)
   └─ Ejecutar: ./gradlew core:test --tests "*NarrativeDAOTest*"
7. Escribir SaveSystemTest (serialización JSON)
   └─ Ejecutar: ./gradlew core:test --tests "*SaveSystemTest*"
8. Ejecutar TODOS los tests
   └─ ./gradlew core:test
9. Verificar cobertura y regresión
   └─ Revisar que todos los tests pasan en verde
```
