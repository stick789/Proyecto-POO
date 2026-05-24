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
 * InstalacionDAO — Acceso a datos de instalaciones.
 *
 * CAMBIOS (sede + nombre):
 *  - SQL_SELECT_BASE ahora selecciona i.nombre e i.idSede.
 *  - mapear() los asigna a la entidad tras construirla.
 *  - Nuevos métodos listarGimnasiosPorSede() y listarPiscinasPorSede()
 *    filtran por i.idSede para mostrar solo las instalaciones de la sede
 *    del usuario logueado.
 */
public class InstalacionDAO implements IInstalacionDAO {

    private final Conexion conexion = Conexion.getInstancia();

    // ── SQL ───────────────────────────────────────────────────────────────────

    private static final String SQL_INSERT_INSTALACION =
            "INSERT INTO instalacion (tipo, capacidadMaxima, aforoActual) VALUES (?, ?, ?)";

    private static final String SQL_INSERT_GIMNASIO =
            "INSERT INTO gimnasio (idInstalacion, aforo_actual) VALUES (?, ?)";

    private static final String SQL_INSERT_PISCINA =
            "INSERT INTO piscina (idInstalacion, numeroCarriles, profundidad) VALUES (?, ?, ?)";

    // nombre, idSede + sede JOIN añadidos al SELECT
    private static final String SQL_SELECT_BASE =
            "SELECT i.idInstalacion, i.tipo, i.capacidadMaxima, i.aforoActual, " +
            "       i.nombre, i.idSede, " +
            "       s.nombre AS nombreSede, s.direccion AS direccionSede, " +
            "       p.numeroCarriles, p.profundidad " +
            "FROM instalacion i " +
            "LEFT JOIN piscina p ON i.idInstalacion = p.idInstalacion " +
            "LEFT JOIN sede    s ON i.idSede        = s.idSede ";

    private static final String SQL_SELECT_POR_ID   = SQL_SELECT_BASE + "WHERE i.idInstalacion = ?";
    private static final String SQL_SELECT_TODOS    = SQL_SELECT_BASE;
    private static final String SQL_SELECT_GIMNAS   = SQL_SELECT_BASE + "WHERE i.tipo = 'GIMNASIO'";
    private static final String SQL_SELECT_PISC     = SQL_SELECT_BASE + "WHERE i.tipo = 'PISCINA'";

    // ← nuevas queries filtradas por sede
    private static final String SQL_SELECT_GIMNAS_SEDE =
            SQL_SELECT_BASE + "WHERE i.tipo = 'GIMNASIO' AND i.idSede = ?";
    private static final String SQL_SELECT_PISC_SEDE =
            SQL_SELECT_BASE + "WHERE i.tipo = 'PISCINA'  AND i.idSede = ?";

    private static final String SQL_UPDATE_INSTALACION =
            "UPDATE instalacion SET capacidadMaxima = ?, aforoActual = ? WHERE idInstalacion = ?";

    private static final String SQL_UPDATE_PISCINA =
            "UPDATE piscina SET numeroCarriles = ?, profundidad = ? WHERE idInstalacion = ?";

    private static final String SQL_UPDATE_AFORO =
            "UPDATE instalacion SET aforoActual = ? WHERE idInstalacion = ?";

    private static final String SQL_DELETE =
            "DELETE FROM instalacion WHERE idInstalacion = ?";

    // ── Implementación ────────────────────────────────────────────────────────

    @Override
    public void insertar(Instalacion instalacion) {
        Connection con = conexion.conectar();
        if (con == null) return;

        try {
            con.setAutoCommit(false);

            int idGenerado;
            try (PreparedStatement ps = con.prepareStatement(
                    SQL_INSERT_INSTALACION, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, instalacion.getTipo());
                ps.setInt(2, instalacion.getCapacidadMaxima());
                ps.setInt(3, instalacion.getAforoActual());
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (!keys.next()) throw new SQLException("No se generó idInstalacion.");
                    idGenerado = keys.getInt(1);
                    instalacion.setIdInstalacion(idGenerado);
                }
            }

            if (instalacion instanceof Piscina) {
                Piscina p = (Piscina) instalacion;
                try (PreparedStatement ps = con.prepareStatement(SQL_INSERT_PISCINA)) {
                    ps.setInt(1, idGenerado);
                    ps.setInt(2, p.getNumeroCarriles());
                    ps.setDouble(3, p.getProfundidad());
                    ps.executeUpdate();
                }
            } else if (instalacion instanceof Gimnasio) {
                try (PreparedStatement ps = con.prepareStatement(SQL_INSERT_GIMNASIO)) {
                    ps.setInt(1, idGenerado);
                    ps.setInt(2, instalacion.getAforoActual());
                    ps.executeUpdate();
                }
            }

            con.commit();

        } catch (SQLException e) {
            revertir(con);
            throw new RuntimeException("Error al insertar instalación", e);
        } finally {
            restaurarAutoCommit(con);
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
    public List<Instalacion> listarTodos()     { return ejecutarLista(SQL_SELECT_TODOS);  }

    @Override
    public List<Instalacion> listarGimnasios() { return ejecutarLista(SQL_SELECT_GIMNAS); }

    @Override
    public List<Instalacion> listarPiscinas()  { return ejecutarLista(SQL_SELECT_PISC);   }

    // ── Nuevos métodos filtrados por sede ─────────────────────────────────────

    @Override
    public List<Instalacion> listarGimnasiosPorSede(int idSede) {
        return ejecutarListaConSede(SQL_SELECT_GIMNAS_SEDE, idSede);
    }

    @Override
    public List<Instalacion> listarPiscinasPorSede(int idSede) {
        return ejecutarListaConSede(SQL_SELECT_PISC_SEDE, idSede);
    }

    // ── Actualización ─────────────────────────────────────────────────────────

    @Override
    public void actualizar(Instalacion instalacion) {
        Connection con = conexion.conectar();
        if (con == null) return;

        int id = instalacion.getIdInstalacion();

        try {
            con.setAutoCommit(false);

            try (PreparedStatement ps = con.prepareStatement(SQL_UPDATE_INSTALACION)) {
                ps.setInt(1, instalacion.getCapacidadMaxima());
                ps.setInt(2, instalacion.getAforoActual());
                ps.setInt(3, id);
                ps.executeUpdate();
            }

            if (instalacion instanceof Piscina) {
                Piscina p = (Piscina) instalacion;
                try (PreparedStatement ps = con.prepareStatement(SQL_UPDATE_PISCINA)) {
                    ps.setInt(1, p.getNumeroCarriles());
                    ps.setDouble(2, p.getProfundidad());
                    ps.setInt(3, id);
                    ps.executeUpdate();
                }
            }

            con.commit();

        } catch (SQLException e) {
            revertir(con);
            throw new RuntimeException("Error al actualizar instalación id=" + id, e);
        } finally {
            restaurarAutoCommit(con);
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
            throw new RuntimeException("Error al actualizar aforo id=" + idInstalacion, e);
        } finally {
            conexion.desconectar();
        }
    }

    @Override
    public void eliminar(int id) {
        Connection con = conexion.conectar();
        if (con == null) return;

        try (PreparedStatement ps = con.prepareStatement(SQL_DELETE)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al eliminar instalación id=" + id, e);
        } finally {
            conexion.desconectar();
        }
    }

    // ── Mapeo ─────────────────────────────────────────────────────────────────

    private Instalacion mapear(ResultSet rs) throws SQLException {
        int    id          = rs.getInt("idInstalacion");
        String tipo        = rs.getString("tipo");
        int    capMax      = rs.getInt("capacidadMaxima");
        int    aforoActual = rs.getInt("aforoActual");
        String nombre      = rs.getString("nombre");
        int    idSede      = rs.getInt("idSede");
        String nombreSede  = rs.getString("nombreSede");    // ← nuevo
        String dirSede     = rs.getString("direccionSede"); // ← nuevo

        int     carriles  = rs.getInt("numeroCarriles");
        boolean esPiscina = !rs.wasNull();

        Instalacion inst;
        if (esPiscina) {
            double profundidad = rs.getDouble("profundidad");
            inst = new Piscina(id, tipo, capMax, aforoActual, carriles, profundidad);
        } else {
            inst = new Gimnasio(id, tipo, capMax, aforoActual);
        }

        inst.setNombre(nombre);
        inst.setIdSede(idSede);
        inst.setNombreSede(nombreSede);    // ← nuevo
        inst.setDireccionSede(dirSede);    // ← nuevo
        return inst;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private List<Instalacion> ejecutarLista(String sql) {
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

    private List<Instalacion> ejecutarListaConSede(String sql, int idSede) {
        Connection con = conexion.conectar();
        List<Instalacion> lista = new ArrayList<>();
        if (con == null) return lista;

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idSede);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar instalaciones por sede id=" + idSede, e);
        } finally {
            conexion.desconectar();
        }
        return lista;
    }

    private void revertir(Connection con) {
        if (con != null) try { con.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
    }

    private void restaurarAutoCommit(Connection con) {
        if (con != null) try { con.setAutoCommit(true); } catch (SQLException e) { e.printStackTrace(); }
    }
}