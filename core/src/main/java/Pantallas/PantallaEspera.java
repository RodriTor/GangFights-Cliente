

package Pantallas;

import Audio.Efecto;
import Audio.GestorAudio;
import Audio.Musica;
import Elementos.Imagen;
import Elementos.Texto;
import Entradas.ControlAudio;
import Interfaces.NavegadorPantallas;
import Red.ClienteRed;
import Red.EstadoJuego;
import Utilidades.Config;
import Utilidades.Recursos;
import Utilidades.Render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

/**
 * Sala de espera. Al apretar PLAY el cliente busca el servidor en la red (broadcast), se conecta y espera al rival:
 * muestra el logo, "ESPERANDO RIVAL" y cuantos jugadores hay (1/2). Cuando entra el segundo jugador el servidor
 * empieza la partida y se pasa a PantallaPartidaOnline.
 * Si no se encuentra el servidor o se pierde, muestra el error y ENTER vuelve a intentar.
 */
public class PantallaEspera implements Screen {

    // LogoPrincipal.png es un lienzo negro de 1024x659 con el logo chico en el centro: se agranda y se
    // posiciona para que el dibujo (centrado en la imagen) quede arriba, sobre el texto.
    private static final float ESCALA_LOGO = 2f;
    private static final float CENTRO_LOGO_Y = 480f;
    private static final float Y_TITULO = 290f;
    private static final float Y_CONTADOR = 215f;
    private static final float Y_DETALLE = 115f;
    private static final float SEGUNDOS_POR_PUNTO = 0.4f;

    private final NavegadorPantallas navegador;
    private final GestorAudio audio;
    private final ControlAudio controlAudio;

    private OrthographicCamera camara;
    private Viewport viewport;
    private Imagen logo;
    private Texto titulo;
    private Texto contador;
    private Texto detalle;
    private Texto ayuda;

    private ClienteRed clienteRed = new ClienteRed();
    private boolean conexionTransferida = false;
    private float tiempo = 0f;

    public PantallaEspera(NavegadorPantallas navegador, GestorAudio audio) {
        this.navegador = navegador;
        this.audio = audio;
        this.controlAudio = new ControlAudio(audio);
    }

    @Override
    public void show() {
        camara = new OrthographicCamera();
        viewport = new FitViewport(Config.ANCHO_MUNDO, Config.ALTO_MUNDO, camara);

        logo = new Imagen(Recursos.LOGO);
        float ancho = logo.getAncho() * ESCALA_LOGO;
        float alto = logo.getAlto() * ESCALA_LOGO;
        logo.setFiltroNitido();
        logo.setTamanio(ancho, alto);
        logo.setPosicion((Config.ANCHO_MUNDO - ancho) / 2f, CENTRO_LOGO_Y - alto / 2f);

        titulo = new Texto(Recursos.FUENTE_MENU, 38, Color.YELLOW);
        contador = new Texto(Recursos.FUENTE_MENU, 84, Color.WHITE);
        detalle = new Texto(Recursos.FUENTE_MENU, 20, Color.LIGHT_GRAY);

        ayuda = new Texto(Recursos.FUENTE_MENU, 16, Color.LIGHT_GRAY);
        ayuda.setPosicion(20, 30);

        Gdx.graphics.setSystemCursor(Cursor.SystemCursor.None);
        audio.reproducirMusica(Musica.MENU);

        clienteRed.iniciar(); // empieza a buscar el servidor apenas se entra
    }

    @Override
    public void render(float delta) {
        tiempo += delta;
        controlAudio.actualizar();

        actualizarConexion();
        procesarTeclado();
        actualizarTextos();

        Render.limpiarPantalla(0, 0, 0);
        viewport.apply();
        camara.update();

        Render.comenzarBatch(camara);
        logo.dibujar();
        titulo.dibujar();
        contador.dibujar();
        detalle.dibujar();
        ayuda.dibujar();
        Render.terminarBatch();
    }

    /** Cuando el servidor tiene a los dos jugadores, empieza la partida. */
    private void actualizarConexion() {
        if (conexionTransferida || clienteRed.getEtapa() != ClienteRed.Etapa.CONECTADO) return;

        EstadoJuego foto = clienteRed.obtenerEstado();
        if (foto != null && foto.fase != EstadoJuego.FASE_ESPERANDO) {
            conexionTransferida = true;
            navegador.irAPartidaOnline(clienteRed);
        }
    }

    private void procesarTeclado() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            audio.reproducirEfecto(Efecto.CLICK);
            navegador.irAMenu(); // al salir de esta pantalla se cierra la conexion
            return;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) && clienteRed.getEtapa() == ClienteRed.Etapa.ERROR) {
            audio.reproducirEfecto(Efecto.CLICK);
            reintentar();
        }
    }

    /** Despues de un error se empieza de cero con una conexion nueva. */
    private void reintentar() {
        clienteRed.cerrar();
        clienteRed = new ClienteRed();
        clienteRed.iniciar();
    }

    private void actualizarTextos() {
        String puntos = ".".repeat(1 + (int) (tiempo / SEGUNDOS_POR_PUNTO) % 3);
        String textoTitulo;
        String textoContador = "";
        String textoDetalle = "";
        String textoAyuda = "ESC: volver al menu    M: silenciar";
        Color colorTitulo = Color.YELLOW;

        switch (clienteRed.getEtapa()) {
            case BUSCANDO:
                textoTitulo = "BUSCANDO SERVIDOR" + puntos;
                textoDetalle = "Revisando la red local";
                break;

            case CONECTANDO:
                textoTitulo = "SERVIDOR ENCONTRADO";
                textoDetalle = "Conectando" + puntos;
                break;

            case CONECTADO:
                textoTitulo = "ESPERANDO RIVAL" + puntos;
                textoContador = jugadoresConectados() + "/2";
                textoDetalle = "Sos el JUGADOR " + clienteRed.getNumeroJugador()
                    + " - la partida empieza cuando entre el otro jugador";
                break;

            case ERROR:
                textoTitulo = "NO SE PUDO CONECTAR";
                colorTitulo = Color.SALMON;
                textoDetalle = clienteRed.getMensaje();
                textoAyuda = "ENTER: reintentar    ESC: volver al menu";
                break;

            default:
                textoTitulo = "INICIANDO" + puntos;
                break;
        }

        titulo.setColor(colorTitulo);
        titulo.setTexto(textoTitulo);
        titulo.centrarEn(Config.ANCHO_MUNDO / 2f, Y_TITULO);

        contador.setTexto(textoContador);
        contador.centrarEn(Config.ANCHO_MUNDO / 2f, Y_CONTADOR);

        detalle.setTexto(textoDetalle);
        detalle.centrarEn(Config.ANCHO_MUNDO / 2f, Y_DETALLE);

        ayuda.setTexto(textoAyuda);
    }

    /** Cuantos jugadores hay en el servidor (el propio ya cuenta aunque todavia no haya llegado la primera foto). */
    private int jugadoresConectados() {
        EstadoJuego foto = clienteRed.obtenerEstado();
        if (foto == null) return 1;

        int cantidad = 0;
        for (int i = 0; i < 2; i++) {
            if (foto.estaConectado(i)) cantidad++;
        }
        return Math.max(1, cantidad);
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override public void pause() { }
    @Override public void resume() { }

    @Override
    public void hide() {
        Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Arrow);
    }

    @Override
    public void dispose() {
        // Si la conexion pasa a la partida, ya no es de esta pantalla
        if (!conexionTransferida) {
            clienteRed.cerrar();
        }

        if (logo != null) logo.dispose();
        if (titulo != null) titulo.dispose();
        if (contador != null) contador.dispose();
        if (detalle != null) detalle.dispose();
        if (ayuda != null) ayuda.dispose();
    }
}
