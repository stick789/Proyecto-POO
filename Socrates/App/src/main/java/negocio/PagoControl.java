package negocio;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import dao.IPagoDAO;
import dao.ITurnoDAO;
import entidades.Pago;
import entidades.Persona;
import entidades.Turno;
import entidades.Usuario;

/**
 * PagoControl — Lógica de negocio para el módulo de pagos.
 *
 * <p><b>Reglas de negocio aplicadas:</b></p>
 * <ul>
 *   <li>Solo se puede pagar un turno en estado {@code RESERVADO}.</li>
 *   <li>El monto debe ser positivo.</li>
 *   <li>Solo el propietario del turno o un administrador pueden registrar un pago.</li>
 *   <li>Solo un administrador puede gestionar reembolsos o cambios de estado de pagos.</li>
 *   <li>No se permite registrar un segundo pago {@code COMPLETADO} para el mismo turno
 *       (evita doble cobro).</li>
 * </ul>
 *
 * <p>Métodos de retorno de estado (igual que {@code PersonaControl.login()}):
 * {@code "OK"}, {@code "ERROR"}, {@code "SIN_PERMISO"}, {@code "TURNO_INVALIDO"},
 * {@code "PAGO_DUPLICADO"}.</p>
 */
public class PagoControl {

    private static final Logger LOG = Logger.getLogger(PagoControl.class.getName());

    private final IPagoDAO pagoDAO;
    private final ITurnoDAO turnoDAO;
    private final PersonaControl personaControl;

    public PagoControl(IPagoDAO pagoDAO, ITurnoDAO turnoDAO, PersonaControl personaControl) {
        this.pagoDAO        = pagoDAO;
        this.turnoDAO       = turnoDAO;
        this.personaControl = personaControl;
    }

    // ================================================================= REGISTRAR PAGO

    /**
     * Registra un nuevo pago para un turno reservado.
     *
     * @param idTurno    ID del turno a pagar.
     * @param monto      monto a cobrar (debe ser positivo).
     * @param metodoPago método de pago ("EFECTIVO", "TARJETA", "TRANSFERENCIA").
     * @param actor      persona que realiza el pago (debe ser el propietario o un admin).
     * @return {@code "OK"} si se registró correctamente, o un código de error descriptivo.
     */
    public String registrarPago(int idTurno, BigDecimal monto, String metodoPago, Persona actor) {
        if (actor == null) return "SIN_PERMISO";

        try {
            // Verificar que el turno existe y está en estado RESERVADO
            Optional<Turno> optTurno = turnoDAO.buscarPorId(idTurno);
            if (optTurno.isEmpty()) return "TURNO_INVALIDO";

            Turno turno = optTurno.get();
            if (!Turno.ESTADO_RESERVADO.equals(turno.getEstado())) {
                return "TURNO_INVALIDO"; // Solo se pagan turnos reservados
            }

            // Solo el propietario del turno o un administrador pueden pagar
            boolean esAdmin = personaControl.esAdministrador(actor);
            if (!esAdmin) {
                if (!(actor instanceof Usuario) ||
                        ((Usuario) actor).getId() != turno.getUsuario().getId()) {
                    return "SIN_PERMISO";
                }
            }

            // Verificar que no exista ya un pago COMPLETADO para este turno (evitar doble cobro)
            List<Pago> pagosExistentes = pagoDAO.listarPorTurno(idTurno);
            boolean yaExistePagoCompletado = pagosExistentes.stream()
                    .anyMatch(p -> Pago.ESTADO_COMPLETADO.equals(p.getEstadoPago()));
            if (yaExistePagoCompletado) return "PAGO_DUPLICADO";

            // Registrar el pago
            Pago pago = new Pago(
                    idTurno,
                    turno.getUsuario().getId(),
                    monto,
                    metodoPago,
                    Pago.ESTADO_COMPLETADO
            );
            pagoDAO.insertar(pago);
            return "OK";

        } catch (IllegalArgumentException e) {
            LOG.log(Level.WARNING, "Datos de pago inválidos: {0}", e.getMessage());
            return "ERROR";
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al registrar pago del turno id=" + idTurno, e);
            return "ERROR";
        }
    }

    /**
     * Crea un pago en estado {@code PENDIENTE} (para flujos en dos pasos:
     * autorizar primero, confirmar después).
     *
     * @param idTurno    ID del turno.
     * @param monto      monto a pagar.
     * @param metodoPago método de pago.
     * @param actor      persona que inicia el pago.
     * @return {@link Pago} creado con estado PENDIENTE, o {@code null} en caso de error.
     */
    public Pago iniciarPago(int idTurno, BigDecimal monto, String metodoPago, Persona actor) {
        if (actor == null) return null;

        try {
            Optional<Turno> optTurno = turnoDAO.buscarPorId(idTurno);
            if (optTurno.isEmpty() || !Turno.ESTADO_RESERVADO.equals(optTurno.get().getEstado())) {
                return null;
            }

            Turno turno = optTurno.get();
            boolean esAdmin = personaControl.esAdministrador(actor);
            if (!esAdmin && (!(actor instanceof Usuario) ||
                    ((Usuario) actor).getId() != turno.getUsuario().getId())) {
                return null;
            }

            Pago pago = new Pago(idTurno, turno.getUsuario().getId(),
                    monto, metodoPago, Pago.ESTADO_PENDIENTE);
            pagoDAO.insertar(pago);
            return pago;

        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al iniciar pago del turno id=" + idTurno, e);
            return null;
        }
    }

    // ================================================================= ACTUALIZAR ESTADO

    /**
     * Confirma un pago pendiente, cambiando su estado a {@code COMPLETADO}.
     * Solo administradores pueden confirmar pagos.
     *
     * @param idPago ID del pago a confirmar.
     * @param actor  administrador que realiza la confirmación.
     * @return {@code true} si se actualizó correctamente.
     */
    public boolean confirmarPago(long idPago, Persona actor) {
        personaControl.validarOperacion(actor, PersonaControl.OperacionPersona.GESTIONAR_PAGOS);
        return pagoDAO.actualizarEstado(idPago, Pago.ESTADO_COMPLETADO);
    }

    /**
     * Marca un pago como {@code REEMBOLSADO}.
     * Solo administradores pueden emitir reembolsos.
     *
     * @param idPago ID del pago a reembolsar.
     * @param actor  administrador que autoriza el reembolso.
     * @return {@code true} si se actualizó correctamente.
     */
    public boolean reembolsarPago(long idPago, Persona actor) {
        personaControl.validarOperacion(actor, PersonaControl.OperacionPersona.GESTIONAR_PAGOS);
        return pagoDAO.actualizarEstado(idPago, Pago.ESTADO_REEMBOLSADO);
    }

    /**
     * Marca un pago como {@code FALLIDO}.
     * Solo administradores pueden registrar fallos de pago.
     *
     * @param idPago ID del pago fallido.
     * @param actor  administrador.
     * @return {@code true} si se actualizó correctamente.
     */
    public boolean marcarComoFallido(long idPago, Persona actor) {
        personaControl.validarOperacion(actor, PersonaControl.OperacionPersona.GESTIONAR_PAGOS);
        return pagoDAO.actualizarEstado(idPago, Pago.ESTADO_FALLIDO);
    }

    // ================================================================= CONSULTAS

    /**
     * Lista los pagos de un usuario.
     * Solo el propio usuario o un administrador pueden consultar.
     *
     * @param idUsuario ID del usuario propietario.
     * @param actor     persona que realiza la consulta.
     * @return lista de pagos del usuario.
     */
    public List<Pago> listarPorUsuario(int idUsuario, Persona actor) {
        boolean esAdmin = personaControl.esAdministrador(actor);
        if (!esAdmin) {
            if (!(actor instanceof Usuario) || ((Usuario) actor).getId() != idUsuario) {
                throw new IllegalAccessError("Sin permiso para consultar pagos del usuario id=" + idUsuario);
            }
        }
        return pagoDAO.listarPorUsuario(idUsuario);
    }

    /**
     * Lista los pagos asociados a un turno.
     * Solo administradores pueden consultar pagos por turno.
     *
     * @param idTurno ID del turno.
     * @param actor   administrador.
     * @return lista de pagos del turno.
     */
    public List<Pago> listarPorTurno(int idTurno, Persona actor) {
        personaControl.validarOperacion(actor, PersonaControl.OperacionPersona.GESTIONAR_PAGOS);
        return pagoDAO.listarPorTurno(idTurno);
    }

    /**
     * Busca un pago por su ID.
     *
     * @param idPago ID del pago.
     * @param actor  administrador (solo admins pueden buscar pagos por ID directamente).
     * @return {@link Optional} con el pago, o vacío si no existe.
     */
    public Optional<Pago> buscarPorId(long idPago, Persona actor) {
        personaControl.validarOperacion(actor, PersonaControl.OperacionPersona.GESTIONAR_PAGOS);
        return pagoDAO.buscarPorId(idPago);
    }
}
