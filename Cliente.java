public class Cliente {
 
    private String nombre;
    private String correo;
    private int NumeroDocumento;
 
    public Cliente(String nom, String car, int numDoc) //Constructor de la clase cliente
    {
        nombre = nom;
        correo = car;
        NumeroDocumento = numDoc;
    }
   
    public int getNumeroDocumento() {
        return NumeroDocumento;
    }
    
    public String getNombre() {
        return nombre;
    }

    public String getCorreo() {
        return correo;
    }
    
    @Override
    public String toString() {
        return "Nombre: " + nombre + ", Correo: " + correo + ", Documento: " + NumeroDocumento;
    }
   
    public void mostrar() {
        System.out.println(toString());
    }
}