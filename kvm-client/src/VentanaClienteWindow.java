import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;
import com.github.kwhat.jnativehook.mouse.NativeMouseEvent;
import com.github.kwhat.jnativehook.mouse.NativeMouseInputListener;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

public class VentanaClienteWindow extends JFrame
        implements NativeMouseInputListener, NativeKeyListener {

    private static final int PUERTO         = 8080;
    private static final int ANCHO_PANTALLA = 1366;
    private static final int ALTO_PANTALLA  = 768;
    private static final int BORDE_DERECHO  = ANCHO_PANTALLA - 2;
    private static final int CENTRO_X       = ANCHO_PANTALLA / 2;
    private static final int CENTRO_Y       = ALTO_PANTALLA  / 2;

    private JTextField txtIp;
    private JButton    btnConectar;
    private JLabel     lblEstado;

    private Socket         socket;
    private PrintWriter    salida;
    private BufferedReader entrada;
    private volatile boolean controlando = false;
    private volatile boolean conectando  = false;
    private volatile boolean anclando    = false; // evita loop de robotLocal

    private Robot robotLocal;

    public VentanaClienteWindow() {
        setTitle("KVM Cliente - Windows");
        setSize(420, 120);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new FlowLayout(FlowLayout.LEFT, 10, 10));
        setResizable(false);

        txtIp       = new JTextField("192.168.1.114", 13);
        btnConectar = new JButton("Iniciar Control");
        lblEstado   = new JLabel("Estado: Desconectado [OFF]");

        add(new JLabel("IP Servidor:"));
        add(txtIp);
        add(btnConectar);
        add(lblEstado);

        btnConectar.addActionListener(e -> alternarConexion());

        try { robotLocal = new Robot(); } catch (AWTException ignored) {}
    }

    private void alternarConexion() {
        if (!controlando) {
            try {
                socket      = new Socket(txtIp.getText().trim(), PUERTO);
                salida      = new PrintWriter(socket.getOutputStream(), true);
                entrada     = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                controlando = true;
                lblEstado.setText("Estado: Controlando [ON]");
                btnConectar.setText("Detener");

                // Anclar mouse al centro al conectar
                if (robotLocal != null) robotLocal.mouseMove(CENTRO_X, CENTRO_Y);

                Thread receptor = new Thread(this::escucharServidor);
                receptor.setDaemon(true);
                receptor.start();

            } catch (Exception ex) {
                lblEstado.setText("Error de conexion");
                conectando = false;
            }
        } else {
            cerrarConexion();
        }
    }

    private void escucharServidor() {
        try {
            String linea;
            while ((linea = entrada.readLine()) != null) {
                if (linea.equals("REGRESAR")) {
                    System.out.println("Servidor pide regresar control");
                    cerrarConexion();
                    // Mover mouse cerca del borde derecho para poder volver a pasar
                    if (robotLocal != null)
                        robotLocal.mouseMove(BORDE_DERECHO - 50, CENTRO_Y);
                    break;
                }
            }
        } catch (Exception ignored) {}
    }

    private void cerrarConexion() {
        controlando = false;
        SwingUtilities.invokeLater(() -> {
            lblEstado.setText("Estado: Desconectado [OFF]");
            btnConectar.setText("Iniciar Control");
        });
        try {
            if (salida != null) salida.println("LIBERAR");
            if (socket != null) socket.close();
        } catch (Exception ignored) {}
        socket  = null;
        salida  = null;
        entrada = null;
    }

    private void enviar(String msg) {
        if (controlando && salida != null) salida.println(msg);
    }

    // ── Mouse ────────────────────────────────────────────────────



    @Override
    public void nativeMouseMoved(NativeMouseEvent e) {
        // Ignorar eventos generados por robotLocal
        if (anclando) return;

        if (controlando) {
            int deltaX = e.getX() - CENTRO_X;
            int deltaY = e.getY() - CENTRO_Y;

            // Anclar mouse al centro
            anclando = true;
            if (robotLocal != null) robotLocal.mouseMove(CENTRO_X, CENTRO_Y);
            anclando = false;

            if (deltaX != 0 || deltaY != 0)
                enviar("D," + deltaX + "," + deltaY);
            return;
        }

        // Borde derecho → conectar automáticamente
        if (!conectando && e.getX() >= BORDE_DERECHO) {
            conectando = true;
            SwingUtilities.invokeLater(() -> {
                alternarConexion();
                conectando = false;
            });
        }
    }

    @Override
    public void nativeMouseDragged(NativeMouseEvent e) {
        if (!controlando) return;
        int deltaX = e.getX() - CENTRO_X;
        int deltaY = e.getY() - CENTRO_Y;
        anclando = true;
        if (robotLocal != null) robotLocal.mouseMove(CENTRO_X, CENTRO_Y);
        anclando = false;
        if (deltaX != 0 || deltaY != 0)
            enviar("D," + deltaX + "," + deltaY);
    }

    @Override
    public void nativeMousePressed(NativeMouseEvent e) {
        if (!controlando) return; // Solo enviar si está controlando
        int boton = nativeBtnToServidor(e.getButton());
        if (boton != -1) enviar("C,PRESIONAR," + boton);
    }

    @Override
    public void nativeMouseReleased(NativeMouseEvent e) {
        if (!controlando) return; // Solo enviar si está controlando
        int boton = nativeBtnToServidor(e.getButton());
        if (boton != -1) enviar("C,LIBERAR," + boton);
    }

    @Override public void nativeMouseClicked(NativeMouseEvent e) {}

    private int nativeBtnToServidor(int btn) {
        if (btn == NativeMouseEvent.BUTTON1) return 1;
        if (btn == NativeMouseEvent.BUTTON2) return 3;
        return -1;
    }

    // ── Teclado ──────────────────────────────────────────────────

    @Override
    public void nativeKeyPressed(NativeKeyEvent e) {
        if (e.getKeyCode() == NativeKeyEvent.VC_ESCAPE && controlando) {
            cerrarConexion();
            return;
        }
        if (!controlando) return;
        enviar("K,PRESIONAR," + e.getKeyCode());
    }

    @Override
    public void nativeKeyReleased(NativeKeyEvent e) {
        if (!controlando) return;
        enviar("K,LIBERAR," + e.getKeyCode());
    }

    @Override public void nativeKeyTyped(NativeKeyEvent e) {}

    // ── Main ─────────────────────────────────────────────────────

    public static void main(String[] args) {
        try {
            Logger log = Logger.getLogger(GlobalScreen.class.getPackage().getName());
            log.setLevel(Level.WARNING);
            log.setUseParentHandlers(false);

            GlobalScreen.registerNativeHook();

            VentanaClienteWindow cliente = new VentanaClienteWindow();
            GlobalScreen.addNativeMouseListener(cliente);
            GlobalScreen.addNativeMouseMotionListener(cliente);
            GlobalScreen.addNativeKeyListener(cliente);

            SwingUtilities.invokeLater(() -> cliente.setVisible(true));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}