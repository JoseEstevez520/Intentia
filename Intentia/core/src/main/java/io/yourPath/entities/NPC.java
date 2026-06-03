package io.yourPath.entities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

public class NPC {
    public enum MovementType {
        STATIC, PATROL_H, PATROL_V, WANDER
    }

    public enum State {
        IDLE, WALKING, TALKING
    }

    private static final int FRAME_WIDTH = 32;
    private static final int FRAME_HEIGHT = 48;
    private static final float IDLE_PAUSE = 2f;
    private static final float WALK_DURATION_MIN = 1f;
    private static final float WALK_DURATION_MAX = 3f;
    private static final float WANDER_PAUSE_MIN = 2f;
    private static final float WANDER_PAUSE_MAX = 4f;

    private Vector2 position;
    private float originX;
    private float originY;
    private Direction direction;
    private MovementType movementType;
    private State state;
    private String npcId;
    private String dialogNodeId;
    private float speed;
    private float patrolDistance;
    private float interactionRadius;
    private float wanderMinX = 32;
    private float wanderMaxX = 600;
    private float wanderMinY = 32;
    private float wanderMaxY = 600;

    private float stateTime;
    private float pauseTimer;
    private float walkTimer;
    private boolean movingRight;
    private boolean movingUp;

    private Texture placeholderTexture;
    private Texture walkSheetTexture;
    private Texture idleSheetTexture;
    private TextureRegion currentFrame;
    private Color placeholderColor;

    private Animation<TextureRegion> animCaminarAbajo;
    private Animation<TextureRegion> animCaminarArriba;
    private Animation<TextureRegion> animCaminarIzquierda;
    private Animation<TextureRegion> animCaminarDerecha;
    private TextureRegion reposoAbajo;
    private TextureRegion reposoArriba;
    private TextureRegion reposoIzquierda;
    private TextureRegion reposoDerecha;

    private boolean usandoSpritesReales;
    private float tiempoAnimacion;

    public NPC(String npcId, String dialogNodeId, float x, float y, MovementType movementType, float speed, float patrolDistance, Color color) {
        this(npcId, dialogNodeId, x, y, movementType, speed, patrolDistance, null, null);
        this.placeholderColor = color;
        if (!usandoSpritesReales) {
            crearPlaceholder(placeholderColor);
            currentFrame = new TextureRegion(placeholderTexture);
        }
    }

    public NPC(String npcId, String dialogNodeId, float x, float y, MovementType movementType, float speed, float patrolDistance, String rutaWalk, String rutaIdle) {
        this.npcId = npcId;
        this.dialogNodeId = dialogNodeId;
        this.position = new Vector2(x, y);
        this.originX = x;
        this.originY = y;
        this.movementType = movementType;
        this.speed = speed;
        this.patrolDistance = patrolDistance * 16;
        this.interactionRadius = 48f;
        this.direction = Direction.ABAJO;
        this.state = State.IDLE;
        this.stateTime = 0;
        this.pauseTimer = IDLE_PAUSE;
        this.movingRight = true;
        this.movingUp = true;
        this.usandoSpritesReales = false;
        this.tiempoAnimacion = 0;

        if (rutaWalk != null && Gdx.files.internal(rutaWalk).exists()) {
            try {
                walkSheetTexture = new Texture(Gdx.files.internal(rutaWalk));
                walkSheetTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
                TextureRegion[][] walkFrames = TextureRegion.split(walkSheetTexture, FRAME_WIDTH, FRAME_HEIGHT);

                if (walkFrames.length > 0) animCaminarAbajo = crearCaminar(walkFrames[0]);
                if (walkFrames.length > 1) animCaminarArriba = crearCaminar(walkFrames[1]);
                if (walkFrames.length > 2) animCaminarIzquierda = crearCaminar(walkFrames[2]);
                if (walkFrames.length > 3) animCaminarDerecha = crearCaminar(walkFrames[3]);

                TextureRegion[][] idleFrames;
                if (rutaIdle != null && Gdx.files.internal(rutaIdle).exists()) {
                    idleSheetTexture = new Texture(Gdx.files.internal(rutaIdle));
                    idleSheetTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
                    idleFrames = TextureRegion.split(idleSheetTexture, FRAME_WIDTH, FRAME_HEIGHT);
                } else {
                    idleFrames = walkFrames;
                }
                reposoAbajo = idleFrames.length > 0 && idleFrames[0].length > 0 ? idleFrames[0][0] : null;
                reposoArriba = idleFrames.length > 1 && idleFrames[1].length > 0 ? idleFrames[1][0] : null;
                reposoIzquierda = idleFrames.length > 2 && idleFrames[2].length > 0 ? idleFrames[2][0] : null;
                reposoDerecha = idleFrames.length > 3 && idleFrames[3].length > 0 ? idleFrames[3][0] : null;

                currentFrame = reposoAbajo != null ? reposoAbajo : new TextureRegion(walkSheetTexture);
                usandoSpritesReales = true;
            } catch (Exception e) {
                Gdx.app.log("NPC", "No se pudo cargar sprite para " + npcId + ": " + e.getMessage());
                if (walkSheetTexture != null) { walkSheetTexture.dispose(); walkSheetTexture = null; }
                if (idleSheetTexture != null) { idleSheetTexture.dispose(); idleSheetTexture = null; }
            }
        }

        if (!usandoSpritesReales) {
            if (Gdx.files.internal("sprites/player.png").exists()) {
                try {
                    walkSheetTexture = new Texture(Gdx.files.internal("sprites/player.png"));
                    walkSheetTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
                    TextureRegion[][] frames = TextureRegion.split(walkSheetTexture, FRAME_WIDTH, FRAME_HEIGHT);
                    if (frames.length > 0 && frames[0].length > 0) {
                        animCaminarAbajo = crearCaminar(frames[0]);
                        reposoAbajo = frames[0][0];
                        currentFrame = reposoAbajo;
                        usandoSpritesReales = true;
                    }
                    if (frames.length > 1) {
                        animCaminarArriba = crearCaminar(frames[1]);
                        reposoArriba = frames[1][0];
                    }
                    if (frames.length > 2) {
                        animCaminarIzquierda = crearCaminar(frames[2]);
                        reposoIzquierda = frames[2][0];
                    }
                    if (frames.length > 3) {
                        animCaminarDerecha = crearCaminar(frames[3]);
                        reposoDerecha = frames[3][0];
                    }
                } catch (Exception e) {
                    Gdx.app.log("NPC", "No se pudo cargar player.png: " + e.getMessage());
                    if (walkSheetTexture != null) { walkSheetTexture.dispose(); walkSheetTexture = null; }
                }
            }
            if (!usandoSpritesReales) {
                crearPlaceholder(Color.WHITE);
                currentFrame = new TextureRegion(placeholderTexture);
            }
        }
    }

    private Animation<TextureRegion> crearCaminar(TextureRegion[] fila) {
        Array<TextureRegion> cuadros = new Array<>();
        if (fila.length > 0 && fila[0] != null) cuadros.add(fila[0]);
        if (fila.length > 1 && fila[1] != null) cuadros.add(fila[1]);
        return new Animation<>(0.15f, cuadros, Animation.PlayMode.LOOP);
    }

    private void crearPlaceholder(Color color) {
        Pixmap pixmap = new Pixmap(FRAME_WIDTH, FRAME_HEIGHT, Pixmap.Format.RGBA8888);
        pixmap.setColor(color);
        pixmap.fillRectangle(4, 0, FRAME_WIDTH - 8, FRAME_HEIGHT);
        Color oscuro = new Color(color);
        oscuro.mul(0.7f);
        pixmap.setColor(oscuro);
        pixmap.fillCircle(FRAME_WIDTH / 2, FRAME_HEIGHT - 8, 8);
        placeholderTexture = new Texture(pixmap);
        pixmap.dispose();
    }

    public void update(float delta) {
        if (state == State.TALKING) return;

        stateTime += delta;

        switch (movementType) {
            case STATIC:
                actualizarStatic(delta);
                break;
            case PATROL_H:
                actualizarPatrolH(delta);
                break;
            case PATROL_V:
                actualizarPatrolV(delta);
                break;
            case WANDER:
                actualizarWander(delta);
                break;
        }

        if (moviendo()) tiempoAnimacion += delta;
        else tiempoAnimacion = 0;

        actualizarFrame();
    }

    private boolean moviendo() {
        return state == State.WALKING;
    }

    private void actualizarFrame() {
        if (!usandoSpritesReales || currentFrame == null) return;

        if (state == State.WALKING) {
            Animation<TextureRegion> anim = null;
            switch (direction) {
                case ABAJO:     anim = animCaminarAbajo; break;
                case ARRIBA:    anim = animCaminarArriba; break;
                case IZQUIERDA: anim = animCaminarIzquierda; break;
                case DERECHA:   anim = animCaminarDerecha; break;
            }
            if (anim != null) {
                currentFrame = anim.getKeyFrame(tiempoAnimacion);
            }
        } else {
            switch (direction) {
                case ABAJO:     if (reposoAbajo != null) currentFrame = reposoAbajo; break;
                case ARRIBA:    if (reposoArriba != null) currentFrame = reposoArriba; break;
                case IZQUIERDA: if (reposoIzquierda != null) currentFrame = reposoIzquierda; break;
                case DERECHA:   if (reposoDerecha != null) currentFrame = reposoDerecha; break;
            }
        }
    }

    private void actualizarStatic(float delta) {
        state = State.IDLE;
    }

    private void actualizarPatrolH(float delta) {
        if (speed <= 0f || patrolDistance <= 0f) {
            state = State.IDLE;
            return;
        }
        switch (state) {
            case IDLE:
                pauseTimer -= delta;
                if (pauseTimer <= 0) {
                    state = State.WALKING;
                    direction = movingRight ? Direction.DERECHA : Direction.IZQUIERDA;
                }
                break;
            case WALKING:
                float step = speed * delta;
                float target = movingRight ? originX + patrolDistance : originX - patrolDistance;
                if (movingRight) {
                    position.x += step;
                    if (position.x >= target) {
                        position.x = target;
                        movingRight = false;
                        state = State.IDLE;
                        pauseTimer = IDLE_PAUSE;
                    }
                } else {
                    position.x -= step;
                    if (position.x <= target) {
                        position.x = target;
                        movingRight = true;
                        state = State.IDLE;
                        pauseTimer = IDLE_PAUSE;
                    }
                }
                break;
        }
    }

    private void actualizarPatrolV(float delta) {
        if (speed <= 0f || patrolDistance <= 0f) {
            state = State.IDLE;
            return;
        }
        switch (state) {
            case IDLE:
                pauseTimer -= delta;
                if (pauseTimer <= 0) {
                    state = State.WALKING;
                    direction = movingUp ? Direction.ARRIBA : Direction.ABAJO;
                }
                break;
            case WALKING:
                float step = speed * delta;
                float target = movingUp ? originY + patrolDistance : originY - patrolDistance;
                if (movingUp) {
                    position.y += step;
                    if (position.y >= target) {
                        position.y = target;
                        movingUp = false;
                        state = State.IDLE;
                        pauseTimer = IDLE_PAUSE;
                    }
                } else {
                    position.y -= step;
                    if (position.y <= target) {
                        position.y = target;
                        movingUp = true;
                        state = State.IDLE;
                        pauseTimer = IDLE_PAUSE;
                    }
                }
                break;
        }
    }

    private void actualizarWander(float delta) {
        switch (state) {
            case IDLE:
                pauseTimer -= delta;
                if (pauseTimer <= 0) {
                    state = State.WALKING;
                    walkTimer = WALK_DURATION_MIN + (float) Math.random() * (WALK_DURATION_MAX - WALK_DURATION_MIN);
                    Direction[] dirs = Direction.values();
                    Direction nueva = dirs[(int) (Math.random() * dirs.length)];
                    if (nueva != direction || Math.random() < 0.3f) {
                        direction = nueva;
                    }
                }
                break;
            case WALKING:
                float step = speed * delta;
                float nuevaX = position.x;
                float nuevaY = position.y;
                switch (direction) {
                    case IZQUIERDA: nuevaX -= step; break;
                    case DERECHA:   nuevaX += step; break;
                    case ARRIBA:    nuevaY += step; break;
                    case ABAJO:     nuevaY -= step; break;
                }
                if (nuevaX >= wanderMinX && nuevaX <= wanderMaxX && nuevaY >= wanderMinY && nuevaY <= wanderMaxY) {
                    position.x = nuevaX;
                    position.y = nuevaY;
                } else {
                    state = State.IDLE;
                    pauseTimer = 1f;
                }
                walkTimer -= delta;
                if (walkTimer <= 0) {
                    state = State.IDLE;
                    pauseTimer = WANDER_PAUSE_MIN + (float) Math.random() * (WANDER_PAUSE_MAX - WANDER_PAUSE_MIN);
                }
                break;
        }
    }

    public void draw(Batch batch) {
        batch.draw(currentFrame, position.x, position.y, FRAME_WIDTH, FRAME_HEIGHT);
    }

    public Rectangle getBounds() {
        return new Rectangle(position.x, position.y, FRAME_WIDTH, FRAME_HEIGHT);
    }

    public Rectangle getInteractionBounds() {
        return new Rectangle(
            position.x - interactionRadius / 2,
            position.y - interactionRadius / 2,
            FRAME_WIDTH + interactionRadius,
            FRAME_HEIGHT + interactionRadius
        );
    }

    public void setWanderBounds(float minX, float maxX, float minY, float maxY) {
        this.wanderMinX = minX;
        this.wanderMaxX = maxX;
        this.wanderMinY = minY;
        this.wanderMaxY = maxY;
    }

    public void setTalking(boolean talking) {
        this.state = talking ? State.TALKING : State.IDLE;
        if (!talking) {
            pauseTimer = 1f;
        }
    }

    public void setDirection(Direction dir) {
        this.direction = dir;
    }

    public Direction getDirection() { return direction; }
    public String getNpcId() { return npcId; }
    public String getDialogNodeId() { return dialogNodeId; }
    public Vector2 getPosition() { return position; }
    public float getX() { return position.x; }
    public float getY() { return position.y; }
    public float getWidth() { return FRAME_WIDTH; }
    public float getHeight() { return FRAME_HEIGHT; }
    public State getState() { return state; }

    public void dispose() {
        if (placeholderTexture != null) {
            placeholderTexture.dispose();
        }
        if (walkSheetTexture != null) {
            walkSheetTexture.dispose();
        }
        if (idleSheetTexture != null) {
            idleSheetTexture.dispose();
        }
    }
}
