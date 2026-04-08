package LogicaAdmin;

public class Cliente {
 

    private String nombre;
    private String correo;
    private int NumeroDocumento;
 
    public Cliente(String nom, String car, int numDoc) //Constructor de la clase cliente
    {
        this.nombre = nom;
        this.correo = car;
        this.NumeroDocumento = numDoc;
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
   
}