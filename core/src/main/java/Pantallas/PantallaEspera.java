package Pantallas;

import Audio.Efecto;
import Audio.GestorAudio;
import Audio.Musica;
import Elementos.Imagen;
import Elementos.Texto;
import Entradas.ControlAudio;
import Interfaces.NavegadorPantallas;
import Red.HiloCliente;
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


public class PantallaEspera implements Screen {

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

    private HiloCliente hiloCliente = new HiloCliente();
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

        hiloCliente.iniciar();
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

    private void actualizarConexion() {
        if (conexionTransferida || hiloCliente.getEtapa() != HiloCliente.Etapa.CONECTADO) return;

        EstadoJuego foto = hiloCliente.obtenerEstado();
        if (foto != null && foto.fase != EstadoJuego.FASE_ESPERANDO) {
            conexionTransferida = true;
            navegador.irAPartidaOnline(hiloCliente);
        }
    }

    private void procesarTeclado() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            audio.reproducirEfecto(Efecto.CLICK);
            navegador.irAMenu();
            return;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) && hiloCliente.getEtapa() == HiloCliente.Etapa.ERROR) {
            audio.reproducirEfecto(Efecto.CLICK);
            reintentar();
        }
    }

    private void reintentar() {
        hiloCliente.cerrar();
        hiloCliente = new HiloCliente();
        hiloCliente.iniciar();
    }

    private void actualizarTextos() {
        String puntos = ".".repeat(1 + (int) (tiempo / SEGUNDOS_POR_PUNTO) % 3);
        String textoTitulo;
        String textoContador = "";
        String textoDetalle = "";
        String textoAyuda = "ESC: volver al menu    M: silenciar";
        Color colorTitulo = Color.YELLOW;

        switch (hiloCliente.getEtapa()) {
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
                textoDetalle = "Sos el JUGADOR " + hiloCliente.getNumeroJugador()
                    + " - la partida empieza cuando entre el otro jugador";
                break;

            case ERROR:
                textoTitulo = "NO SE PUDO CONECTAR";
                colorTitulo = Color.SALMON;
                textoDetalle = hiloCliente.getMensaje();
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

   private int jugadoresConectados() {
        EstadoJuego foto = hiloCliente.obtenerEstado();
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
            hiloCliente.cerrar();
        }

        if (logo != null) logo.dispose();
        if (titulo != null) titulo.dispose();
        if (contador != null) contador.dispose();
        if (detalle != null) detalle.dispose();
        if (ayuda != null) ayuda.dispose();
    }
}
