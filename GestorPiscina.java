import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Scanner;
import java.util.Stack;

public class GestorPiscina{

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        ArrayList<Cliente> registro = new ArrayList<>();
        Queue<Cliente> cola = new LinkedList<>();
        Stack<Cliente> historial = new Stack<>();

        boolean iniciando = true;

        while (iniciando) {
            System.out.println("\nMenú de Facturación");
            System.out.println("1. Registrar cliente ");
            System.out.println("2. Mostrar registro de clientes");
            System.out.println("3. Atender siguiente cliente ");
            System.out.println("4. Mostrar cola de espera");
            System.out.println("5. Mostrar historial de atendidos");
            System.out.println("6. Ver último atendido");
            System.out.println("7. Eliminar cliente del registro");
            System.out.println("8. Salir");
            System.out.print("Seleccione una opción: ");

            String opt = sc.next();

            switch (opt) {
                case "1": // Registrar
                    System.out.print("Nombre: ");
                    String nombre = readNextLine(sc);
                    System.out.print("Correo: ");
                    String correo = readNextLine(sc);
                    System.out.print("Número de documento: ");
                    int doc;
                    try {
                        doc = Integer.parseInt(readNextLine(sc));
                    } catch (NumberFormatException ex) {
                        System.out.println("Documento inválido. Operación cancelada.");
                        break;
                    }
                    Cliente c = new Cliente(nombre, correo, doc);
                    registro.add(c);
                    cola.add(c);
                    System.out.println("Cliente registrado y encolado: " + c.getNombre());
                    break;

                case "2": // Mostrar registro
                    if (registro.isEmpty()) {
                        System.out.println("Registro vacío.");
                    } else {
                        System.out.println("Registro de clientes");
                        for (int i = 0; i < registro.size(); i++) {
                            System.out.println((i + 1) + ". " + registro.get(i));
                        }
                    }
                    break;

                case "3": // Atender siguiente
                    if (cola.isEmpty()) {
                        System.out.println("No hay clientes en la cola.");
                    } else {
                        Cliente atendido = cola.poll();
                        historial.push(atendido);
                        System.out.println("Atendiendo cliente: " + atendido);
                    }
                    break;

                case "4": // Mostrar cola
                    if (cola.isEmpty()) {
                        System.out.println("La cola está vacía.");
                    } else {
                        System.out.println(" Cola de espera");
                        int pos = 1;
                        for (Cliente cli : cola) {
                            System.out.println(pos++ + ". " + cli);
                        }
                    }
                    break;

                case "5": // Mostrar historial
                    if (historial.isEmpty()) {
                        System.out.println("Historial vacío.");
                    } else {
                        System.out.println(" Historial de atendidos ");
                        for (int i = historial.size() - 1; i >= 0; i--) {
                            System.out.println((historial.size() - i) + ". " + historial.get(i));
                        }
                    }
                    break;

                case "6": // Ver último atendido
                    if (historial.isEmpty()) {
                        System.out.println("Nadie ha sido atendido aún.");
                    } else {
                        System.out.println("Último atendido: " + historial.peek());
                    }
                    break;

                case "7": // Eliminar cliente del registro por documento
                    System.out.print("Ingrese número de documento del cliente a eliminar: ");
                    int numToRemove;
                    try {
                        numToRemove = Integer.parseInt(readNextLine(sc));
                    } catch (NumberFormatException ex) {
                        System.out.println("Documento inválido.");
                        break;
                    }
                    boolean removed = false;
                    for (int i = 0; i < registro.size(); i++) {
                        if (registro.get(i).getNumeroDocumento() == numToRemove) {
                            Cliente removedClient = registro.remove(i);
                            // lo quita de la cola si está alli
                            cola.removeIf(cli -> cli.getNumeroDocumento() == numToRemove);
                            System.out.println("Cliente eliminado del registro: " + removedClient);
                            removed = true;
                            break;
                        }
                    }
                    if (!removed) {
                        System.out.println("Cliente no encontrado en el registro.");
                    }
                    break;

                case "8": // Salir
                    iniciando = false;
                    break;

                default:
                    System.out.println("Opción inválida.");
            }
        }

        System.out.println("Saliendo. Hasta luego.");
        sc.close();
    }

   
    private static String readNextLine(Scanner sc) {
        String s = sc.nextLine();
        if (s.isEmpty()) {
            // si la línea estaba vacía , leer de nuevo
            s = sc.nextLine();
        }
        return s.trim();
    }
}
