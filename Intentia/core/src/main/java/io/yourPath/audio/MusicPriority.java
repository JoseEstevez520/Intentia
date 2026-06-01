package io.yourPath.audio;

public enum MusicPriority {
    MENU(0),
    GAMEPLAY(1),
    CINEMATIC(2),
    DIALOGUE(3);

    public final int valor;

    MusicPriority(int valor) {
        this.valor = valor;
    }

    public boolean esMasPrioritariaQue(MusicPriority otra) {
        return this.valor >= otra.valor;
    }
}
