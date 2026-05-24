package negocio;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import dao.IPersonaDAO;
import entidades.Administrador;
import entidades.Persona;
import entidades.Usuario;

/**
 * PersonaControl — Lógica de negocio centralizada para personas del sistema.
 *
 * <p><b>Responsabilidades:</b></p>
 * <ol>
 *   <li><b>Autenticación</b>: login universal (Usuario y Administrador) con
 *       hashing PBKDF2. Soporta migración automática desde hashes SHA-256 legacy.</li>
 *   <li><b>Autorización</b>: comprueba si una persona puede realizar una operación
 *       determinada según su rol (USUARIO, ENTRENADOR, ADMINISTRADOR).</li>
 *   <li><b>Registro</b>: valida unicidad de email y delega la persistencia al DAO.</li>
 * </ol>
 *
 * <p><b>Flujo de login (inspirado en {@code UsuarioControl.login()} del sistema de
 * referencia comentado):</b></p>
 * <ol>
 *   <li>El DAO devuelve la persona con el hash almacenado.</li>
 *   <li>Si el hash contiene ":" → PBKDF2 moderno.</li>
 *   <li>Si no contiene ":" → SHA-256 legacy; si es válido, migra automáticamente
 *       a PBKDF2 para el siguiente login.</li>
 *   <li>Retorna {@code "0"} (credenciales incorrectas) o {@code "1"} (login OK).</li>
 * </ol>
 *
 * <p><b>Precondición:</b> la columna {@code usuarios.contraseña} debe ser
 * {@code VARCHAR(255)} para almacenar el formato PBKDF2
 * {@code "iteraciones:saltHex:hashHex"} (~140 caracteres). Ver script SQL adjunto.</p>
 */
public class PersonaControl {

    private static final Logger LOG = Logger.getLogger(PersonaControl.class.getName());

    // ── Parámetros PBKDF2 (mismos que el sistema de referencia) ──────────────
    private static final int PBKDF2_ITERATIONS = 65_536;
    private static final int PBKDF2_KEY_LENGTH = 256;
    private static final int SALT_BYTES        = 16;

    private final IPersonaDAO datos;

    // Constructor con inyección (preferido para testing)
    public PersonaControl(IPersonaDAO datos) {
        this.datos = datos;
    }

    // Constructor por defecto (compatibilidad con código existente)
    public PersonaControl() {
        this.datos = new dao.PersonaDAO();
    }

    // ================================================================= LOGIN

    /**
     * Autenticación universal: funciona para Usuario y Administrador.
     *
     * @param email correo electrónico del usuario.
     * @param clave contraseña en texto plano.
     * @return {@code "0"} = credenciales incorrectas, {@code "1"} = login OK.
     */
    public String login(String email, String clave) {
        if (email == null || clave == null) return "0";

        Optional<Persona> opt = datos.buscarPorEmail(email);
        if (opt.isEmpty()) return "0";

        Persona persona = opt.get();
        String hashAlmacenado = obtenerHash(persona);
        if (hashAlmacenado == null) return "0";

        boolean claveValida;

        if (hashAlmacenado.contains(":")) {
            // Hash moderno PBKDF2: "iteraciones:saltHex:hashHex"
            claveValida = verificarPBKDF2(clave, hashAlmacenado);
        } else {
            // Hash legacy SHA-256 sin salt
            claveValida = hashAlmacenado.equals(sha256Legacy(clave));
            if (claveValida) {
                // Migración automática → próximo login ya usará PBKDF2
                String nuevoHash = generarHashPBKDF2(clave);
                if (nuevoHash != null) {
                    datos.actualizarContraseña(persona.getId(), nuevoHash);
                    LOG.log(Level.INFO, "Hash migrado a PBKDF2 para persona id={0}", persona.getId());
                }
            }
        }

        return claveValida ? "1" : "0";
    }

    // ================================================================= REGISTRO

    /**
     * Registra un nuevo usuario en el sistema.
     *
     * @param usuario  objeto Usuario con todos los datos (sin contraseña hasheada).
     * @param claveRaw contraseña en texto plano; se hashea aquí antes de persistir.
     * @return {@code "OK"} si se registró correctamente,
     *         {@code "EMAIL_DUPLICADO"} si el email ya existe,
     *         {@code "ERROR"} en caso de fallo.
     */
    public String registrar(Usuario usuario, String claveRaw) {
        try {
            if (datos.existe(usuario.getEmail())) return "EMAIL_DUPLICADO";

            String hash = generarHashPBKDF2(claveRaw);
            if (hash == null) return "ERROR";

            usuario.setContraseña(hash);
            datos.insertar(usuario);
            return "OK";
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al registrar usuario", e);
            return "ERROR";
        }
    }

    // ================================================================= AUTORIZACIÓN

    /**
     * Operaciones que el sistema puede autorizar o denegar según el rol.
     */
    public enum OperacionPersona {
        AGENDAR_CITA,
        CANCELAR_CITA,
        REAGENDAR_CITA,
        ADMINISTRAR_HISTORIAL,
        MODIFICAR_USUARIO,
        ELIMINAR_USUARIO,
        LISTAR_USUARIOS,
        ASIGNAR_TURNO_ENTRENADOR,
        GESTIONAR_PAGOS
    }

    public boolean esUsuario(Persona persona) {
        return persona != null && "USUARIO".equalsIgnoreCase(persona.getRol());
    }

    public boolean esEntrenador(Persona persona) {
        return persona != null && "ENTRENADOR".equalsIgnoreCase(persona.getRol());
    }

    public boolean esAdministrador(Persona persona) {
        return persona != null && "ADMINISTRADOR".equalsIgnoreCase(persona.getRol());
    }

    /**
     * Comprueba si una persona tiene permiso para realizar una operación.
     *
     * <p>Matriz de permisos:</p>
     * <ul>
     *   <li>AGENDAR / CANCELAR / REAGENDAR → Usuario, Administrador.</li>
     *   <li>ADMINISTRAR_HISTORIAL / MODIFICAR / ELIMINAR / LISTAR /
     *       ASIGNAR_ENTRENADOR / GESTIONAR_PAGOS → solo Administrador.</li>
     * </ul>
     */
    public boolean puedeRealizar(Persona persona, OperacionPersona operacion) {
        if (persona == null || operacion == null) {
            throw new IllegalArgumentException("La persona y la operación no pueden ser null.");
        }
        switch (operacion) {
            case AGENDAR_CITA:
            case CANCELAR_CITA:
            case REAGENDAR_CITA:
                return esUsuario(persona) || esAdministrador(persona);

            case ADMINISTRAR_HISTORIAL:
            case MODIFICAR_USUARIO:
            case ELIMINAR_USUARIO:
            case LISTAR_USUARIOS:
            case ASIGNAR_TURNO_ENTRENADOR:
            case GESTIONAR_PAGOS:
                return esAdministrador(persona);

            default:
                return false;
        }
    }

    /**
     * Versión que lanza excepción si el permiso es denegado.
     *
     * @throws IllegalAccessError si la persona no tiene el permiso requerido.
     */
    public void validarOperacion(Persona persona, OperacionPersona operacion) {
        if (!puedeRealizar(persona, operacion)) {
            throw new IllegalAccessError(
                "La persona con rol '" + (persona != null ? persona.getRol() : "null") +
                "' no tiene permiso para: " + operacion);
        }
    }

    // ================================================================= HASHING

    /**
     * Genera un hash PBKDF2 con salt aleatorio.
     * Formato almacenado: {@code "iteraciones:saltHex:hashHex"}
     * Longitud aproximada: 140 caracteres → requiere VARCHAR(255).
     *
     * @param password contraseña en texto plano.
     * @return hash formateado, o {@code null} si ocurre un error criptográfico.
     */
    public static String generarHashPBKDF2(String password) {
        try {
            SecureRandom rnd = new SecureRandom();
            byte[] salt      = new byte[SALT_BYTES];
            rnd.nextBytes(salt);

            PBEKeySpec spec = new PBEKeySpec(
                    password.toCharArray(), salt, PBKDF2_ITERATIONS, PBKDF2_KEY_LENGTH);
            byte[] hash = SecretKeyFactory
                    .getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec).getEncoded();

            return PBKDF2_ITERATIONS + ":" + hexDe(salt) + ":" + hexDe(hash);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            LOG.log(Level.SEVERE, "Error al generar hash PBKDF2", e);
            return null;
        }
    }

    // ── Métodos privados de hashing ───────────────────────────────────────────

    /**
     * Verifica una clave contra un hash PBKDF2 almacenado.
     * Usa {@link MessageDigest#isEqual} para comparación en tiempo constante
     * (evita ataques de timing).
     */
    private static boolean verificarPBKDF2(String password, String stored) {
        try {
            String[] parts = stored.split(":");
            if (parts.length != 3) return false;

            int    iters    = Integer.parseInt(parts[0]);
            byte[] salt     = bytesDeHex(parts[1]);
            byte[] expected = bytesDeHex(parts[2]);

            PBEKeySpec spec = new PBEKeySpec(
                    password.toCharArray(), salt, iters, expected.length * 8);
            byte[] actual = SecretKeyFactory
                    .getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec).getEncoded();

            return MessageDigest.isEqual(expected, actual); // tiempo constante
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al verificar PBKDF2", e);
            return false;
        }
    }

    /** Hash SHA-256 simple para compatibilidad con hashes legacy (sin salt). */
    private static String sha256Legacy(String valor) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(valor.getBytes());
            return hexDe(hash);
        } catch (NoSuchAlgorithmException e) {
            LOG.log(Level.SEVERE, "Error SHA-256 legacy", e);
            return null;
        }
    }

    /** Obtiene el hash almacenado según el subtipo concreto de Persona. */
    private static String obtenerHash(Persona persona) {
        if (persona instanceof Administrador) {
            return ((Administrador) persona).getContraseñaAdmin();
        }
        if (persona instanceof Usuario) {
            return ((Usuario) persona).getContraseña();
        }
        return null;
    }

    // ── Utilidades hex ────────────────────────────────────────────────────────

    private static String hexDe(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    private static byte[] bytesDeHex(String hex) {
        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
        }
        return bytes;
    }
}
