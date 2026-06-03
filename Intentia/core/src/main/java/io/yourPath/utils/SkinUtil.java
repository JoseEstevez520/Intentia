package io.yourPath.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.List;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Array;
import static io.yourPath.utils.Colors.*;

public class SkinUtil {

    private static final int TILE = 32;
    private static final int GAP = 1;
    private static final int BORDE = 5;

    public static Skin crear() {
        Skin skin = new Skin();
        TexturaUtil texturas = new TexturaUtil();

        Texture tilemap = new Texture("ui/tilemap.png");
        tilemap.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

        TextureRegion btnUpTex = extraerTile(tilemap, 0, 0);
        TextureRegion btnOverTex = extraerTile(tilemap, 1, 0);
        TextureRegion btnDownTex = extraerTile(tilemap, 2, 0);

        TextureRegion tituloIzqTex = extraerTile(tilemap, 4, 3);
        TextureRegion tituloCentroTex = extraerTile(tilemap, 5, 3);
        TextureRegion tituloDerTex = extraerTile(tilemap, 6, 3);

        NinePatchDrawable upBtn = new NinePatchDrawable(new NinePatch(btnUpTex, BORDE, BORDE, BORDE, BORDE));
        NinePatchDrawable overBtn = new NinePatchDrawable(new NinePatch(btnOverTex, BORDE, BORDE, BORDE, BORDE));
        NinePatchDrawable downBtn = new NinePatchDrawable(new NinePatch(btnDownTex, BORDE, BORDE, BORDE, BORDE));
        NinePatchDrawable panelBg = new NinePatchDrawable(new NinePatch(btnUpTex, BORDE, BORDE, BORDE, BORDE));

        NinePatchDrawable fondoDialogo = new NinePatchDrawable(new NinePatch(btnUpTex, BORDE, BORDE, BORDE, BORDE));
        NinePatchDrawable bloqueadoBtn = new NinePatchDrawable(texturas.crearNinePatchBoton(new Color(0x33 / 255f, 0x33 / 255f, 0x33 / 255f, 1)));

        NinePatchDrawable retratoBorde = new NinePatchDrawable(texturas.crearNinePatchRetrato(MARRON_OSCURO));

        NinePatchDrawable fondoPausa = new NinePatchDrawable(new NinePatch(btnUpTex, BORDE, BORDE, BORDE, BORDE));

        BitmapFont font = crearFuente(16);
        font.getData().markupEnabled = true;
        BitmapFont fontNombre = crearFuente(32);

        Label.LabelStyle labelStyle = new Label.LabelStyle(font, Color.WHITE);
        Label.LabelStyle labelNombreStyle = new Label.LabelStyle(fontNombre, MARRON_MADERA);
        Label.LabelStyle labelInteraccionStyle = new Label.LabelStyle(font, VERDE_AGUA);
        Label.LabelStyle labelDialogoStyle = new Label.LabelStyle(font, MARRON_OSCURO);
        Label.LabelStyle labelNombreDialogoStyle = new Label.LabelStyle(font, MARRON_MADERA);
        Label.LabelStyle labelDireccionStyle = new Label.LabelStyle(font, new Color(0.55f, 0.55f, 0.55f, 1f));

        TextButton.TextButtonStyle btnStyle = new TextButton.TextButtonStyle();
        btnStyle.up = upBtn;
        btnStyle.over = overBtn;
        btnStyle.down = downBtn;
        btnStyle.font = font;
        btnStyle.fontColor = MARRON_OSCURO;
        btnStyle.overFontColor = Color.WHITE;
        btnStyle.downFontColor = Color.WHITE;

        TextButton.TextButtonStyle btnDisabledStyle = new TextButton.TextButtonStyle();
        btnDisabledStyle.up = bloqueadoBtn;
        btnDisabledStyle.font = font;
        btnDisabledStyle.fontColor = new Color(0.5f, 0.5f, 0.5f, 0.5f);

        Window.WindowStyle winStyle = new Window.WindowStyle();
        winStyle.titleFont = fontNombre;
        winStyle.titleFontColor = MARRON_OSCURO;
        winStyle.stageBackground = fondoPausa;
        winStyle.background = panelBg;

        skin.add("fondo-dialogo", fondoDialogo, Drawable.class);
        skin.add("retrato-borde", retratoBorde, Drawable.class);
        skin.add("font-normal", font);
        skin.add("font-nombre", fontNombre);
        skin.add("default", labelStyle);
        skin.add("nombre", labelNombreStyle);
        skin.add("interaccion", labelInteraccionStyle);
        skin.add("dialogo-texto", labelDialogoStyle);
        skin.add("nombre-dialogo", labelNombreDialogoStyle);
        skin.add("dialogo-direccion", labelDireccionStyle);
        skin.add("default", btnStyle);
        skin.add("bloqueado", btnDisabledStyle);
        skin.add("default", winStyle);
        skin.add("pausa-fondo", fondoPausa, Drawable.class);

        skin.add("titulo-izq", new TextureRegionDrawable(tituloIzqTex), Drawable.class);
        skin.add("titulo-centro", new NinePatchDrawable(new NinePatch(tituloCentroTex, 0, 0, 1, 1)), Drawable.class);
        skin.add("titulo-der", new TextureRegionDrawable(tituloDerTex), Drawable.class);

        Slider.SliderStyle sliderStyle = new Slider.SliderStyle();
        sliderStyle.background = texturas.crearDrawableSolido(new Color(0x0F / 255f, 0x2F / 255f, 0x2F / 255f, 1));
        sliderStyle.knob = new TextureRegionDrawable(new TextureRegion(texturas.crearTextura(8, 12, VERDE_AGUA)));
        sliderStyle.background.setMinHeight(6);

        SelectBox.SelectBoxStyle selectBoxStyle = new SelectBox.SelectBoxStyle();
        selectBoxStyle.font = font;
        selectBoxStyle.fontColor = Color.WHITE;
        selectBoxStyle.background = upBtn;
        selectBoxStyle.overFontColor = Color.WHITE;
        selectBoxStyle.listStyle = new List.ListStyle(font, Color.WHITE, VERDE_AGUA, texturas.crearDrawableSolido(new Color(0x2A / 255f, 0x7A / 255f, 0x7A / 255f, 1)));
        selectBoxStyle.scrollStyle = new ScrollPane.ScrollPaneStyle();

        CheckBox.CheckBoxStyle checkBoxStyle = new CheckBox.CheckBoxStyle();
        checkBoxStyle.font = font;
        checkBoxStyle.fontColor = Color.WHITE;
        Drawable cbOff = texturas.crearDrawableSolido(new Color(0x0F / 255f, 0x2F / 255f, 0x2F / 255f, 1));
        cbOff.setMinWidth(16);
        cbOff.setMinHeight(16);
        checkBoxStyle.checkboxOff = cbOff;
        Drawable cbOn = texturas.crearDrawableSolido(VERDE_AGUA);
        cbOn.setMinWidth(16);
        cbOn.setMinHeight(16);
        checkBoxStyle.checkboxOn = cbOn;
        checkBoxStyle.checkboxOver = texturas.crearDrawableSolido(new Color(0x2A / 255f, 0x7A / 255f, 0x7A / 255f, 1));

        skin.add("default-slider", sliderStyle);
        skin.add("default-select", selectBoxStyle);
        skin.add("default-check", checkBoxStyle);

        texturas.limpiar();
        texturas.transferirAlSkin(skin);

        return skin;
    }

    private static TextureRegion extraerTile(Texture textura, int col, int fila) {
        return new TextureRegion(textura, col * (TILE + GAP), fila * (TILE + GAP), TILE, TILE);
    }

    private static BitmapFont crearFuente(int tamano) {
        try {
            FreeTypeFontGenerator generador = new FreeTypeFontGenerator(Gdx.files.internal("fonts/gnf.regular.ttf"));
            FreeTypeFontGenerator.FreeTypeFontParameter params = new FreeTypeFontGenerator.FreeTypeFontParameter();
            params.size = tamano;
            params.minFilter = Texture.TextureFilter.Nearest;
            params.magFilter = Texture.TextureFilter.Nearest;
            BitmapFont fuente = generador.generateFont(params);
            generador.dispose();
            return fuente;
        } catch (Exception e) {
        }
        BitmapFont fallback = new BitmapFont();
        fallback.getData().setScale(tamano / 15f);
        return fallback;
    }

    private static class TexturaUtil {
        private final Array<Texture> texturas = new Array<>();

        private Texture crearTextura(int ancho, int alto, Color color) {
            Pixmap pixmap = new Pixmap(ancho, alto, Pixmap.Format.RGBA8888);
            pixmap.setColor(color);
            pixmap.fill();
            Texture tex = new Texture(pixmap);
            pixmap.dispose();
            texturas.add(tex);
            return tex;
        }

        private Texture crearTexturaBorde(int ancho, int alto, Color borde, Color centro, int grosor) {
            Pixmap pixmap = new Pixmap(ancho, alto, Pixmap.Format.RGBA8888);
            pixmap.setColor(borde);
            pixmap.fill();
            pixmap.setColor(centro);
            pixmap.fillRectangle(grosor, grosor, ancho - grosor * 2, alto - grosor * 2);
            Texture tex = new Texture(pixmap);
            pixmap.dispose();
            texturas.add(tex);
            return tex;
        }

        NinePatch crearNinePatchBoton(Color color) {
            Texture tex = crearTextura(8, 8, color);
            return new NinePatch(tex, 2, 2, 2, 2);
        }

        NinePatchDrawable crearNinePatchRetrato(Color color) {
            Texture tex = crearTexturaBorde(8, 8, color, new Color(0, 0, 0, 0.9f), 1);
            NinePatch np = new NinePatch(tex, 1, 1, 1, 1);
            return new NinePatchDrawable(np);
        }

        Drawable crearDrawableSolido(Color color) {
            Texture tex = crearTextura(1, 1, color);
            return new TextureRegionDrawable(new TextureRegion(tex));
        }

        void limpiar() {
            for (Texture t : texturas) {
                t.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            }
        }

        void transferirAlSkin(Skin skin) {
            for (int i = 0; i < texturas.size; i++) {
                skin.add("_tex_" + i, texturas.get(i));
            }
            texturas.clear();
        }
    }
}
