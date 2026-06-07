import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class ServidorKVM {
    public static void main(String[] args) {
        try {
            Robot robot  = new Robot();
            ServerSocket servidor = new ServerSocket(8080);
            System.out.println("=== SERVIDOR KVM ENCENDIDO (puerto 8080) ===");

            while (true) {
                System.out.println("Esperando cliente...");
                Socket cliente = servidor.accept();
                System.out.println("¡Cliente conectado desde " + cliente.getInetAddress() + "!");

                BufferedReader entrada = new BufferedReader(
                        new InputStreamReader(cliente.getInputStream()));
                String linea;

                while ((linea = entrada.readLine()) != null) {

                    if (linea.equals("LIBERAR")) {
                        System.out.println("Cliente liberó el control.");
                        break;
                    }

                    String[] p = linea.split(",");

                    // ── Mouse absoluto: "A,X,Y" ───────────────────────
                    if (p[0].equals("A") && p.length == 3) {
                        int x = Integer.parseInt(p[1]);
                        int y = Integer.parseInt(p[2]);
                        robot.mouseMove(x, y);
                    }

                    // ── Clics: "C,PRESIONAR|LIBERAR,BOTON" ───────────
                    else if (p[0].equals("C") && p.length == 3) {
                        int boton   = Integer.parseInt(p[2]);
                        int mascara = (boton == 1)
                                ? InputEvent.BUTTON1_DOWN_MASK
                                : InputEvent.BUTTON3_DOWN_MASK;

                        if (p[1].equals("PRESIONAR")) robot.mousePress(mascara);
                        else if (p[1].equals("LIBERAR")) robot.mouseRelease(mascara);
                    }

                    // ── Teclado: "K,PRESIONAR|LIBERAR,KEYCODE" ────────
                    else if (p[0].equals("K") && p.length == 3) {
                        int keyCode = convertirKeyCode(Integer.parseInt(p[2]));
                        if (keyCode != -1) {
                            try {
                                if (p[1].equals("PRESIONAR")) robot.keyPress(keyCode);
                                else if (p[1].equals("LIBERAR")) robot.keyRelease(keyCode);
                            } catch (IllegalArgumentException ex) {
                                // Ignorar keycodes que Robot no puede ejecutar
                                System.out.println("Keycode no soportado: " + keyCode);
                            }
                        }
                    }
                }

                entrada.close();
                cliente.close();
                System.out.println("Conexión cerrada.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Convierte el keycode de jnativehook (usado en Arch/Linux)
     * al keycode de java.awt.event.KeyEvent (usado por Robot en Windows).
     *
     * La mayoría coincide directamente, pero algunas teclas especiales difieren.
     */
    private static int convertirKeyCode(int nativeCode) {
        // jnativehook usa los mismos valores que KeyEvent para la mayoría
        // Solo necesitamos mapear los que difieren:
        return switch (nativeCode) {
            case 3232 -> KeyEvent.VK_LEFT;       // VC_LEFT
            case 3233 -> KeyEvent.VK_RIGHT;      // VC_RIGHT
            case 3234 -> KeyEvent.VK_UP;         // VC_UP
            case 3235 -> KeyEvent.VK_DOWN;       // VC_DOWN
            case 3584 -> KeyEvent.VK_HOME;
            case 3585 -> KeyEvent.VK_END;
            case 3586 -> KeyEvent.VK_PAGE_UP;
            case 3587 -> KeyEvent.VK_PAGE_DOWN;
            case 3655 -> KeyEvent.VK_INSERT;
            case 3657 -> KeyEvent.VK_DELETE;
            case 57421 -> KeyEvent.VK_WINDOWS;   // Super key
            default   -> nativeCode;             // La mayoría pasan directo
        };
    }
}