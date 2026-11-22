# Registro de cambios - Cliente

## [1.0.0] - 2026-11-22
### Añadido
- Cliente de red (UDP): `ClienteRed` busca el servidor por broadcast, se conecta y recibe el estado en un hilo propio.
- `PantallaEspera`: logo, "ESPERANDO RIVAL" y contador 1/2. Al entrar el segundo jugador empieza la partida.
- `PantallaPartidaOnline`: manda teclas y dibuja el estado del servidor (jugadores, items, balas, mira, rondas, HUD); revancha con ENTER.
- Modo disparo visible: sprite de disparo, mira que gira (hasta vertical), balas, destello rojo al recibir una bala.
- Opción SALIR en el menú.
### Cambiado
- PLAY ahora lleva a la sala de espera online. En la partida el sonido se silencia con F1 (la M se usa para apuntar).
- Animación de golpe más rápida (0,15 s por imagen).
### Eliminado
- La partida local con física (`PantallaJuego`, `Jugador`, Box2D): ahora toda la lógica corre en el servidor.
