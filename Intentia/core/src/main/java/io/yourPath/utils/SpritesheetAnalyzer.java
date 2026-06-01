package io.yourPath.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;

public class SpritesheetAnalyzer {

    public static void analizar(String archivo, int anchoPrueba, int altoPrueba) {
        Pixmap pixmap = new Pixmap(Gdx.files.internal(archivo));

        int texAncho = pixmap.getWidth();
        int texAlto = pixmap.getHeight();
        int cols = texAncho / anchoPrueba;
        int filas = texAlto / altoPrueba;

        System.out.println("\n=== ANALISIS: " + archivo + " ===");
        System.out.println("Dimensiones: " + texAncho + "x" + texAlto);
        System.out.println("Frame prueba: " + anchoPrueba + "x" + altoPrueba);
        System.out.println("Grid: " + cols + " cols x " + filas + " filas");
        System.out.println();

        for (int f = 0; f < filas; f++) {
            System.out.print("Fila " + f + ": ");
            for (int c = 0; c < cols; c++) {
                int pixelesNoTransparentes = 0;
                int totalPixeles = anchoPrueba * altoPrueba;
                for (int py = 0; py < altoPrueba; py++) {
                    for (int px = 0; px < anchoPrueba; px++) {
                        int x = c * anchoPrueba + px;
                        int y = f * altoPrueba + py;
                        int rgba = pixmap.getPixel(x, y);
                        int alpha = (rgba >>> 24) & 0xff;
                        if (alpha > 10) {
                            pixelesNoTransparentes++;
                        }
                    }
                }
                int porcentaje = pixelesNoTransparentes * 100 / totalPixeles;
                System.out.print("[" + porcentaje + "%] ");
            }
            System.out.println();
        }

        System.out.println("\nLeyenda:");
        for (int f = 0; f < filas; f++) {
            System.out.print("  Fila " + f + " columnas con > 5% pixeles: ");
            for (int c = 0; c < cols; c++) {
                int pixelesNoTransparentes = 0;
                for (int py = 0; py < altoPrueba; py++) {
                    for (int px = 0; px < anchoPrueba; px++) {
                        int x = c * anchoPrueba + px;
                        int y = f * altoPrueba + py;
                        int rgba = pixmap.getPixel(x, y);
                        int alpha = (rgba >>> 24) & 0xff;
                        if (alpha > 10) pixelesNoTransparentes++;
                    }
                }
                if (pixelesNoTransparentes > anchoPrueba * altoPrueba * 0.05f) {
                    System.out.print(c + " ");
                }
            }
            System.out.println();
        }

        pixmap.dispose();
    }
}
