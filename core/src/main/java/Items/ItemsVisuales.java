package Items;

import Utilidades.Recursos;
import Utilidades.Render;
import Utilidades.Sprites;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

/** Dibuja el botiquin y la pistola en las posiciones que manda el servidor (el servidor decide cuando aparecen). */
public class ItemsVisuales {

    private static final float ESCALA = 2f;

    private final Texture texturaBotiquin = new Texture(Gdx.files.internal(Recursos.VIDA));
    private final Texture texturaPistola = new Texture(Gdx.files.internal(Recursos.PISTOLA));
    private final TextureRegion imagenBotiquin;
    private final TextureRegion imagenPistola;

    public ItemsVisuales() {
        texturaBotiquin.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        texturaPistola.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        imagenBotiquin = Sprites.sinBordeTransparente(texturaBotiquin, Recursos.VIDA);
        imagenPistola = Sprites.sinBordeTransparente(texturaPistola, Recursos.PISTOLA);
    }

    /** Cada item es {x, base} (centro y superficie donde se apoya) o null si no hay. */
    public void dibujar(int[] botiquin, int[] pistola) {
        dibujarItem(imagenBotiquin, botiquin);
        dibujarItem(imagenPistola, pistola);
    }

    private void dibujarItem(TextureRegion imagen, int[] posicion) {
        if (posicion == null) return;

        float ancho = imagen.getRegionWidth() * ESCALA;
        float alto = imagen.getRegionHeight() * ESCALA;
        Render.batch.draw(imagen, posicion[0] - ancho / 2f, posicion[1], ancho, alto);
    }

    public void dispose() {
        texturaBotiquin.dispose();
        texturaPistola.dispose();
    }
}
