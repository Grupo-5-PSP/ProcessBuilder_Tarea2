import java.util.Random;

/**
 * Subproceso autónomo que simula un servicio de infraestructura IT (Grupo 5).
 * Emite continuamente su estado (OK, WARNING, ERROR) cada 3 segundos hacia la salida
 * estándar e incorpora una probabilidad del 15% de anomalías (datos corruptos,
 * congelamiento por timeout o caída crítica), además de soportar el parámetro {@code --force-error}.
 *
 * @author Alonso, Aldair Antonio Mato Gonzales y Sergio
 * @version 1.0
 */
public class Servicio {

    /**
     * Punto de entrada del subproceso de servicio. Recoge su identificador desde {@code args[0]},
     * verifica si debe forzar un fallo inmediato mediante {@code args[1]} y ejecuta el bucle infinito
     * de emisión de latidos (Heartbeat) cada 3 segundos.
     *
     * @param args Argumentos de consola: {@code args[0]} nombre del servicio, {@code args[1]} opcional {@code --force-error}.
     */
    public static void main(String[] args) {
        // SRV-01: Recoge el nombre del servicio desde args[0]
        String nombreServicio = (args.length > 0 && args[0] != null && !args[0].isEmpty())
                                ? args[0]
                                : "DESCONOCIDO";

        // SRV-03: Comprobación del argumento opcional --force-error para forzar un fallo en las pruebas
        if (args.length > 1 && args[1].equalsIgnoreCase("--force-error")) {
            System.out.println("[" + nombreServicio + "] [ERROR] Error forzado por argumento externo");
            System.out.flush();
            System.exit(1);
        }

        Random random = new Random();

        // SRV-01: Bucle continuo de emisión de estados cada 3 segundos
        while (true) {
            double prob = Math.random();
            String estado = "OK";
            String mensaje = "";

            // SRV-03: Distribución de probabilidades (70% OK, 15% WARNING, 15% Fallos/Anomalías)
            if (prob < 0.70) {
                estado = "OK";
                mensaje = generarMensaje(nombreServicio, estado);
                System.out.println("[" + nombreServicio + "] [" + estado + "] " + mensaje);
                System.out.flush();

            } else if (prob < 0.85) {
                estado = "WARNING";
                mensaje = generarMensaje(nombreServicio, estado);
                System.out.println("[" + nombreServicio + "] [" + estado + "] " + mensaje);
                System.out.flush();

            } else if (prob < 0.90) {
                // 5%: Emite dato corrupto sin el formato estándar para probar el filtro de FMT-01
                System.out.println("###DATO_CORRUPTO_SIN_FORMATO###");
                System.out.flush();

            } else if (prob < 0.95) {
                // 5%: Simula servicio congelado/mudo durante 15 segundos para probar el Timeout de RES-01
                try {
                    Thread.sleep(15000);
                } catch (InterruptedException e) {
                    break;
                }

            } else {
                // 5%: Simula caída crítica cerrando el proceso con código de salida 1
                mensaje = generarMensaje(nombreServicio, "ERROR");
                System.out.println("[" + nombreServicio + "] [ERROR] " + mensaje);
                System.out.flush();
                System.exit(1);
            }

            // Pausa de 3 segundos (Heartbeat) exigida en la especificación del Grupo 5
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    /**
     * SRV-02: Genera un mensaje descriptivo y realista acorde al tipo de servicio de infraestructura
     * y a su nivel de estado actual (OK, WARNING o ERROR).
     *
     * @param nombreServicio Identificador del servicio (DATABASE, WEB_SERVER, FTP_SERVER, etc.).
     * @param estado         Estado actual del servicio ("OK", "WARNING" o "ERROR").
     * @return Cadena de texto con el detalle técnico del estado del servicio.
     */
    public static String generarMensaje(String nombreServicio, String estado) {
        String servicio = (nombreServicio != null) ? nombreServicio.toUpperCase() : "DESCONOCIDO";

        switch (servicio) {
            case "DATABASE":
                if (estado.equals("OK")) return "Estado del servicio: Operativo (Ping 12ms)";
                if (estado.equals("WARNING")) return "Lentitud en consultas SQL (Ping 450ms)";
                return "Conexión con la base de datos perdida";

            case "WEB_SERVER":
                if (estado.equals("OK")) return "Procesando peticiones HTTP con normalidad";
                if (estado.equals("WARNING")) return "Alta concurrencia de usuarios detectada";
                return "Caída del servidor web / Error 500";

            case "FTP_SERVER":
                if (estado.equals("OK")) return "Transferencia de ficheros estable";
                if (estado.equals("WARNING")) return "Conexiones simultáneas elevadas";
                return "Fallo crítico en el canal de transferencia";

            case "MAIL_SERVER":
                if (estado.equals("OK")) return "Cola de correo limpia y operativa";
                if (estado.equals("WARNING")) return "Retraso en el envío de correos salientes";
                return "Servidor SMTP no responde";

            case "FIREWALL":
                if (estado.equals("OK")) return "Reglas de filtrado activas";
                if (estado.equals("WARNING")) return "Elevado tráfico de red detectado";
                return "Brecha de seguridad detectada. Bloqueo total";

            case "BALANCER":
            case "LOAD_BALANCER":
                if (estado.equals("OK")) return "Distribución de carga equilibrada entre nodos";
                if (estado.equals("WARNING")) return "Desbalanceo de carga en nodos secundarios";
                return "Fallo en el encaminamiento de tráfico";

            case "BACKUP":
                if (estado.equals("OK")) return "Sincronización y copias de seguridad al día";
                if (estado.equals("WARNING")) return "Copia de seguridad en progreso con lentitud";
                return "Error crítico al volcar los datos en el disco";

            default:
                if (estado.equals("OK")) return "Servicio genérico operativo";
                if (estado.equals("WARNING")) return "Elevada carga detectada";
                return "Fallo crítico general. Apagando...";
        }
    }
}

