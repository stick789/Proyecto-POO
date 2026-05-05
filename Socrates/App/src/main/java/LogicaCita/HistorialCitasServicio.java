package LogicaCita;

import java.time.LocalDateTime;

import entidades.Historial_citas;
import entidades.Turno;

public final class HistorialCitasServicio {

    private HistorialCitasServicio() {}

    public static Historial_citas desdeTurno(Turno turno, String detalle) {
        if (turno == null) {
            throw new IllegalArgumentException("El turno no puede ser null.");
        }

        // getIdTurno() y getIdInstalacion() ahora devuelven int (corregido en entidades),
        // pero Historial_citas.idTurno e idInstalacion siguen siendo String en BD.
        // Se convierten aquí con String.valueOf() para no alterar Historial_citas.
        return new Historial_citas(
                0,
                String.valueOf(turno.getIdTurno()),
                turno.getEstado(),
                LocalDateTime.now(),
                turno.getUsuario()      != null ? turno.getUsuario().getId()                         : null,
                turno.getInstalacion()  != null ? String.valueOf(turno.getInstalacion().getIdInstalacion()) : null,
                detalle
        );
    }
}
