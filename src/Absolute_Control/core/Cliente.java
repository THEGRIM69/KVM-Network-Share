package Absolute_Control.core;

import Absolute_Control.input.KeyboardHandler;
import Absolute_Control.input.MouseHandler;
import com.github.kwhat.jnativehook.GlobalScreen;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.function.Consumer;

public class Cliente {

    private final String          ip;
    private final int             puerto;
    private final boolean         servidorALaDerecha;
    private final int             anchoPantalla;
    private final Consumer<String> logger;
    private final Runnable         onDesconectado;

    private Socket         socket;
    private PrintWriter    salida;
    private BufferedReader entrada;
    private volatile boolean conectado = false;

    private MouseHandler    mouseHandler;
    private KeyboardHandler keyboardHandler;

    public Cliente(String ip, int puerto, boolean servidorALaDerecha,
                   int anchoPantalla, Consumer<String> logger, Runnable onDesconectado) {
        this.ip                  = ip;
        this.puerto              = puerto;
        this.servidorALaDerecha  = servidorALaDerecha;
        this.anchoPantalla       = anchoPantalla;
        this.logger              = logger;
        this.onDesconectado      = onDesconectado;
    }

    public void iniciarHandlers() {
        mouseHandler = new MouseHandler(
                this::enviar,
                this::conectar,
                this::desconectar
        );
        mouseHandler.setLado(servidorALaDerecha, anchoPantalla);

        keyboardHandler = new KeyboardHandler(this::enviar, this::desconectar);

        try {
            GlobalScreen.addNativeMouseListener(mouseHandler);
            GlobalScreen.addNativeMouseMotionListener(mouseHandler);
            GlobalScreen.addNativeMouseWheelListener(mouseHandler);
            GlobalScreen.addNativeKeyListener(keyboardHandler);
        } catch (Exception e) {
            logger.accept("Error registrando hooks: " + e.getMessage());
        }
    }

    public void detenerHandlers() {
        try {
            if (mouseHandler != null) {
                GlobalScreen.removeNativeMouseListener(mouseHandler);
                GlobalScreen.removeNativeMouseMotionListener(mouseHandler);
                GlobalScreen.removeNativeMouseWheelListener(mouseHandler);
            }
            if (keyboardHandler != null)
                GlobalScreen.removeNativeKeyListener(keyboardHandler);
        } catch (Exception ignored) {}
    }

    private void conectar() {
        if (conectado) return;
        try {
            socket    = new Socket(ip, puerto);
            salida    = new PrintWriter(socket.getOutputStream(), true);
            entrada   = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            conectado = true;

            mouseHandler.setControlando(true);
            keyboardHandler.setControlando(true);

            logger.accept("Conectado a " + ip + ":" + puerto);

            Thread receptor = new Thread(this::escucharServidor);
            receptor.setDaemon(true);
            receptor.start();

        } catch (Exception e) {
            logger.accept("Error al conectar: " + e.getMessage());
        }
    }

    private void escucharServidor() {
        try {
            String linea;
            while ((linea = entrada.readLine()) != null) {
                if (linea.equals("REGRESAR")) {
                    logger.accept("Control regresado por el servidor");
                    desconectar();
                    break;
                }
            }
        } catch (Exception ignored) {}
    }

    public void desconectar() {
        if (!conectado) return;
        conectado = false;
        mouseHandler.setControlando(false);
        keyboardHandler.setControlando(false);
        try {
            if (salida != null) salida.println("LIBERAR");
            if (socket != null) socket.close();
        } catch (Exception ignored) {}
        socket  = null;
        salida  = null;
        entrada = null;
        logger.accept("Desconectado.");
        onDesconectado.run();
    }

    private void enviar(String msg) {
        if (conectado && salida != null) salida.println(msg);
    }

    public boolean isConectado() { return conectado; }
}