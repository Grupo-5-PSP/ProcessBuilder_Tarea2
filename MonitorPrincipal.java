import java.io.IOException;

public class MonitorPrincipal {

    public static void main(String[] args) {
        // Nombres de los 7 servicios que exige la práctica
        String[] nombresServicios = {"BaseDatos", "Web", "FTP", "Correo", "Firewall", "Balanceador", "Backup"};
        Process[] procesos = new Process[nombresServicios.length];

        System.out.println("[MONITOR] Iniciando el arranque de servicios...");

        // 1. TAREA DE SERGIO: Lanzamiento inicial de los 7 procesos
        for (int i = 0; i < nombresServicios.length; i++) {
            procesos[i] = lanzarServicio(nombresServicios[i]);
        }

        // 2. TU TAREA (FMT-01 / RES-02): Detección de caídas y reinicio automático
        while (true) {
            for (int i = 0; i < procesos.length; i++) {
                // Si el proceso es nulo o isAlive() devuelve false, significa que ha fallado o se ha apagado
                if (procesos[i] == null || !procesos[i].isAlive()) {
                    System.out.println("[ALERTA] [" + nombresServicios[i] + "] [ERROR] El servicio ha caído. Reiniciando...");
                    // Se invoca un nuevo ProcessBuilder.start() exclusivamente para el servicio caído
                    procesos[i] = lanzarServicio(nombresServicios[i]);
                }
            }

            // Pequeña pausa de 2 segundos para no saturar el procesador del ordenador con el bucle infinito
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                System.out.println("Error en la pausa del monitor.");
            }
        }
    }

    // Método auxiliar que instancia el ProcessBuilder
    private static Process lanzarServicio(String nombreServicio) {
        try {
            // Llama a la clase Servicio.java compilada y le pasa el nombre como argumento
            ProcessBuilder pb = new ProcessBuilder("java", "Servicio", nombreServicio);
            
            // TRUCO CRÍTICO: Al no poder usar hilos, INHERIT hace que los System.out del 
            // subproceso se impriman directamente en la consola principal sin bloquear el código.
            pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
            pb.redirectError(ProcessBuilder.Redirect.INHERIT);
            
            return pb.start();
        } catch (IOException e) {
            System.out.println("[MONITOR] Error grave al intentar lanzar: " + nombreServicio);
            return null;
        }
    }
}