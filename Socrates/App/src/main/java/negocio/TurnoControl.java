package negocio;


import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

import LogicaCita.PoliticaCancelacion;
import dao.ITurnoDAO;
import entidades.Persona;
import entidades.Turno;
import entidades.Usuario;

public class TurnoControl {

	private final ITurnoDAO turnoDao;


	public TurnoControl(ITurnoDAO turnoDao) {
		this.turnoDao = turnoDao;
	}

	// Reserva un turno aplicando reglas de negocio. La persistencia la delega al DAO.
	public boolean reservarTurno(Turno turno) {
		if (turno == null) return false;

		/*  TODO: implementar validaciones concretas:
		 - campos obligatorios
		 - disponibilidad de la instalación
		 - solapamientos del usuario
		 - estado de pagos

		 Ejemplo de llamadas que deben existir en la interfaz ITurnoDAO:
		 if (!turnoDao.estaDisponible(turno.getInstalacion().getId(), turno.getFechaHora(), turno.getDuracionMinutos())) {
		 	return false;
		 }
		 */
		return false;
	}

	public boolean cancelarTurno(int turnoId, int usuarioId, boolean esAdministrador) {
		try {
			Optional<Turno> opt = turnoDao.buscarPorId(turnoId);
			if (!opt.isPresent()) return false;

			Turno turno = opt.get();

			// Verificar propiedad o privilegio administrativo
			if (turno.getUsuario() == null || (!esAdministrador && turno.getUsuario().getId() != usuarioId)) {
				return false;
			}

			PoliticaCancelacion politica = new PoliticaCancelacion();

			// Si es admin, puede cancelar independientemente del tiempo
			if (!politica.puedeCancelarComoAdmin(turno, esAdministrador)) {
				return false;
			}

			// Actualizar estado vía DAO
			turnoDao.actualizarEstado(turnoId, Turno.ESTADO_CANCELADO);
			return true;
		} catch (Exception ex) {
			Logger.getLogger("Error al cancelar turno: " + ex.getMessage());
			return false;
		}
	}

	public boolean cancelarTurno(int turnoId, Persona persona) {
		PersonaControl personaControl = new PersonaControl();
		boolean esAdministrador = personaControl.esAdministrador(persona);
		int usuarioId = persona instanceof Usuario ? ((Usuario) persona).getId() : -1;
		return cancelarTurno(turnoId, usuarioId, esAdministrador);
	}

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