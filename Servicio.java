import java.util.Random;

public class Servicio {
    public static void main(String[] args) {
        
        // Cojo el nombre del servicio desde args[0]. Si me he olvidado de pasarlo, 
        // le pongo "DESCONOCIDO" por defecto para que no pete el programa.
        String nombreServicio = (args.length > 0 && args[0] != null && !args[0].isEmpty()) 
                                ? args[0] 
                                : "DESCONOCIDO";
        
        // 1. Si se le pasa este segundo argumento,
        // el proceso se rompe de golpe y se cierra con código 1 para sus pruebas.
        if (args.length > 1 && args[1].equals("--force-error")) {
            System.out.println(nombreServicio + ";ERROR;Error forzado por argumento externo");
            System.out.flush();
            System.exit(1);
        }
        
        // Instancio la clase Random (por si la necesitamos para otros utils).
        Random random = new Random();

        // no para nunca por sí mismo, 
        // se queda emitiendo su estado hasta que el proceso padre lo mate.
        while (true) {
            
            // 2. Generamos un número aleatorio decimal (entre 0.0 y 1.0) para evaluar las probabilidades.
            double prob = Math.random();
            
            String estado = "OK";
            String mensaje = "";

            // 3. APLICAMOS LOS RANGOS DE PROBABILIDAD (en cada ciclo de 3 segundos):
            
            if (prob < 0.70) {
                // 70% (prob < 0.70): Emite estado normal OK con su mensaje realista del switch.
                estado = "OK";
                mensaje = generarMensaje(nombreServicio, estado);
                System.out.println(nombreServicio + ";" + estado + ";" + mensaje);
                System.out.flush();
                
            } else if (prob < 0.85) { 
                // 15% (prob >= 0.70 && prob < 0.85): Emite estado de alerta WARNING.
                estado = "WARNING";
                mensaje = generarMensaje(nombreServicio, estado);
                System.out.println(nombreServicio + ";" + estado + ";" + mensaje);
                System.out.flush();
                
            } else if (prob < 0.90) { 
                // 5% (prob >= 0.85 && prob < 0.90): Emite dato corrupto o malformado 
                // (sin puntos y comas) para poner a prueba el validador del coordinador.
                System.out.println("###DATO_CORRUPTO_SIN_FORMATO###");
                System.out.flush();
                
            } else if (prob < 0.95) { 
                // 5% (prob >= 0.90 && prob < 0.95): Simula que el servicio se queda colgado 
                // o congelado dejando de emitir mensajes durante 15 segundos.
                try {
                    Thread.sleep(15000);
                } catch (InterruptedException e) {
                    break;
                }
                
            } else { 
                // 5% restante (prob >= 0.95): Simula caída total del servicio, 
                // imprime el fallo crítico y cierra el proceso con código 1.
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

    // --- MÉTODO AUXILIAR: SWITCH CON LOS 7 SERVICIOS ---
    public static String generarMensaje(String nombreServicio, String estado) {
        // Normalizamos el nombre a mayúsculas por seguridad
        // Al pasarle el argumento en java Servicio (x) pues sabe cual es
        // si no le pasamos nada como al principio [args0], pues pasa desconocido
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