package LogicaCita;

import dao.IHistorialCitasDAO;
import dao.ITurnoDAO;
import entidades.Entrenador;
import entidades.Instalacion;
import entidades.Persona;
import entidades.Piscina;
import entidades.Turno;
import negocio.PersonaControl;

/**
 * ServicioAsignarEntrenador — Asigna un entrenador a un turno existente.
 *
 * CORRECCIONES:
 *
 * 1. BUG: Integer.parseInt(turno.getIdTurno()) eliminado.
 *    ANTES: int idTurno = Integer.parseInt(turno.getIdTurno());
 *           Como idTurno era String, se hacía la conversión String→int.
 *    AHORA: int idTurno = turno.getIdTurno();
 *           idTurno es int directamente en Turno.java.
 *
 * 2. BUG: mensaje del historial tenía código fuente como texto literal.
 *    ANTES: historialDAO.insertar(... "\"Entrenador asignado: \" + entrenador.getNombre()..."
 *           Se guardaba literalmente esa cadena de código en la BD.
 *    AHORA: el detalle se construye correctamente con concatenación de valores reales.
 *
 * 3. MEJORA: nombre de método en camelCase (convención Java).
 *    ANTES: public void AsignarEntrenadorATurno(...)  ← PascalCase (convención de clase)
 *    AHORA: public void asignarEntrenadorATurno(...)  ← camelCase (convención de método)
 */
public class ServicioAsignarEntrenador {

    private final ITurnoDAO          turnoDAO;
    private final IHistorialCitasDAO historialDAO;
    private final PersonaControl     personaControl;

    public ServicioAsignarEntrenador(ITurnoDAO turnoDAO,
                                     IHistorialCitasDAO historialDAO,
                                     PersonaControl personaControl) {
        this.turnoDAO      = turnoDAO;
        this.historialDAO  = historialDAO;
        this.personaControl = personaControl;
    }

    /**
     * Asigna un entrenador a un turno de piscina o gimnasio.
     * Solo puede ser invocado por un administrador.
     *
     * @param turno      turno al que se asigna el entrenador.
     * @param entrenador entrenador con la especialidad compatible.
     * @param actor      administrador que realiza la operación.
     */
    public void asignarEntrenadorATurno(Turno turno, Entrenador entrenador, Persona actor) {
        if (actor      == null) throw new IllegalArgumentException("El actor no puede ser null.");
        if (turno      == null) throw new IllegalArgumentException("El turno no puede ser null.");
        if (entrenador == null) throw new IllegalArgumentException("El entrenador no puede ser null.");

        personaControl.validarOperacion(actor, PersonaControl.OperacionPersona.ASIGNAR_TURNO_ENTRENADOR);

        Instalacion instalacion = turno.getInstalacion();
        if (instalacion == null)
            throw new IllegalArgumentException("El turno debe tener una instalación asociada.");

        if (!personaControl.esEntrenador(entrenador))
            throw new IllegalArgumentException(
                "La persona asignada no tiene el rol de entrenador.");

        if (!especialidadCompatible(entrenador, instalacion))
            throw new IllegalArgumentException(
                "El entrenador '" + entrenador.getNombre() +
                "' no tiene la especialidad requerida para: " + instalacion.getTipo());

        // Asignar en memoria y persistir
        turno.setEntrenador(entrenador);

        int idTurno      = turno.getIdTurno();          // ← int directo, sin parseInt()
        int idEntrenador = entrenador.getId();

        turnoDAO.actualizarEntrenador(idTurno, idEntrenador);

        // Registrar en historial con el mensaje correcto (no código como texto)
        String detalle = "Entrenador asignado: " + entrenador.getNombre() +
                         " (ID: " + idEntrenador + ").";  // ← concatenación real
        historialDAO.insertar(HistorialCitasServicio.desdeTurno(turno, detalle));
    }

    /**
     * Valida que la especialidad del entrenador sea compatible con la instalación.
     * Piscina requiere "Natación"; Gimnasio requiere "Gimnasio".
     */
    private boolean especialidadCompatible(Entrenador entrenador, Instalacion instalacion) {
        String especialidad = entrenador.getEspecialidad();
        if (instalacion instanceof Piscina)
            return "Natación".equalsIgnoreCase(especialidad);
        else
            return "Gimnasio".equalsIgnoreCase(especialidad);
    }
}
