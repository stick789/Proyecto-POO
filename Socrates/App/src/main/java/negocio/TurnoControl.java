package negocio;


import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import dao.ITurnoDAO;
import entidades.Persona;
import entidades.Turno;

public class TurnoControl {

	private final ITurnoDAO turnoDao;


	public TurnoControl(ITurnoDAO turnoDao) {
		this.turnoDao = turnoDao;
	}

	// Reserva un turno aplicando reglas de negocio. La persistencia la delega al DAO.
	public boolean reservarTurno(Turno turno) {
		public boolean reservarTurno(Turno turno, Persona actor) {
    try {
        //Validamos que el turno y el actor no sean null
        if (turno == null || actor == null) return false;

        // Llamamos al método de lógica avanzada que nos mostraste.
        // Extraemos los datos del objeto 'turno' para enviarlos como parámetros.
        Turno resultado = reservarTurno(
            turno.getFechaHora(), 
            turno.getDuracionMinutos(), 
            turno.getUsuario(), 
            turno.getInstalacion(), 
            turno.getNumeroCarril(), 
            actor
        );

        //  Si el método nos devuelve un objeto Turno, la reserva fue exitosa
        return resultado != null;

    } catch (Exception ex) {
        // Si no es el dueño, no tiene permisos, o no hay cupo,
        // el error caerá aquí y saldrá en rojo en tu consola.
        System.err.println("Error al reservar: " + ex.getMessage());
        return false;
    }
}
	}
// Cancela un turno aplicando reglas de negocio. La persistencia la delega al DAO.
	public boolean cancelarTurno(int turnoId, int usuarioId, boolean esAdministrador) {
		try{
			// se hace el constructor y se llama al metodo de buscar id para verificar 
        Turno turno = turnoDAO.buscarPorId(turnoId); 
        
        // Validamos que el id o bueno el turno exista, si no existe no se puede cancelar
        if (turno == null) return false;
        // llamamos al metodo cancelarturnointerno 
        cancelarTurnoInterno(turno, esAdministrador);
        return true;
    } catch (Exception ex) {
        // Si falla la política o el DAO, llega aquí
        return false;
		};
	}
// Lista los turnos de un usuario aplicando filtros de fecha. La consulta la delega al DAO.
	public boolean cancelarTurno(int turnoId, Persona persona) {
		Turno turno = turnoDao.buscarPorId(turnoId);
        if (turno == null) return false;

       //llamamos al metodo para cancelar y verficar si es un admin
        cancelarTurno(turno, persona); 
        
        return true;
    } catch (Exception ex) {
        // Si no es el dueño, no es admin, o la política falla, 
        // capturamos el IllegalAccessError o IllegalStateException aquí
		//y utilizamos system.err para imprimir el mensaje de error
        System.err.println("Cancelación rechazada: " + ex.getMessage());
        return false;
		};
	}
// Lista los turnos de un usuario aplicando filtros de fecha. La consulta la delega al DAO.
	public List<Turno> listarTurnosUsuario(int usuarioId, LocalDate desde, LocalDate hasta) {
		try {
			List<Turno> todos = turnoDao.listarPorUsuario(usuarioId);
			List<Turno> filtrados = new ArrayList<>();
			for (Turno t : todos) {
				if (t.getFechaHora() == null) continue;
				LocalDate fecha = t.getFechaHora().toLocalDate();
				if ((desde == null || !fecha.isBefore(desde)) && (hasta == null || !fecha.isAfter(hasta))) {
					filtrados.add(t);
				}
			}
			return filtrados;
		} catch (Exception ex) {
			Logger.getLogger("Error al listar turnos: " + ex.getMessage());
			return new ArrayList<>();
		}
	}

