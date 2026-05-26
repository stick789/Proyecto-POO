# TAREAS PROYECTO
- Se finalizo la agregación del **DAO**
> [!NOTE]
> Se aplico el patron DAO dentro del proyecto, en base a lo presente en las entidades de las bases de datos y el funcionamiento del proyecto como tal.
> Se actualizaron los archivos de conexion y modulos de informacion para solucionar errores de ciclo en relación a los DAO.
# Pendientes del proyecto 
-  Agregar entidad entrenador, corregir ciertos aspectos de la logica del codigo
>[!NOTE]
>Agregar la entidad entrenador no como una persona que puede ingresar al sistema, sino que, por el momento, unicamente con los metodos correspondientes para que pueda ser asignado a turnos de piscina o gimnasio para.☑️
>Corregir en el paquete DAO, debido a que la clase padre es Persona.☑️
>Terminar la parte de logica de negocio ☑️
>Agregar Pagos en la base de datos☑️, en el DAO y en negocio
>Corregir logica del Gestor de Citas ☑️
>Agregar Entrenador y a la base de datos, también agregar el atributo del mismo dentro de Turno ☑️

# Terminado

> [!NOTE]
> Se terminó de agregar lo faltante, se separaron entrenadores de usuarios y administradores, se aplicó el cifrado SHA256 para las contraseñas además de rellenar el espacio tipo blank del SecondaryView.

> [!IMPORTANT]
> Revisar la especialización a futuro para entrenadores logrando así su uso según la disponibilidad de la instalación sin necesidad de que estos lleguen a quedar bloqueados en una sola área.
>
> Asignar ID a las instalaciones para el uso de los entrenadores.

# Avances Importantes

> [!NOTE]
>La pasarela de pagos ya quedo funcional, el problema es que la public y private key sepueden ver dentro del proyecto, pero eso se solucionara a futuro (no se como).

> [!IMPORTANT]
> Para probar la pasarela puse  un id predeterminado de forma temporal para que sea el  1, pronto habra que cambiar eso, por lo que todavia no tenemos el metodo de agendar turnos, el archivo que use para iniciar la pasarla de pagos por si sola, no la commitee para evitar confuciones.
> Abajo dejo el codigo para el main a utilizar por si quieren probar la pasarela de pagos

## Pasarela de Pagos 

Clase temporal para lanzar la vista de la pasarela de pagos. Nunca se va a poder completar un pego porque la cuenta no esta habilitada para recibirlos.
### 🧪 Datos de Prueba

**Importante**: Estas credenciales son solo para entornos de **pruebas** (sandbox). No utilizar en producción.

#### Tarjetas de Crédito de Prueba

| Estado                    | Franquicia         | Número                  | Expiración | CVV | Respuesta                              |
|---------------------------|--------------------|-------------------------|------------|-----|----------------------------------------|
| **Aceptada**              | Visa               | 4575 6231 8229 0326     | 12/2027    | 123 | Aceptada                               |
| **Fondos insuficientes**  | Visa               | 4151 6115 2758 3283     | 12/2027    | 123 | Fondos insuficientes                   |
| **Fallida**               | Mastercard         | 5170 3944 9037 9427     | 12/2027    | 123 | Error de comunicación con el centro    |
| **Pendiente**             | American Express   | 3731 1885 6457 642      | 12/2027    | 123 | Transacción pendiente por validación   |

#### Cuentas Daviplata de Prueba

| Tipo de Documento         | Número              |
|---------------------------|---------------------|
| **Cédula de Ciudadanía**  | 1134568019          |
| **Cédula de Extranjería** | 786630              |

<details>
<summary>📄 Ver código completo (PasarelaPagosLauncher.java)</summary>

```java
package socratesGui;

import java.io.IOException;
import java.util.Optional;

import dao.EntrenadorDAO;
import dao.InstalacionDAO;
import dao.PersonaDAO;
import dao.TurnoDAO;
import database.Conexion;
import entidades.Turno;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Launcher temporal para abrir solo la vista de pasarela de pagos.
 */
public class PasarelaPagosLauncher extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        try {
            Conexion.getInstancia().conectarConFeedback();
        } catch (RuntimeException e) {
            throw e;
        }

        FXMLLoader loader = new FXMLLoader(
            PasarelaPagosLauncher.class.getResource("/Interface/pasarelaPagos.fxml")
        );

        Scene scene = new Scene(loader.load(), 700, 550);
        PasarelaPagosController controller = loader.getController();
        controller.setHostServices(getHostServices());

        // Cargar turno por parámetro (opcional)
        String turnoIdParam = System.getProperty("turnoId");
        if (turnoIdParam == null || turnoIdParam.isBlank()) {
            turnoIdParam = System.getenv("TURNOID");
        }

        if (turnoIdParam != null && !turnoIdParam.isBlank()) {
            try {
                int turnoId = Integer.parseInt(turnoIdParam.trim());
                TurnoDAO turnoDAO = new TurnoDAO(new PersonaDAO(), new InstalacionDAO(), new EntrenadorDAO());
                Optional<Turno> turnoOpt = turnoDAO.buscarPorId(turnoId);
                
                if (turnoOpt.isPresent()) {
                    controller.setTurno(turnoOpt.get());
                } else {
                    Turno turno = new Turno();
                    turno.setIdTurno(turnoId);
                    controller.setTurno(turno);
                }
            } catch (Exception e) {
                System.err.println("No se pudo cargar el turno: " + e.getMessage());
            }
        }

        stage.setTitle("Pasarela ePayco (Prueba)");
        stage.setResizable(false);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
```
</details>

>[!IMPORTANT]
>Rehacer la base de datos por si acaso llegan a salir errores, se hizo la visual para ambos usuario y admin.
>
>Revisar errores del tamaño de la ventana de registro exitoso.
>
>Añadir información a la base de datos.
>
>Designar rol al usuario recién registrado para evitar errores.
>
>Añadir finalmente pasarela de pagos y JasperReports y conectarlos al programa/base de datos para así tener el sistema completo.
>

# Ultimos pendientes

> [!NOTE]
>Mejorar interface, Añadir Jasper, y organizar presios dependiendo de categoria. Ademas de preparar presentación final.

**Credenciales de prueba**

- **ESTUDIANTE**: estudiante@test.com — **Contraseña:** TestEstudiante123!
- **Categoría A**: userA@test.com — **Contraseña:** TestUserA123!
- **Categoría B**: userB@test.com — **Contraseña:** TestUserB123!
- **Categoría C**: userC@test.com — **Contraseña:** TestUserC123!

**Cómo importar la BD / añadir estas cuentas**

- Importar el dump completo:
```bash
mysql -u USUARIO -p proyecto_poo < "C:/Users/LENOVO/Socrates/Proyecto-POO/proyecto_poo (1).sql"