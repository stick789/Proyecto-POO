package LogicaCita;

import java.time.LocalDateTime;

import entidades.Historial_citas;
import entidades.Turno;

public final class HistorialCitasServicio {

    private HistorialCitasServicio() {
    }

    public static Historial_citas desdeTurno(Turno turno, String detalle) {
        if (turno == null) {
            throw new IllegalArgumentException("El turno no puede ser null.");
        }

        return new Historial_citas(
                0,
                turno.getIdTurno(),
                turno.getEstado(),
                LocalDateTime.now(),
                turno.getUsuario() != null ? turno.getUsuario().getId() : null,
                turno.getInstalacion() != null ? turno.getInstalacion().getIdInstalacion() : null,
                detalle
        );
    }
}