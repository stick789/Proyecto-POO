package entidades;

public class Entrenador extends Persona  {
    private String especialidad;

    public Entrenador(String nombre, String email, String especialidad, String tipoDocumento, String numDocumento) {
        super(nombre, email, tipoDocumento, numDocumento);
        this.especialidad = especialidad;
    }

    public String getEspecialidad() {
        return especialidad;
    }

    public void setEspecialidad(String especialidad) {
        this.especialidad = especialidad;
    }
@ Override
public String getRol() {
    return "ENTRENADOR";
}
    
}
