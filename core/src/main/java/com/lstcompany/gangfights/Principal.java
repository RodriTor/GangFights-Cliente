package com.lstcompany.gangfights;

import Audio.GestorAudio;
import Interfaces.NavegadorPantallas;
import Pantallas.MenuInicial;
import Pantallas.PantallaConfiguracion;
import Pantallas.PantallaInicio;
import Pantallas.PantallaEspera;
import Red.ClienteRed;
import Pantallas.PantallaPartidaOnline;
import Utilidades.Render;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;


public class Principal extends Game implements NavegadorPantallas {

    private GestorAudio audio;
    private Screen pantallaPendiente;

    @Override
    public void create() {
        Render.batch = new SpriteBatch();
        audio = new GestorAudio();
        setScreen(new PantallaInicio(this));
    }

    @Override
    public void render() {
        super.render();
        aplicarCambioDePantalla();
    }

    private void aplicarCambioDePantalla() {
        if (pantallaPendiente == null) return;

        Screen anterior = getScreen();
        setScreen(pantallaPendiente);
        pantallaPendiente = null;

        if (anterior != null) {
            anterior.dispose();
        }
    }

    @Override
    public void irAMenu() {
        pantallaPendiente = new MenuInicial(this, audio);
    }

    @Override
    public void irAEspera() {
        pantallaPendiente = new PantallaEspera(this, audio);
    }

    @Override
    public void irAConfiguracion() {
        pantallaPendiente = new PantallaConfiguracion(this, audio);
    }

    @Override
    public void irAPartidaOnline(ClienteRed clienteRed) {
        pantallaPendiente = new PantallaPartidaOnline(this, audio, clienteRed);
    }

    @Override
    public void dispose() {
        super.dispose();
        if (getScreen() != null) getScreen().dispose();
        audio.dispose();
        Render.batch.dispose();
    }
}
