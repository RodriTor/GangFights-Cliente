package Red;

/**
 * Mensajes y constantes del protocolo UDP de GangFights.
 * Es una copia de Servidor/Protocolo.java del proyecto del servidor: si se cambia una, se cambia la otra.
 * Todos los mensajes son texto con campos separados por ";".
 */
public final class Protocolo {

    private Protocolo() { }

    public static final int PUERTO = 6767;
    public static final String SEPARADOR = ";";

    // Cliente -> servidor
    public static final String BUSCAR = "BUSCAR";
    public static final String CONECTAR = "CONECTAR";
    public static final String ENTRADA = "ENTRADA";
    public static final String LATIDO = "LATIDO";
    public static final String REVANCHA = "REVANCHA";
    public static final String SALIR = "SALIR";

    // Servidor -> cliente
    public static final String SERVIDOR = "SERVIDOR";
    public static final String JUGADOR = "JUGADOR";
    public static final String LLENO = "LLENO";
    public static final String ESTADO = "ESTADO";
    public static final String CERRADO = "CERRADO";

    public static final int TAMANIO_PAQUETE = 2048;

    /** Si pasan tantos milisegundos sin recibir nada, se considera que el otro lado se desconecto. */
    public static final int TIMEOUT_MS = 3000;
}
