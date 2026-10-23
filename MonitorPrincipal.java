import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class MonitorPrincipal {

    // 1. Definir un array constante con los nombres de los 7 servicios
    private static final String[] SERVICIOS = {
        "DATABASE", "WEB_SERVER", "FTP_SERVER", "MAIL_SERVER", "FIREWALL", "LOAD_BALANCER", "BACKUP"
    };

    public static void main(String[] args) {
        System.out.println("=== INICIALIZANDO MONITOR PRINCIPAL ===");

        // 2. Crear los arrays para los procesos y sus lectores de flujo
        Process[] procesos = new Process[7];
        BufferedReader[] lectores = new BufferedReader[7];

        // 4. Recorrer con un for los 7 nombres en el main
        for (int i = 0; i < SERVICIOS.length; i++) {
            try {
                // Arrancar cada proceso y guardarlo en su posición i
                procesos[i] = lanzarServicio(SERVICIOS[i]);
                
                // Conectar su tubería de lectura
                lectores[i] = new BufferedReader(new InputStreamReader(procesos[i].getInputStream()));
                
                System.out.println("Servicio iniciado correctamente: " + SERVICIOS[i]);
            } catch (Exception e) {
                System.err.println("Error al arrancar el servicio " + SERVICIOS[i] + ": " + e.getMessage());
            }
        }
        
        // Mantener el programa activo o proceder con la lectura de los lectores según convenga
    }

    // 3. Crear el método lanzarServicio que configura y devuelve un Process usando ProcessBuilder
    private static Process lanzarServicio(String nombreServicio) throws IOException {
        ProcessBuilder pb = new ProcessBuilder(
            "java", 
            "-cp", 
            System.getProperty("java.class.path"), 
            "ServicioSistema", 
            nombreServicio
        );
        
        // Arranca el proceso mediante pb.start() y lo devuelve
        return pb.start();
    }
}
