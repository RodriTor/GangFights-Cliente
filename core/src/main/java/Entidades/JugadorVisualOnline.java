package Entidades;

import Red.EstadoJuego;
import Utilidades.Render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import java.util.ArrayList;

/**
 * Como se ve un jugador en el modo online. No tiene fisica ni reglas: el servidor le dice
 * donde esta y que animacion hacer (recibirEstado) y esta clase solo lo dibuja.
 * Para que no se vea "a saltos" (la red llega 30 veces por segundo) se acerca suavemente a la posicion recibida.
 */
public class JugadorVisualOnline {

    private static final int ANCHO_FRAME = 32;
    private static final int ALTO_FRAME = 42;
    private static final float ANCHO_CUERPO = 30f;
    private static final float ALTO_CUERPO = 60f;

    private static final float VELOCIDAD_SUAVIZADO = 20f;
    private static final float DISTANCIA_PARA_TELETRANSPORTAR = 150f;

    private static final Color COLOR_DANIO = new Color(1f, 0.55f, 0.55f, 1f);

    // Mira del modo disparo (mismas medidas que usa el servidor para calcular hacia donde va la bala)
    private static final float DISTANCIA_MIRA = 70f;
    private static final float ALTURA_BOCA = 8f;
    private static final float SALIDA_BALA_X = 22f;
    private static final float TAMANIO_MIRA = 12f;
    private static final float GROSOR_MIRA = 2f;

    private final ArrayList<Texture> texturas = new ArrayList<>();
    private final TextureRegion[] regionesRecibir = new TextureRegion[3];

    private Animation<TextureRegion> animacionQuieto;
    private Animation<TextureRegion> animacionAgachado;
    private Animation<TextureRegion> animacionCorrer;
    private Animation<TextureRegion> animacionGolpe;
    private TextureRegion regionSalto;
    private TextureRegion regionMuerto;
    private TextureRegion regionDisparo;
    private Texture pixelBlanco;

    private float x;
    private float y;
    private float objetivoX;
    private float objetivoY;
    private int animacion = EstadoJuego.ANIM_QUIETO;
    private boolean mirandoDerecha;
    private boolean rojo;
    private boolean apuntando;
    private int anguloMira;
    private boolean posicionInicial = true;

    private float tiempoAnimacion;
    private float tiempoGolpe;

    public JugadorVisualOnline(Personaje personaje, float x, float y, boolean mirandoDerecha) {
        this.x = x;
        this.y = y;
        this.objetivoX = x;
        this.objetivoY = y;
        this.mirandoDerecha = mirandoDerecha;
        cargarAnimaciones(personaje);
    }

    private TextureRegion[][] cargarHoja(String ruta) {
        Texture hoja = new Texture(Gdx.files.internal(ruta));
        texturas.add(hoja);
        return TextureRegion.split(hoja, ANCHO_FRAME, ALTO_FRAME);
    }

    /** Si la hoja no tiene ese frame, usa el primero. */
    private TextureRegion obtenerFrame(TextureRegion[][] hoja, int fila, int columna) {
        if (fila < hoja.length && columna < hoja[fila].length) {
            return hoja[fila][columna];
        }
        return hoja[0][0];
    }

    private void cargarAnimaciones(Personaje personaje) {
        TextureRegion[][] quieto = cargarHoja(personaje.getRutaQuieto());
        animacionQuieto = new Animation<>(0.15f, quieto[0][0], quieto[0][1], quieto[1][0]);
        animacionQuieto.setPlayMode(Animation.PlayMode.LOOP);

        animacionAgachado = new Animation<>(0.15f, cargarHoja(personaje.getRutaAgachado())[0][0]);

        TextureRegion[][] correr = cargarHoja(personaje.getRutaCorrer());
        animacionCorrer = new Animation<>(0.10f, correr[0][0], correr[0][1], correr[1][0]);
        animacionCorrer.setPlayMode(Animation.PlayMode.LOOP);

        regionSalto = cargarHoja(personaje.getRutaSalto())[0][0];

        TextureRegion[][] golpe = cargarHoja(personaje.getRutaGolpe());
        animacionGolpe = new Animation<>(0.15f, golpe[0][0], golpe[0][1]);
        animacionGolpe.setPlayMode(Animation.PlayMode.NORMAL);

        TextureRegion[][] recibir = cargarHoja(personaje.getRutaRecibir());
        regionesRecibir[0] = obtenerFrame(recibir, 0, 0);
        regionesRecibir[1] = obtenerFrame(recibir, 0, 1);
        regionesRecibir[2] = obtenerFrame(recibir, 1, 0);

        regionMuerto = cargarHoja(personaje.getRutaMuerto())[0][0];
        regionDisparo = cargarHoja(personaje.getRutaDisparo())[0][0];

        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        pixelBlanco = new Texture(pixmap);
        pixmap.dispose();
    }


    /** Lo que dijo el servidor en la ultima foto. */
    public void recibirEstado(float nuevaX, float nuevaY, int nuevaAnimacion, boolean mirando,
                              boolean rojo, boolean apuntando, int anguloMira) {
        if (nuevaAnimacion == EstadoJuego.ANIM_GOLPE && animacion != EstadoJuego.ANIM_GOLPE) {
            tiempoGolpe = 0f; // empieza un golpe nuevo
        }

        objetivoX = nuevaX;
        objetivoY = nuevaY;
        animacion = nuevaAnimacion;
        mirandoDerecha = mirando;
        this.rojo = rojo;
        this.apuntando = apuntando;
        this.anguloMira = anguloMira;

        if (posicionInicial) {
            x = nuevaX;
            y = nuevaY;
            posicionInicial = false;
        }
    }

    public void actualizar(float delta) {
        tiempoAnimacion += delta;
        if (animacion == EstadoJuego.ANIM_GOLPE) {
            tiempoGolpe += delta;
        }

        float distanciaX = objetivoX - x;
        float distanciaY = objetivoY - y;

        // Si el cambio es enorme (empezo una ronda nueva) no tiene sentido deslizarse
        if (Math.abs(distanciaX) > DISTANCIA_PARA_TELETRANSPORTAR || Math.abs(distanciaY) > DISTANCIA_PARA_TELETRANSPORTAR) {
            x = objetivoX;
            y = objetivoY;
            return;
        }

        float avance = Math.min(1f, VELOCIDAD_SUAVIZADO * delta);
        x += distanciaX * avance;
        y += distanciaY * avance;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public void dibujar() {
        if (animacion == EstadoJuego.ANIM_MUERTO) {
            dibujarEchado();
            return;
        }

        TextureRegion frame = obtenerFrameActual();
        boolean aturdido = animacion >= EstadoJuego.ANIM_RECIBIR_1 && animacion <= EstadoJuego.ANIM_RECIBIR_3;

        if (aturdido || rojo) {
            Render.batch.setColor(COLOR_DANIO);
        }
        Render.batch.draw(frame, x - ANCHO_CUERPO / 2f, y - ALTO_CUERPO / 2f, ANCHO_CUERPO, ALTO_CUERPO);
        Render.batch.setColor(Color.WHITE);

        if (apuntando) {
            dibujarMira();
        }
    }

    /** Un cuadradito hueco que gira alrededor de la boca del arma: hacia ahi va a salir la bala. */
    private void dibujarMira() {
        float sentido = mirandoDerecha ? 1f : -1f;
        double radianes = Math.toRadians(anguloMira);

        float centroX = x + sentido * SALIDA_BALA_X + sentido * DISTANCIA_MIRA * (float) Math.cos(radianes);
        float centroY = y + ALTURA_BOCA + DISTANCIA_MIRA * (float) Math.sin(radianes);

        float izquierda = centroX - TAMANIO_MIRA / 2f;
        float abajo = centroY - TAMANIO_MIRA / 2f;

        Render.batch.draw(pixelBlanco, izquierda, abajo, TAMANIO_MIRA, GROSOR_MIRA);
        Render.batch.draw(pixelBlanco, izquierda, abajo + TAMANIO_MIRA - GROSOR_MIRA, TAMANIO_MIRA, GROSOR_MIRA);
        Render.batch.draw(pixelBlanco, izquierda, abajo, GROSOR_MIRA, TAMANIO_MIRA);
        Render.batch.draw(pixelBlanco, izquierda + TAMANIO_MIRA - GROSOR_MIRA, abajo, GROSOR_MIRA, TAMANIO_MIRA);
    }

    /** Muerto: la imagen se acuesta (girada 90 grados) y queda apoyada en el piso. */
    private void dibujarEchado() {
        TextureRegion frame = obtenerFrameActual();
        float giro = mirandoDerecha ? 90f : -90f;
        float centroY = y - (ALTO_CUERPO - ANCHO_CUERPO) / 2f;

        Render.batch.draw(frame, x - ANCHO_CUERPO / 2f, centroY - ALTO_CUERPO / 2f,
            ANCHO_CUERPO / 2f, ALTO_CUERPO / 2f, ANCHO_CUERPO, ALTO_CUERPO, 1f, 1f, giro);
    }

    private TextureRegion obtenerFrameActual() {
        TextureRegion region;

        switch (animacion) {
            case EstadoJuego.ANIM_MUERTO:
                region = regionMuerto;
                break;
            case EstadoJuego.ANIM_RECIBIR_1:
                region = regionesRecibir[0];
                break;
            case EstadoJuego.ANIM_RECIBIR_2:
                region = regionesRecibir[1];
                break;
            case EstadoJuego.ANIM_RECIBIR_3:
                region = regionesRecibir[2];
                break;
            case EstadoJuego.ANIM_DISPARO:
                region = regionDisparo;
                break;
            case EstadoJuego.ANIM_GOLPE:
                region = animacionGolpe.getKeyFrame(tiempoGolpe, false);
                break;
            case EstadoJuego.ANIM_SALTO:
                region = regionSalto;
                break;
            case EstadoJuego.ANIM_AGACHADO:
                region = animacionAgachado.getKeyFrame(tiempoAnimacion, false);
                break;
            case EstadoJuego.ANIM_CORRER:
                region = animacionCorrer.getKeyFrame(tiempoAnimacion, true);
                break;
            default:
                region = animacionQuieto.getKeyFrame(tiempoAnimacion, true);
                break;
        }

        // Cada region recuerda si esta espejada, asi que solo la damos vuelta si hace falta
        if (region.isFlipX() == mirandoDerecha) {
            region.flip(true, false);
        }
        return region;
    }

    public void dispose() {
        for (Texture textura : texturas) {
            textura.dispose();
        }
        texturas.clear();
        if (pixelBlanco != null) pixelBlanco.dispose();
    }
}
