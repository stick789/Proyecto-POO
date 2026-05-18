package dao;

import java.util.List;
import java.util.Optional;

import entidades.Sede;

public interface ISedeDAO {

    void insertar(Sede sede);

    Optional<Sede> buscarPorId(int id);

    List<Sede> listarTodos();

    void actualizar(Sede sede);

    void eliminar(int id);
}
