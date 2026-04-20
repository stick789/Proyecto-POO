package entidades;

public class Administrador extends Persona {
    private int id_administrador;
    private String contraseña_administrador;

    public Administrador() {
        super();
    }

    public Administrador(int id_administrador, String nombre, String email, String contraseña_administrador, String tipoDocumento, String numDocumento){
        super(nombre, email, tipoDocumento, numDocumento); // Llamada al constructor de la clase padre (persona)
        this.id_administrador = id_administrador;
        this.contraseña_administrador = contraseña_administrador;
    
}

// Getters
public int getIdAdmin() {
    return id_administrador;
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
// Implementación del método abstracto de la clase persona
@Override
public String getRol() {
    return "ADMINISTRADOR";
}
}
