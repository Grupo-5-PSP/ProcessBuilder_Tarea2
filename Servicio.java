import java.util.Random;

/**
 * Clase principal que he programado para simular un servicio del sistema en red.
 * Me encargo de emitir latidos periódicos de su estado (OK, WARNING, ERROR o datos corruptos)
 * basándome en probabilidades aleatorias y soportando argumentos externos de control.
 * 
 * @author Alonso
 * @version 2.0
 */
public class Servicio {
    
    /**
     * Método principal que gestiono en esta aplicación. Aquí recojo los argumentos de consola,
     * compruebo si me llega la opción de error forzado y ejecuto el bucle infinito 
     * con la simulación de fallos (70% OK, 15% WARNING, 5% datos corruptos, 
     * 5% congelación y 5% caída total).
     * 
     * @param args Argumentos de la línea de comandos:
     *             args[0] = Nombre de mi servicio (ej. DATABASE, WEB_SERVER, etc.).
     *             args[1] = Opcional, argumento de control (--force-error).
     */
    public static void main(String[] args) {
        
        // Cojo el nombre del servicio desde args[0]. Si me he olvidado de pasarlo, 
        // le pongo "DESCONOCIDO" por defecto para que no pete el programa.
        String nombreServicio = (args.length > 0 && args[0] != null && !args[0].isEmpty()) 
                                ? args[0] 
                                : "DESCONOCIDO";
        
        // 1. Compruebo si me llega el argumento para forzar el error y cerrar de golpe.
        if (args.length > 1 && args[1].equals("--force-error")) {
            System.out.println(nombreServicio + ";ERROR;Error forzado por argumento externo");
            System.out.flush();
            System.exit(1);
        }
        
        // Instancio la clase Random por si la necesito.
        Random random = new Random();

        // Bucle infinito: este servicio emite su estado de manera continua.
        while (true) {
            
            // 2. Genero un número aleatorio decimal (entre 0.0 y 1.0) para calcular las probabilidades.
            double prob = Math.random();
            
            String estado = "OK";
            String mensaje = "";

            // 3. Aplico mis rangos de probabilidad en cada ciclo de 3 segundos:
            
            if (prob < 0.70) {
                // 70% (prob < 0.70): Emito estado normal OK con su mensaje realista del switch.
                estado = "OK";
                mensaje = generarMensaje(nombreServicio, estado);
                System.out.println(nombreServicio + ";" + estado + ";" + mensaje);
                System.out.flush();
                
            } else if (prob < 0.85) { 
                // 15% (prob >= 0.70 && prob < 0.85): Emito estado de alerta WARNING.
                estado = "WARNING";
                mensaje = generarMensaje(nombreServicio, estado);
                System.out.println(nombreServicio + ";" + estado + ";" + mensaje);
                System.out.flush();
                
            } else if (prob < 0.90) { 
                // 5% (prob >= 0.85 && prob < 0.90): Emito un dato corrupto o malformado 
                // (sin puntos y comas) para probar el validador.
                System.out.println("###DATO_CORRUPTO_SIN_FORMATO###");
                System.out.flush();
                
            } else if (prob < 0.95) { 
                // 5% (prob >= 0.90 && prob < 0.95): Simulo que me quedo congelado 
                // dejando de emitir mensajes durante 15 segundos.
                try {
                    Thread.sleep(15000);
                } catch (InterruptedException e) {
                    break;
                }
                
            } else { 
                // 5% restante (prob >= 0.95): Simulo una caída total, 
                // imprimo el fallo crítico y cierro el proceso con código 1.
                System.out.println(nombreServicio + ";ERROR;Fallo crítico del proceso");
                System.out.flush();
                System.exit(1);
            }

            // 4. Pausa estándar de 3 segundos entre cada latido normal.
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    /**
     * Método auxiliar que he creado para generar un mensaje descriptivo y realista 
     * según el tipo de servicio y su estado de salud utilizando un switch.
     * Al pasarle el argumento en consola sabe cuál es, y si no le pasamos nada usa "DESCONOCIDO".
     * 
     * @param nombreServicio El nombre identificativo de mi servicio (ej. DATABASE, FIREWALL).
     * @param estado El estado actual ("OK" o "WARNING").
     * @return Un String con el texto técnico correspondiente.
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