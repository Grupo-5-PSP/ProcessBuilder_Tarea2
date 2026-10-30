import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class MonitorPrincipal {

    /** Formato de hora de todas las líneas de la consola. */
    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm:ss");

    /** RES-01: milisegundos sin mensajes tras los cuales un servicio vivo se considera mudo. */
    private static final long TIMEOUT_MS = 8000;

    //  INICIO RES-02 (PARTE DE ALDAIR) 
    /** RES-02: milisegundos que se espera desde el último mensaje de un servicio caído antes de reiniciarlo. */
    private static final long ESPERA_REINICIO_MS = 3000;
    // FIN RES-02 (constante)

    public static void main(String[] args) {
        String[] nombresServicios = {"DATABASE", "WEB_SERVER", "FTP_SERVER", "MAIL_SERVER", "FIREWALL", "BALANCER", "BACKUP"};
        Process[] procesos = new Process[nombresServicios.length];
        // Array de lectores BufferedReader (Requisito obligatorio 2.A)
        BufferedReader[] lectores = new BufferedReader[nombresServicios.length];
        // RES-01: hora (en ms) del último mensaje recibido de cada servicio
        long[] ultimoMensaje = new long[nombresServicios.length];

        System.out.println("[" + LocalTime.now().format(FORMATO_HORA) + "] [MONITOR] [OK] Iniciando el arranque de los 7 servicios...");

        // 1. Lanzamiento inicial de los 7 procesos y conexión de sus BufferedReader
        for (int i = 0; i < nombresServicios.length; i++) {
            procesos[i] = lanzarServicio(nombresServicios[i]);
            if (procesos[i] != null) {
                lectores[i] = new BufferedReader(new InputStreamReader(procesos[i].getInputStream()));
            }
            ultimoMensaje[i] = System.currentTimeMillis();
        }

        // 2. Bucle principal: Lectura sin bloqueos (.ready()) + vigilancia (RES-01) + reinicio (RES-02)
        while (true) {
            for (int i = 0; i < procesos.length; i++) {
                try {
                    // PARTE DE SERGIO (CORE-02): Leer sin hilos usando .ready() para no bloquear
                    while (lectores[i] != null && lectores[i].ready()) {
                        String linea = lectores[i].readLine();
                        if (linea != null) {
                            // RES-01: el servicio acaba de hablar
                            ultimoMensaje[i] = System.currentTimeMillis();
                            // PARTE DE ALDAIR (FMT-01): Añadir [HH:mm:ss] al mensaje del servicio
                            String horaActual = LocalTime.now().format(FORMATO_HORA);
                            System.out.println("[" + horaActual + "] " + linea);
                        }
                    }

                    // PARTE DE ALDAIR (RES-01): detectar caída (con código de salida) y servicio mudo
                    verificarSaludServicio(i, nombresServicios[i], procesos, lectores, ultimoMensaje);

                    // ================== INICIO RES-02 (PARTE DE ALDAIR) ==================
                    // Reinicio automático: si el servicio quedó caído (procesos[i] == null, lo deja así RES-01)
                    // y han pasado ESPERA_REINICIO_MS desde su último mensaje, se vuelve a lanzar.
                    if (procesos[i] == null && System.currentTimeMillis() - ultimoMensaje[i] >= ESPERA_REINICIO_MS) {
                        reiniciarServicio(i, nombresServicios[i], procesos, lectores, ultimoMensaje);
                    }
                    // =================== FIN RES-02 (bucle principal) ====================

                } catch (IOException e) {
                    System.out.println("[" + LocalTime.now().format(FORMATO_HORA) + "] [MONITOR] [ERROR] Fallo leyendo el servicio " + nombresServicios[i]);
                }
            }

            // Pausa breve para no saturar la CPU mientras escanea los 7 servicios
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    /**
     * RES-01: comprueba si un servicio ha terminado inesperadamente o ha dejado de enviar mensajes.
     * Caso 1: el proceso ha muerto, se informa de su código de salida.
     * Caso 2: el proceso sigue vivo pero lleva más de TIMEOUT_MS sin hablar, se destruye.
     * Tras reportar el fallo, el servicio queda marcado como caído (procesos[indice] = null),
     * de modo que se informa una sola vez. Su reinicio lo hace reiniciarServicio (RES-02).
     *
     * @param indice        posición del servicio en los arrays
     * @param nombre        nombre del servicio
     * @param procesos      procesos lanzados (se pone a null el del servicio caído)
     * @param lectores      lectores de la salida de cada servicio (se pone a null el del servicio caído)
     * @param ultimoMensaje hora (ms) del último mensaje recibido de cada servicio
     */
    private static void verificarSaludServicio(int indice, String nombre, Process[] procesos,
                                               BufferedReader[] lectores, long[] ultimoMensaje) {
        Process proceso = procesos[indice];
        if (proceso == null) {
            return; // Servicio ya reportado como caído, o no se pudo lanzar
        }

        // Caso 1: caída
        if (!proceso.isAlive()) {
            // Se recogen las últimas líneas que dejó antes de morir
            try {
                while (lectores[indice] != null && lectores[indice].ready()) {
                    String linea = lectores[indice].readLine();
                    if (linea != null) {
                        System.out.println("[" + LocalTime.now().format(FORMATO_HORA) + "] " + linea);
                    }
                }
            } catch (IOException e) {
                // No queda nada más que leer
            }
            System.out.println("[" + LocalTime.now().format(FORMATO_HORA) + "] [" + nombre
                    + "] [ERROR] El servicio se ha detenido inesperadamente (Código: " + proceso.exitValue() + ").");
            marcarComoCaido(indice, procesos, lectores);
            return;
        }

        // Caso 2: servicio mudo o congelado
        long tiempoSinHablar = System.currentTimeMillis() - ultimoMensaje[indice];
        if (tiempoSinHablar > TIMEOUT_MS) {
            System.out.println("[" + LocalTime.now().format(FORMATO_HORA) + "] [" + nombre
                    + "] [WARNING] El servicio no responde (Timeout > 8s). Deteniendo el proceso...");
            proceso.destroyForcibly();
            try {
                proceso.waitFor(); // Espera a que el proceso termine del todo
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            marcarComoCaido(indice, procesos, lectores);
        }
    }

    //  INICIO RES-02 (PARTE DE ALDAIR) 
    /**
     * RES-02: reinicia un servicio caído. Cierra su lector antiguo, lanza un proceso nuevo,
     * conecta su nuevo lector, reinicia su reloj y confirma el reinicio por consola.
     * Solo afecta al servicio indicado: los demás siguen monitorizándose.
     *
     * @param indice        posición del servicio en los arrays
     * @param nombre        nombre del servicio
     * @param procesos      procesos lanzados
     * @param lectores      lectores de la salida de cada servicio
     * @param ultimoMensaje hora (ms) del último mensaje recibido de cada servicio
     */
    private static void reiniciarServicio(int indice, String nombre, Process[] procesos,
                                          BufferedReader[] lectores, long[] ultimoMensaje) {
        // Se cierra el lector antiguo (si quedó alguno abierto) para liberar recursos
        try {
            if (lectores[indice] != null) {
                lectores[indice].close();
            }
        } catch (IOException e) {
            // Se ignora: el lector ya no sirve
        }

        // Se lanza el proceso nuevo y se conecta su lector
        procesos[indice] = lanzarServicio(nombre);
        if (procesos[indice] != null) {
            lectores[indice] = new BufferedReader(new InputStreamReader(procesos[indice].getInputStream()));
            System.out.println("[" + LocalTime.now().format(FORMATO_HORA) + "] [" + nombre
                    + "] [OK] Servicio reiniciado y operativo nuevamente");
        } else {
            lectores[indice] = null; // lanzarServicio ya ha informado del error
        }

        // El servicio recién lanzado empieza a contar desde ahora
        ultimoMensaje[indice] = System.currentTimeMillis();
    }
    //  FIN RES-02 (método)

    /**
     * Marca un servicio como caído: cierra su lector y deja su proceso a null
     * para que no se vuelva a reportar.
     *
     * @param indice   posición del servicio en los arrays
     * @param procesos procesos lanzados
     * @param lectores lectores de la salida de cada servicio
     */
    private static void marcarComoCaido(int indice, Process[] procesos, BufferedReader[] lectores) {
        try {
            if (lectores[indice] != null) {
                lectores[indice].close();
            }
        } catch (IOException e) {
            // Se ignora: el lector ya no sirve
        }
        lectores[indice] = null;
        procesos[indice] = null;
    }

    private static Process lanzarServicio(String nombreServicio) {
        try {
            ProcessBuilder pb = new ProcessBuilder("java", "Servicio", nombreServicio);
            // Unimos errores con salida estándar para leer todo con el mismo BufferedReader
            pb.redirectErrorStream(true);
            return pb.start();
        } catch (IOException e) {
            System.out.println("[" + LocalTime.now().format(FORMATO_HORA) + "] [MONITOR] [ERROR] No se pudo lanzar: " + nombreServicio);
            return null;
        }
    }
}
