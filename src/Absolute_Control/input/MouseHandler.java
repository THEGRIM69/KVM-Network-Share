package Absolute_Control.input;

import com.github.kwhat.jnativehook.mouse.NativeMouseEvent;
import com.github.kwhat.jnativehook.mouse.NativeMouseInputListener;
import com.github.kwhat.jnativehook.mouse.NativeMouseWheelEvent;
import com.github.kwhat.jnativehook.mouse.NativeMouseWheelListener;

import java.awt.*;
import java.util.function.Consumer;

public class MouseHandler implements NativeMouseInputListener, NativeMouseWheelListener {

    private static final int CENTRO_X = 683;
    private static final int CENTRO_Y = 384;

    private final Consumer<String> onEnviar;
    private final Runnable         onConectar;
    private final Runnable         onDesconectar;

    private volatile boolean controlando = false;
    private volatile boolean anclando    = false;
    private volatile boolean conectando  = false;

    private int bordeActivacion; // borde donde se activa el control
    private Robot robotLocal;

    public MouseHandler(Consumer<String> onEnviar, Runnable onConectar, Runnable onDesconectar) {
        this.onEnviar      = onEnviar;
        this.onConectar    = onConectar;
        this.onDesconectar = onDesconectar;
        try { robotLocal = new Robot(); } catch (AWTException ignored) {}
    }

    public void setControlando(boolean controlando) {
        this.controlando = controlando;
        if (controlando && robotLocal != null)
            robotLocal.mouseMove(CENTRO_X, CENTRO_Y);
    }

    /** true = servidor a la derecha, false = servidor a la izquierda */
    public void setLado(boolean derecha, int anchoPantalla) {
        bordeActivacion = derecha ? anchoPantalla - 2 : 2;
    }

    @Override
    public void nativeMouseMoved(NativeMouseEvent e) {
        if (anclando) return;

        if (controlando) {
            int dx = e.getX() - CENTRO_X;
            int dy = e.getY() - CENTRO_Y;
            anclando = true;
            if (robotLocal != null) robotLocal.mouseMove(CENTRO_X, CENTRO_Y);
            anclando = false;
            if (dx != 0 || dy != 0) onEnviar.accept("D," + dx + "," + dy);
            return;
        }

        boolean enBorde = bordeActivacion > 2
                ? e.getX() >= bordeActivacion
                : e.getX() <= bordeActivacion;

        if (!conectando && enBorde) {
            conectando = true;
            onConectar.run();
            conectando = false;
        }
    }

    @Override
    public void nativeMouseDragged(NativeMouseEvent e) {
        if (!controlando) return;
        int dx = e.getX() - CENTRO_X;
        int dy = e.getY() - CENTRO_Y;
        anclando = true;
        if (robotLocal != null) robotLocal.mouseMove(CENTRO_X, CENTRO_Y);
        anclando = false;
        if (dx != 0 || dy != 0) onEnviar.accept("D," + dx + "," + dy);
    }

    @Override
    public void nativeMousePressed(NativeMouseEvent e) {
        if (!controlando) return;
        int b = e.getButton() == NativeMouseEvent.BUTTON1 ? 1
              : e.getButton() == NativeMouseEvent.BUTTON2 ? 3 : -1;
        if (b != -1) onEnviar.accept("C,PRESIONAR," + b);
    }

    @Override
    public void nativeMouseReleased(NativeMouseEvent e) {
        if (!controlando) return;
        int b = e.getButton() == NativeMouseEvent.BUTTON1 ? 1
              : e.getButton() == NativeMouseEvent.BUTTON2 ? 3 : -1;
        if (b != -1) onEnviar.accept("C,LIBERAR," + b);
    }

    @Override public void nativeMouseClicked(NativeMouseEvent e) {}

    @Override
    public void nativeMouseWheelMoved(NativeMouseWheelEvent e) {
        if (!controlando) return;
        int delta = e.getWheelRotation();
        if (delta != 0) onEnviar.accept("W," + delta);
    }
}