package socratesGui;

import entidades.Persona;

/**
 * SesionActual — Almacena el usuario autenticado durante la sesión.
 *
 * Patrón simple de sesión: los controladores escriben y leen aquí
 * sin acoplarse entre sí. Se limpia al hacer logout.
 */
public class SesionActual {

    private static Persona usuarioActual;

    private SesionActual() {}

    public static Persona getUsuario() {
        return usuarioActual;
    }

    public static void setUsuario(Persona persona) {
        usuarioActual = persona;
    }

    public static boolean haySesion() {
        return usuarioActual != null;
    }

    public static void cerrar() {
        usuarioActual = null;
    }
}
