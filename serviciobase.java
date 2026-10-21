import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Plantilla base para los servicios del sistema.
 * Genera eventos periodicos cada 3 segundos con estados aleatorios (OK, WARNING, ERROR)
 */
public class ServicioBase {

    public static void main(String[] args) {
        // Comprobamos si nos han pasado el parametro para forzar un fallo inmediato
        boolean forceError = args.length > 0 && args[0].equalsIgnoreCase("--force-error");

        // Nombre del servicio (esto se cambiara segun el componente: DATABASE, WEB, FTP, etc.)
        String serviceName = "DATABASE"; 
        
        // Formateador para obtener la hora en formato HH:mm:ss
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

        // Bucle infinito: el servicio se mantiene emitiendo hasta que el monitor principal lo cierre
        while (true) {
            String currentTime = LocalTime.now().format(timeFormatter);
            String status;
            String message;

            // --- LOGICA DE ESTADOS Y PROBABILIDAD DE FALLO ---
            if (forceError) {
                // Caso 1: Error forzado manualmente mediante argumento de consola
                status = "ERROR";
                message = "Fallo critico provocado por parametro --force-error.";
            } else {
                // Caso 2: Emision aleatoria basada en probabilidades
                double probability = Math.random(); // Devuelve un numero entre 0.0 y 1.0

                if (probability < 0.15) {
                    // 15% de probabilidad de fallo (Requisito B del enunciado)
                    status = "ERROR";
                    message = "Error de conexion o fallo interno del servicio.";
                } else if (probability < 0.30) {
                    // 15% de probabilidad de advertencia (valores entre 0.15 y 0.30)
                    status = "WARNING";
                    message = "Elevado tiempo de respuesta o alta carga detectada.";
                } else {
                    // 70% de probabilidad restante de funcionamiento normal
                    status = "OK";
                    message = "Estado del servicio: Operativo (Ping correcto).";
                }
            }

            // --- IMPRESION CON EL FORMATO REQUERIDO ---
            // Formato estricto: [HH:mm:ss] [<IDENTIFICADOR_ORIGEN>] [<ESTADO>] <MENSAJE>
            System.out.printf("[%s] [%s] [%s] %s%n", currentTime, serviceName, status, message);

            // Importante para limpiar el bufer de salida e informar en tiempo real
            System.out.flush();

            // Pausa de 3 segundos entre transmisiones (Heartbeat)
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                // Si el proceso principal interrumpe el hilo, salimos limpiamente
                System.err.println("Servicio " + serviceName + " interrumpido externamente.");
                break;
            }
        }
    }
}