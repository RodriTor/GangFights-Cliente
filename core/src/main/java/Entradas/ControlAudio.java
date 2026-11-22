package Entradas;

import Audio.GestorAudio;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;

public class ControlAudio {

    private final GestorAudio audio;
    private final int teclaSilencio;

    /** Silencia con la tecla M. */
    public ControlAudio(GestorAudio audio) {
        this(audio, Input.Keys.M);
    }

    /** Para pantallas donde M se usa para otra cosa (en la partida M es apuntar del jugador 2). */
    public ControlAudio(GestorAudio audio, int teclaSilencio) {
        this.audio = audio;
        this.teclaSilencio = teclaSilencio;
    }

    public void actualizar() {
        if (Gdx.input.isKeyJustPressed(teclaSilencio)) {
            audio.alternarSilencio();
        }
    }
}
