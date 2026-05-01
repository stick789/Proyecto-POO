package LogicaCita;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import dao.IHistorialCitasDAO;
import dao.IInstalacionDAO;
import dao.ITurnoDAO;
import entidades.Instalacion;
import entidades.Piscina;
import entidades.Turno;
import entidades.Usuario;

/**
 * ServicioTurnos — Gestiona operaciones de reserva, cancelación, completación y reagendamiento de turnos.
 *
 * Funcionalidades principales:
 *   - Reservar turnos validando disponibilidad y aforo
 *   - Cancelar turnos aplicando políticas de cancelación
 *   - Completar turnos marcándolos como finalizados
 *   - Reagendar turnos a nuevas fechas con registro de historial
 *   - Asignar carriles automáticamente en piscinas
 *   - Mantener registro de cambios en el historial de citas
 */
public class ServicioTurnos {

    private int maxPersonasPorCarril;

    private final ITurnoDAO           turnoDAO;
    private final IHistorialCitasDAO  historialDAO;
    private final IInstalacionDAO     instalacionDAO;
    private final ValidadorTurno      validador;
    private final PoliticaCancelacion politica;

    // Constructores

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
    }

    // Operación Reservar 

    /**
     * Reserva un turno asignando automáticamente un carril si es una piscina.
     * Registra el evento en el historial de citas.
     */
    public Turno reservarTurno(LocalDateTime fechaHora, int duracionMinutos,
                               Usuario usuario, Instalacion instalacion) {
        Integer carrilAsignado = null;
        if (instalacion instanceof Piscina) {
            carrilAsignado = asignarCarrilAutomatico((Piscina) instalacion);
        }
        return reservarTurno(fechaHora, duracionMinutos, usuario, instalacion, carrilAsignado);
    }

    public Turno reservarTurno(LocalDateTime fechaHora, int duracionMinutos,
                               Usuario usuario, Instalacion instalacion,
                               Integer numeroCarril) {
        // Reserva el turno internamente y registra en historial
        Turno turno = reservarTurnoInterno(fechaHora, duracionMinutos, usuario, instalacion, numeroCarril);
        historialDAO.insertar(HistorialCitasServicio.desdeTurno(turno, "Turno reservado."));
        return turno;
    }

    /**
     * Crea y persiste un turno en la base de datos.
     * Valida disponibilidad, descuenta cupo y actualiza aforo.
     * No registra en historial para permitir control granular desde operaciones de nivel superior.
     */
    private Turno reservarTurnoInterno(LocalDateTime fechaHora, int duracionMinutos,
                                       Usuario usuario, Instalacion instalacion,
                                       Integer numeroCarril) {
        if (instalacion == null) throw new IllegalArgumentException("La instalacion no puede ser null.");

        List<Turno> turnosActivos = turnoDAO.listarPorUsuario(usuario.getId()).stream()
                .filter(t -> Turno.ESTADO_RESERVADO.equals(t.getEstado()))
                .collect(Collectors.toList());

        validador.validarReserva(instalacion, fechaHora, duracionMinutos, turnosActivos);

        Integer carrilValidado = validarCarril(instalacion, numeroCarril);
        instalacion.descontarCupo();

        // idTurno temporal — turnoDAO.insertar() lo reemplaza con el INT real de la BD
        String idTurnoTemp = "TMP-" + System.currentTimeMillis();
        Turno turno = new Turno(idTurnoTemp, fechaHora, duracionMinutos, usuario, instalacion);
        turno.setNumeroCarrilAsignado(carrilValidado);
        instalacion.getTurnos().add(turno);

        turnoDAO.insertar(turno);         // actualiza turno.idTurno con el INT de la BD
        actualizarAforoBD(instalacion);

        return turno;
    }

    // Operación Cancelar 

    /**
     * Cancela un turno validando políticas de cancelación.
     * Libera el cupo, actualiza aforo y registra en historial.
     *
     * @param turno           turno a cancelar
     * @param esAdministrador si true, aplica políticas de administrador
     */
    public void cancelarTurno(Turno turno, boolean esAdministrador) {
        if (turno == null) throw new IllegalArgumentException("El turno no puede ser null.");

        // Valida cancelación aplicando políticas según privilegios
        if (esAdministrador) {
            if (!politica.puedeCancelarComoAdmin(turno, true)) {
                throw new IllegalStateException(politica.motivoCancelacionBloqueada(turno));
            }
        } else {
            politica.validarCancelacion(turno); // lanza excepción si no se puede cancelar
        }

        turno.setEstado(Turno.ESTADO_CANCELADO);
        turno.getInstalacion().liberarCupo();

        turnoDAO.actualizarEstado(Integer.parseInt(turno.getIdTurno()), Turno.ESTADO_CANCELADO);
        actualizarAforoBD(turno.getInstalacion());
        historialDAO.insertar(HistorialCitasServicio.desdeTurno(turno, "Turno cancelado."));
    }

    /** Versión sin privilegios de admin. */
    public void cancelarTurno(Turno turno) {
        cancelarTurno(turno, false);
    }

    //Operación Completar

    public void completarTurno(Turno turno) {
        if (turno == null) throw new IllegalArgumentException("El turno no puede ser null.");
        if (!Turno.ESTADO_RESERVADO.equals(turno.getEstado())) {
            throw new IllegalStateException("Solo se puede completar un turno en estado RESERVADO.");
        }

        turno.setEstado(Turno.ESTADO_COMPLETADO);
        turno.getInstalacion().liberarCupo();

        turnoDAO.actualizarEstado(Integer.parseInt(turno.getIdTurno()), Turno.ESTADO_COMPLETADO);
        actualizarAforoBD(turno.getInstalacion());
        historialDAO.insertar(HistorialCitasServicio.desdeTurno(turno, "Turno completado."));
    }

    //Operación Reagendar

    /**
     * Reagenda un turno existente a una nueva fecha y hora.
     * Valida la cancelación del turno original y crea uno nuevo.
     * Mantiene registro detallado en el historial de ambas operaciones.
     * Solo calcula el carril automáticamente si la nueva fecha es diferente.
     */
    public Turno reagendarTurno(Turno turnoExistente,
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

        List<Turno> otrosTurnos = turnoDAO.listarPorUsuario(usuario.getId()).stream()
                .filter(t -> Turno.ESTADO_RESERVADO.equals(t.getEstado())
                          && !t.getIdTurno().equals(turnoExistente.getIdTurno()))
                .collect(Collectors.toList());

        validador.validarFechaFutura(nuevaFechaHora);
        validador.validarSinSolapamiento(otrosTurnos, nuevaFechaHora, duracion);

        String detalle = "Reagendado desde " + turnoExistente.getFechaHora() + " hacia " + nuevaFechaHora;

        // Cancela el turno original y actualiza base de datos
        turnoExistente.setEstado(Turno.ESTADO_CANCELADO);
        instalacion.liberarCupo();
        turnoDAO.actualizarEstado(Integer.parseInt(turnoExistente.getIdTurno()), Turno.ESTADO_CANCELADO);
        actualizarAforoBD(instalacion);
        historialDAO.insertar(
            HistorialCitasServicio.desdeTurno(turnoExistente, "Reagendado — turno original cancelado. " + detalle)
        );

        // Crea el nuevo turno con validaciones
        Turno nuevoTurno = reservarTurnoInterno(
            nuevaFechaHora, duracion, usuario, instalacion,
            turnoExistente.getNumeroCarrilAsignado()
        );

        // Registra el nuevo turno en el historial
        historialDAO.insertar(
            HistorialCitasServicio.desdeTurno(nuevoTurno, "Reagendado — nuevo turno creado. " + detalle)
        );

        return nuevoTurno;
    }

    /**
     * Reagenda un turno sin privilegios de administrador.
     */
    public Turno reagendarTurno(Turno turnoExistente, LocalDateTime nuevaFechaHora) {
        return reagendarTurno(turnoExistente, nuevaFechaHora, false);
    }

    // Métodos auxiliares privados 

    private void actualizarAforoBD(Instalacion instalacion) {
        instalacionDAO.actualizarAforo(
                Integer.parseInt(instalacion.getIdInstalacion()),
                instalacion.getAforoActual()
        );
    }

    private Integer validarCarril(Instalacion instalacion, Integer numeroCarril) {
        if (instalacion instanceof Piscina) {
            Piscina piscina = (Piscina) instalacion;
            if (numeroCarril == null)
                throw new IllegalArgumentException("Debe asignar un carril para reservas de piscina.");
            if (numeroCarril <= 0 || numeroCarril > piscina.getNumeroCarriles())
                throw new IllegalArgumentException("Carril invalido. Rango: 1 a " + piscina.getNumeroCarriles());
            if (contarReservasActivasEnCarril(piscina, numeroCarril) >= maxPersonasPorCarril)
                throw new IllegalStateException("El carril " + numeroCarril + " ya alcanzo el maximo de " + maxPersonasPorCarril + " personas.");
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

    private int contarReservasActivasEnCarril(Piscina piscina, int numeroCarril) {
        int reservas = 0;
        for (Turno t : piscina.getTurnos()) {
            Integer c = t.getNumeroCarrilAsignado();
            if (c != null && c == numeroCarril && Turno.ESTADO_RESERVADO.equals(t.getEstado())) reservas++;
        }
        return reservas;
    }

    private int validarMax(int max) {
        if (max <= 0) throw new IllegalArgumentException("El maximo de personas por carril debe ser mayor a 0.");
        return max;
    }

    public int getMaxPersonasPorCarril() { return maxPersonasPorCarril; }
    public void setMaxPersonasPorCarril(int max) { this.maxPersonasPorCarril = validarMax(max); }
}