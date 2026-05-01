package entidades;

public class Administrador extends Persona {
    private int id_administrador;
    private String contraseña_administrador;
    private String rolBD; // Rol leído desde la base de datos

    public Administrador() {
        super();
    }

    public Administrador(int id_administrador, String nombre, String email, String contraseña_administrador, String tipoDocumento, String numDocumento){
        super(nombre, email, tipoDocumento, numDocumento); // Llamada al constructor de la clase padre (persona)
        this.id_administrador = id_administrador;
        this.contraseña_administrador = contraseña_administrador;
    
}

// Getters
@Override
public int getId() {
    return id_administrador;
}
 public String getRolBD() {
        return rolBD;
    }
public String getContraseñaAdmin() {
    return contraseña_administrador;
}
// Setters
public void setIdAdmin(int id_administrador) {
    this.id_administrador = id_administrador; 
}
public void setContraseñaAdmin(String contraseña_administrador){
    this.contraseña_administrador = contraseña_administrador;
}



    public void setRolBD(String rolBD) {
        this.rolBD = rolBD;
    }

    // Implementación del método abstracto de la clase persona
    @Override
    public String getRol() {
        // Prioriza el rol desde BD si está disponible, sino usa la clase por defecto
        return rolBD != null ? rolBD : "ADMINISTRADOR";
    }
}
