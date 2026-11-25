package Red;


public class EstadoJuego {

    // Fases
    public static final int FASE_ESPERANDO = 0;
    public static final int FASE_CUENTA = 1;
    public static final int FASE_EN_CURSO = 2;
    public static final int FASE_RONDA_TERMINADA = 3;
    public static final int FASE_FINALIZADA = 4;

    // Animaciones
    public static final int ANIM_QUIETO = 0;
    public static final int ANIM_CORRER = 1;
    public static final int ANIM_SALTO = 2;
    public static final int ANIM_AGACHADO = 3;
    public static final int ANIM_GOLPE = 4;
    public static final int ANIM_RECIBIR_1 = 5;
    public static final int ANIM_RECIBIR_2 = 6;
    public static final int ANIM_RECIBIR_3 = 7;
    public static final int ANIM_MUERTO = 8;
    public static final int ANIM_DISPARO = 9;

    public static class DatosJugador {
        public final int x;
        public final int y;
        public final int vida;
        public final int animacion;
        public final boolean mirandoDerecha;
        public final boolean pistola;
        public final boolean apuntando;
        public final int anguloMira;
        public final boolean rojo;

        DatosJugador(String texto) {
            String[] c = texto.split(",");
            x = Integer.parseInt(c[0]);
            y = Integer.parseInt(c[1]);
            vida = Integer.parseInt(c[2]);
            animacion = Integer.parseInt(c[3]);
            mirandoDerecha = c[4].equals("1");
            pistola = c[5].equals("1");
            apuntando = c[6].equals("1");
            anguloMira = Integer.parseInt(c[7]);
            rojo = c[8].equals("1");
        }
    }

    public final int secuencia;
    public final int fase;
    public final int ronda;
    public final int ganadorRonda;
    public final int[] rondasGanadas = new int[2];
    public final int segundosCuenta;
    public final DatosJugador[] jugadores = new DatosJugador[2];

    public final int[] botiquin;
    public final int[] pistola;

    public final int[][] balas;

    private final int conectados;
    private final int revanchas;

    private EstadoJuego(String[] t) {
        secuencia = Integer.parseInt(t[1]);
        fase = Integer.parseInt(t[2]);
        ronda = Integer.parseInt(t[3]);
        ganadorRonda = Integer.parseInt(t[4]);
        rondasGanadas[0] = Integer.parseInt(t[5]);
        rondasGanadas[1] = Integer.parseInt(t[6]);
        conectados = Integer.parseInt(t[7]);
        revanchas = Integer.parseInt(t[8]);
        segundosCuenta = Integer.parseInt(t[9]);
        jugadores[0] = new DatosJugador(t[10]);
        jugadores[1] = new DatosJugador(t[11]);
        botiquin = leerItem(t[12]);
        pistola = leerItem(t[13]);
        balas = leerBalas(t[14]);
    }

    private static int[][] leerBalas(String texto) {
        if (texto.equals("-")) return new int[0][];

        String[] cada = texto.split("/");
        int[][] balas = new int[cada.length][];

        for (int i = 0; i < cada.length; i++) {
            String[] c = cada[i].split(",");
            balas[i] = new int[] {Integer.parseInt(c[0]), Integer.parseInt(c[1]), Integer.parseInt(c[2])};
        }
        return balas;
    }

    private static int[] leerItem(String texto) {
        if (texto.equals("-")) return null;
        String[] c = texto.split(",");
        return new int[] {Integer.parseInt(c[0]), Integer.parseInt(c[1])};
    }

    public static EstadoJuego leer(String mensaje) {
        try {
            String[] t = mensaje.split(Protocolo.SEPARADOR);
            if (t.length < 15) return null;
            return new EstadoJuego(t);
        } catch (RuntimeException e) {
            return null;
        }
    }

    public boolean estaConectado(int indice) {
        return (conectados & (1 << indice)) != 0;
    }

    public boolean pidioRevancha(int indice) {
        return (revanchas & (1 << indice)) != 0;
    }
}
