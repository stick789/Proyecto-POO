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
		throw new UnsupportedOperationException(
			"TurnoControl no gestiona escrituras. Use LogicaCita.ServicioTurnos para reservar turnos."
		);
	}
// Cancela un turno aplicando reglas de negocio. La persistencia la delega al DAO.
	public boolean cancelarTurno(int turnoId, int usuarioId, boolean esAdministrador) {
		throw new UnsupportedOperationException(
			"TurnoControl no gestiona escrituras. Use LogicaCita.ServicioTurnos para cancelar turnos."
		);
	}
// Lista los turnos de un usuario aplicando filtros de fecha. La consulta la delega al DAO.
	public boolean cancelarTurno(int turnoId, Persona persona) {
		throw new UnsupportedOperationException(
			"TurnoControl no gestiona escrituras. Use LogicaCita.ServicioTurnos para cancelar turnos."
		);
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

}