# GangFights - Cliente

Cliente del juego **GangFights** (proyecto final de *Programación sobre redes*, E.T. N°35 "Latzina").
Equipo: Lafflitto / Torrez / Sivale. Tutor: Prof. Pablo Jasinski.

El servidor está en el repositorio **GangFights-Servidor** y hay que dejarlo abierto antes de jugar.

## Qué hace

- **No tiene física ni reglas**: lee el teclado, manda las teclas al servidor y dibuja lo que el servidor dice (posiciones, animaciones, vida, balas, items, rondas).
- Al apretar **PLAY** busca el servidor por **broadcast** (no hace falta escribir la IP), se conecta y muestra **ESPERANDO RIVAL 1/2** con el logo. Cuando entra el segundo jugador empieza la partida.
- Al terminar la partida (3 rondas ganadas) **ENTER** pide la revancha sin cerrar el juego.

## Cómo se ejecuta

Requisitos: **Java 21**.

```
java -jar GangFights-1.0.0.jar
```

(Conviene hacerlo desde CMD para ver los errores.)

### Generar el .jar

```
gradlew lwjgl3:jar
```

Queda en `lwjgl3/build/libs/GangFights-1.0.0.jar`.

### Desde IntelliJ

Clase principal: `com.lstcompany.gangfights.lwjgl3.Lwjgl3Launcher` (módulo `lwjgl3`, carpeta de trabajo `assets`).

### Si el broadcast no encuentra el servidor

Se puede indicar la IP a mano (solo para pruebas):

```
java -Dgangfights.servidor=192.168.1.20 -jar GangFights-1.0.0.jar
```

## Controles

Los dos jugadores (clientes) usan las mismas teclas:

| Acción | Tecla |
|---|---|
| Moverse | A / D |
| Saltar | W |
| Agacharse (recoger items) | S |
| Golpear | 1 o ESPACIO |
| Sacar / guardar la pistola | O |
| Subir / bajar la mira | W / S |
| Disparar | P |
`F1` silencia el sonido durante la partida. `ESC` vuelve al menú.

La pistola se recoge agachándose sobre ella. Solo se puede apuntar en el piso; apuntando no se mueve.
Un golpe saca 10 de vida y aturde (combo de 3); una bala saca 10 de vida y solo pone al rival rojo un instante.

## Estructura

```
core/src/main/java
  Red/        HiloCliente (hilo de red, broadcast, conexión), EstadoJuego (foto del servidor), Protocolo
  Pantallas/  MenuInicial, PantallaEspera (1/2), PantallaPartidaOnline, PantallaConfiguracion, PantallaInicio
  Entidades/  JugadorVisualOnline, BalasVisuales, Personaje   (solo dibujan)
  Items/      ItemsVisuales
  Elementos/  Hud, Texto, Imagen
  Logica/     Partida, EstadoPartida   (copia de los datos del servidor para el HUD)
  Mundo/      MapaOnline (dibuja el mapa de Tiled)
assets/       sprites, audio, mapa
```

`Red/Protocolo.java` es copia de `Servidor/Protocolo.java` del repositorio del servidor.
