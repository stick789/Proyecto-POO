package negocio;

import entidades.Persona;
import entidades.Usuario;

/**
 * PersonaControl centraliza las reglas de permisos por rol.
 *
 * Usuario y administrador pueden agendar, cancelar y reagendar citas.
 * Administrador además puede administrar historial y modificar usuarios.
 * Entrenador queda como rol reconocido, pero sin permisos sobre citas.
 *
 * Ahora lee los roles desde la base de datos a través de PersonaDAO.
 */
public class PersonaControl {

    public enum OperacionPersona {
        AGENDAR_CITA,
        CANCELAR_CITA,
        REAGENDAR_CITA,
        ADMINISTRAR_HISTORIAL,
        MODIFICAR_USUARIO,
        ELIMINAR_USUARIO,
        LISTAR_USUARIOS
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
     * Si la persona es Usuario, obtiene el rol desde la BD.
     * En otros casos, usa getRol() de la clase.
     */
    private String obtenerRol(Persona persona) {
        if (persona == null) {
            return "";
        }
        if (persona instanceof Usuario) {
            Usuario usuario = (Usuario) persona;
            if (usuario.getRolBD() != null) {
                return usuario.getRolBD();
            }
        }
        return persona.getRol();
    }

    public boolean puedeRealizar(Persona persona, OperacionPersona operacion) {
        if (persona == null || operacion == null) {
            return false;
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

    public boolean puedeAgendarCitas(Persona persona) {
        return puedeRealizar(persona, OperacionPersona.AGENDAR_CITA);
    }

    public boolean puedeCancelarCitas(Persona persona) {
        return puedeRealizar(persona, OperacionPersona.CANCELAR_CITA);
    }

    public boolean puedeReagendarCitas(Persona persona) {
        return puedeRealizar(persona, OperacionPersona.REAGENDAR_CITA);
    }

    public boolean puedeAdministrarHistorial(Persona persona) {
        return puedeRealizar(persona, OperacionPersona.ADMINISTRAR_HISTORIAL);
    }

    public boolean puedeModificarUsuario(Persona persona) {
        return puedeRealizar(persona, OperacionPersona.MODIFICAR_USUARIO);
    }

    public boolean puedeEliminarUsuario(Persona persona) {
        return puedeRealizar(persona, OperacionPersona.ELIMINAR_USUARIO);
    }

    public boolean puedeListarUsuarios(Persona persona) {
        return puedeRealizar(persona, OperacionPersona.LISTAR_USUARIOS);
    }
}