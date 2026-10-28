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

        // ACTUALIZACIÓN CORE-02 (PASO 1): Array de control ultimoMensaje
     
        long[] ultimoMensaje = new long[nombresServicios.length];
        for (int i = 0; i < nombresServicios.length; i++) {
            ultimoMensaje[i] = System.currentTimeMillis();
        }

        // Lanzamiento inicial de los 7 procesos y conexión de sus BufferedReader
        for (int i = 0; i < nombresServicios.length; i++) {
            procesos[i] = lanzarServicio(nombresServicios[i]);
            if (procesos[i] != null) {
                lectores[i] = new BufferedReader(new InputStreamReader(procesos[i].getInputStream()));
            }
        }

        // ACTUALIZACIÓN CORE-02 (PASOS 2, 3, 4 y 5): Bucle de lectura no bloqueante
     
        
        // PASO 2: Bucle infinito en el main
        while (true) {
            try {
                // PASO 3: Recorrer los servicios uno por uno rápidamente
                for (int i = 0; i < procesos.length; i++) {
                    
                    // PASO 4: Lectura no bloqueante comprobando .ready()
                    if (lectores[i] != null && lectores[i].ready()) {
                        String linea = lectores[i].readLine();
                        if (linea != null) {
                            // Actualizar el timestamp del último mensaje recibido
                            ultimoMensaje[i] = System.currentTimeMillis();
                            
                            // Mostrar la línea capturada con formato de hora
                            String horaActual = LocalTime.now().format(formatoHora);
                            System.out.println("[" + horaActual + "] [" + nombresServicios[i] + "] " + linea);
                        }
                    }
                    // Si ready() es false, no se bloquea y pasa inmediatamente al siguiente servicio.

                    // Control de caídas y reinicio automático del proceso
                    if (procesos[i] == null || !procesos[i].isAlive()) {
                        String horaActual = LocalTime.now().format(formatoHora);
                        System.out.println("[" + horaActual + "] [" + nombresServicios[i] + "] [ALERTA] El servicio ha caído. Reiniciando...");
                        
                        procesos[i] = lanzarServicio(nombresServicios[i]);
                        if (procesos[i] != null) {
                            lectores[i] = new BufferedReader(new InputStreamReader(procesos[i].getInputStream()));
                            ultimoMensaje[i] = System.currentTimeMillis();
                        }
                    }
                }

                // PASO 5: Micro-pausa de 100ms para no saturar el procesador
                Thread.sleep(100);

            } catch (IOException e) {
                System.out.println("[MONITOR] [ERROR] Fallo en la lectura de los flujos I/O.");
            } catch (InterruptedException e) {
                System.out.println("[MONITOR] [INFO] El monitor principal ha sido interrumpido.");
                break;
            }
        }

        // === FIN DE ACTUALIZACIÓN CORE-02 =======
        
    }

    private static Process lanzarServicio(String nombreServicio) {
        try {
       
            // ACTUALIZACIÓN CORE-01: Configuración de classpath para ProcessBuilder
    
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

