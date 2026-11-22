package Entidades;

import Red.EstadoJuego;
import Utilidades.Render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;

/**
 * Dibuja las balas que dice el servidor. Como el estado llega 30 veces por segundo y la bala es muy rapida,
 * entre una foto y la siguiente se la desliza en su direccion para que se vea fluida
 * (el servidor es el que decide donde esta realmente y si pego).
 */
public class BalasVisuales {

    private static final float ANCHO = 10f;
    private static final float ALTO = 4f;
    private static final float VELOCIDAD = 900f;
    private static final float TIEMPO_MAXIMO_DESLIZADO = 0.08f;

    private final Texture pixel;
    private int[][] balas = new int[0][];
    private int ultimaSecuencia = -1;
    private float tiempoDesdeFoto = 0f;

    public BalasVisuales() {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        pixel = new Texture(pixmap);
        pixmap.dispose();
    }

    public void recibirEstado(EstadoJuego estado) {
        if (estado.secuencia == ultimaSecuencia) return;

        ultimaSecuencia = estado.secuencia;
        balas = estado.balas;
        tiempoDesdeFoto = 0f;
    }

    public void actualizar(float delta) {
        tiempoDesdeFoto += delta;
    }

    public int cantidad() {
        return balas.length;
    }

    public void dibujar() {
        float desliz = VELOCIDAD * Math.min(tiempoDesdeFoto, TIEMPO_MAXIMO_DESLIZADO);

        Render.batch.setColor(Color.YELLOW);
        for (int[] bala : balas) {
            double radianes = Math.toRadians(bala[2]);
            float x = bala[0] + (float) Math.cos(radianes) * desliz;
            float y = bala[1] + (float) Math.sin(radianes) * desliz;

            Render.batch.draw(pixel, x - ANCHO / 2f, y - ALTO / 2f, ANCHO / 2f, ALTO / 2f, ANCHO, ALTO,
                1f, 1f, bala[2], 0, 0, 1, 1, false, false);
        }
        Render.batch.setColor(Color.WHITE);
    }

    public void limpiar() {
        balas = new int[0][];
    }

    public void dispose() {
        pixel.dispose();
    }
}
