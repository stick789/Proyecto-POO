package entidades;

/**
 * Entrenador — Persona con rol ENTRENADOR en el sistema.
 *
 * CORRECCIONES APLICADAS:
 *
 * 1. BUG: self-assignment en constructor
 *    ANTES: this.idInstalacion = idInstalacion;  ← idInstalacion no era parámetro,
 *           se leía el campo vacío (siempre quedaba 0).
 *    AHORA: se eliminó el campo idInstalacion porque no pertenece a esta entidad.
 *           La BD solo guarda (id_entrenador, especialidad) en la tabla entrenador.
 *           La relación con instalaciones se gestiona a través del Turno.
 *
 * 2. BUG: login de entrenadores no funcionaba
 *    ANTES: Entrenador no tenía campo contraseña, por lo que PersonaDAO.mapear()
 *           creaba un Usuario cuando el rol era ENTRENADOR, perdiendo el tipo real.
 *    AHORA: se agrega contraseña con getter/setter. PersonaDAO.mapear() ya puede
 *           instanciar Entrenador correctamente para el flujo de login.
 *
 * 3. MEJORA: consistencia con la jerarquía Persona
 *    Se agrega setId() además de setIdEntrenador() para que el DAO pueda usar
 *    el setter estándar sin conocer el subtipo concreto.
 */
public class Entrenador extends Persona {

    private String especialidad;   // "Natación" o "Gimnasio"
    private int    idEntrenador;
    private String contraseña;     // Hash PBKDF2 — necesario para el flujo de login
    private String rolBD;          // Rol leído desde la base de datos

    public Entrenador() {
        super();
    }

    /**
     * Constructor principal.
     * No recibe contraseña porque se hashea externamente (EntrenadorControl)
     * antes de persistir; se establece luego con setContraseña().
     */
    public Entrenador(String nombre, String email, String especialidad,
                      String tipoDocumento, String numDocumento, int idEntrenador) {
        super(nombre, email, tipoDocumento, numDocumento);
        this.especialidad  = especialidad;
        this.idEntrenador  = idEntrenador;
        // ← idInstalacion ELIMINADO: no existe en la tabla entrenador de la BD.
        //   La relación con instalaciones es a través del Turno, no del Entrenador.
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    @Override
    public int getId() {
        return idEntrenador;
    }

    public String getEspecialidad() {
        return especialidad;
    }

    public String getContraseña() {
        return contraseña;
    }

    public String getRolBD() {
        return rolBD;
    }

    @Override
    public String getRol() {
        return rolBD != null ? rolBD : "ENTRENADOR";
    }

    // ── Setters ──────────────────────────────────────────────────────────────

    /** Setter estándar — consistente con setId() esperado por la jerarquía. */
    public void setId(int idEntrenador) {
        this.idEntrenador = idEntrenador;
    }

    /** Alias explícito para el DAO de entrenadores. */
    public void setIdEntrenador(int idEntrenador) {
        this.idEntrenador = idEntrenador;
    }

    public void setEspecialidad(String especialidad) {
        this.especialidad = especialidad;
    }

    /** Recibe la contraseña ya hasheada (PBKDF2). Nunca almacenar texto plano. */
    public void setContraseña(String contraseña) {
        this.contraseña = contraseña;
    }

    public void setRolBD(String rolBD) {
        this.rolBD = rolBD;
    }
}
