
package entidades;

public class Usuario extends Persona {
    
    private int idUsuario;
    private String contraseña;
    private boolean esAfiliado;
    private String categoria; //puede se A B o C o  null si no es afiliado
    private String rolBD; // Rol leído desde la base de datos

public Usuario() {
    super();
}

/*
Constructor  sin contraseña
Para listados administrativos
 */

public Usuario(int idUsuario, String nombre, String email, String tipoDocumento, String numDocumento, boolean esAfiliado, String categoria){
    super(nombre, email, tipoDocumento, numDocumento); // Llamada al constructor de la clase padre (persona)
    this.idUsuario = idUsuario;    
    this.categoria = categoria;
    // El usuario es marcado como  no afiliado si la categoría es nula o "NO AFILIADO", entonces no se considera afiliado
    this.esAfiliado = esAfiliado && categoria != null && !categoria.trim().equalsIgnoreCase("NO AFILIADO");
}

    /*
    Constructor completo con contraseña
    Para proceso de autenticación, se necesita la contraseña.
    */
  public Usuario(int idUsuario, String nombre, String email, String contraseña, String tipoDocumento, String numDocumento, boolean esAfiliado, String categoria){
    super(nombre, email, tipoDocumento, numDocumento); // Llamada al constructor de la clase padre (persona)
    this.idUsuario = idUsuario;
    this.contraseña = contraseña;
    this.categoria = categoria ;
        this.esAfiliado = esAfiliado && categoria != null && !categoria.trim().equalsIgnoreCase("NO AFILIADO");
  }

   
    // Getters
    public int getId() {
        return idUsuario;
    }
    public String getContraseña() {
        return contraseña;
    }
    public String getCategoria() {
        return categoria;
    }
    public boolean isEsAfiliado() {// Método para determinar si el usuario es afiliado o no
        return esAfiliado;
    }
    // Setters
    public void setId(int idUsuario) {
        this.idUsuario = idUsuario;
    }
   
    public void setContraseña(String contraseña){
        this.contraseña = contraseña;
    }
    public void setCategoria(String categoria){
        this.categoria = categoria;
        this.esAfiliado = categoria != null && 
                         !categoria.trim().equalsIgnoreCase("NO AFILIADO");// Si la categoría es nula o "NO AFILIADO", el usuario no es considerado afiliado
    }

    public String getRolBD() {
        return rolBD;
    }

    public void setRolBD(String rolBD) {
        this.rolBD = rolBD;
    }

    // Implementación del método abstracto de la clase persona
    @Override
    public String getRol() {
        // Prioriza el rol desde BD si está disponible, sino usa la clase por defecto
        return rolBD != null ? rolBD : "USUARIO";
    }
}
