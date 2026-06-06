import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.PrintWriter;
import java.net.Socket;

public class VentanaCliente extends JFrame { // Corregido JWindows por JFrame
    
    private JTextField campoIP;
    private JButton botonConectar;
    private JLabel etiquetaEstado;
    
    // Variables de red y control
    private Socket socket;
    private PrintWriter salida;
    private boolean enEjecucion = false;
    private Thread hiloKVM;
    
    // Nueva ventana para la barra invisible del borde
    private JWindow barraInvisible; 

    public VentanaCliente() {
        setTitle("KVM Controller");
        setSize(350, 200);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); 
        setLayout(new GridLayout(4, 1, 10, 10)); 

        JLabel titulo = new JLabel("🚀 KVM NETWORK SHARE", SwingConstants.CENTER);
        titulo.setFont(new Font("Arial", Font.BOLD, 16));
        add(titulo);

        JPanel panelIP = new JPanel();
        panelIP.add(new JLabel("IP del Receptor:"));
        campoIP = new JTextField("192.168.1.102", 15); // Cambiada a tu IP de Windows .104 por defecto
        panelIP.add(campoIP);
        add(panelIP);

        botonConectar = new JButton("Iniciar Control");
        add(botonConectar);

        etiquetaEstado = new JLabel("Estado: Desconectado 🔴", SwingConstants.CENTER);
        add(etiquetaEstado);

        botonConectar.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!enEjecucion) {
                    iniciarKVM(campoIP.getText());
                } else {
                    detenerKVM();
                }
            }
        });
    }

    private void iniciarKVM(String ip) {
        enEjecucion = true;
        etiquetaEstado.setText("Conectando... 🟡");
        botonConectar.setText("Detener");

        hiloKVM = new Thread(() -> {
            try {
                int puerto = 8080;
                socket = new Socket(ip, puerto);
                salida = new PrintWriter(socket.getOutputStream(), true);
                
                SwingUtilities.invokeLater(() -> {
                    etiquetaEstado.setText("¡Conectado y transmitiendo! 🟢");
                    crearBarraInvisible(); // Activamos el portal en el borde de la pantalla
                });
                
                Dimension tamañoPantalla = Toolkit.getDefaultToolkit().getScreenSize();
                int anchoMax = (int) tamañoPantalla.getWidth();
                int altoMax = (int) tamañoPantalla.getHeight();

                double pctX = 0.5;
                double pctY = 0.5;
                int ultimoX = anchoMax / 2;
                int ultimoY = altoMax / 2;

                Robot robotLocal = new Robot(); 
                boolean controlandoWindows = false;

                while (enEjecucion) {
                    Point puntoMouse = MouseInfo.getPointerInfo().getLocation();
                    int xActual = puntoMouse.x;
                    int yActual = puntoMouse.y;

                    // DETECCIÓN: Si toca el borde derecho, saltamos a Windows
                    if (!controlandoWindows && xActual >= (anchoMax - 2)) {
                        controlandoWindows = true;
                        ultimoX = anchoMax / 2; // Lo centramos virtualmente en la laptop
                        ultimoY = yActual;
                        robotLocal.mouseMove(ultimoX, ultimoY);
                        continue;
                    }


                    if (controlandoWindows) {
                        // ESCAPE: Si tiras el mouse bruscamente a la izquierda, regresas a Arch
                        if (xActual < (anchoMax / 2 - 150)) {
                            controlandoWindows = false;
                            salida.println("LIBERAR"); // Avisa al servidor que libere el mouse
                            Thread.sleep(100);
                            continue;
                        }

                        // Calculamos cuántos píxeles reales se movió tu mano en la laptop
                        int deltaX = xActual - ultimoX;
                        int deltaY = yActual - ultimoY;

                        if (deltaX != 0 || deltaY != 0) {
                            // Puedes cambiar esta sensibilidad (ej. 1.2 para más rápido, 0.8 para más lento)
                            double sensibilidad = 1.0; 
                            
                            int envioX = (int) (deltaX * sensibilidad);
                            int envioY = (int) (deltaY * sensibilidad);

                            // Enviamos los píxeles puros de movimiento directo
                            salida.println("P," + envioX + "," + envioY);
                        }

                        // Regresamos el mouse al centro virtual de la laptop para tener recorrido infinito
                        ultimoX = anchoMax / 2;
                        ultimoY = altoMax / 2; 
                        robotLocal.mouseMove(ultimoX, ultimoY);
                    }

                    // REEMPLAZA ESE FINAL CON ESTO:
                    Thread.sleep(10); // Sincronización limpia a 100Hz
                }

            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    etiquetaEstado.setText("Error de conexión 🔴");
                    detenerKVM();
                });
            }
        });

        hiloKVM.start(); 
    }

    // Crea el hilo invisible en el extremo derecho de tu monitor
    private void crearBarraInvisible() {
        Dimension tamaño = Toolkit.getDefaultToolkit().getScreenSize();
        barraInvisible = new JWindow();
        barraInvisible.setSize(2, (int) tamaño.getHeight());
        barraInvisible.setLocation((int) tamaño.getWidth() - 2, 0);
        barraInvisible.setAlwaysOnTop(true);
        
        // Hacerlo completamente transparente
        barraInvisible.setBackground(new Color(0, 0, 0, 1)); 
        barraInvisible.setVisible(true);
    }

    private void detenerKVM() {
        enEjecucion = false;
        botonConectar.setText("Iniciar Control");
        etiquetaEstado.setText("Estado: Desconectado 🔴");
        
        if (barraInvisible != null) {
            barraInvisible.dispose();
        }
        
        try {
            if (salida != null) salida.close();
            if (socket != null) socket.close();
            if (hiloKVM != null) hiloKVM.interrupt();
        } catch (Exception ex) {
            // Ignorar al cerrar
        }
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) { e.printStackTrace(); }

        SwingUtilities.invokeLater(() -> {
            new VentanaCliente().setVisible(true);
        });
    }
}