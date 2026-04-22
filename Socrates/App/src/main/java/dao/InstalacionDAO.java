package dao;

import database.Conexion;
import entidades.Gimnasio;
import entidades.Instalacion;
import entidades.Piscina;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * InstalacionDAO — Acceso a datos de instalaciones (gimnasios y piscinas).
 *
 * OPERACIONES:
 *   • insertar(): Crea una nueva instalación (gimnasio o piscina).
 *   • buscarPorId(): Obtiene una instalación específica.
 *   • listarTodos(): Obtiene todas las instalaciones.
 *   • listarGimnasios(): Obtiene solo gimnasios.
 *   • listarPiscinas(): Obtiene solo piscinas.
 *   • actualizar(): Modifica datos de una instalación.
 *   • actualizarAforo(): Cambia los cupos disponibles.
 *   • eliminar(): Borra una instalación.
 *
 * ESTRUCTURA:
 *   Cada instalación puede ser un gimnasio o una piscina.
 */
public class InstalacionDAO implements IInstalacionDAO {

    private final Conexion conexion = Conexion.getInstancia();

    // ── SQL ────────────────────────────────────────────────────────────────────

    private static final String SQL_INSERT_INSTALACION =
            "INSERT INTO instalacion (tipo, capacidadMaxima, aforoActual) VALUES (?, ?, ?)";

    private static final String SQL_INSERT_GIMNASIO =
            "INSERT INTO gimnasio (idInstalacion, aforo_actual) VALUES (?, ?)";

    private static final String SQL_INSERT_PISCINA =
            "INSERT INTO piscina (idInstalacion, numeroCarriles, profundidad) VALUES (?, ?, ?)";

    /**
     * Consulta base que obtiene información de todas las instalaciones.
     * Incluye datos de piscinas cuando corresponde.
     */
    private static final String SQL_SELECT_BASE =
            "SELECT i.idInstalacion, i.tipo, i.capacidadMaxima, i.aforoActual, " +
            "       p.numeroCarriles, p.profundidad " +
            "FROM instalacion i " +
            "LEFT JOIN piscina p ON i.idInstalacion = p.idInstalacion ";

    private static final String SQL_SELECT_POR_ID  = SQL_SELECT_BASE + "WHERE i.idInstalacion = ?";
    private static final String SQL_SELECT_TODOS   = SQL_SELECT_BASE;
    private static final String SQL_SELECT_GIMNAS  = SQL_SELECT_BASE + "WHERE i.tipo = 'GIMNASIO'";
    private static final String SQL_SELECT_PISC    = SQL_SELECT_BASE + "WHERE i.tipo = 'PISCINA'";

    private static final String SQL_UPDATE_INSTALACION =
            "UPDATE instalacion SET capacidadMaxima = ?, aforoActual = ? WHERE idInstalacion = ?";

    private static final String SQL_UPDATE_PISCINA =
            "UPDATE piscina SET numeroCarriles = ?, profundidad = ? WHERE idInstalacion = ?";

    private static final String SQL_UPDATE_AFORO =
            "UPDATE instalacion SET aforoActual = ? WHERE idInstalacion = ?";

    private static final String SQL_DELETE =
            "DELETE FROM instalacion WHERE idInstalacion = ?";
    // gimnasio/piscina se borran solos por ON DELETE CASCADE

    // ── Implementación ─────────────────────────────────────────────────────────

    @Override
    public void insertar(Instalacion instalacion) {
        Connection con = conexion.conectar();
        if (con == null) return;

        try {
            con.setAutoCommit(false);

            // Guarda la instalación base en la BD
            int idGenerado;
            try (PreparedStatement psI = con.prepareStatement(
                    SQL_INSERT_INSTALACION, Statement.RETURN_GENERATED_KEYS)) {

                psI.setString(1, instalacion.getTipo());
                psI.setInt(2, instalacion.getCapacidadMaxima());
                psI.setInt(3, instalacion.getAforoActual());
                psI.executeUpdate();

                try (ResultSet keys = psI.getGeneratedKeys()) {
                    if (!keys.next()) throw new SQLException("No se generó idInstalacion.");
                    idGenerado = keys.getInt(1);
                    instalacion.setIdInstalacion(String.valueOf(idGenerado)); // reflejar en objeto
                }
            }

            // Guarda datos específicos según el tipo (gimnasio o piscina)
            if (instalacion instanceof Piscina) {
                Piscina p = (Piscina) instalacion;
                try (PreparedStatement psP = con.prepareStatement(SQL_INSERT_PISCINA)) {
                    psP.setInt(1, idGenerado);
                    psP.setInt(2, p.getNumeroCarriles());
                    psP.setDouble(3, p.getProfundidad());
                    psP.executeUpdate();
                }
            } else if (instalacion instanceof Gimnasio) {
                try (PreparedStatement psG = con.prepareStatement(SQL_INSERT_GIMNASIO)) {
                    psG.setInt(1, idGenerado);
                    psG.setInt(2, instalacion.getAforoActual());
                    psG.executeUpdate();
                }
            }

            con.commit();

        } catch (SQLException e) {
            try { con.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            throw new RuntimeException("Error al insertar instalación", e);
        } finally {
            try { con.setAutoCommit(true); } catch (SQLException e) { e.printStackTrace(); }
            conexion.desconectar();
        }
    }

    @Override
    public Optional<Instalacion> buscarPorId(int id) {
        Connection con = conexion.conectar();
        if (con == null) return Optional.empty();

        try (PreparedStatement ps = con.prepareStatement(SQL_SELECT_POR_ID)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapear(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar instalación id=" + id, e);
        } finally {
            conexion.desconectar();
        }
        return Optional.empty();
    }

    @Override
    public List<Instalacion> listarTodos() {
        return ejecutarConsultaLista(SQL_SELECT_TODOS);
    }

    @Override
    public List<Instalacion> listarGimnasios() {
        return ejecutarConsultaLista(SQL_SELECT_GIMNAS);
    }

    @Override
    public List<Instalacion> listarPiscinas() {
        return ejecutarConsultaLista(SQL_SELECT_PISC);
    }

    @Override
    public void actualizar(Instalacion instalacion) {
        Connection con = conexion.conectar();
        if (con == null) return;

        int id = Integer.parseInt(instalacion.getIdInstalacion());

        try {
            con.setAutoCommit(false);

            try (PreparedStatement psI = con.prepareStatement(SQL_UPDATE_INSTALACION)) {
                psI.setInt(1, instalacion.getCapacidadMaxima());
                psI.setInt(2, instalacion.getAforoActual());
                psI.setInt(3, id);
                psI.executeUpdate();
            }

            if (instalacion instanceof Piscina) {
                Piscina p = (Piscina) instalacion;
                try (PreparedStatement psP = con.prepareStatement(SQL_UPDATE_PISCINA)) {
                    psP.setInt(1, p.getNumeroCarriles());
                    psP.setDouble(2, p.getProfundidad());
                    psP.setInt(3, id);
                    psP.executeUpdate();
                }
            }

            con.commit();

        } catch (SQLException e) {
            try { con.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            throw new RuntimeException("Error al actualizar instalación", e);
        } finally {
            try { con.setAutoCommit(true); } catch (SQLException e) { e.printStackTrace(); }
            conexion.desconectar();
        }
    }

    @Override
    public void actualizarAforo(int idInstalacion, int nuevoAforo) {
        Connection con = conexion.conectar();
        if (con == null) return;

        try (PreparedStatement ps = con.prepareStatement(SQL_UPDATE_AFORO)) {
            ps.setInt(1, nuevoAforo);
            ps.setInt(2, idInstalacion);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al actualizar aforo", e);
        } finally {
            conexion.desconectar();
        }
    }

    @Override
    public void eliminar(int id) {
        Connection con = conexion.conectar();
        if (con == null) return;

        // Al borrar la instalación, los datos del gimnasio o piscina se borran automáticamente
        try (PreparedStatement ps = con.prepareStatement(SQL_DELETE)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al eliminar instalación id=" + id, e);
        } finally {
            conexion.desconectar();
        }
    }

    // ── Mapeo ResultSet → Instalacion (polimórfico) ────────────────────────────

    /**
     * Convierte una fila de la BD en un objeto Instalacion (Gimnasio o Piscina).
     * Si tiene información de piscina, crea una Piscina; si no, crea un Gimnasio.
     */
    private Instalacion mapear(ResultSet rs) throws SQLException {
        String idStr    = String.valueOf(rs.getInt("idInstalacion"));
        String tipo     = rs.getString("tipo");
        int capMax      = rs.getInt("capacidadMaxima");
        int aforoActual = rs.getInt("aforoActual");

        int numeroCarriles = rs.getInt("numeroCarriles"); // 0 si NULL (getInt devuelve 0 para null)
        boolean esPiscina  = !rs.wasNull();               // wasNull() detecta si el campo era NULL

        if (esPiscina) {
            double profundidad = rs.getDouble("profundidad");
            return new Piscina(idStr, tipo, capMax, aforoActual, numeroCarriles, profundidad);
        } else {
            return new Gimnasio(idStr, tipo, capMax, aforoActual);
        }
    }

    /** Ejecuta cualquier SELECT sin parámetros y devuelve la lista de instalaciones. */
    private List<Instalacion> ejecutarConsultaLista(String sql) {
        Connection con = conexion.conectar();
        List<Instalacion> lista = new ArrayList<>();
        if (con == null) return lista;

        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(mapear(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar instalaciones", e);
        } finally {
            conexion.desconectar();
        }
        return lista;
    }
}