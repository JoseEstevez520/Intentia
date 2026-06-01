package io.yourPath.mapgen;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class BlankMapGenerator {

    static final String[] TILESETS = {
        "1,Atlas_Buildings.tsx",
        "443,Objects_Buildings.tsx",
        "449,Objects_Props.tsx",
        "468,Objects_Rocks.tsx",
        "473,Objects_Trees.tsx",
        "484,Atlas_Props.tsx",
        "664,Atlas_Rocks.tsx",
        "686,Tileset_Ground.tsx",
        "818,Tileset_RockSlope.tsx",
        "4914,Tileset_RockSlope_Simple.tsx",
        "4968,Tileset_Water.tsx",
        "5280,Tilesets_Road.tsx",
        "5340,Atlas_Trees_Bushes.tsx",
        "5484,Animation_Flowers_Red.tsx",
        "5580,Animation_Flowers_White.tsx",
        "5676,Animation_Campfire.tsx"
    };

    public static void main(String[] args) throws IOException {
        String dir = "TileSet/Tiled/Tilemaps";
        String name = "NuevoMapa";
        int w = 40, h = 40;
        if (args.length > 0) name = args[0];
        if (args.length > 1) w = Integer.parseInt(args[1]);
        if (args.length > 2) h = Integer.parseInt(args[2]);
        if (args.length > 3) dir = args[3];
        String path = dir + "/" + name + ".tmx";
        generate(path, w, h);
        System.out.println("Creado: " + path);
    }

    public static void generate(String path, int w, int h) throws IOException {
        try (PrintWriter out = new PrintWriter(new FileWriter(path))) {
            out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            out.printf("<map version=\"1.10\" tiledversion=\"1.10.1\" orientation=\"orthogonal\" "
                + "renderorder=\"right-down\" width=\"%d\" height=\"%d\" "
                + "tilewidth=\"16\" tileheight=\"16\" infinite=\"0\" "
                + "nextlayerid=\"10\" nextobjectid=\"1\">%n", w, h);

            for (String ts : TILESETS) {
                String[] p = ts.split(",", 2);
                out.printf(" <tileset firstgid=\"%s\" source=\"../Tilesets/%s\"/>%n", p[0], p[1]);
            }

            String[] tileLayers = {"Ground", "Flowers", "Road", "Trees", "RockSlopes", "RockSlopes_Auto", "Water", "AbovePlayer"};
            int layId = 1;
            for (String layer : tileLayers) {
                boolean visible = !layer.equals("RockSlopes");
                out.printf(" <layer id=\"%d\" name=\"%s\" width=\"%d\" height=\"%d\"%s>%n",
                    layId++, layer, w, h, visible ? "" : " visible=\"0\"");
                out.println("  <data encoding=\"csv\">");
                for (int r = 0; r < h; r++) {
                    StringBuilder sb = new StringBuilder();
                    for (int c = 0; c < w; c++) {
                        if (c > 0) sb.append(',');
                        sb.append('0');
                    }
                    out.println(" " + sb.toString() + (r < h - 1 ? "," : ""));
                }
                out.println("  </data>");
                out.println(" </layer>");
            }

            out.printf(" <objectgroup id=\"%d\" name=\"Object Layer 1\"/>%n", layId++);
            out.printf(" <objectgroup id=\"%d\" name=\"Collisions\"/>%n", layId++);
            out.printf(" <objectgroup id=\"%d\" name=\"NPCs\"/>%n", layId++);

            out.println("</map>");
        }
    }
}
