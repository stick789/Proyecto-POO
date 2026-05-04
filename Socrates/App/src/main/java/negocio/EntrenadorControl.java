package negocio;

import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import dao.IEntrenadorDAO;
import entidades.Entrenador;
import entidades.Persona;

/**
 * EntrenadorControl — Lógica de negocio para el módulo de entrenadores.
 *
 * <p><b>Operaciones:</b></p>
 * <ul>
 *   <li>Registrar y actualizar entrenadores (solo administradores).</li>
 *   <li>Activar / desactivar entrenadores (baja lógica).</li>
 *   <li>Consultar entrenadores disponibles por especialidad.</li>
 *   <li>Paginación de listados para vistas administrativas.</li>
 * </ul>
 *
 * <p>La contraseña del entrenador se hashea con PBKDF2 usando
 * {@link PersonaControl#generarHashPBKDF2(String)} antes de persistir,
 * igual que para usuarios regulares.</p>
 */
public class EntrenadorControl {

    private static final Logger LOG = Logger.getLogger(EntrenadorControl.class.getName());

    private final IEntrenadorDAO entrenadorDAO;
    private final PersonaControl personaControl;

    public EntrenadorControl(IEntrenadorDAO entrenadorDAO, PersonaControl personaControl) {
        this.entrenadorDAO  = entrenadorDAO;
        this.personaControl = personaControl;
    }

    // ================================================================= REGISTRO

    /**
     * Registra un nuevo entrenador en el sistema.
     *
     * @param entrenador objeto con los datos del entrenador (sin contraseña hasheada).
     * @param claveRaw   contraseña en texto plano (se hashea aquí antes de persistir).
     * @param actor      administrador que realiza el registro.
     * @return {@code "OK"} si se registró correctamente, o un código de error.
     */
    public String registrar(Entrenador entrenador, String claveRaw, Persona actor) {
        personaControl.validarOperacion(actor, PersonaControl.OperacionPersona.MODIFICAR_USUARIO);

        if (entrenador == null || claveRaw == null || claveRaw.isBlank()) {
            return "DATOS_INVALIDOS";
        }
        if (!esEspecialidadValida(entrenador.getEspecialidad())) {
            return "ESPECIALIDAD_INVALIDA";
        }

        try {
            String hash = PersonaControl.generarHashPBKDF2(claveRaw);
            if (hash == null) return "ERROR";

            entrenadorDAO.insertar(entrenador, hash);
            return "OK";
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al registrar entrenador", e);
            return "ERROR";
        }
    }

    // ================================================================= ACTUALIZACIÓN

    /**
     * Actualiza los datos de un entrenador existente.
     * Solo administradores pueden modificar entrenadores.
     *
     * @param entrenador datos actualizados.
     * @param actor      administrador que realiza la modificación.
     */
    public void actualizar(Entrenador entrenador, Persona actor) {
        personaControl.validarOperacion(actor, PersonaControl.OperacionPersona.MODIFICAR_USUARIO);

        if (!esEspecialidadValida(entrenador.getEspecialidad())) {
            throw new IllegalArgumentException("Especialidad inválida: " + entrenador.getEspecialidad() +
                    ". Valores permitidos: Natación, Gimnasio.");
        }

        entrenadorDAO.actualizar(entrenador);
    }

    /**
     * Desactiva un entrenador (baja lógica). No elimina el registro.
     * Solo administradores pueden desactivar entrenadores.
     *
     * @param idEntrenador ID del entrenador.
     * @param actor        administrador.
     * @return {@code true} si se desactivó correctamente.
     */
    public boolean desactivar(int idEntrenador, Persona actor) {
        personaControl.validarOperacion(actor, PersonaControl.OperacionPersona.ELIMINAR_USUARIO);
        return entrenadorDAO.desactivar(idEntrenador);
    }

    /**
     * Reactiva un entrenador previamente desactivado.
     * Solo administradores pueden reactivar entrenadores.
     *
     * @param idEntrenador ID del entrenador.
     * @param actor        administrador.
     * @return {@code true} si se reactivó correctamente.
     */
    public boolean activar(int idEntrenador, Persona actor) {
        personaControl.validarOperacion(actor, PersonaControl.OperacionPersona.MODIFICAR_USUARIO);
        return entrenadorDAO.activar(idEntrenador);
    }

    /**
     * Elimina físicamente un entrenador. Solo administradores pueden eliminarlo.
     * Usar {@link #desactivar} en la mayoría de los casos para preservar auditoría.
     *
     * @param idEntrenador ID del entrenador.
     * @param actor        administrador.
     */
    public void eliminar(int idEntrenador, Persona actor) {
        personaControl.validarOperacion(actor, PersonaControl.OperacionPersona.ELIMINAR_USUARIO);
        entrenadorDAO.eliminar(idEntrenador);
    }

    // ================================================================= CONSULTAS

    /**
     * Busca un entrenador por su ID.
     *
     * @param idEntrenador ID del entrenador.
     * @return {@link Optional} con el entrenador, o vacío si no existe.
     */
    public Optional<Entrenador> buscarPorId(int idEntrenador) {
        return entrenadorDAO.buscarPorId(idEntrenador);
    }

    /**
     * Lista entrenadores de forma paginada, filtrados por nombre.
     * Solo administradores pueden listar entrenadores.
     *
     * @param texto          fragmento de nombre (vacío = todos).
     * @param totalPorPagina registros por página.
     * @param numPagina      página actual empezando en 1.
     * @param actor          administrador.
     * @return lista paginada de entrenadores.
     */
    public List<Entrenador> listar(String texto, int totalPorPagina, int numPagina, Persona actor) {
        personaControl.validarOperacion(actor, PersonaControl.OperacionPersona.LISTAR_USUARIOS);
        return entrenadorDAO.listar(texto, totalPorPagina, numPagina);
    }

    /**
     * Lista entrenadores disponibles (activos) con una especialidad dada.
     * Útil para que el administrador elija qué entrenador asignar a un turno.
     *
     * @param especialidad "Natación" o "Gimnasio".
     * @param actor        administrador.
     * @return lista de entrenadores con esa especialidad.
     */
    public List<Entrenador> listarPorEspecialidad(String especialidad, Persona actor) {
        personaControl.validarOperacion(actor, PersonaControl.OperacionPersona.LISTAR_USUARIOS);
        if (!esEspecialidadValida(especialidad)) {
            throw new IllegalArgumentException("Especialidad inválida: " + especialidad);
        }
        return entrenadorDAO.listarPorEspecialidad(especialidad);
    }

    /**
     * Retorna el total de entrenadores activos.
     * Usado para calcular páginas en la vista de administrador:
     * {@code totalPaginas = (int) Math.ceil((double) total() / totalPorPagina)}
     */
    public int total() {
        return entrenadorDAO.total();
    }

    // ================================================================= PRIVADOS

    /**
     * Valida que la especialidad sea uno de los valores permitidos.
     * Los valores permitidos son: "Natación" y "Gimnasio" (sin distinguir mayúsculas).
     */
    private boolean esEspecialidadValida(String especialidad) {
        return especialidad != null
                && ("natación".equalsIgnoreCase(especialidad)
                    || "gimnasio".equalsIgnoreCase(especialidad));
    }
}
