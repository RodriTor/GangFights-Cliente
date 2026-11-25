# Registro de cambios - Cliente

## [Actualización] - 2026-11-24
### Cambiado
- Controles iguales para los dos clientes: A/D mover, W saltar, S agacharse, 1 o ESPACIO golpear, O sacar/guardar la pistola, P disparar (W/S suben y bajan la mira). Se quitaron las flechas y las teclas N, M, E. La pantalla de opciones y la ayuda muestran las teclas nuevas.
- `ClienteRed` pasa a ser `HiloCliente extends Thread` (el hilo de red es una clase propia, como en los trabajos de referencia). Mismo comportamiento y protocolo.

## [1.0.0] - 2026-11-22
### Añadido
- Cliente de red (UDP): `HiloCliente` busca el servidor por broadcast, se conecta y recibe el estado en un hilo propio.
- `PantallaEspera`: logo, "ESPERANDO RIVAL" y contador 1/2. Al entrar el segundo jugador empieza la partida.
- `PantallaPartidaOnline`: manda teclas y dibuja el estado del servidor (jugadores, items, balas, mira, rondas, HUD); revancha con ENTER.
- Modo disparo visible: sprite de disparo, mira que gira (hasta vertical), balas, destello rojo al recibir una bala.
- Opción SALIR en el menú.
### Cambiado
- PLAY ahora lleva a la sala de espera online. En la partida el sonido se silencia con F1 (la M se usa para apuntar).
- Animación de golpe más rápida (0,15 s por imagen).
### Eliminado
- La partida local con física (`PantallaJuego`, `Jugador`, Box2D): ahora toda la lógica corre en el servidor.
