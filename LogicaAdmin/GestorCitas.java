package LogicaAdmin;

import java.util.ArrayList;
import java.util.Scanner;
import java.util.Stack;

public class GestorCitas {

    private final Scanner sc = new Scanner(System.in);
    private final ArrayList<Cliente> registro = new ArrayList<>();
    private final Stack<Cliente> historial = new Stack<>();

    public void iniciar() {
        boolean iniciando = true;

        while (iniciando) {
            System.out.println("\nGestor de Citas Agendadas");
            System.out.println("1. Agendar cita");
            System.out.println("2. Mostrar registro de citas");
            System.out.println("3. Mostrar historial de citas realizadas");
            System.out.println("4. Ver ultima cita registrada");
            System.out.println("5. Eliminar cita del registro");
            System.out.println("6. Salir");
            System.out.print("Seleccione una opcion: ");

            String opt = sc.next();

            switch (opt) {
                case "1":
                    System.out.print("Nombre: ");
                    String nombre = readNextLine(sc);
                    System.out.print("Correo: ");
                    String correo = readNextLine(sc);
                    System.out.print("Numero de documento: ");
                    int doc;
                    try {
                        doc = Integer.parseInt(readNextLine(sc));
                    } catch (NumberFormatException ex) {
                        System.out.println("Documento invalido. Operacion cancelada.");
                        break;
                    }
                    Cliente c = new Cliente(nombre, correo, doc);
                    // Agregar al registro y al historial
                    registro.add(c);
                    // Agregar al historial para mantener un registro de las citas realizadas
                    historial.push(c);
                    System.out.println("Cita agendada para: " + c.getNombre());
                    break;

                case "2":
                    if (registro.isEmpty()) {
                        System.out.println("Registro vacio.");
                    } else {
                        System.out.println("Registro de citas agendadas:");
                        // Mostrar el registro en orden de agendamiento
                        for (int i = 0; i < registro.size(); i++) {
                            System.out.println((i + 1) + ". " + registro.get(i));
                        }
                    }
                    break;

                case "3":
                    if (historial.isEmpty()) {
                        System.out.println("Historial vacio.");
                    } else {
                        System.out.println("Historial de citas realizadas");
                        for (int i = historial.size() - 1; i >= 0; i--) {
                            System.out.println((historial.size() - i) + ". " + historial.get(i));
                        }
                    }
                    break;

                case "4":
                    if (historial.isEmpty()) {
                        System.out.println("No hay citas registradas aun.");
                    } else {
                        System.out.println("Ultima cita registrada: " + historial.peek());// peek() devuelve el elemento en la parte superior de la pila sin eliminarlo, lo que permite ver la última cita registrada sin modificar el historial.
                    }
                    break;

                case "5":
                    System.out.print("Ingrese numero de documento del cliente a eliminar: ");
                    int numToRemove;
                    try {
                        numToRemove = Integer.parseInt(readNextLine(sc));
                    } catch (NumberFormatException ex) {
                        System.out.println("Documento invalido.");
                        break;
                    }

                    boolean removed = false;
                    for (int i = 0; i < registro.size(); i++) {
                        if (registro.get(i).getNumeroDocumento() == numToRemove) {
                            Cliente removedClient = registro.remove(i);
                            System.out.println("Cliente eliminado del registro: " + removedClient);
                            removed = true;
                            break;
                        }
                    }

                    if (!removed) {
                        System.out.println("Cliente no encontrado en el registro.");
                    }
                    break;

                case "6":
                    iniciando = false;
                    break;

                default:
                    System.out.println("Opcion invalida.");
            }
        }

        System.out.println("Saliendo. Hasta luego.");
        sc.close();
    }

    private static String readNextLine(Scanner sc) {
        String s = sc.nextLine();
        if (s.isEmpty()) {
            s = sc.nextLine();
        }
        return s.trim();
    }
}
