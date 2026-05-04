package negocio;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import dao.ITurnoDAO;
import entidades.Persona;
import entidades.Turno;
import entidades.Usuario;

/**
 * TurnoControl — Capa de negocio para consultas y operaciones simples sobre turnos.
 *
 * <p><b>Diseño de responsabilidades:</b></p>
 * <ul>
 *   <li>{@link LogicaCita.ServicioTurnos} → operaciones transaccionales que modifican
 *       estado (reservar, cancelar, reagendar, completar). Requiere múltiples DAOs
 *       y aplica políticas complejas.</li>
 *   <li>{@code TurnoControl} → consultas y operaciones simples que no requieren
 *       validaciones de política de cancelación ni aforo.</li>
 * </ul>
 *
 * <p>Este control solo usa {@link ITurnoDAO}; para operaciones que modifican
 * estado de instalaciones o registran historial, usar
 * {@link LogicaCita.ServicioTurnos} directamente.</p>
 */
public class TurnoControl {

    private static final Logger LOG = Logger.getLogger(TurnoControl.class.getName());

    private final ITurnoDAO turnoDao;
    private final PersonaControl personaControl;

    public TurnoControl(ITurnoDAO turnoDao) {
        this.turnoDao      = turnoDao;
        this.personaControl = new PersonaControl();
    }

    public TurnoControl(ITurnoDAO turnoDao, PersonaControl personaControl) {
        this.turnoDao      = turnoDao;
        this.personaControl = personaControl;
    }

    // ================================================================= CONSULTAS

    /**
     * Lista los turnos de un usuario filtrados por rango de fechas.
     *
     * @param usuarioId ID del usuario.
     * @param desde     fecha inicio del rango (inclusive); {@code null} = sin límite inferior.
     * @param hasta     fecha fin del rango (inclusive); {@code null} = sin límite superior.
     * @return lista filtrada, vacía si no hay resultados.
     */
    public List<Turno> listarTurnosUsuario(int usuarioId, LocalDate desde, LocalDate hasta) {
        try {
            List<Turno> todos = turnoDao.listarPorUsuario(usuarioId);
            List<Turno> filtrados = new ArrayList<>();

            for (Turno t : todos) {
                if (t.getFechaHora() == null) continue;
                LocalDate fecha = t.getFechaHora().toLocalDate();
                boolean dentroRango =
                        (desde == null || !fecha.isBefore(desde)) &&
                        (hasta == null || !fecha.isAfter(hasta));
                if (dentroRango) filtrados.add(t);
            }
            return filtrados;
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Error al listar turnos del usuario id=" + usuarioId, ex);
            return new ArrayList<>();
        }
    }

    /**
     * Lista los turnos RESERVADOS (activos) de un usuario.
     * Solo el propio usuario o un administrador pueden consultar esta lista.
     *
     * @param idUsuario ID del usuario cuya lista se consulta.
     * @param actor     persona que realiza la consulta.
     * @throws IllegalAccessError si el actor no tiene permiso.
     */
    public List<Turno> listarTurnosActivos(int idUsuario, Persona actor) {
        validarAccesoUsuario(idUsuario, actor);
        try {
            List<Turno> activos = new ArrayList<>();
            for (Turno t : turnoDao.listarPorUsuario(idUsuario)) {
                if (Turno.ESTADO_RESERVADO.equals(t.getEstado())) activos.add(t);
            }
            return activos;
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Error al listar turnos activos del usuario id=" + idUsuario, ex);
            return new ArrayList<>();
        }
    }

    /**
     * Lista el historial (turnos CANCELADOS y COMPLETADOS) de un usuario.
     * Solo el propio usuario o un administrador pueden consultarlo.
     *
     * @param idUsuario ID del usuario.
     * @param actor     persona que realiza la consulta.
     * @throws IllegalAccessError si el actor no tiene permiso.
     */
    public List<Turno> listarHistorial(int idUsuario, Persona actor) {
        validarAccesoUsuario(idUsuario, actor);
        try {
            List<Turno> historial = new ArrayList<>();
            for (Turno t : turnoDao.listarPorUsuario(idUsuario)) {
                if (!Turno.ESTADO_RESERVADO.equals(t.getEstado())) historial.add(t);
            }
            return historial;
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Error al listar historial del usuario id=" + idUsuario, ex);
            return new ArrayList<>();
        }
    }

    /**
     * Obtiene el próximo turno reservado de un usuario (el más cercano en el tiempo).
     *
     * @param idUsuario ID del usuario.
     * @param actor     persona que realiza la consulta.
     * @return {@code Optional} con el próximo turno, o vacío si no hay ninguno.
     */
    public Optional<Turno> obtenerProximoTurno(int idUsuario, Persona actor) {
        validarAccesoUsuario(idUsuario, actor);
        return listarTurnosActivos(idUsuario, actor).stream()
                .min((a, b) -> a.getFechaHora().compareTo(b.getFechaHora()));
    }

    /**
     * Lista todos los turnos del sistema. Solo administradores pueden invocar este método.
     *
     * @param actor persona que realiza la consulta.
     * @throws IllegalAccessError si el actor no es administrador.
     */
    public List<Turno> listarTodos(Persona actor) {
        personaControl.validarOperacion(actor, PersonaControl.OperacionPersona.LISTAR_USUARIOS);
        try {
            return turnoDao.listarTodos();
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Error al listar todos los turnos", ex);
            return new ArrayList<>();
        }
    }

    /**
     * Busca un turno por su ID.
     *
     * @param idTurno ID del turno a buscar.
     * @return {@code Optional} con el turno, o vacío si no existe.
     */
    public Optional<Turno> buscarPorId(int idTurno) {
        try {
            return turnoDao.buscarPorId(idTurno);
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Error al buscar turno id=" + idTurno, ex);
            return Optional.empty();
        }
    }

    // ================================================================= ACTUALIZACIÓN SIMPLE

    /**
     * Cambia el estado de un turno directamente.
     * Para lógica compleja (validar políticas de cancelación, liberar aforo,
     * registrar historial), usar {@link LogicaCita.ServicioTurnos}.
     *
     * @param idTurno     ID del turno.
     * @param nuevoEstado nuevo estado (RESERVADO, CANCELADO, COMPLETADO).
     * @param actor       persona que realiza la operación.
     */
    public void cambiarEstado(int idTurno, String nuevoEstado, Persona actor) {
        if (actor == null) throw new IllegalArgumentException("El actor no puede ser null.");

        // Solo admins pueden cambiar estados directamente desde este control simple
        personaControl.validarOperacion(actor, PersonaControl.OperacionPersona.ADMINISTRAR_HISTORIAL);

        Turno turno = turnoDao.buscarPorId(idTurno)
                .orElseThrow(() -> new IllegalArgumentException("Turno id=" + idTurno + " no encontrado."));

        // Validar el nuevo estado antes de persistir
        turno.setEstado(nuevoEstado); // lanza IllegalArgumentException si es inválido
        turnoDao.actualizarEstado(idTurno, nuevoEstado);
    }

    // ================================================================= PRIVADOS

    /**
     * Valida que el actor tiene acceso a los datos del usuario solicitado.
     * El acceso se permite si el actor es el propio usuario o un administrador.
     */
    private void validarAccesoUsuario(int idUsuario, Persona actor) {
        if (actor == null) throw new IllegalArgumentException("El actor no puede ser null.");

        boolean esAdmin = personaControl.esAdministrador(actor);
        if (!esAdmin) {
            if (!(actor instanceof Usuario) || ((Usuario) actor).getId() != idUsuario) {
                throw new IllegalAccessError(
                    "Solo el propio usuario (id=" + idUsuario + ") o un administrador " +
                    "puede consultar esta información.");
            }
        }
    }
}
