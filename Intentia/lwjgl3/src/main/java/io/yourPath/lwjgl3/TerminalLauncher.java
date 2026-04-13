package io.yourPath.lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import io.yourPath.ui.TerminalApp;

public class TerminalLauncher {
    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Intentia Terminal Prototype");
        new Lwjgl3Application(new TerminalApp(), config);
    }
}
