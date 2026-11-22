package Pantallas;

import Audio.Efecto;
import Audio.GestorAudio;
import Audio.Musica;
import Elementos.Hud;
import Elementos.Texto;
import Entidades.BalasVisuales;
import Entidades.JugadorVisualOnline;
import Entidades.Personaje;
import Entradas.ControlAudio;
import Interfaces.NavegadorPantallas;
import Items.ItemsVisuales;
import Logica.EstadoPartida;
import Logica.Partida;
import Mundo.MapaOnline;
import Red.ClienteRed;
import Red.EstadoJuego;
import Utilidades.Config;
import Utilidades.Recursos;
import Utilidades.Render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

/**
 * La partida online vista desde un cliente. Esta pantalla NO tiene fisica ni reglas:
 *  1) lee las teclas y se las manda al servidor (enviarEntrada),
 *  2) dibuja lo que el servidor dice que esta pasando (obtenerEstado).
 * El jugador 1 es Jasinski (izquierda) y el jugador 2 es Schepis (derecha).
 */
public class PantallaPartidaOnline implements Screen {

    private static final float INTERVALO_ENTRADA = 1f / 60f;
    private static final float INTERVALO_REVANCHA = 0.25f;
    private static final String AYUDA =
        "A/D mover  W saltar  S agachar/recoger  1 golpe  2 pistola  W/S mira  O disparo  F1 silenciar  ESC salir";

    private final NavegadorPantallas navegador;
    private final GestorAudio audio;
    private final ControlAudio controlAudio;
    private final ClienteRed clienteRed;

    private OrthographicCamera camara;
    private Viewport viewport;
    private MapaOnline mapa;
    private ItemsVisuales items;
    private BalasVisuales balas;
    private Hud hud;
    private Partida partida;
    private final JugadorVisualOnline[] jugadores = new JugadorVisualOnline[2];

    private Texto cuentaRegresiva;
    private Texto mensaje;
    private Texto etiquetaTu;

    private EstadoJuego estadoAnterior;
    private final int indiceLocal;

    // Teclas: los saltos y golpes se cuentan (ver ClienteRed.enviarEntrada)
    private int contadorSaltos = 0;
    private int contadorGolpes = 0;
    private int contadorApuntar = 0;
    private int contadorDisparos = 0;
    private float tiempoDesdeEntrada = 0f;

    private boolean pidioRevancha = false;
    private float tiempoDesdeRevancha = 0f;

    public PantallaPartidaOnline(NavegadorPantallas navegador, GestorAudio audio, ClienteRed clienteRed) {
        this.navegador = navegador;
        this.audio = audio;
        // La M se usa para apuntar (jugador 2), por eso en la partida el sonido se silencia con F1
        this.controlAudio = new ControlAudio(audio, Input.Keys.F1);
        this.clienteRed = clienteRed;
        this.indiceLocal = clienteRed.getNumeroJugador() - 1;
    }

    @Override
    public void show() {
        camara = new OrthographicCamera();
        viewport = new FitViewport(Config.ANCHO_MUNDO, Config.ALTO_MUNDO, camara);

        mapa = new MapaOnline();
        items = new ItemsVisuales();
        balas = new BalasVisuales();
        jugadores[0] = new JugadorVisualOnline(Personaje.JASINSKI, 230, 300, true);
        jugadores[1] = new JugadorVisualOnline(Personaje.SCHEPIS, 1050, 300, false);

        partida = new Partida(Personaje.JASINSKI.getNombre(), Personaje.SCHEPIS.getNombre());
        hud = new Hud(Personaje.JASINSKI, Personaje.SCHEPIS);
        hud.setAyuda(AYUDA);

        cuentaRegresiva = new Texto(Recursos.FUENTE_MENU, 120, Color.YELLOW);
        mensaje = new Texto(Recursos.FUENTE_MENU, 28, Color.WHITE);
        etiquetaTu = new Texto(Recursos.FUENTE_MENU, 20, Color.YELLOW);
        etiquetaTu.setTexto("VOS");

        audio.reproducirMusica(Musica.JUEGO);
    }

    @Override
    public void render(float delta) {
        controlAudio.actualizar();

        boolean conectado = clienteRed.getEtapa() == ClienteRed.Etapa.CONECTADO;
        EstadoJuego estado = clienteRed.obtenerEstado();

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)
            || (!conectado && Gdx.input.isKeyJustPressed(Input.Keys.ENTER))) {
            navegador.irAMenu(); // al salir de esta pantalla se cierra la conexion
            return;
        }

        if (conectado && estado != null) {
            aplicarEstado(estado);
            enviarTeclas(delta, estado);
            reproducirSonidos(estado);
            estadoAnterior = estado;
        }

        for (JugadorVisualOnline jugador : jugadores) {
            jugador.actualizar(delta);
        }
        balas.actualizar(delta);

        dibujar(estado, conectado);
    }


    /** Copia al dibujo lo que dijo el servidor. */
    private void aplicarEstado(EstadoJuego estado) {
        for (int i = 0; i < 2; i++) {
            EstadoJuego.DatosJugador datos = estado.jugadores[i];
            jugadores[i].recibirEstado(datos.x, datos.y, datos.animacion, datos.mirandoDerecha,
                datos.rojo, datos.apuntando, datos.anguloMira);
            hud.actualizarJugador(i, datos.vida, 100, datos.pistola);
        }

        balas.recibirEstado(estado);

        partida.sincronizar(estadoPartidaDe(estado.fase), estado.ronda,
            estado.rondasGanadas[0], estado.rondasGanadas[1], estado.ganadorRonda);

        if (estado.fase != EstadoJuego.FASE_FINALIZADA) {
            pidioRevancha = false;
        }
    }

    private EstadoPartida estadoPartidaDe(int fase) {
        switch (fase) {
            case EstadoJuego.FASE_RONDA_TERMINADA:
                return EstadoPartida.RONDA_TERMINADA;
            case EstadoJuego.FASE_FINALIZADA:
                return EstadoPartida.FINALIZADA;
            default:
                return EstadoPartida.EN_CURSO;
        }
    }

    private void enviarTeclas(float delta, EstadoJuego estado) {
        boolean izquierda = Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT);
        boolean derecha = Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT);
        boolean agachar = Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN);
        boolean arriba = Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP);

        if (Gdx.input.isKeyJustPressed(Input.Keys.W) || Gdx.input.isKeyJustPressed(Input.Keys.UP)) {
            contadorSaltos++;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1) || Gdx.input.isKeyJustPressed(Input.Keys.N)
            || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            contadorGolpes++;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2) || Gdx.input.isKeyJustPressed(Input.Keys.M)) {
            contadorApuntar++;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.O) || Gdx.input.isKeyJustPressed(Input.Keys.E)) {
            contadorDisparos++;
        }

        tiempoDesdeEntrada += delta;
        if (tiempoDesdeEntrada >= INTERVALO_ENTRADA) {
            tiempoDesdeEntrada = 0f;
            clienteRed.enviarEntrada(izquierda, derecha, agachar, arriba,
                contadorSaltos, contadorGolpes, contadorApuntar, contadorDisparos);
        }

        manejarRevancha(delta, estado);
    }

    /** Con la partida terminada, ENTER pide la revancha. Se repite el pedido hasta que el servidor lo anota. */
    private void manejarRevancha(float delta, EstadoJuego estado) {
        if (estado.fase != EstadoJuego.FASE_FINALIZADA) return;

        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            pidioRevancha = true;
            tiempoDesdeRevancha = INTERVALO_REVANCHA;
        }

        if (pidioRevancha && !estado.pidioRevancha(indiceLocal)) {
            tiempoDesdeRevancha += delta;
            if (tiempoDesdeRevancha >= INTERVALO_REVANCHA) {
                tiempoDesdeRevancha = 0f;
                clienteRed.pedirRevancha();
            }
        }
    }

    /** Los sonidos se deducen de los cambios entre una foto y la siguiente. */
    private void reproducirSonidos(EstadoJuego estado) {
        if (estadoAnterior == null) return;

        for (int i = 0; i < 2; i++) {
            int antes = estadoAnterior.jugadores[i].animacion;
            int ahora = estado.jugadores[i].animacion;
            if (antes == ahora) continue;

            if (ahora == EstadoJuego.ANIM_GOLPE) audio.reproducirEfecto(Efecto.GOLPE);
            if (ahora == EstadoJuego.ANIM_SALTO && antes != EstadoJuego.ANIM_GOLPE) audio.reproducirEfecto(Efecto.SALTO);
            if (ahora == EstadoJuego.ANIM_MUERTO) audio.reproducirEfecto(Efecto.ELIMINACION);
        }

        // Sonido del disparo (provisorio: usa el del golpe, igual que el juego local)
        if (balas.cantidad() > estadoAnterior.balas.length) {
            audio.reproducirEfecto(Efecto.GOLPE);
        }

        if (estado.fase == EstadoJuego.FASE_FINALIZADA && estadoAnterior.fase != EstadoJuego.FASE_FINALIZADA) {
            audio.reproducirEfecto(Efecto.VICTORIA);
        }
    }


    private void dibujar(EstadoJuego estado, boolean conectado) {
        Render.limpiarPantalla(0, 0, 0);
        viewport.apply();
        camara.update();

        mapa.dibujar(camara);

        Render.comenzarBatch(camara);
        if (estado != null) {
            items.dibujar(estado.botiquin, estado.pistola);
        }
        for (int i = 0; i < 2; i++) {
            // Si el rival se fue, su personaje no se dibuja
            if (estado == null || estado.estaConectado(i)) {
                jugadores[i].dibujar();
            }
        }
        balas.dibujar();
        dibujarEtiquetaTu();
        Render.terminarBatch();

        hud.actualizar(partida, audio);
        hud.dibujar();

        dibujarMensajes(estado, conectado);
    }

    private void dibujarEtiquetaTu() {
        JugadorVisualOnline propio = jugadores[indiceLocal];
        etiquetaTu.centrarEn(propio.getX(), propio.getY() + 62f);
        etiquetaTu.dibujar();
    }

    private void dibujarMensajes(EstadoJuego estado, boolean conectado) {
        String texto = "";
        String cuenta = "";

        if (!conectado) {
            texto = clienteRed.getMensaje() + "   ENTER: volver al menu";
        } else if (estado != null) {
            switch (estado.fase) {
                case EstadoJuego.FASE_ESPERANDO:
                    texto = "EL RIVAL SE DESCONECTO - ESPERANDO A OTRO JUGADOR...";
                    break;
                case EstadoJuego.FASE_CUENTA:
                    cuenta = String.valueOf(estado.segundosCuenta);
                    texto = "RONDA " + estado.ronda;
                    break;
                case EstadoJuego.FASE_FINALIZADA:
                    texto = pidioRevancha ? "ESPERANDO AL RIVAL..." : "ENTER: REVANCHA     ESC: SALIR";
                    break;
                default:
                    break;
            }
        }

        Render.comenzarBatch(camara);

        if (!cuenta.isEmpty()) {
            cuentaRegresiva.setTexto(cuenta);
            cuentaRegresiva.centrarEn(Config.ANCHO_MUNDO / 2f, 460f);
            cuentaRegresiva.dibujar();
        }
        if (!texto.isEmpty()) {
            mensaje.setTexto(texto);
            mensaje.centrarEn(Config.ANCHO_MUNDO / 2f, !cuenta.isEmpty() ? 300f : 260f);
            mensaje.dibujar();
        }

        Render.terminarBatch();
    }


    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
        hud.redimensionar(width, height);
    }

    @Override public void pause() { }
    @Override public void resume() { }
    @Override public void hide() { }

    @Override
    public void dispose() {
        // Al salir de la partida se avisa al servidor y se cierra el socket
        clienteRed.cerrar();

        if (hud != null) hud.dispose();
        if (mapa != null) mapa.dispose();
        if (items != null) items.dispose();
        if (balas != null) balas.dispose();
        if (cuentaRegresiva != null) cuentaRegresiva.dispose();
        if (mensaje != null) mensaje.dispose();
        if (etiquetaTu != null) etiquetaTu.dispose();

        for (JugadorVisualOnline jugador : jugadores) {
            if (jugador != null) jugador.dispose();
        }
    }
}
