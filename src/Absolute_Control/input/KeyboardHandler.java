package Absolute_Control.input;

import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public class KeyboardHandler implements NativeKeyListener {

    private final Consumer<String> onEnviar;
    private final Runnable         onDesconectar;

    private volatile boolean controlando  = false;
    private volatile boolean shiftActivo  = false;
    private volatile boolean altGrActivo  = false;

    private final Set<Integer> teclasPresionadas = new HashSet<>();

    public KeyboardHandler(Consumer<String> onEnviar, Runnable onDesconectar) {
        this.onEnviar      = onEnviar;
        this.onDesconectar = onDesconectar;
    }

    public void setControlando(boolean controlando) {
        this.controlando = controlando;
    }

    private boolean esModificador(int raw) {
        return raw == 160 || raw == 161
            || raw == 162 || raw == 163
            || raw == 164 || raw == 165
            || raw == 91  || raw == 92;
    }

    private char oemRawToChar(int raw) {
        if (altGrActivo) {
            return switch (raw) {
                case 219 -> '[';
                case 221 -> ']';
                case 222 -> '{';
                case 220 -> '|';
                case 187 -> '~';
                case 50  -> '@';
                case 51  -> '#';
                case 53  -> '€';
                default  -> 0;
            };
        }
        if (shiftActivo) {
            return switch (raw) {
                case 192 -> 'Ñ';
                case 186 -> 'Ñ';
                case 187 -> '*';
                case 188 -> ';';
                case 189 -> '_';
                case 190 -> ':';
                case 191 -> '¡';
                case 219 -> '¿';
                case 220 -> '°';
                case 221 -> 'ª';
                case 222 -> '¨';
                case 161 -> '?';
                default  -> 0;
            };
        }
        return switch (raw) {
            case 192 -> 'ñ';
            case 186 -> 'ñ';
            case 187 -> '+';
            case 188 -> ',';
            case 189 -> '-';
            case 190 -> '.';
            case 191 -> ']';
            case 219 -> '?';
            case 220 -> '|';
            case 221 -> '¿';
            case 222 -> '´';
            case 161 -> ';';
            default  -> 0;
        };
    }

    @Override
    public void nativeKeyPressed(NativeKeyEvent e) {
        int raw = e.getRawCode();

        if (raw == 160 || raw == 161) shiftActivo = true;
        if (raw == 165)               altGrActivo = true;

        if (e.getKeyCode() == NativeKeyEvent.VC_ESCAPE && controlando) {
            onDesconectar.run();
            return;
        }
        if (!controlando) return;

        if (esModificador(raw)) {
            onEnviar.accept("K,PRESIONAR," + e.getKeyCode());
            return;
        }

        char c = e.getKeyChar();
        boolean tieneChar = (c != NativeKeyEvent.CHAR_UNDEFINED && !Character.isISOControl(c));

        if (tieneChar) {
            teclasPresionadas.add(raw);
        } else {
            char oemChar = oemRawToChar(raw);
            if (oemChar != 0) {
                onEnviar.accept("T," + oemChar);
            } else {
                onEnviar.accept("K,PRESIONAR," + e.getKeyCode());
            }
        }
    }

    @Override
    public void nativeKeyReleased(NativeKeyEvent e) {
        int raw = e.getRawCode();

        if (raw == 160 || raw == 161) shiftActivo = false;
        if (raw == 165)               altGrActivo = false;

        if (!controlando) return;

        if (esModificador(raw)) {
            onEnviar.accept("K,LIBERAR," + e.getKeyCode());
            return;
        }

        char c = e.getKeyChar();
        boolean tieneChar = (c != NativeKeyEvent.CHAR_UNDEFINED && !Character.isISOControl(c));

        if (!tieneChar) {
            char oemChar = oemRawToChar(raw);
            if (oemChar == 0) onEnviar.accept("K,LIBERAR," + e.getKeyCode());
        }
        teclasPresionadas.remove(raw);
    }

    @Override
    public void nativeKeyTyped(NativeKeyEvent e) {
        if (!controlando) return;
        char c = e.getKeyChar();
        if (c == NativeKeyEvent.CHAR_UNDEFINED || Character.isISOControl(c)) return;
        if (teclasPresionadas.remove(e.getRawCode())) {
            onEnviar.accept("T," + c);
        }
    }
}