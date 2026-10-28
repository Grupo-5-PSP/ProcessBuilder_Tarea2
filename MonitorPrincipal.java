import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class MonitorPrincipal {

    public static void main(String[] args) {
        String[] nombresServicios = {"DATABASE", "WEB_SERVER", "FTP_SERVER", "MAIL_SERVER", "FIREWALL", "BALANCER", "BACKUP"};
        Process[] procesos = new Process[nombresServicios.length];
        BufferedReader[] lectores = new BufferedReader[nombresServicios.length];
        DateTimeFormatter formatoHora = DateTimeFormatter.ofPattern("HH:mm:ss");

        System.out.println("[" + LocalTime.now().format(formatoHora) + "] [MONITOR] [OK] Iniciando el arranque de los 7 servicios...");

        // Lanzamiento inicial de los 7 procesos y conexión de sus BufferedReader
        for (int i = 0; i < nombresServicios.length; i++) {
            procesos[i] = lanzarServicio(nombresServicios[i]);
            if (procesos[i] != null) {
                lectores[i] = new BufferedReader(new InputStreamReader(procesos[i].getInputStream()));
            }
        }

        // Bucle principal de monitorización
        while (true) {
            for (int i = 0; i < procesos.length; i++) {
                try {
                    while (lectores[i] != null && lectores[i].ready()) {
                        String linea = lectores[i].readLine();
                        if (linea != null) {
                            String horaActual = LocalTime.now().format(formatoHora);
                            System.out.println("[" + horaActual + "] " + linea);
                        }
                    }

                    // Detección de caídas y reinicio automático
                    if (procesos[i] == null || !procesos[i].isAlive()) {
                        String horaActual = LocalTime.now().format(formatoHora);
                        System.out.println("[" + horaActual + "] [" + nombresServicios[i] + "] [ALERTA] El servicio ha caído. Reiniciando...");
                        
                        procesos[i] = lanzarServicio(nombresServicios[i]);
                        if (procesos[i] != null) {
                            lectores[i] = new BufferedReader(new InputStreamReader(procesos[i].getInputStream()));
                        }
                    }
                } catch (IOException e) {
                    System.out.println("[MONITOR] [ERROR] Fallo leyendo el servicio " + nombresServicios[i]);
                }
            }

            // Pausa breve para no saturar la CPU
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    private static Process lanzarServicio(String nombreServicio) {
        try {
            // === ACTUALIZACIÓN CORE-01: Configuración del classpath para ProcessBuilder ===
            ProcessBuilder pb = new ProcessBuilder(
                "java", 
                "-cp", 
                System.getProperty("java.class.path"), 
                "Servicio", 
                nombreServicio
            );
            
            pb.redirectErrorStream(true);
            return pb.start();
        } catch (IOException e) {
            System.out.println("[MONITOR] [ERROR] No se pudo lanzar: " + nombreServicio);
            return null;
        }
    }
}

