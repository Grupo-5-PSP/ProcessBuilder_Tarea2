import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class MonitorPrincipal {

    public static void main(String[] args) {
        String[] nombresServicios = {"DATABASE", "WEB_SERVER", "FTP_SERVER", "MAIL_SERVER", "FIREWALL", "BALANCER", "BACKUP"};
        Process[] procesos = new Process[nombresServicios.length];

        System.out.println("[MONITOR] Iniciando los servicios...");

        // Lanzamiento básico de los procesos
        for (int i = 0; i < nombresServicios.length; i++) {
            procesos[i] = lanzarServicio(nombresServicios[i]);
        }

        System.out.println("[MONITOR] Servicios lanzados correctamente.");
    }

    private static Process lanzarServicio(String nombreServicio) {
        try {
            // Lanzamiento estándar sin classpath avanzado
            ProcessBuilder pb = new ProcessBuilder("java", "Servicio", nombreServicio);
            pb.redirectErrorStream(true);
            return pb.start();
        } catch (IOException e) {
            System.out.println("[MONITOR] [ERROR] No se pudo lanzar: " + nombreServicio);
            return null;
        }
    }
}
