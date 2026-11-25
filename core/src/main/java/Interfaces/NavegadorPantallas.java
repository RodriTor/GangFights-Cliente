package Interfaces;

import Red.HiloCliente;

public interface NavegadorPantallas {

    void irAMenu();

    /** PLAY: busca el servidor y espera al rival. */
    void irAEspera();

    void irAConfiguracion();

    /** La partida empieza: se pasa la conexion que ya tenia la sala de espera. */
    void irAPartidaOnline(HiloCliente hiloCliente);
}
