package io.yourPath.entities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

public class Player {

    private static final int ANCHO_FRAME = 32;
    private static final int ALTO_FRAME = 48;
    private static final float VELOCIDAD = 100f;
    private static final float DURACION_FRAME = 0.15f;

    private static final int FILA_ABAJO = 0;
    private static final int FILA_ARRIBA = 1;
    private static final int FILA_IZQUIERDA = 2;
    private static final int FILA_DERECHA = 3;

    private Animation<TextureRegion> animCaminarAbajo;
    private Animation<TextureRegion> animCaminarIzquierda;
    private Animation<TextureRegion> animCaminarDerecha;
    private Animation<TextureRegion> animCaminarArriba;

    private TextureRegion reposoAbajo;
    private TextureRegion reposoIzquierda;
    private TextureRegion reposoDerecha;
    private TextureRegion reposoArriba;

    private TextureRegion frameActual;
    private int filaDireccion = FILA_ABAJO;
    private boolean moviendo = false;
    private float tiempoAnimacion = 0;

    private Texture idleSheetTexture;
    private Texture walkSheetTexture;
    private Texture fallbackTexture;
    private Texture placeholderTexture;

    public Vector2 posicion;

    public Player(float x, float y) {
        this(x, y, "sprites/npc/prota_walk.png", "sprites/npc/prota_idle.png");
    }

    public Player(float x, float y, String rutaWalk, String rutaIdle) {
        posicion = new Vector2(x, y);

        try {
            if (Gdx.files.internal(rutaIdle).exists()) {
                idleSheetTexture = new Texture(Gdx.files.internal(rutaIdle));
                idleSheetTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
                TextureRegion[][] idleFrames = TextureRegion.split(idleSheetTexture, ANCHO_FRAME, ALTO_FRAME);
                reposoAbajo = idleFrames.length > FILA_ABAJO && idleFrames[FILA_ABAJO].length > 0 ? idleFrames[FILA_ABAJO][0] : null;
                reposoIzquierda = idleFrames.length > FILA_IZQUIERDA && idleFrames[FILA_IZQUIERDA].length > 0 ? idleFrames[FILA_IZQUIERDA][0] : null;
                reposoDerecha = idleFrames.length > FILA_DERECHA && idleFrames[FILA_DERECHA].length > 0 ? idleFrames[FILA_DERECHA][0] : null;
                reposoArriba = idleFrames.length > FILA_ARRIBA && idleFrames[FILA_ARRIBA].length > 0 ? idleFrames[FILA_ARRIBA][0] : null;
            } else {
                fallbackTexture = new Texture(Gdx.files.internal("sprites/player.png"));
                TextureRegion[][] frames = TextureRegion.split(fallbackTexture, ANCHO_FRAME, ALTO_FRAME);
                reposoAbajo = frames.length > 0 && frames[0].length > 0 ? frames[0][0] : null;
                reposoArriba = frames.length > 1 && frames[1].length > 0 ? frames[1][0] : null;
                reposoDerecha = frames.length > 2 && frames[2].length > 0 ? frames[2][0] : null;
                reposoIzquierda = frames.length > 3 && frames[3].length > 0 ? frames[3][0] : null;
            }
        } catch (Exception e) {
            Gdx.app.log("Player", "Error loading idle sprites: " + e.getMessage());
        }

        try {
            if (Gdx.files.internal(rutaWalk).exists()) {
                walkSheetTexture = new Texture(Gdx.files.internal(rutaWalk));
                walkSheetTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
                TextureRegion[][] walkFrames = TextureRegion.split(walkSheetTexture, ANCHO_FRAME, ALTO_FRAME);
                animCaminarAbajo = walkFrames.length > FILA_ABAJO ? crearCaminar(walkFrames[FILA_ABAJO]) : null;
                animCaminarIzquierda = walkFrames.length > FILA_IZQUIERDA ? crearCaminar(walkFrames[FILA_IZQUIERDA]) : null;
                animCaminarDerecha = walkFrames.length > FILA_DERECHA ? crearCaminar(walkFrames[FILA_DERECHA]) : null;
                animCaminarArriba = walkFrames.length > FILA_ARRIBA ? crearCaminar(walkFrames[FILA_ARRIBA]) : null;
            } else {
                if (fallbackTexture == null) {
                    fallbackTexture = new Texture(Gdx.files.internal("sprites/player.png"));
                }
                TextureRegion[][] frames = TextureRegion.split(fallbackTexture, ANCHO_FRAME, ALTO_FRAME);
                animCaminarAbajo = frames.length > 0 ? crearCaminar(frames[0]) : null;
                animCaminarArriba = frames.length > 1 ? crearCaminar(frames[1]) : null;
                animCaminarDerecha = frames.length > 2 ? crearCaminar(frames[2]) : null;
                animCaminarIzquierda = frames.length > 3 ? crearCaminar(frames[3]) : null;
            }
        } catch (Exception e) {
            Gdx.app.log("Player", "Error loading walk sprites: " + e.getMessage());
        }

        frameActual = reposoAbajo;
        if (frameActual == null) {
            crearPlaceholder();
            frameActual = new TextureRegion(placeholderTexture);
        }
    }

    private void crearPlaceholder() {
        if (placeholderTexture != null) return;
        com.badlogic.gdx.graphics.Pixmap pixmap = new com.badlogic.gdx.graphics.Pixmap(ANCHO_FRAME, ALTO_FRAME, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
        pixmap.setColor(com.badlogic.gdx.graphics.Color.WHITE);
        pixmap.fill();
        placeholderTexture = new Texture(pixmap);
        pixmap.dispose();
    }

    private Animation<TextureRegion> crearCaminar(TextureRegion[] fila) {
        Array<TextureRegion> cuadros = new Array<>();
        for (int i = 0; i < 2 && i < fila.length; i++) {
            if (fila[i] != null) cuadros.add(fila[i]);
        }
        return new Animation<>(DURACION_FRAME, cuadros, Animation.PlayMode.LOOP);
    }

    public void actualizar(float delta) {
        moviendo = false;
        float dx = 0;
        float dy = 0;

        if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            dx = -VELOCIDAD * delta;
            filaDireccion = FILA_IZQUIERDA;
            moviendo = true;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            dx = VELOCIDAD * delta;
            filaDireccion = FILA_DERECHA;
            moviendo = true;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP)) {
            dy = VELOCIDAD * delta;
            filaDireccion = FILA_ARRIBA;
            moviendo = true;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
            dy = -VELOCIDAD * delta;
            filaDireccion = FILA_ABAJO;
            moviendo = true;
        }

        posicion.x += dx;
        posicion.y += dy;

        if (moviendo) {
            tiempoAnimacion += delta;
        } else {
            tiempoAnimacion = 0;
        }

        if (moviendo) {
            switch (filaDireccion) {
                case FILA_IZQUIERDA: frameActual = animCaminarIzquierda.getKeyFrame(tiempoAnimacion); break;
                case FILA_DERECHA: frameActual = animCaminarDerecha.getKeyFrame(tiempoAnimacion); break;
                case FILA_ARRIBA: frameActual = animCaminarArriba.getKeyFrame(tiempoAnimacion); break;
                default: frameActual = animCaminarAbajo.getKeyFrame(tiempoAnimacion); break;
            }
        } else {
            switch (filaDireccion) {
                case FILA_IZQUIERDA: frameActual = reposoIzquierda; break;
                case FILA_DERECHA: frameActual = reposoDerecha; break;
                case FILA_ARRIBA: frameActual = reposoArriba; break;
                default: frameActual = reposoAbajo; break;
            }
        }
    }

    public void dibujar(Batch batch) {
        if (frameActual != null) {
            batch.draw(frameActual, posicion.x, posicion.y, ANCHO_FRAME, ALTO_FRAME);
        }
    }

    public void dispose() {
        if (idleSheetTexture != null) idleSheetTexture.dispose();
        if (walkSheetTexture != null) walkSheetTexture.dispose();
        if (fallbackTexture != null) fallbackTexture.dispose();
        if (placeholderTexture != null) placeholderTexture.dispose();
    }
}
