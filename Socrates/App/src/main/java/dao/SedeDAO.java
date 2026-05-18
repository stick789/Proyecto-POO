package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import database.Conexion;
import entidades.Sede;

public class SedeDAO implements ISedeDAO {

    private final Conexion conexion = Conexion.getInstancia();

    private static final String SQL_INSERT =
            "INSERT INTO sede (nombre, direccion, telefono, email) VALUES (?, ?, ?, ?)";

    private static final String SQL_SELECT_POR_ID =
            "SELECT idSede, nombre, direccion, telefono, email FROM sede WHERE idSede = ?";

    private static final String SQL_SELECT_TODOS =
            "SELECT idSede, nombre, direccion, telefono, email FROM sede";

    private static final String SQL_UPDATE =
            "UPDATE sede SET nombre = ?, direccion = ?, telefono = ?, email = ? WHERE idSede = ?";

    private static final String SQL_DELETE =
            "DELETE FROM sede WHERE idSede = ?";

    @Override
    public void insertar(Sede sede) {
        Connection con = conexion.conectar();
        if (con == null) return;

        try (PreparedStatement ps = con.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, sede.getNombre());
            ps.setString(2, sede.getDireccion());
            ps.setString(3, sede.getTelefono());
            ps.setString(4, sede.getEmail());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) sede.setIdSede(keys.getInt(1));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al insertar sede", e);
        } finally {
            conexion.desconectar();
        }
    }

    @Override
    public Optional<Sede> buscarPorId(int id) {
        Connection con = conexion.conectar();
        if (con == null) return Optional.empty();

        try (PreparedStatement ps = con.prepareStatement(SQL_SELECT_POR_ID)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Sede s = mapear(rs);
                    return Optional.of(s);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar sede id=" + id, e);
        } finally {
            conexion.desconectar();
        }
        return Optional.empty();
    }

    @Override
    public List<Sede> listarTodos() {
        List<Sede> lista = new ArrayList<>();
        Connection con = conexion.conectar();
        if (con == null) return lista;

        try (PreparedStatement ps = con.prepareStatement(SQL_SELECT_TODOS);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(mapear(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar sedes", e);
        } finally {
            conexion.desconectar();
        }
        return lista;
    }

    @Override
    public void actualizar(Sede sede) {
        Connection con = conexion.conectar();
        if (con == null) return;

        try (PreparedStatement ps = con.prepareStatement(SQL_UPDATE)) {
            ps.setString(1, sede.getNombre());
            ps.setString(2, sede.getDireccion());
            ps.setString(3, sede.getTelefono());
            ps.setString(4, sede.getEmail());
            ps.setInt(5, sede.getIdSede());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al actualizar sede id=" + sede.getIdSede(), e);
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
            throw new RuntimeException("Error al eliminar sede id=" + id, e);
        } finally {
            conexion.desconectar();
        }
    }

    private Sede mapear(ResultSet rs) throws SQLException {
        int id = rs.getInt("idSede");
        String nombre = rs.getString("nombre");
        String direccion = rs.getString("direccion");
        String telefono = rs.getString("telefono");
        String email = rs.getString("email");
        return new Sede(id, nombre, direccion, telefono, email);
    }
}
