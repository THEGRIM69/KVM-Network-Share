import java.awt.Dimension;
import java.awt.Robot;
import java.awt.Toolkit;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class ServidorKVM {
    public static void main(String[] args) {
        int puerto = 8080;

        try {
            // 1. REDES: Iniciamos el servidor en el puerto 8080
            ServerSocket servidor = new ServerSocket(puerto);
            System.out.println("=== RECEPTOR KVM PROPORCIONAL INICIADO ===");
            System.out.println("Escuchando en el puerto " + puerto + "...");
            System.out.println("Esperando conexión de la PC Maestra...");

            // El programa se detiene aquí hasta que el cliente se conecte
            Socket cliente = servidor.accept(); 
            System.out.println("¡PC Maestra conectada desde: " + cliente.getInetAddress() + "!");

            // 2. SISTEMAS OPERATIVOS: Detectamos la resolución de ESTA pantalla automáticamente
            Dimension tamañoPantalla = Toolkit.getDefaultToolkit().getScreenSize();
            int anchoLocal = (int) tamañoPantalla.getWidth();
            int altoLocal = (int) tamañoPantalla.getHeight();
            System.out.println("Resolución de pantalla local detectada: " + anchoLocal + "x" + altoLocal);

            // Creamos el Robot que moverá el mouse real a nivel de Sistema Operativo
            Robot robot = new Robot();
            
            // Preparamos el lector de la red
            BufferedReader entrada = new BufferedReader(new InputStreamReader(cliente.getInputStream()));
            String comando;

            // 3. PROGRAMACIÓN 2: Bucle que procesa los datos optimizado para pantalla secundaria
            while ((comando = entrada.readLine()) != null) {
                
                // Si el cliente pide liberar el control, reiniciamos el estado en el servidor
                if (comando.equals("LIBERAR")) {
                    System.out.println("Control devuelto a la PC Maestra temporalmente...");
                    continue; 
                }

                // Formato esperado: "P,porcentajeX,porcentajeY"
                if (comando.startsWith("P")) {
                    try {
                        String[] partes = comando.split(",");
                        double porcX = Double.parseDouble(partes[1]);
                        double porcY = Double.parseDouble(partes[2]);

                        // Control de seguridad: Evitar que el mouse se salga de los límites lógicos
                        if (porcX < 0) porcX = 0; if (porcX > 1) porcX = 1;
                        if (porcY < 0) porcY = 0; if (porcY > 1) porcY = 1;

                        // Convertimos el porcentaje a los píxeles reales de Windows
                        int xFinal = (int) (porcX * anchoLocal);
                        int yFinal = (int) (porcY * altoLocal);

                        // El Robot ejecuta el movimiento de forma inmediata y directa
                        robot.mouseMove(xFinal, yFinal);
                    } catch (Exception ex) {
                        // Si un paquete de red llega corrupto, lo ignora y pasa al siguiente sin tumbar el programa
                    }
                }
            }

            System.out.println("La PC Maestra se ha desconectado.");
            entrada.close();
            cliente.close();
            servidor.close();

        } catch (Exception e) {
            System.out.println("Error en el servidor KVM: " + e.getMessage());
            e.printStackTrace();
        }
    }
}