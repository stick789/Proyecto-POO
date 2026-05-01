package negocio;

import entidades.Persona;

/**
 * PersonaControl centraliza las reglas de permisos por rol.
 *
 * Usuario y administrador pueden agendar, cancelar y reagendar citas.
 * Administrador además puede administrar historial y modificar usuarios.
 * Entrenador queda como rol reconocido, pero sin permisos sobre citas.
 *
 *  Lee los roles desde la base de datos a través de PersonaDAO.
 */
public class PersonaControl {

    public enum OperacionPersona {
        AGENDAR_CITA,
        CANCELAR_CITA,
        REAGENDAR_CITA,
        ADMINISTRAR_HISTORIAL,
        MODIFICAR_USUARIO,
        ELIMINAR_USUARIO,
        LISTAR_USUARIOS,
        ASIGNAR_TURNO_ENTRENADOR
    }

    public boolean esUsuario(Persona persona) {
        if (persona == null) return false;
        String rol = obtenerRol(persona);
        return "USUARIO".equalsIgnoreCase(rol);
    }

    public boolean esEntrenador(Persona persona) {
        if (persona == null) return false;
        String rol = obtenerRol(persona);
        return "ENTRENADOR".equalsIgnoreCase(rol);
    }

    public boolean esAdministrador(Persona persona) {
        if (persona == null) return false;
        String rol = obtenerRol(persona);
        return "ADMINISTRADOR".equalsIgnoreCase(rol);
    }

    /**
     * Obtiene el rol de una persona, priorizando el valor desde BD.
     * Delegamos en la propia instancia para mantener el comportamiento polimórfico.
     */
    private String obtenerRol(Persona persona) {
        if (persona == null) {
            return "";
        }
        return persona.getRol();
    }

    public boolean puedeRealizar(Persona persona, OperacionPersona operacion) {
        if (persona == null || operacion == null) {
            throw new IllegalArgumentException("La persona y la operación no pueden ser null.");
        }

        switch (operacion) {
            case AGENDAR_CITA:
            case CANCELAR_CITA:
            case REAGENDAR_CITA:
                return esUsuario(persona) || esAdministrador(persona);

            case ADMINISTRAR_HISTORIAL:
            case MODIFICAR_USUARIO:
            case ELIMINAR_USUARIO:
            case LISTAR_USUARIOS:
            case ASIGNAR_TURNO_ENTRENADOR:
                return esAdministrador(persona);

            default:
                return false;
        }
    }

    public void validarOperacion(Persona persona, OperacionPersona operacion) {
        if (!puedeRealizar(persona, operacion)) {
            throw new IllegalAccessError("La persona no tiene permiso para realizar la operación: " + operacion);
        }
    }
}