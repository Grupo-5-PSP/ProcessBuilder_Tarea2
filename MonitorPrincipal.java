import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Programa Coordinador Principal de la arquitectura multiproceso (Grupo 5).
 * Instancia, lanza, supervisa y sincroniza en tiempo real la ejecución de 7 subprocesos
 * autónomos ({@link Servicio}) mediante {@link ProcessBuilder}, gestionando la lectura
 * no bloqueante de flujos E/S, detección de anomalías y auto-reinicio en caliente.
 *
 * @author Aldair Antonio Mato Gonzales, Sergio y Alonso
 * @version 1.0
 */
public class MonitorPrincipal {

    /** Formato de hora estándar [HH:mm:ss] para todas las líneas de la consola (FMT-01). */
    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm:ss");

    /** RES-01: Tiempo máximo en milisegundos sin recibir mensajes antes de considerar un servicio colgado (Timeout). */
    private static final long TIMEOUT_MS = 8000;

    /** RES-02: Tiempo de espera en milisegundos antes de reiniciar automáticamente un servicio caído. */
    private static final long ESPERA_REINICIO_MS = 3000;

    /**
     * Método principal del coordinador. Lanza los 7 servicios del sistema, mantiene el bucle
     * de lectura no bloqueante mediante {@code BufferedReader.ready()}, actualiza el panel de control
     * y gestiona el apagado seguro.
     *
     * @param args Argumentos de línea de comandos (no utilizados en el monitor principal).
     */
    public static void main(String[] args) {
        String[] nombresServicios = {"DATABASE", "WEB_SERVER", "FTP_SERVER", "MAIL_SERVER", "FIREWALL", "BALANCER", "BACKUP"};
        Process[] procesos = new Process[nombresServicios.length];
        BufferedReader[] lectores = new BufferedReader[nombresServicios.length];
        long[] ultimoMensaje = new long[nombresServicios.length];

        System.out.println("[" + LocalTime.now().format(FORMATO_HORA) + "] [MONITOR] [OK] Iniciando el arranque de los 7 servicios...");
        System.out.println("[MONITOR] [INFO] Escribe 'q' en la consola y pulsa Enter para apagar el monitor de forma segura.");

        // Inicialización de tiempos de control (CORE-02)
        for (int i = 0; i < nombresServicios.length; i++) {
            ultimoMensaje[i] = System.currentTimeMillis();
        }

        // Inicialización del array de estados para el panel (CORE-03)
        String[] ultimosEstados = new String[nombresServicios.length];
        for (int i = 0; i < nombresServicios.length; i++) {
            ultimosEstados[i] = "INICIALIZANDO";
        }

        BufferedReader lectorTeclado = new BufferedReader(new InputStreamReader(System.in));
        long ultimoTiempoPanel = System.currentTimeMillis();

        // Lanzamiento inicial de los 7 subprocesos y conexión de sus tuberías (CORE-01)
        for (int i = 0; i < nombresServicios.length; i++) {
            procesos[i] = lanzarServicio(nombresServicios[i]);
            if (procesos[i] != null) {
                lectores[i] = new BufferedReader(new InputStreamReader(procesos[i].getInputStream()));
            }
            ultimoMensaje[i] = System.currentTimeMillis();
        }

        // Bucle principal: Lectura sin bloqueos (.ready()) + vigilancia (RES-01) + reinicio (RES-02)
        while (true) {
            try {
                // CORE-03: Lectura no bloqueante de teclado para apagado seguro con 'q'
                if (lectorTeclado.ready()) {
                    String comando = lectorTeclado.readLine();
                    if (comando != null && comando.trim().equalsIgnoreCase("q")) {
                        String horaActual = LocalTime.now().format(FORMATO_HORA);
                        System.out.println("[" + horaActual + "] [MONITOR] [INFO] Comando de apagado recibido. Cerrando monitor...");
                        break;
                    }
                }

                // Recorrido rápido por los 7 servicios
                for (int i = 0; i < procesos.length; i++) {
                    try {
                        // CORE-02: Lectura no bloqueante comprobando .ready()
                        if (lectores[i] != null && lectores[i].ready()) {
                            String linea = lectores[i].readLine();
                            if (linea != null) {
                                ultimoMensaje[i] = System.currentTimeMillis();

                                // CORE-03: Actualizar estado para el panel
                                if (linea.contains("[OK]")) {
                                    ultimosEstados[i] = "OK";
                                } else if (linea.contains("[WARNING]")) {
                                    ultimosEstados[i] = "WARNING";
                                } else if (linea.contains("[ERROR]")) {
                                    ultimosEstados[i] = "ERROR";
                                }

                                // FMT-01: Añadir [HH:mm:ss] y validar formato estricto para filtrar datos corruptos
                                String horaActual = LocalTime.now().format(FORMATO_HORA);
                                if (!linea.matches("^\\[[A-Za-z0-9_]+\\] \\[(OK|WARNING|ERROR)\\] .+$")) {
                                    ultimosEstados[i] = "ANOMALÍA";
                                    System.out.println("[" + horaActual + "] [" + nombresServicios[i] + "] [ANOMALÍA] Dato corrupto detectado: " + linea);
                                } else {
                                    System.out.println("[" + horaActual + "] " + linea);
                                }
                            }
                        }

                        // CORE-03: Comprobación de inactividad previa al timeout (> 5 segundos)
                        if (procesos[i] != null && procesos[i].isAlive()) {
                            if (System.currentTimeMillis() - ultimoMensaje[i] > 5000) {
                                ultimosEstados[i] = "SIN RESPUESTA";
                            }
                        }

                        // RES-01: Verificar si el proceso ha caído o superado el timeout de 8s
                        verificarSaludServicio(i, nombresServicios[i], procesos, lectores, ultimoMensaje);

                        if (procesos[i] == null) {
                            ultimosEstados[i] = "CAÍDO";
                        }

                        // RES-02: Auto-reinicio en caliente tras cumplirse ESPERA_REINICIO_MS (3s)
                        if (procesos[i] == null && System.currentTimeMillis() - ultimoMensaje[i] >= ESPERA_REINICIO_MS) {
                            reiniciarServicio(i, nombresServicios[i], procesos, lectores, ultimoMensaje);
                            if (procesos[i] != null) {
                                ultimosEstados[i] = "OK";
                            }
                        }

                    } catch (IOException e) {
                        System.out.println("[" + LocalTime.now().format(FORMATO_HORA) + "] [MONITOR] [ERROR] Fallo leyendo el servicio " + nombresServicios[i]);
                    }
                }

                // CORE-03: Impresión periódica del panel de estado cada 3 segundos
                if (System.currentTimeMillis() - ultimoTiempoPanel > 3000) {
                    System.out.println("\n========== [PANEL DE ESTADO DE LOS SERVICIOS] ==========");
                    for (int j = 0; j < nombresServicios.length; j++) {
                        System.out.println("  > " + String.format("%-12s", nombresServicios[j]) + " -> Estado: " + ultimosEstados[j]);
                    }
                    System.out.println("========================================================\n");
                    ultimoTiempoPanel = System.currentTimeMillis();
                }

                Thread.sleep(100);

            } catch (IOException e) {
                System.out.println("[MONITOR] [ERROR] Fallo en la lectura de los flujos I/O.");
            } catch (InterruptedException e) {
                System.out.println("[MONITOR] [INFO] El monitor principal ha sido interrumpido.");
                break;
            }
        }

        // CORE-03: Apagado seguro destruyendo los 7 subprocesos para evitar procesos huérfanos
        System.out.println("[MONITOR] [INFO] Destruyendo subprocesos activos...");
        for (int i = 0; i < procesos.length; i++) {
            if (procesos[i] != null && procesos[i].isAlive()) {
                procesos[i].destroy();
            }
        }
        System.out.println("[MONITOR] [OK] Todos los servicios han sido detenidos. Monitor finalizado.");
    }

    /**
     * RES-01: Comprueba en tiempo real si un servicio ha terminado inesperadamente o ha dejado de enviar mensajes.
     * Si el proceso ha muerto, informa de su código de salida. Si lleva más de {@link #TIMEOUT_MS} sin emitir,
     * lo destruye forzosamente.
     *
     * @param indice        Posición del servicio en los arrays de control.
     * @param nombre        Identificador del servicio (ej. DATABASE).
     * @param procesos      Array de procesos activos.
     * @param lectores      Array de lectores BufferedReader asociados a cada proceso.
     * @param ultimoMensaje Array con la marca de tiempo (ms) del último mensaje de cada servicio.
     */
    private static void verificarSaludServicio(int indice, String nombre, Process[] procesos,
                                               BufferedReader[] lectores, long[] ultimoMensaje) {
        Process proceso = procesos[indice];
        if (proceso == null) {
            return;
        }

        // Caso 1: Caída inesperada del proceso
        if (!proceso.isAlive()) {
            try {
                while (lectores[indice] != null && lectores[indice].ready()) {
                    String linea = lectores[indice].readLine();
                    if (linea != null) {
                        System.out.println("[" + LocalTime.now().format(FORMATO_HORA) + "] " + linea);
                    }
                }
            } catch (IOException e) {
                // No quedan más líneas en el buffer
            }
            System.out.println("[" + LocalTime.now().format(FORMATO_HORA) + "] [" + nombre
                    + "] [ERROR] El servicio se ha detenido inesperadamente (Código: " + proceso.exitValue() + ").");
            marcarComoCaido(indice, procesos, lectores);
            return;
        }

        // Caso 2: Servicio mudo o congelado (Timeout > 8s)
        long tiempoSinHablar = System.currentTimeMillis() - ultimoMensaje[indice];
        if (tiempoSinHablar > TIMEOUT_MS) {
            System.out.println("[" + LocalTime.now().format(FORMATO_HORA) + "] [" + nombre
                    + "] [WARNING] El servicio no responde (Timeout > 8s). Deteniendo el proceso...");
            proceso.destroyForcibly();
            try {
                proceso.waitFor();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            marcarComoCaido(indice, procesos, lectores);
        }
    }

    /**
     * RES-02: Reinicia en caliente un servicio caído sin detener la monitorización de los otros 6.
     * Cierra el lector antiguo, instancia un nuevo proceso, reconecta el flujo de entrada y resetea su reloj.
     *
     * @param indice        Posición del servicio en los arrays de control.
     * @param nombre        Identificador del servicio a reiniciar.
     * @param procesos      Array de procesos activos.
     * @param lectores      Array de lectores BufferedReader.
     * @param ultimoMensaje Array con la marca de tiempo (ms) del último mensaje recibido.
     */
    private static void reiniciarServicio(int indice, String nombre, Process[] procesos,
                                          BufferedReader[] lectores, long[] ultimoMensaje) {
        try {
            if (lectores[indice] != null) {
                lectores[indice].close();
            }
        } catch (IOException e) {
            // Se ignora al estar cerrando un flujo antiguo
        }

        procesos[indice] = lanzarServicio(nombre);
        if (procesos[indice] != null) {
            lectores[indice] = new BufferedReader(new InputStreamReader(procesos[indice].getInputStream()));
            System.out.println("[" + LocalTime.now().format(FORMATO_HORA) + "] [" + nombre
                    + "] [OK] Servicio reiniciado y operativo nuevamente");
        } else {
            lectores[indice] = null;
        }

        ultimoMensaje[indice] = System.currentTimeMillis();
    }

    /**
     * Auxiliar de RES-01: Cierra el canal de lectura de un servicio fallido y establece su referencia
     * a {@code null} para indicar a RES-02 que debe ser reiniciado.
     *
     * @param indice   Posición del servicio en los arrays.
     * @param procesos Array de procesos activos.
     * @param lectores Array de lectores BufferedReader.
     */
    private static void marcarComoCaido(int indice, Process[] procesos, BufferedReader[] lectores) {
        try {
            if (lectores[indice] != null) {
                lectores[indice].close();
            }
        } catch (IOException e) {
            // Se ignora
        }
        lectores[indice] = null;
        procesos[indice] = null;
    }

    /**
     * CORE-01: Configura e inicia un subproceso independiente ejecutando la clase {@link Servicio}
     * mediante {@link ProcessBuilder} y unificando su salida de error con la estándar.
     *
     * @param nombreServicio Identificador del servicio que se pasa como argumento {@code args[0]}.
     * @return Objeto {@link Process} iniciado, o {@code null} si ocurre un error de E/S al arrancar.
     */
    private static Process lanzarServicio(String nombreServicio) {
        try {
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
            System.out.println("[" + LocalTime.now().format(FORMATO_HORA) + "] [MONITOR] [ERROR] No se pudo lanzar: " + nombreServicio);
            return null;
        }
    }
}
