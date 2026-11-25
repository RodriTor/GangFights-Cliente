package Red;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class HiloCliente extends Thread {

    public enum Etapa { INACTIVO, BUSCANDO, CONECTANDO, CONECTADO, ERROR }

    private static final int INTENTOS_DE_BUSQUEDA = 10;
    private static final int ESPERA_POR_INTENTO_MS = 500;
    private static final int INTENTOS_DE_CONEXION = 5;
    private static final int ESPERA_CONEXION_MS = 400;
    private static final int INTERVALO_LATIDO_MS = 1000;
    private static final int TIEMPO_ESPERA_SOCKET_MS = 100;

    private volatile Etapa etapa = Etapa.INACTIVO;
    private volatile String mensaje = "";
    private volatile int numeroJugador = 0;
    private volatile int jugadoresEnServidor = 0;
    private volatile EstadoJuego ultimoEstado = null;
    private volatile boolean activo = false;

    private DatagramSocket socket;
    private InetAddress servidor;
    private int secuenciaEntrada = 0;

    public HiloCliente() {
        super("Hilo-Cliente");
        setDaemon(true);
    }

    public void iniciar() {
        if (activo) return;

        activo = true;
        etapa = Etapa.BUSCANDO;
        mensaje = "";
        start();
    }

    @Override
    public void run() {
        try {
            socket = new DatagramSocket();
            socket.setBroadcast(true);
            socket.setSoTimeout(TIEMPO_ESPERA_SOCKET_MS);
        } catch (SocketException e) {
            fallar("No se pudo abrir la conexion de red: " + e.getMessage());
            return;
        }

        try {
            if (!buscarServidor()) return;
            if (!conectar()) return;
            recibirEstados();
        } catch (SocketException e) {
            if (activo) fallar("Se cerro la conexion de red.");
        } catch (IOException e) {
            if (activo) fallar("Error de red: " + e.getMessage());
        } finally {
            cerrarSocket();
        }
    }


    private boolean buscarServidor() throws IOException {
        List<InetAddress> destinos = direccionesDeBusqueda();

        for (int intento = 0; intento < INTENTOS_DE_BUSQUEDA && activo; intento++) {
            for (InetAddress destino : destinos) {
                enviarA(destino, Protocolo.BUSCAR);
            }

            long limite = System.currentTimeMillis() + ESPERA_POR_INTENTO_MS;
            while (System.currentTimeMillis() < limite && activo) {
                DatagramPacket respuesta = recibir();
                if (respuesta == null) continue;

                String[] partes = textoDe(respuesta).split(Protocolo.SEPARADOR);
                if (partes[0].equals(Protocolo.SERVIDOR)) {
                    servidor = respuesta.getAddress();
                    jugadoresEnServidor = partes.length > 1 ? Integer.parseInt(partes[1]) : 0;
                    etapa = Etapa.CONECTANDO;
                    System.out.println("[CLIENTE] Servidor encontrado en " + servidor.getHostAddress());
                    return true;
                }
            }
        }

        if (activo) fallar("No se encontro ningun servidor en la red.");
        return false;
    }


    private List<InetAddress> direccionesDeBusqueda() throws IOException {
        List<InetAddress> destinos = new ArrayList<>();

        String manual = System.getProperty("gangfights.servidor");
        if (manual != null && !manual.isEmpty()) {
            destinos.add(InetAddress.getByName(manual));
        }

        destinos.add(InetAddress.getByName("255.255.255.255"));

        for (NetworkInterface red : Collections.list(NetworkInterface.getNetworkInterfaces())) {
            if (!red.isUp() || red.isLoopback()) continue;

            for (InterfaceAddress direccion : red.getInterfaceAddresses()) {
                InetAddress broadcast = direccion.getBroadcast();
                if (broadcast != null && !destinos.contains(broadcast)) {
                    destinos.add(broadcast);
                }
            }
        }

        // Si el servidor corre en esta misma computadora
        destinos.add(InetAddress.getLoopbackAddress());
        return destinos;
    }



    private boolean conectar() throws IOException {
        for (int intento = 0; intento < INTENTOS_DE_CONEXION && activo; intento++) {
            enviar(Protocolo.CONECTAR);

            long limite = System.currentTimeMillis() + ESPERA_CONEXION_MS;
            while (System.currentTimeMillis() < limite && activo) {
                DatagramPacket respuesta = recibir();
                if (respuesta == null || !respuesta.getAddress().equals(servidor)) continue;

                String[] partes = textoDe(respuesta).split(Protocolo.SEPARADOR);

                if (partes[0].equals(Protocolo.JUGADOR) && partes.length > 1) {
                    numeroJugador = Integer.parseInt(partes[1]);
                    etapa = Etapa.CONECTADO;
                    System.out.println(" Conectado como jugador " + numeroJugador);
                    return true;
                }
                if (partes[0].equals(Protocolo.LLENO)) {
                    fallar("La partida del servidor ya esta llena.");
                    return false;
                }
            }
        }

        if (activo) fallar("El servidor no respondio.");
        return false;
    }



    private void recibirEstados() throws IOException {
        long ultimaRecepcion = System.currentTimeMillis();
        long ultimoLatido = 0;

        while (activo) {
            DatagramPacket paquete = recibir();
            long ahora = System.currentTimeMillis();

            if (paquete != null && paquete.getAddress().equals(servidor)) {
                ultimaRecepcion = ahora;
                String texto = textoDe(paquete);

                if (texto.startsWith(Protocolo.ESTADO)) {
                    EstadoJuego estado = EstadoJuego.leer(texto);
                    if (estado != null && esMasNuevo(estado)) {
                        ultimoEstado = estado;
                    }
                } else if (texto.equals(Protocolo.CERRADO)) {
                    fallar("El servidor se cerro.");
                    return;
                }
            }

            if (ahora - ultimaRecepcion > Protocolo.TIMEOUT_MS) {
                fallar("Se perdio la conexion con el servidor.");
                return;
            }

            if (ahora - ultimoLatido >= INTERVALO_LATIDO_MS) {
                enviar(Protocolo.LATIDO);
                ultimoLatido = ahora;
            }
        }
    }

    private boolean esMasNuevo(EstadoJuego estado) {
        EstadoJuego anterior = ultimoEstado;
        return anterior == null || estado.secuencia > anterior.secuencia;
    }


    public void enviarEntrada(boolean izquierda, boolean derecha, boolean agachar, boolean arriba,
                              int saltos, int golpes, int apuntar, int disparos) {
        if (etapa != Etapa.CONECTADO) return;

        enviar(Protocolo.ENTRADA + ";" + (secuenciaEntrada++)
            + ";" + (izquierda ? 1 : 0) + ";" + (derecha ? 1 : 0) + ";" + (agachar ? 1 : 0) + ";" + (arriba ? 1 : 0)
            + ";" + saltos + ";" + golpes + ";" + apuntar + ";" + disparos);
    }

    public void pedirRevancha() {
        if (etapa == Etapa.CONECTADO) {
            enviar(Protocolo.REVANCHA);
        }
    }


    private DatagramPacket recibir() throws IOException {
        DatagramPacket paquete = new DatagramPacket(new byte[Protocolo.TAMANIO_PAQUETE], Protocolo.TAMANIO_PAQUETE);
        try {
            socket.receive(paquete);
            return paquete;
        } catch (SocketTimeoutException e) {
            return null;
        }
    }

    private String textoDe(DatagramPacket paquete) {
        return new String(paquete.getData(), 0, paquete.getLength(), StandardCharsets.UTF_8).trim();
    }

    private void enviar(String texto) {
        if (servidor != null) {
            enviarA(servidor, texto);
        }
    }

    private void enviarA(InetAddress destino, String texto) {
        byte[] datos = texto.getBytes(StandardCharsets.UTF_8);
        try {
            socket.send(new DatagramPacket(datos, datos.length, destino, Protocolo.PUERTO));
        } catch (IOException | RuntimeException e) {
        }
    }

    private void fallar(String texto) {
        System.out.println("[CLIENTE] " + texto);
        mensaje = texto;
        etapa = Etapa.ERROR;
        activo = false;
    }

    private void cerrarSocket() {
        DatagramSocket actual = socket;
        if (actual != null && !actual.isClosed()) {
            actual.close();
        }
    }

    public void cerrar() {
        if (etapa == Etapa.CONECTADO) {
            // Se avisa varias veces porque UDP puede perder mensajes
            for (int i = 0; i < 3; i++) {
                enviar(Protocolo.SALIR);
            }
        }

        activo = false;
        if (etapa != Etapa.ERROR) {
            etapa = Etapa.INACTIVO;
        }
        cerrarSocket();

        try {
            if (Thread.currentThread() != this) {
                join(500);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }


    public Etapa getEtapa() {
        return etapa;
    }

    public String getMensaje() {
        return mensaje;
    }

    public int getNumeroJugador() {
        return numeroJugador;
    }

    public int getJugadoresEnServidor() {
        return jugadoresEnServidor;
    }
    public EstadoJuego obtenerEstado() {
        return ultimoEstado;
    }
}
