package entidades;

/*
Clase abstracta persona que contiene los atributos comunes a usuario y afiliado
*/
public abstract class Persona {
    private String nombre;
    private String email;
    private String tipoDocumento;
    private String numDocumento;

public Persona() {
}

// Constructor
public Persona(String nombre, String email, String tipoDocumento, String numDocumento){
    this.nombre = nombre;
    this.email = email;
    this.tipoDocumento = tipoDocumento;
    this.numDocumento = numDocumento;
}
// Getters

public String getNombre() {
    return nombre;
}
public String getEmail() {
    return email;
}

public String getTipoDocumento() {
    return tipoDocumento;
}
public String getNumDocumento() {
    return numDocumento;
}
// Setters

public void setNombre(String nombre){
    this.nombre = nombre;
}
public void setEmail(String email){
    this.email = email;
}
public void setTipoDocumento(String tipoDocumento){
    this.tipoDocumento = tipoDocumento;
}
public void setNumDocumento(String numDocumento){
    this.numDocumento = numDocumento;
}
// Método abstracto para obtener el rol de la persona (usuario o administrador)
public abstract String getRol();
}