package entidades;

public class Entrenador extends Persona  {
    private String especialidad; //  "Natación", "Gimnasio"
    private String rolBD; // Rol leído desde la base de datos
    private int idEntrenador; 
    private int idInstalacion;
    public Entrenador(String nombre, String email, String especialidad, String tipoDocumento, String numDocumento, int idEntrenador ) {
        super(nombre, email, tipoDocumento, numDocumento);
        this.especialidad = especialidad; //Natación o Gimnasio
        this.idEntrenador = idEntrenador;
        this.idInstalacion = idInstalacion;
    }

    public String getEspecialidad() {
        return especialidad;
    }

    public void setEspecialidad(String especialidad) {
        this.especialidad = especialidad;
    }

    public String getRolBD() {
        return rolBD;
    }

    public void setRolBD(String rolBD) {
        this.rolBD = rolBD;
    }
    @Override
    public int getId() {
        return idEntrenador;
    }

    public void setIdEntrenador(int idEntrenador) {
        this.idEntrenador = idEntrenador;
    }
    @Override
    public String getRol() {
        // Prioriza el rol desde BD si está disponible, sino usa la clase por defecto
        return rolBD != null ? rolBD : "ENTRENADOR";
    }

   
}

