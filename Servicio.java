import java.util.Random;

public class Servicio {
    public static void main(String[] args) {
        
        // Cojo el nombre del servicio desde args[0]. Si me he olvidado de pasarlo, 
        // le pongo "DESCONOCIDO" por defecto para que no pete el programa.
        String nombreServicio = (args.length > 0 && args[0] != null && !args[0].isEmpty()) 
                                ? args[0] 
                                : "DESCONOCIDO";
        
        // Instancio la clase Random para tirar los números aleatorios de las probabilidades.
        Random random = new Random();

        // Bucle infinito: este servicio no para nunca por sí mismo, 
        // se queda emitiendo su estado hasta que el proceso padre lo mate.
        while (true) {
            
            // Saco un número del 0 al 99 para ver qué salud tiene el servicio en este ciclo.
            int probabilidad = random.nextInt(100);
            
            // Inicializamos las variables con valores por defecto
            String estado = "OK";
            String mensaje = "";

            // Lógica de estados con sus porcentajes (70% OK, 20% WARNING, 10% ERROR)
            if (probabilidad < 70) {
                estado = "OK";
            } else if (probabilidad < 90) {
                estado = "WARNING";
            } else {
                // Error crítico. Imprimimos el mensaje específico de error del servicio, hacemos flush y cerramos con código 1.
                System.out.println(nombreServicio + ";ERROR;" + generarMensaje(nombreServicio, "ERROR"));
                System.out.flush();
                System.exit(1); 
            }

            // Generamos el mensaje realista llamando al método auxiliar con el switch
            mensaje = generarMensaje(nombreServicio, estado);

            // Saco el resultado con el formato exacto de puntos y comas
            System.out.println(nombreServicio + ";" + estado + ";" + mensaje);

            // Fuerzo el búfer con flush para que lea el mensaje al instante y no se quede atascado.
            System.out.flush();

            // Hacemos que espere 3 segundos exactos antes de volver a emitir el siguiente latido.
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    // --- NUEVO MÉTODO AUXILIAR: SWITCH CON LOS 7 SERVICIOS ---
    public static String generarMensaje(String nombreServicio, String estado) {
        // Normalizamos el nombre a mayúsculas por seguridad
        // Al pasarle el argumento en java Servicio (x) pues sabe cual es
        //  si no le pasamos nada como al principio [args0], pues pasa desconocido
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
                // Por si se introduce cualquier otro nombre por consola
                if (estado.equals("OK")) return "Servicio genérico operativo";
                if (estado.equals("WARNING")) return "Elevada carga detectada";
                return "Fallo crítico general. Apagando...";
        }
    }
}