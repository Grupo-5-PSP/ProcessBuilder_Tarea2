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
        BufferedReader[] lectores = new BufferedReader[nombresServicios.length];
        // RES-01: hora (en ms) del último mensaje recibido de cada servicio
        long[] ultimoMensaje = new long[nombresServicios.length];


        System.out.println("[" + LocalTime.now().format(FORMATO_HORA) + "] [MONITOR] [OK] Iniciando el arranque de los 7 servicios...");
        System.out.println("[MONITOR] [INFO] Escribe 'q' en la consola y pulsa Enter para apagar el monitor de forma segura.");

        


        // ACTUALIZACIÓN CORE-02 (PASO 1): Array de control ultimoMensaje====
    
        for (int i = 0; i < nombresServicios.length; i++) {
            ultimoMensaje[i] = System.currentTimeMillis();
        }

        // CORE-03 INICIO: Array para almacenar el último estado de cada servicio====
  
        String[] ultimosEstados = new String[nombresServicios.length];
        for (int i = 0; i < nombresServicios.length; i++) {
            ultimosEstados[i] = "INICIALIZANDO";
        }
        
        // Lector para la entrada de teclado (System.in) sin bloquear hilos
        BufferedReader lectorTeclado = new BufferedReader(new InputStreamReader(System.in));
        long ultimoTiempoPanel = System.currentTimeMillis();
   
        // CORE-03 FIN===


        // Lanzamiento inicial de los 7 procesos y conexión de sus BufferedReader
        for (int i = 0; i < nombresServicios.length; i++) {
            procesos[i] = lanzarServicio(nombresServicios[i]);
            if (procesos[i] != null) {
                lectores[i] = new BufferedReader(new InputStreamReader(procesos[i].getInputStream()));
            }
            ultimoMensaje[i] = System.currentTimeMillis();
        }


        // ACTUALIZACIÓN CORE-02 (PASOS 2, 3, 4 y 5): Bucle de lectura no bloqueante===

        
        // PASO 2: Bucle infinito en el main

        // 2. Bucle principal: Lectura sin bloqueos (.ready()) + vigilancia (RES-01) + reinicio (RES-02)
        while (true) {
            try {
              
                // CORE-03 INICIO: Lectura no bloqueante de consola para apagado con "q"====
             
                if (lectorTeclado.ready()) {
                    String comando = lectorTeclado.readLine();
                    if (comando != null && comando.trim().equalsIgnoreCase("q")) {
                        String horaActual = LocalTime.now().format(FORMATO_HORA);
                        System.out.println("[" + horaActual + "] [MONITOR] [INFO] Comando de apagado recibido. Cerrando monitor...");
                        break; // Sale del bucle while
                    }
                }
                // CORE-03 FIN===
           

                // PASO 3: Recorrer los servicios uno por uno rápidamente
                for (int i = 0; i < procesos.length; i++) {
                    try {

                    // PASO 4: Lectura no bloqueante comprobando .ready()
                    if (lectores[i] != null && lectores[i].ready()) {
                        String linea = lectores[i].readLine();
                        if (linea != null) {

                            // Actualizar el timestamp del último mensaje recibido
                            ultimoMensaje[i] = System.currentTimeMillis();
                            
                         
                            // CORE-03 INICIO: Actualizar el último estado según el mensaje recibido ===
                           
                            if (linea.contains("OK")) {
                                ultimosEstados[i] = "OK";
                            } else if (linea.contains("WARNING")) {
                                ultimosEstados[i] = "WARNING";
                            } else if (linea.contains("ERROR")) {
                                ultimosEstados[i] = "ERROR";
                            }
                            // CORE-03 FIN
                         

                            // PARTE DE ALDAIR (FMT-01): Añadir [HH:mm:ss] al mensaje del servicio y filtrar datos corruptos
                            String horaActual = LocalTime.now().format(FORMATO_HORA);
                            // Una línea válida tiene el formato [ID] [ESTADO] mensaje, con ESTADO = OK, WARNING o ERROR
                            if (!linea.matches("^\\[[A-Za-z0-9_]+\\] \\[(OK|WARNING|ERROR)\\] .+$")) {
                                ultimosEstados[i] = "ANOMALÍA";
                                System.out.println("[" + horaActual + "] [" + nombresServicios[i] + "] [ANOMALÍA] Dato corrupto detectado: " + linea);
                            } else {
                                System.out.println("[" + horaActual + "] " + linea);
                            }

                        }
                    }
                    // Si ready() es false, no se bloquea y pasa inmediatamente al siguiente servicio.


                    // CORE-03 INICIO: Comprobación de "SIN RESPUESTA" por inactividad (> 5 segundos)
                 
                    if (procesos[i] != null && procesos[i].isAlive()) {
                        if (System.currentTimeMillis() - ultimoMensaje[i] > 5000) {
                            ultimosEstados[i] = "SIN RESPUESTA";
                        }
                    }
                    // CORE-03 FIN===
                  

                    // PARTE DE ALDAIR (RES-01): detectar caída (con código de salida) y servicio mudo
                    verificarSaludServicio(i, nombresServicios[i], procesos, lectores, ultimoMensaje);

                    // CORE-03: Marcar estado como CAÍDO si RES-01 ha dado el servicio por caído
                    if (procesos[i] == null) {
                        ultimosEstados[i] = "CAÍDO";
                    }

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

     
                // CORE-03 INICIO: Impresión periódica del panel de estados cada 3 segundos===
               
                if (System.currentTimeMillis() - ultimoTiempoPanel > 3000) {
                    System.out.println("\n========== [PANEL DE ESTADO DE LOS SERVICIOS] ==========");
                    for (int j = 0; j < nombresServicios.length; j++) {
                        System.out.println("  > " + String.format("%-12s", nombresServicios[j]) + " -> Estado: " + ultimosEstados[j]);
                    }
                    System.out.println("========================================================\n");
                    ultimoTiempoPanel = System.currentTimeMillis();
                }
             
                // CORE-03 FIN===
               

                // PASO 5: Micro-pausa de 100ms para no saturar el procesador
                Thread.sleep(100);

            } catch (IOException e) {
                System.out.println("[MONITOR] [ERROR] Fallo en la lectura de los flujos I/O.");
            } catch (InterruptedException e) {
                System.out.println("[MONITOR] [INFO] El monitor principal ha sido interrumpido.");
                break;
            }
        }

        // CORE-03 INICIO: Apagado seguro destruyendo los 7 procesos para evitar huérfanos===

        System.out.println("[MONITOR] [INFO] Destruyendo subprocesos activos...");
        for (int i = 0; i < procesos.length; i++) {
            if (procesos[i] != null && procesos[i].isAlive()) {
                procesos[i].destroy(); // Envía señal de terminación
            }
        }
        System.out.println("[MONITOR] [OK] Todos los servicios han sido detenidos. Monitor finalizado.");
       
        // CORE-03 FIN===
       
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
          
            // ACTUALIZACIÓN CORE-01: Configuración de classpath para ProcessBuilder====
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