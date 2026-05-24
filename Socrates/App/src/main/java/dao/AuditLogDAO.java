package dao;

import database.Conexion;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * AuditLogDAO — Registra acciones de administrador en audit_log.
 *
 * Uso: AuditLogDAO.registrar(adminId, "usuario_desactivado", "Se desactivó al usuario #5", 5, "USUARIO");
 */
public class AuditLogDAO {

    private static final String SQL_INSERT =
        "INSERT INTO audit_log (admin_id, accion, descripcion, id_objetivo, tipo_objetivo) " +
        "VALUES (?, ?, ?, ?, ?)";

    private final Conexion conexion = Conexion.getInstancia();

    public void registrar(int adminId, String accion, String descripcion,
                          Integer idObjetivo, String tipoObjetivo) {
        Connection con = conexion.conectar();
        if (con == null) return;
        try (PreparedStatement ps = con.prepareStatement(SQL_INSERT)) {
            ps.setInt(1, adminId);
            ps.setString(2, accion);
            ps.setString(3, descripcion);
            if (idObjetivo != null) ps.setInt(4, idObjetivo);
            else                    ps.setNull(4, java.sql.Types.INTEGER);
            ps.setString(5, tipoObjetivo);
            ps.executeUpdate();
        } catch (SQLException e) {
            // Auditoría falla silenciosamente — no interrumpe la operación principal
            System.err.println("[AuditLog] Error al registrar: " + e.getMessage());
        } finally {
            conexion.desconectar();
        }
    }
}
