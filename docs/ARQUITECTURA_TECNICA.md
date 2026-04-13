# Especificacion Tecnica: Proyecto Intentia

Documentacion de arquitectura del motor narrativo y sistema de estados.

---

## 1. Arquitectura de Capas
Separacion de responsabilidades entre datos, procesamiento y vista.

```mermaid
graph LR
    subgraph Disco
        A[historia.json]
        S[save.json]
    end
    subgraph RAM_Java
        M[StoryManager]
        G[GameState]
    end
    subgraph Interfaz
        T[Terminal / CLI]
        L[LibGDX Screen]
    end
    A --> M
    M <--> G
    G --> S
    M --> T
    M --> L
```

---

## 2. Estructura de Clases (Modelos y Logica)
Definicion de las entidades principales y su relacion.

```mermaid
classDiagram
    class NodoDialogo {
        +String id
        +String texto
        +List options
        +List actions
    }
    class Opcion {
        +String texto
        +String idDestino
        +String requisito
    }
    class GameState {
        +Set flags
        +String nodoActualId
    }
    class StoryManager {
        +Map nodos
        +avanzar(int)
    }
    NodoDialogo *-- Opcion
    StoryManager o-- GameState
```

---

## 3. Ciclo de Ejecucion del Turno
Flujo de interaccion desde el input hasta la actualizacion de pantalla.

```mermaid
sequenceDiagram
    participant U as Usuario
    participant M as StoryManager
    participant G as GameState
    M->>G: Consultar flags/requisitos
    M->>U: Imprimir texto y opciones filtradas
    U->>M: Enviar opcion seleccionada
    M->>G: Ejecutar acciones y actualizar estado
    M->>M: Cambiar a nodoActualId
```

---

## 4. Gestion de Estados de Interfaz
Control de entrada mediante el patron State (Enum).

```mermaid
stateDiagram-v2
    direction LR
    [*] --> DIALOGANDO: Inicio partida
    DIALOGANDO --> MENU: Pulsar 'M' (Pausa)
    MENU --> DIALOGANDO: Elegir 'Volver'
    DIALOGANDO --> EXPLORANDO: Fin de charla
    EXPLORANDO --> DIALOGANDO: Trigger proximidad
```

---

## 5. Especificacion de Persistencia
Estandar de guardado y carga mediante serealizacion JSON.

```mermaid
graph TD
    A[GameState en RAM] -->|Json.toJson| B[save.json]
    B -->|Json.fromJson| C[Nuevo GameState]
    C -->|Set nodoActualId| D[StoryManager]
```

---

## 6. Formato de Datos (JSON)
Ejemplo de estructura para historia.json.

```json
[
  {
    "id": "inicio",
    "texto": "Dialogo principal...",
    "opciones": [
      { "texto": "Ir al bosque", "idDestino": "bosque", "requisito": "tiene_brujula" }
    ],
    "actions": ["set_flag:aventura_iniciada"]
  }
]
```

---

## 7. Estandares de Calidad y Rubrica

### Control de Excepciones (Robustez)
- Se define una jerarquia de excepciones (ej: `JuegoException` > `NodoNotFoundException`).
- El motor utiliza bloques `try-catch` especificos para la carga de archivos JSON.
- Se evita el "swallowing" de excepciones, informando siempre al usuario/log.

### Optimizacion de Cadenas (StringBuilder)
- La construccion de menus y pantallas de texto en la terminal se realiza mediante `StringBuilder` para optimizar el uso de memoria en lugar de concatenaciones simples.

### Encapsulamiento y Visibilidad
- Todos los atributos de clase son `private`.
- Se proporcionan metodos `Getter` publicos solo para los campos necesarios.
- Solo se definen `Setters` para campos mutables durante la ejecucion (fomentando la inmutabilidad).

### Gestion de Recursos (IO/NIO)
- La lectura de archivos utiliza el patron `try-with-resources` para garantizar el cierre automatico de flujos de datos.
- Se utiliza la API interna de LibGDX (`Gdx.files`) para una gestion de rutas eficiente.

### Clean Code y Convenciones
- Nomenclatura: `PascalCase` para clases, `camelCase` para metodos y variables.
- Documentacion: Uso de `JavaDoc` en todas las clases y metodos publicos.
