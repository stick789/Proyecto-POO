package LogicaCita;


import dao.IHistorialCitasDAO;
import dao.ITurnoDAO;
import entidades.Entrenador;
import entidades.Instalacion;
import entidades.Persona;
import entidades.Piscina;
import entidades.Turno;
import negocio.PersonaControl;

public class ServicioAsignarEntrenador {
    private final ITurnoDAO turnoDAO;
    private final IHistorialCitasDAO historialDAO;
    private final PersonaControl personaControl;

    public ServicioAsignarEntrenador(ITurnoDAO turnoDAO, IHistorialCitasDAO historialDAO, PersonaControl personaControl) {
        this.turnoDAO = turnoDAO;
        this.historialDAO = historialDAO;
        this.personaControl = personaControl;
    }
     /**
     * Asigna un entrenador a un turno de piscina o gimnasio.
     * Solo puede ser invocado por un administrador.
     */
    public void AsignarEntrenadorATurno(Turno turno, Entrenador entrenador, Persona actor) {
       if (actor == null) throw new IllegalArgumentException("El actor no puede ser null.");
       if (turno == null) throw new IllegalArgumentException("El turno no puede ser null.");
       if (entrenador == null) throw new IllegalArgumentException("El entrenador no puede ser null.");
        // Verificar que el actor es administrador
        personaControl.validarOperacion(actor, PersonaControl.OperacionPersona.ASIGNAR_TURNO_ENTRENADOR);

       Instalacion instalacion = turno.getInstalacion();
       if (instalacion == null) {
           throw new IllegalArgumentException("El turno debe tener una instalación asociada.");
       }

       if (!personaControl.esEntrenador(entrenador)) {
           throw new IllegalArgumentException("La persona asignada como entrenador no tiene el rol de entrenador.");
       }
     //Validar la especialidad del entrenador para la instalación
       if (!EspecialidadCompatible(entrenador, instalacion)) {
           throw new IllegalArgumentException(
               "El entrenador no tiene la especialidad requerida para esta instalación: " + instalacion.getTipo()
           );
       }
    // Asignar el entrenador al turno
       turno.setEntrenador(entrenador);
      
    // Persistir Id del entrenador 

    int idEntrenador = entrenador.getId();
         
    // Se parsea el idTurno a entero,.
    int idTurno = Integer.parseInt(turno.getIdTurno());

       // Actualizar el turno en la base de datos con el nuevo entrenador asignado
        turnoDAO.actualizarEntrenador(idTurno, idEntrenador);

// Registrar la asignación en el historial de citas
        historialDAO.insertar(HistorialCitasServicio.desdeTurno(turno, " \"Entrenador asignado: \" + entrenador.getNombre() +\r\n" + //
                        "                \" (ID: \" + idEntrenador + \")."));

        
    }
// Método helper para validar que la especialidad del entrenador es compatible con el tipo de instalación
private boolean EspecialidadCompatible(Entrenador entrenador, Instalacion instalacion) {
    String especialidad = entrenador.getEspecialidad();

    if (instalacion instanceof Piscina){
        return "Natación".equalsIgnoreCase(especialidad);
    }
    else {
        return "Gimnasio".equalsIgnoreCase(especialidad);
    }
}

}
