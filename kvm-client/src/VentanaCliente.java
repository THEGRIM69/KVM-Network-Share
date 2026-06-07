import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;
import com.github.kwhat.jnativehook.mouse.NativeMouseEvent;
import com.github.kwhat.jnativehook.mouse.NativeMouseInputListener;

import javax.swing.*;
import java.awt.*;
import java.io.PrintWriter;
import java.net.Socket;

public class VentanaCliente extends JFrame
        implements NativeMouseInputListener, NativeKeyListener {

    private JTextField txtIp;
    private JButton    btnConectar;
    private JLabel     lblEstado;

    private Socket      socket;
    private PrintWriter salida;
    private boolean     controlando = false;

    public VentanaCliente() {
        setTitle("KVM Cliente - Arch Linux");
        setSize(320, 160);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new FlowLayout(FlowLayout.LEFT, 10, 10));
        setResizable(false);

        txtIp       = new JTextField("192.168.1.102", 13);
        btnConectar = new JButton("Iniciar Control");
        lblEstado   = new JLabel("Estado: Desconectado 🔴");

        add(new JLabel("IP Servidor:"));
        add(txtIp);
        add(btnConectar);
        add(lblEstado);

        btnConectar.addActionListener(e -> alternarConexion());
    }

    // ── Conexión ─────────────────────────────────────────────────

    private void alternarConexion() {
        if (!controlando) {
            try {
                socket    = new Socket(txtIp.getText().trim(), 8080);
                salida    = new PrintWriter(socket.getOutputStream(), true);
                controlando = true;
                lblEstado.setText("Estado: Controlando 🟢");
                btnConectar.setText("Detener");
            } catch (Exception ex) {
                lblEstado.setText("Error de conexión 🔴");
            }
        } else {
            cerrarConexion();
        }
    }

    private void cerrarConexion() {
        controlando = false;
        try {
            if (salida != null) salida.println("LIBERAR");
            if (socket != null) socket.close();
        } catch (Exception ignored) {}
        socket  = null;
        salida  = null;
        lblEstado.setText("Estado: Desconectado 🔴");
        btnConectar.setText("Iniciar Control");
    }

    private void enviar(String mensaje) {
        if (controlando && salida != null) salida.println(mensaje);
    }

    // ── Mouse ────────────────────────────────────────────────────

    @Override
    public void nativeMouseMoved(NativeMouseEvent e) {
        if (!controlando) return;

        // Borde izquierdo = recuperar control en Arch
        if (e.getX() <= 2) {
            cerrarConexion();
            return;
        }
        enviar("A," + e.getX() + "," + e.getY());
    }

    @Override
    public void nativeMouseDragged(NativeMouseEvent e) {
        // Arrastrar también mueve el cursor en el servidor
        if (!controlando) return;
        enviar("A," + e.getX() + "," + e.getY());
    }

    @Override
    public void nativeMousePressed(NativeMouseEvent e) {
        int boton = nativeBtnToServidor(e.getButton());
        if (boton != -1) enviar("C,PRESIONAR," + boton);
    }

    @Override
    public void nativeMouseReleased(NativeMouseEvent e) {
        int boton = nativeBtnToServidor(e.getButton());
        if (boton != -1) enviar("C,LIBERAR," + boton);
    }

    @Override public void nativeMouseClicked(NativeMouseEvent e) {}

    private int nativeBtnToServidor(int btn) {
        if (btn == NativeMouseEvent.BUTTON1) return 1;
        if (btn == NativeMouseEvent.BUTTON2) return 3; // botón derecho
        return -1;
    }

    // ── Teclado ──────────────────────────────────────────────────

    @Override
    public void nativeKeyPressed(NativeKeyEvent e) {
        // Tecla de escape global: LeftAlt + F12 libera el control
        if (e.getKeyCode() == NativeKeyEvent.VC_F12
                && (e.getModifiers() & NativeKeyEvent.ALT_MASK) != 0) {
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
            // Silenciar logs verbosos de jnativehook
            java.util.logging.Logger hookLog =
                java.util.logging.Logger.getLogger(GlobalScreen.class.getPackage().getName());
            hookLog.setLevel(java.util.logging.Level.WARNING);
            hookLog.setUseParentHandlers(false);

            GlobalScreen.registerNativeHook();

            VentanaCliente cliente = new VentanaCliente();
            GlobalScreen.addNativeMouseListener(cliente);
            GlobalScreen.addNativeMouseMotionListener(cliente);
            GlobalScreen.addNativeKeyListener(cliente);   // ← teclado registrado

            SwingUtilities.invokeLater(() -> cliente.setVisible(true));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}