package LogicaCita;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import dao.IHistorialCitasDAO;
import dao.IInstalacionDAO;
import dao.ITurnoDAO;
import entidades.Instalacion;
import entidades.Persona;
import entidades.Piscina;
import entidades.Turno;
import entidades.Usuario;
import negocio.PersonaControl;

/**
 * ServicioTurnos — Gestiona operaciones de reserva, cancelación, completación y reagendamiento de turnos.
 *
 * CORRECCIONES aplicadas respecto a la versión anterior:
 *   - Eliminados Integer.parseInt() sobre getIdTurno() y getIdInstalacion() (ya son int).
 *   - Eliminada String temporal idTurnoTemp: Turno ahora recibe int 0 como id temporal.
 *   - Eliminada instalacion.getTurnos().add(): getTurnos() fue removido de Instalacion.
 *   - Corregido .equals() sobre int primitivo → operador ==.
 *   - contarReservasActivasEnCarril: usa turnoDAO en vez de piscina.getTurnos() (eliminado).
 */
public class ServicioTurnos {

    private int maxPersonasPorCarril;

    private final ITurnoDAO           turnoDAO;
    private final IHistorialCitasDAO  historialDAO;
    private final IInstalacionDAO     instalacionDAO;
    private final ValidadorTurno      validador;
    private final PoliticaCancelacion politica;
    private final PersonaControl      personaControl;

    // ── Constructores ─────────────────────────────────────────────────────────

    public ServicioTurnos(ITurnoDAO turnoDAO,
                          IHistorialCitasDAO historialDAO,
                          IInstalacionDAO instalacionDAO) {
        this(3, turnoDAO, historialDAO, instalacionDAO,
             new ValidadorTurno(), new PoliticaCancelacion());
    }

    public ServicioTurnos(int maxPersonasPorCarril,
                          ITurnoDAO turnoDAO,
                          IHistorialCitasDAO historialDAO,
                          IInstalacionDAO instalacionDAO,
                          ValidadorTurno validador,
                          PoliticaCancelacion politica) {
        this.maxPersonasPorCarril = validarMax(maxPersonasPorCarril);
        this.turnoDAO       = turnoDAO;
        this.historialDAO   = historialDAO;
        this.instalacionDAO = instalacionDAO;
        this.validador      = validador;
        this.politica       = politica;
        this.personaControl = new PersonaControl();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Operación: Reservar
    // ══════════════════════════════════════════════════════════════════════════

    public Turno reservarTurno(LocalDateTime fechaHora, int duracionMinutos,
                               Usuario usuario, Instalacion instalacion,
                               Integer numeroCarril, Persona actor) {
        if (actor == null) throw new IllegalArgumentException("El actor no puede ser null.");

        boolean esAdmin = personaControl.esAdministrador(actor);

        if (!esAdmin) {
            if (!(actor instanceof Usuario) || ((Usuario) actor).getId() != usuario.getId()) {
                throw new IllegalAccessError("Solo el propio usuario puede reservar su turno.");
            }
        }
        personaControl.validarOperacion(actor, PersonaControl.OperacionPersona.AGENDAR_CITA);

        Integer carrilFinal = numeroCarril;
        if (instalacion instanceof Piscina && carrilFinal == null) {
            carrilFinal = asignarCarrilAutomatico((Piscina) instalacion);
        }

        Turno turno = reservarTurnoInterno(fechaHora, duracionMinutos, usuario, instalacion, carrilFinal);
        historialDAO.insertar(HistorialCitasServicio.desdeTurno(turno, "Turno reservado."));
        return turno;
    }

    /**
     * Crea y persiste un turno.
     * Se pasa 0 como id temporal; turnoDAO.insertar() lo reemplaza con el INT real de la BD.
     * No se llama instalacion.getTurnos() porque esa lista fue eliminada de Instalacion.
     */
    private Turno reservarTurnoInterno(LocalDateTime fechaHora, int duracionMinutos,
                                       Usuario usuario, Instalacion instalacion,
                                       Integer numeroCarril) {
        if (instalacion == null) throw new IllegalArgumentException("La instalacion no puede ser null.");

        List<Turno> turnosActivosUsuario = turnoDAO.listarPorUsuario(usuario.getId()).stream()
                .filter(t -> Turno.ESTADO_RESERVADO.equals(t.getEstado()))
                .collect(Collectors.toList());

        // getIdInstalacion() ya devuelve int — sin parseInt()
        List<Turno> reservasActivasInstalacion =
            turnoDAO.listarReservadosPorInstalacion(instalacion.getIdInstalacion());

        validador.validarReserva(instalacion, fechaHora, duracionMinutos,
            turnosActivosUsuario, reservasActivasInstalacion);

        Integer carrilValidado = validarCarril(instalacion, numeroCarril);
        instalacion.descontarCupo();

        // id temporal 0 — turnoDAO.insertar() asigna el id real de la BD
        Turno turno = new Turno(0, fechaHora, duracionMinutos, usuario, instalacion);
        turno.setNumeroCarrilAsignado(carrilValidado);
        // instalacion.getTurnos().add(turno)  ← ELIMINADO: getTurnos() fue removido de Instalacion

        turnoDAO.insertar(turno);
        actualizarAforoBD(instalacion);

        return turno;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Operación: Cancelar
    // ══════════════════════════════════════════════════════════════════════════

    private void cancelarTurnoInterno(Turno turno, boolean esAdministrador) {
        if (turno == null) throw new IllegalArgumentException("El turno no puede ser null.");

        if (esAdministrador) {
            if (!politica.puedeCancelarComoAdmin(turno, true)) {
                throw new IllegalStateException(politica.motivoCancelacionBloqueada(turno));
            }
        } else {
            politica.validarCancelacion(turno);
        }

        turno.setEstado(Turno.ESTADO_CANCELADO);
        turno.getInstalacion().liberarCupo();

        // getIdTurno() ya devuelve int — sin parseInt()
        turnoDAO.actualizarEstado(turno.getIdTurno(), Turno.ESTADO_CANCELADO);
        actualizarAforoBD(turno.getInstalacion());
        historialDAO.insertar(HistorialCitasServicio.desdeTurno(turno, "Turno cancelado."));
    }

    public void cancelarTurno(Turno turno, Persona actor) {
        if (actor == null) throw new IllegalArgumentException("El actor no puede ser null.");

        boolean esAdmin = personaControl.esAdministrador(actor);

        if (!esAdmin) {
            if (!(actor instanceof Usuario) || ((Usuario) actor).getId() != turno.getUsuario().getId()) {
                throw new IllegalAccessError("Solo el dueño del turno o un administrador puede cancelarlo.");
            }
        }

        personaControl.validarOperacion(actor, PersonaControl.OperacionPersona.CANCELAR_CITA);
        cancelarTurnoInterno(turno, esAdmin);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Operación: Completar
    // ══════════════════════════════════════════════════════════════════════════

    private void completarTurnoInterno(Turno turno) {
        if (turno == null) throw new IllegalArgumentException("El turno no puede ser null.");
        if (!Turno.ESTADO_RESERVADO.equals(turno.getEstado())) {
            throw new IllegalStateException("Solo se puede completar un turno en estado RESERVADO.");
        }

        turno.setEstado(Turno.ESTADO_COMPLETADO);
        turno.getInstalacion().liberarCupo();

        // getIdTurno() ya devuelve int — sin parseInt()
        turnoDAO.actualizarEstado(turno.getIdTurno(), Turno.ESTADO_COMPLETADO);
        actualizarAforoBD(turno.getInstalacion());
        historialDAO.insertar(HistorialCitasServicio.desdeTurno(turno, "Turno completado."));
    }

    public void completarTurno(Turno turno, Persona actor) {
        if (actor == null) throw new IllegalArgumentException("El actor no puede ser null.");
        personaControl.validarOperacion(actor, PersonaControl.OperacionPersona.ADMINISTRAR_HISTORIAL);
        completarTurnoInterno(turno);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Operación: Reagendar
    // ══════════════════════════════════════════════════════════════════════════

    private Turno reagendarTurnoInterno(Turno turnoExistente,
                                        LocalDateTime nuevaFechaHora,
                                        boolean esAdministrador) {
        if (turnoExistente == null) throw new IllegalArgumentException("El turno no puede ser null.");
        if (nuevaFechaHora == null) throw new IllegalArgumentException("La nueva fecha no puede ser null.");

        if (!politica.puedeCancelarComoAdmin(turnoExistente, esAdministrador)) {
            throw new IllegalStateException(
                "No es posible reagendar: " + politica.motivoCancelacionBloqueada(turnoExistente)
            );
        }

        Instalacion instalacion = turnoExistente.getInstalacion();
        Usuario usuario         = turnoExistente.getUsuario();
        int duracion            = turnoExistente.getDuracionMinutos();

        // getIdTurno() ya devuelve int — comparación con == en vez de .equals()
        List<Turno> otrosTurnos = turnoDAO.listarPorUsuario(usuario.getId()).stream()
                .filter(t -> Turno.ESTADO_RESERVADO.equals(t.getEstado())
                          && t.getIdTurno() != turnoExistente.getIdTurno())
                .collect(Collectors.toList());

        validador.validarFechaFutura(nuevaFechaHora);
        validador.validarSinSolapamiento(otrosTurnos, nuevaFechaHora, duracion);

        String detalle = "Reagendado desde " + turnoExistente.getFechaHora() + " hacia " + nuevaFechaHora;

        turnoExistente.setEstado(Turno.ESTADO_CANCELADO);
        instalacion.liberarCupo();

        // getIdTurno() ya devuelve int — sin parseInt()
        turnoDAO.actualizarEstado(turnoExistente.getIdTurno(), Turno.ESTADO_CANCELADO);
        actualizarAforoBD(instalacion);
        historialDAO.insertar(
            HistorialCitasServicio.desdeTurno(turnoExistente, "Reagendado — turno original cancelado. " + detalle)
        );

        Turno nuevoTurno = reservarTurnoInterno(
            nuevaFechaHora, duracion, usuario, instalacion,
            turnoExistente.getNumeroCarrilAsignado()
        );

        historialDAO.insertar(
            HistorialCitasServicio.desdeTurno(nuevoTurno, "Reagendado — nuevo turno creado. " + detalle)
        );

        return nuevoTurno;
    }

    public Turno reagendarTurno(Turno turnoExistente, LocalDateTime nuevaFechaHora, Persona actor) {
        if (actor == null) throw new IllegalArgumentException("El actor no puede ser null.");

        boolean esAdmin = personaControl.esAdministrador(actor);

        if (!esAdmin) {
            if (!(actor instanceof Usuario) || ((Usuario) actor).getId() != turnoExistente.getUsuario().getId()) {
                throw new IllegalAccessError("Solo el dueño del turno o un administrador puede reagendarlo.");
            }
        }

        personaControl.validarOperacion(actor, PersonaControl.OperacionPersona.REAGENDAR_CITA);
        return reagendarTurnoInterno(turnoExistente, nuevaFechaHora, esAdmin);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Métodos auxiliares privados
    // ══════════════════════════════════════════════════════════════════════════

    private void actualizarAforoBD(Instalacion instalacion) {
        // getIdInstalacion() ya devuelve int — sin parseInt()
        instalacionDAO.actualizarAforo(instalacion.getIdInstalacion(), instalacion.getAforoActual());
    }

    private Integer validarCarril(Instalacion instalacion, Integer numeroCarril) {
        if (instalacion instanceof Piscina) {
            Piscina piscina = (Piscina) instalacion;
            if (numeroCarril == null)
                throw new IllegalArgumentException("Debe asignar un carril para reservas de piscina.");
            if (numeroCarril <= 0 || numeroCarril > piscina.getNumeroCarriles())
                throw new IllegalArgumentException("Carril invalido. Rango: 1 a " + piscina.getNumeroCarriles());
            if (contarReservasActivasEnCarril(piscina, numeroCarril) >= maxPersonasPorCarril)
                throw new IllegalStateException(
                    "El carril " + numeroCarril + " ya alcanzo el maximo de " + maxPersonasPorCarril + " personas.");
            return numeroCarril;
        }
        if (numeroCarril != null)
            throw new IllegalArgumentException("Solo se puede asignar carril en instalaciones tipo piscina.");
        return null;
    }

    private int asignarCarrilAutomatico(Piscina piscina) {
        int carriles = piscina.getNumeroCarriles();
        if (carriles <= 0) throw new IllegalStateException("La piscina no tiene carriles configurados.");
        for (int carril = 1; carril <= carriles; carril++) {
            if (contarReservasActivasEnCarril(piscina, carril) < maxPersonasPorCarril) return carril;
        }
        throw new IllegalStateException("No hay carriles disponibles.");
    }

    /**
     * Cuenta reservas activas en un carril consultando la BD vía turnoDAO.
     * piscina.getTurnos() fue eliminado de Instalacion — se usa el DAO directamente.
     */
    private int contarReservasActivasEnCarril(Piscina piscina, int numeroCarril) {
        // getIdInstalacion() ya devuelve int — sin parseInt()
        List<Turno> reservas = turnoDAO.listarReservadosPorInstalacion(piscina.getIdInstalacion());
        int count = 0;
        for (Turno t : reservas) {
            Integer c = t.getNumeroCarrilAsignado();
            if (c != null && c == numeroCarril) count++;
        }
        return count;
    }

    private int validarMax(int max) {
        if (max <= 0) throw new IllegalArgumentException("El maximo de personas por carril debe ser mayor a 0.");
        return max;
    }

    public int getMaxPersonasPorCarril() { return maxPersonasPorCarril; }
    public void setMaxPersonasPorCarril(int max) { this.maxPersonasPorCarril = validarMax(max); }
}
