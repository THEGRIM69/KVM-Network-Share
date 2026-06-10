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

                    if (p[0].equals("A") && p.length == 3) {
                        int x = Integer.parseInt(p[1]);
                        int y = Integer.parseInt(p[2]);
                        robot.mouseMove(x, y);
                    }

                    else if (p[0].equals("C") && p.length == 3) {
                        int boton   = Integer.parseInt(p[2]);
                        int mascara = (boton == 1)
                                ? InputEvent.BUTTON1_DOWN_MASK
                                : InputEvent.BUTTON3_DOWN_MASK;
                        if (p[1].equals("PRESIONAR")) robot.mousePress(mascara);
                        else if (p[1].equals("LIBERAR")) robot.mouseRelease(mascara);
                    }

                    else if (p[0].equals("K") && p.length == 3) {
                        int rawCode = Integer.parseInt(p[2]);
                        int keyCode = convertirKeyCode(rawCode);
                        if (keyCode != -1) {
                            try {
                                if (p[1].equals("PRESIONAR")) robot.keyPress(keyCode);
                                else if (p[1].equals("LIBERAR")) robot.keyRelease(keyCode);
                            } catch (IllegalArgumentException ex) {
                                System.out.println("Keycode no ejecutable: " + rawCode + " → " + keyCode);
                            }
                        } else {
                            System.out.println("Keycode sin mapear: " + rawCode);
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

    private static int convertirKeyCode(int code) {
        return switch (code) {
            // Letras
            case 30 -> KeyEvent.VK_A;
            case 48 -> KeyEvent.VK_B;
            case 46 -> KeyEvent.VK_C;
            case 32 -> KeyEvent.VK_D;
            case 18 -> KeyEvent.VK_E;
            case 33 -> KeyEvent.VK_F;
            case 34 -> KeyEvent.VK_G;
            case 35 -> KeyEvent.VK_H;
            case 23 -> KeyEvent.VK_I;
            case 36 -> KeyEvent.VK_J;
            case 37 -> KeyEvent.VK_K;
            case 38 -> KeyEvent.VK_L;
            case 50 -> KeyEvent.VK_M;
            case 49 -> KeyEvent.VK_N;
            case 24 -> KeyEvent.VK_O;
            case 25 -> KeyEvent.VK_P;
            case 16 -> KeyEvent.VK_Q;
            case 19 -> KeyEvent.VK_R;
            case 31 -> KeyEvent.VK_S;
            case 20 -> KeyEvent.VK_T;
            case 22 -> KeyEvent.VK_U;
            case 47 -> KeyEvent.VK_V;
            case 17 -> KeyEvent.VK_W;
            case 45 -> KeyEvent.VK_X;
            case 21 -> KeyEvent.VK_Y;
            case 44 -> KeyEvent.VK_Z;
            // Números
            case 11 -> KeyEvent.VK_0;
            case 2  -> KeyEvent.VK_1;
            case 3  -> KeyEvent.VK_2;
            case 4  -> KeyEvent.VK_3;
            case 5  -> KeyEvent.VK_4;
            case 6  -> KeyEvent.VK_5;
            case 7  -> KeyEvent.VK_6;
            case 8  -> KeyEvent.VK_7;
            case 9  -> KeyEvent.VK_8;
            case 10 -> KeyEvent.VK_9;
            // Especiales
            case 14   -> KeyEvent.VK_BACK_SPACE;
            case 15   -> KeyEvent.VK_TAB;
            case 28   -> KeyEvent.VK_ENTER;
            case 1    -> KeyEvent.VK_ESCAPE;
            case 57   -> KeyEvent.VK_SPACE;
            case 58   -> KeyEvent.VK_CAPS_LOCK;
            // Modificadores
            case 42   -> KeyEvent.VK_SHIFT;
            case 54   -> KeyEvent.VK_SHIFT;
            case 29   -> KeyEvent.VK_CONTROL;
            case 3613 -> KeyEvent.VK_CONTROL;
            case 56   -> KeyEvent.VK_ALT;
            case 3640 -> KeyEvent.VK_ALT;
            case 3675 -> KeyEvent.VK_WINDOWS;
            // Puntuación — usando scancodes originales del cliente
            case 12  -> KeyEvent.VK_MINUS;
            case 13  -> KeyEvent.VK_EQUALS;
            case 26  -> KeyEvent.VK_OPEN_BRACKET;
            case 27  -> KeyEvent.VK_CLOSE_BRACKET;
            case 39  -> KeyEvent.VK_SEMICOLON;
            case 40  -> KeyEvent.VK_QUOTE;
            case 41  -> KeyEvent.VK_BACK_QUOTE;
            case 43  -> KeyEvent.VK_BACK_SLASH;
            case 51  -> KeyEvent.VK_COMMA;
            case 52  -> KeyEvent.VK_PERIOD;
            case 53  -> KeyEvent.VK_SLASH;
            // F1-F12
            case 59 -> KeyEvent.VK_F1;
            case 60 -> KeyEvent.VK_F2;
            case 61 -> KeyEvent.VK_F3;
            case 62 -> KeyEvent.VK_F4;
            case 63 -> KeyEvent.VK_F5;
            case 64 -> KeyEvent.VK_F6;
            case 65 -> KeyEvent.VK_F7;
            case 66 -> KeyEvent.VK_F8;
            case 67 -> KeyEvent.VK_F9;
            case 68 -> KeyEvent.VK_F10;
            case 87 -> KeyEvent.VK_F11;
            case 88 -> KeyEvent.VK_F12;
            // Flechas
            case 57419 -> KeyEvent.VK_LEFT;
            case 57416 -> KeyEvent.VK_UP;
            case 57421 -> KeyEvent.VK_RIGHT;
            case 57424 -> KeyEvent.VK_DOWN;
            // Navegación
            case 57415 -> KeyEvent.VK_HOME;
            case 57423 -> KeyEvent.VK_END;
            case 57417 -> KeyEvent.VK_PAGE_UP;
            case 57425 -> KeyEvent.VK_PAGE_DOWN;
            case 57426 -> KeyEvent.VK_INSERT;
            case 57427 -> KeyEvent.VK_DELETE;
            // Enter numpad
            case 3667 -> KeyEvent.VK_ENTER;
            default   -> -1;
        };
    }
}