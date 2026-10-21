import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Plantilla base para los servicios independientes del sistema.
 * Contiene un bucle continuo (while(true)) que emite un heartbeat
 * cada 3 segundos exactos.
 */
public class ServicioBasebucle {

    public static void main(String[] args) {
        // Identificador del servicio (se puede adaptar para DATABASE, WEB, FTP, etc.)
        String serviceName = "SERVICIO_BASE";

        // Formateador de tiempo para la salida por consola
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

        // Bucle continuo: se ejecuta de forma ininterrumpida hasta que sea detenido por el programa principal
        while (true) {
            // Obtener la hora actual del sistema
            String currentTime = LocalTime.now().format(timeFormatter);

            // Emision del mensaje de estado del servicio
            System.out.printf("[%s] [%s] [OK] Servicio en ejecucion constante.%n", currentTime, serviceName);
            
            // Forzar la salida limpia del bufer para transmitir en tiempo real
            System.out.flush();

            // Retraso exacto de 3 segundos (3000 milisegundos) entre cada emision
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                // Manejo de la interrupcion del hilo cuando el monitor ordena detener el proceso
                System.err.println("El servicio " + serviceName + " ha sido detenido.");
                break;
            }
        }
    }
}