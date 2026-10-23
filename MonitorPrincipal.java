import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class MonitorPrincipal {

    private static final String[] NOMBRES = {
        "DATABASE", "WEB_SERVER", "FTP_SERVER", "MAIL_SERVER", "FIREWALL", "LOAD_BALANCER", "BACKUP"
    };

    public static void main(String[] args) {
        System.out.println("=== PANEL DE CONTROL - LECTURA NO BLOQUEANTE ===");

        Process[] procesos = new Process[7];
        BufferedReader[] lectores = new BufferedReader[7];

        // === ACTUALIZACIÓN (PASO 1): Array para controlar el último mensaje de cada servicio ===
        long[] ultimoMensaje = new long[7];
        for (int i = 0; i < 7; i++) {
            ultimoMensaje[i] = System.currentTimeMillis();
        }

        // Arrancar los 7 procesos hijos
        for (int i = 0; i < NOMBRES.length; i++) {
            try {
                procesos[i] = lanzarServicio(NOMBRES[i]);
                lectores[i] = new BufferedReader(new InputStreamReader(procesos[i].getInputStream()));
            } catch (Exception e) {
                System.err.println("[-] Error al arrancar el servicio: " + NOMBRES[i]);
            }
        }

        // === ACTUALIZACIÓN (PASO 2): Bucle infinito principal ===
        while (true) {
            try {
                // === ACTUALIZACIÓN (PASO 3): Recorrer los 7 servicios uno por uno ===
                for (int i = 0; i < 7; i++) {
                    if (lectores[i] != null) {
                        
                        // === ACTUALIZACIÓN (PASO 4): Lectura no bloqueante con ready() ===
                        if (lectores[i].ready()) {
                            String linea = lectores[i].readLine();
                            if (linea != null) {
                                ultimoMensaje[i] = System.currentTimeMillis();
                                procesarEImprimir(linea, NOMBRES[i]);
                            }
                        }
                    }
                }

                // === ACTUALIZACIÓN (PASO 5): Micro-pausa de 100ms para no saturar la CPU ===
                Thread.sleep(100);

            } catch (Exception e) {
                System.err.println("Error en el bucle principal: " + e.getMessage());
            }
        }
    }

    private static Process lanzarServicio(String nombreServicio) throws IOException {
        ProcessBuilder pb = new ProcessBuilder(
            "java",
            "-cp",
            System.getProperty("java.class.path"),
            "Servicio",
            nombreServicio
        );
        return pb.start();
    }

    private static void procesarEImprimir(String linea, String servicio) {
        String timestamp = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        String estado = "INFO";
        String mensaje = linea;

        if (linea.contains("|")) {
            String[] partes = linea.split("\\|", 2);
            estado = partes[0];
            mensaje = partes[1];
        }

        System.out.printf("[%s] [%s] [%s] %s%n", timestamp, servicio, estado, mensaje);
    }
}
