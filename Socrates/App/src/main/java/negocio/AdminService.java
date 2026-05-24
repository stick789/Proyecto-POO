package negocio;

import dao.*;
import entidades.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.LinkedHashMap;

import database.Conexion;

/**
 * AdminService — Lógica de negocio exclusiva del administrador.
 *
 * Orquesta las operaciones admin: desactivar usuario (con cascada de turnos),
 * cancelar turno como admin, editar capacidad de instalación, cerrar instalación,
 * editar sede, y agregar auditoría en cada operación.
 */
@SuppressWarnings("unused")
public class AdminService {

    private final PersonaDAO      personaDAO;
    private final TurnoDAO        turnoDAO;
    private final InstalacionDAO  instalacionDAO;
    private final SedeDAO         sedeDAO;
    private final AuditLogDAO     auditLogDAO;
    private final Conexion        conexion;

    public AdminService() {
        this.personaDAO     = new PersonaDAO();
        this.turnoDAO       = new TurnoDAO(new PersonaDAO(), new InstalacionDAO(), new EntrenadorDAO());
        this.instalacionDAO = new InstalacionDAO();
        this.sedeDAO        = new SedeDAO();
        this.auditLogDAO    = new AuditLogDAO();
        this.conexion       = Conexion.getInstancia();
    }

    // ─── USUARIOS ────────────────────────────────────────────────────────────

    /**
     * Desactiva un usuario y cancela todos sus turnos futuros RESERVADOS.
     * Registra la acción en audit_log.
     *
     * @param adminId  ID del admin que realiza la acción.
     * @param usuarioId ID del usuario a desactivar.
     * @return número de turnos cancelados en cascada.
     */
    public int desactivarUsuario(int adminId, int usuarioId) {
        // 1. Desactivar cuenta
        boolean ok = personaDAO.desactivar(usuarioId);
        if (!ok) throw new RuntimeException("No se pudo desactivar el usuario id=" + usuarioId);

        // 2. Cancelar turnos futuros RESERVADOS del usuario
        int turnosCancelados = cancelarTurnosFuturosDeUsuario(adminId, usuarioId);

        // 3. Auditoría
        auditLogDAO.registrar(adminId, "usuario_desactivado",
                "Cuenta desactivada. Turnos cancelados en cascada: " + turnosCancelados,
                usuarioId, "USUARIO");

        return turnosCancelados;
    }

    /**
     * Reactiva un usuario previamente desactivado.
     */
    public void activarUsuario(int adminId, int usuarioId) {
        boolean ok = personaDAO.activar(usuarioId);
        if (!ok) throw new RuntimeException("No se pudo activar el usuario id=" + usuarioId);
        auditLogDAO.registrar(adminId, "usuario_activado",
                "Cuenta reactivada por administrador.", usuarioId, "USUARIO");
    }

    /**
     * Cambia el rol de un usuario (ej: USUARIO → ADMINISTRADOR).
     */
    public void cambiarRol(int adminId, int usuarioId, String nuevoRol) {
        personaDAO.actualizarRol(usuarioId, nuevoRol);
        auditLogDAO.registrar(adminId, "rol_cambiado",
                "Rol cambiado a: " + nuevoRol, usuarioId, "USUARIO");
    }

    /**
     * Actualiza categoría y estado de afiliación de un usuario.
     */
    public void editarUsuario(int adminId, Usuario usuario) {
        personaDAO.actualizar(usuario);
        auditLogDAO.registrar(adminId, "usuario_editado",
                "Datos editados: categoría=" + usuario.getCategoria(), usuario.getId(), "USUARIO");
    }

    /**
     * Elimina físicamente un usuario del sistema.
     */
    public void eliminarUsuario(int adminId, int usuarioId) {
        // Primero cancelar turnos activos
        cancelarTurnosFuturosDeUsuario(adminId, usuarioId);
        personaDAO.eliminar(usuarioId);
        auditLogDAO.registrar(adminId, "usuario_eliminado",
                "Usuario eliminado físicamente del sistema.", usuarioId, "USUARIO");
    }

    // ─── TURNOS ──────────────────────────────────────────────────────────────

    /**
     * Cancela un turno específico como administrador y libera el cupo.
     */
    public void cancelarTurnoComoAdmin(int adminId, int turnoId) {
        Connection con = conexion.conectar();
        if (con == null) throw new RuntimeException("Sin conexión a BD");
        String sql = "UPDATE turno SET estado = 'CANCELADO', cancelado_por = ?, cancelado_en = NOW() " +
                     "WHERE idTurno = ? AND estado = 'RESERVADO'";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, adminId);
            ps.setInt(2, turnoId);
            int rows = ps.executeUpdate();
            if (rows == 0) throw new RuntimeException("Turno no encontrado o ya cancelado.");
        } catch (SQLException e) {
            throw new RuntimeException("Error al cancelar turno id=" + turnoId, e);
        } finally {
            conexion.desconectar();
        }
        // Liberar cupo en instalación
        liberarCupoTurno(turnoId);

        auditLogDAO.registrar(adminId, "turno_cancelado_admin",
                "Turno cancelado por administrador.", turnoId, "TURNO");
    }

    /**
     * Crea un turno como administrador, ignorando (override) el límite de capacidad.
     * Marca el turno con admin_override = 1.
     */
    public void crearTurnoConOverride(int adminId, Turno turno) {
        // Insertar sin validar cupo (override)
        Connection con = conexion.conectar();
        if (con == null) throw new RuntimeException("Sin conexión a BD");
        String sql = "INSERT INTO turno (fechaHora, duracionMinutos, id_usuario, id_instalacion, " +
                     "numero_carril_asignado, estado, id_entrenador, admin_override) " +
                     "VALUES (?, ?, ?, ?, ?, 'RESERVADO', ?, 1)";
        try (PreparedStatement ps = con.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            ps.setTimestamp(1, java.sql.Timestamp.valueOf(turno.getFechaHora()));
            ps.setInt(2, turno.getDuracionMinutos());
            ps.setInt(3, turno.getUsuario().getId());
            ps.setInt(4, turno.getInstalacion().getIdInstalacion());
            if (turno.getNumeroCarrilAsignado() != null) ps.setInt(5, turno.getNumeroCarrilAsignado());
            else ps.setNull(5, java.sql.Types.INTEGER);
            if (turno.getIdEntrenador() != null) ps.setInt(6, turno.getIdEntrenador());
            else ps.setNull(6, java.sql.Types.INTEGER);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) turno.setIdTurno(keys.getInt(1));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al crear turno con override", e);
        } finally {
            conexion.desconectar();
        }
        auditLogDAO.registrar(adminId, "turno_creado_override",
                "Turno creado con override de capacidad en instalación id=" +
                turno.getInstalacion().getIdInstalacion(), turno.getIdTurno(), "TURNO");
    }

    // ─── INSTALACIONES ────────────────────────────────────────────────────────

    /**
     * Edita la capacidad máxima de una instalación. Actualiza en BD y registra auditoría.
     */
    public void editarCapacidadInstalacion(int adminId, int instalacionId, int nuevaCapacidad) {
        Connection con = conexion.conectar();
        if (con == null) throw new RuntimeException("Sin conexión a BD");
        String sql = "UPDATE instalacion SET capacidadMaxima = ? WHERE idInstalacion = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, nuevaCapacidad);
            ps.setInt(2, instalacionId);
            int rows = ps.executeUpdate();
            if (rows == 0) throw new RuntimeException("Instalación id=" + instalacionId + " no encontrada.");
        } catch (SQLException e) {
            throw new RuntimeException("Error al editar capacidad instalación id=" + instalacionId, e);
        } finally {
            conexion.desconectar();
        }
        auditLogDAO.registrar(adminId, "capacidad_editada",
                "Nueva capacidad máxima: " + nuevaCapacidad, instalacionId, "INSTALACION");
    }

    /**
     * Cierra (o reabre) una instalación. Cerrada = todos los turnos nuevos son rechazados.
     */
    public void toggleCerrarInstalacion(int adminId, int instalacionId, boolean cerrar) {
        Connection con = conexion.conectar();
        if (con == null) throw new RuntimeException("Sin conexión a BD");
        String sql = "UPDATE instalacion SET cerrada = ? WHERE idInstalacion = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, cerrar ? 1 : 0);
            ps.setInt(2, instalacionId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al cambiar estado de instalación", e);
        } finally {
            conexion.desconectar();
        }
        String accion = cerrar ? "instalacion_cerrada" : "instalacion_reabierta";
        auditLogDAO.registrar(adminId, accion,
                (cerrar ? "Instalación cerrada." : "Instalación reabierta."), instalacionId, "INSTALACION");
    }

    /**
     * Edita nombre y capacidad de instalación.
     */
    public void editarInstalacion(int adminId, int instalacionId, String nombre, int capacidad) {
        Connection con = conexion.conectar();
        if (con == null) throw new RuntimeException("Sin conexión a BD");
        String sql = "UPDATE instalacion SET nombre = ?, capacidadMaxima = ? WHERE idInstalacion = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nombre);
            ps.setInt(2, capacidad);
            ps.setInt(3, instalacionId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al editar instalación id=" + instalacionId, e);
        } finally {
            conexion.desconectar();
        }
        auditLogDAO.registrar(adminId, "instalacion_editada",
                "Nombre: " + nombre + ", Capacidad: " + capacidad, instalacionId, "INSTALACION");
    }

    // ─── SEDES ────────────────────────────────────────────────────────────────

    /**
     * Actualiza los datos de una sede.
     */
    public void editarSede(int adminId, Sede sede) {
        Connection con = conexion.conectar();
        if (con == null) throw new RuntimeException("Sin conexión a BD");
        String sql = "UPDATE sede SET nombre = ?, direccion = ?, telefono = ?, email = ? WHERE idSede = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, sede.getNombre());
            ps.setString(2, sede.getDireccion());
            ps.setString(3, sede.getTelefono());
            ps.setString(4, sede.getEmail());
            ps.setInt(5, sede.getIdSede());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al editar sede id=" + sede.getIdSede(), e);
        } finally {
            conexion.desconectar();
        }
        auditLogDAO.registrar(adminId, "sede_editada",
                "Sede actualizada: " + sede.getNombre(), sede.getIdSede(), "SEDE");
    }

    // ─── ESTADÍSTICAS ─────────────────────────────────────────────────────────

    /**
     * Retorna estadísticas globales desde la vista v_estadisticas_admin.
     * Keys: totalUsuariosActivos, turnosHoy, turnosFuturosActivos,
     *       totalEntrenadores, cancelacionesMes, ingresosMes
     */
    public Map<String, String> obtenerEstadisticas() {
        Map<String, String> stats = new LinkedHashMap<>();
        Connection con = conexion.conectar();
        if (con == null) {
            stats.put("totalUsuariosActivos", "—");
            stats.put("turnosHoy", "—");
            stats.put("turnosFuturosActivos", "—");
            stats.put("totalEntrenadores", "—");
            stats.put("cancelacionesMes", "—");
            stats.put("ingresosMes", "—");
            return stats;
        }
        try (PreparedStatement ps = con.prepareStatement("SELECT * FROM v_estadisticas_admin");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                stats.put("totalUsuariosActivos",  rs.getString("total_usuarios_activos"));
                stats.put("turnosHoy",             rs.getString("turnos_hoy"));
                stats.put("turnosFuturosActivos",  rs.getString("turnos_futuros_activos"));
                stats.put("totalEntrenadores",     rs.getString("total_entrenadores"));
                stats.put("cancelacionesMes",      rs.getString("cancelaciones_mes"));
                stats.put("ingresosMes",           rs.getString("ingresos_mes"));
            }
        } catch (SQLException e) {
            // Vista puede no existir aún — fallback a consultas individuales
            stats.put("totalUsuariosActivos", contarSimple("SELECT COUNT(*) FROM usuarios WHERE activo=1"));
            stats.put("turnosHoy",            contarSimple("SELECT COUNT(*) FROM turno WHERE DATE(fechaHora)=CURDATE()"));
            stats.put("turnosFuturosActivos", contarSimple("SELECT COUNT(*) FROM turno WHERE estado='RESERVADO' AND fechaHora>=NOW()"));
            stats.put("totalEntrenadores",    contarSimple("SELECT COUNT(*) FROM entrenador"));
            stats.put("cancelacionesMes",     "—");
            stats.put("ingresosMes",          "—");
        } finally {
            conexion.desconectar();
        }
        return stats;
    }

    // ─── PRIVADOS ─────────────────────────────────────────────────────────────

    /**
     * Cancela todos los turnos RESERVADOS futuros de un usuario.
     * @return número de turnos cancelados.
     */
    private int cancelarTurnosFuturosDeUsuario(int adminId, int usuarioId) {
        Connection con = conexion.conectar();
        if (con == null) return 0;
        String sql = "UPDATE turno SET estado = 'CANCELADO', cancelado_por = ?, cancelado_en = NOW() " +
                     "WHERE id_usuario = ? AND estado = 'RESERVADO' AND fechaHora >= NOW()";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, adminId);
            ps.setInt(2, usuarioId);
            return ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[AdminService] Error cancelando turnos de usuario " + usuarioId + ": " + e.getMessage());
            return 0;
        } finally {
            conexion.desconectar();
        }
    }

    /** Libera el cupo en instalación cuando se cancela un turno. */
    private void liberarCupoTurno(int turnoId) {
        Connection con = conexion.conectar();
        if (con == null) return;
        String sql = "UPDATE instalacion i " +
                     "JOIN turno t ON i.idInstalacion = t.id_instalacion " +
                     "SET i.aforoActual = i.aforoActual + 1 " +
                     "WHERE t.idTurno = ? AND i.aforoActual < i.capacidadMaxima";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, turnoId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[AdminService] Error liberando cupo turno " + turnoId + ": " + e.getMessage());
        } finally {
            conexion.desconectar();
        }
    }

    private String contarSimple(String sql) {
        Connection con = conexion.conectar();
        if (con == null) return "—";
        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getString(1) : "0";
        } catch (SQLException e) {
            return "—";
        } finally {
            conexion.desconectar();
        }
    }
}