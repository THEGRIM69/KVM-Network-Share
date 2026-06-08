import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Cliente KVM — Lee /dev/input/eventX directo (funciona en Wayland).
 * Protocolo con el servidor:
 *   A,X,Y        → mover mouse (absoluto)
 *   C,PRESIONAR,boton
 *   C,LIBERAR,boton
 *   K,PRESIONAR,keycode
 *   K,LIBERAR,keycode
 *   LIBERAR       → soltar control
 */
public class VentanaClienteLinux extends JFrame {

    // ── Config ────────────────────────────────────────────────────
    private static final String DEVICE_MOUSE   = "/dev/input/event10";
    private static final int    PUERTO         = 8080;
    private static final int    ANCHO_PANTALLA = 1366;
    private static final int    ALTO_PANTALLA  = 768;
    private static final int    BORDE_DERECHO  = ANCHO_PANTALLA - 2;

    // ── Estado ────────────────────────────────────────────────────
    private Socket      socket;
    private PrintWriter salida;
    private volatile boolean controlando = false;

    // Posición acumulada del mouse
    private volatile int mouseX = ANCHO_PANTALLA / 2;
    private volatile int mouseY = ALTO_PANTALLA  / 2;

    // ── UI ───────────────────────────────────────────────────────
    private JTextField txtIp;
    private JButton    btnConectar;
    private JLabel     lblEstado;

    public VentanaClienteLinux() {
        setTitle("KVM Cliente - Arch Linux");
        setSize(420, 120);
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
                socket      = new Socket(txtIp.getText().trim(), PUERTO);
                salida      = new PrintWriter(socket.getOutputStream(), true);
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

    void cerrarConexion() {
        controlando = false;
        SwingUtilities.invokeLater(() -> {
            lblEstado.setText("Estado: Desconectado 🔴");
            btnConectar.setText("Iniciar Control");
        });
        try {
            if (salida != null) salida.println("LIBERAR");
            if (socket != null) socket.close();
        } catch (Exception ignored) {}
        socket = null;
        salida = null;
    }

    void enviar(String msg) {
        if (controlando && salida != null) salida.println(msg);
    }

    // ── Lector de /dev/input ──────────────────────────────────────
    /**
     * Estructura input_event Linux (24 bytes):
     *   8 bytes timeval, 2 bytes type, 2 bytes code, 4 bytes value
     */
    void iniciarLecturaDispositivo(String ruta) {
        Thread t = new Thread(() -> {
            try (FileInputStream fis = new FileInputStream(ruta)) {
                byte[] buf = new byte[24];
                while (true) {
                    int leidos = 0;
                    while (leidos < 24) {
                        int r = fis.read(buf, leidos, 24 - leidos);
                        if (r < 0) return;
                        leidos += r;
                    }
                    ByteBuffer bb = ByteBuffer.wrap(buf).order(ByteOrder.LITTLE_ENDIAN);
                    bb.getLong(); // timeval — ignorar
                    int type  = bb.getShort() & 0xFFFF;
                    int code  = bb.getShort() & 0xFFFF;
                    int value = bb.getInt();

                    procesarEvento(type, code, value);
                }
            } catch (Exception e) {
                System.out.println("Error leyendo " + ruta + ": " + e.getMessage());
            }
        });
        t.setDaemon(true);
        t.start();
    }

    /**
     * EV_REL=2 (movimiento relativo), EV_KEY=1 (teclas/botones)
     * REL_X=0, REL_Y=1
     * BTN_LEFT=272, BTN_RIGHT=273, BTN_MIDDLE=274
     */
    private void procesarEvento(int type, int code, int value) {
        if (type == 2) { // EV_REL
            if (code == 0) { // REL_X
                mouseX = Math.max(0, Math.min(ANCHO_PANTALLA, mouseX + value));

                // Borde derecho → activar control automáticamente
                if (!controlando && mouseX >= BORDE_DERECHO) {
                    SwingUtilities.invokeLater(this::alternarConexion);
                }

                if (controlando) enviar("A," + mouseX + "," + mouseY);

            } else if (code == 1) { // REL_Y
                mouseY = Math.max(0, Math.min(ALTO_PANTALLA, mouseY + value));
                if (controlando) enviar("A," + mouseX + "," + mouseY);
            }

        } else if (type == 1) { // EV_KEY
            if (code == 272) { // BTN_LEFT
                enviar("C," + (value == 1 ? "PRESIONAR" : "LIBERAR") + ",1");
            } else if (code == 273) { // BTN_RIGHT
                enviar("C," + (value == 1 ? "PRESIONAR" : "LIBERAR") + ",3");
            } else if (code == 274) { // BTN_MIDDLE
                enviar("C," + (value == 1 ? "PRESIONAR" : "LIBERAR") + ",2");
            } else if (value == 1 && code == 1 && controlando) {
                // ESC → liberar control
                cerrarConexion();
            } else {
                // Teclas de teclado
                if (value == 1)      enviar("K,PRESIONAR," + code);
                else if (value == 0) enviar("K,LIBERAR,"   + code);
            }
        }
    }

    // ── Main ─────────────────────────────────────────────────────

    public static void main(String[] args) {
        VentanaClienteLinux cliente = new VentanaClienteLinux();
        cliente.iniciarLecturaDispositivo(DEVICE_MOUSE);
        SwingUtilities.invokeLater(() -> cliente.setVisible(true));
    }
}