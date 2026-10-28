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
            
            // Inicializamos las variables con valores por defecto para que Java no se queje
            String estado = "OK";
            String mensaje = "Servicio operativo";

            // Lógica de estados con sus porcentajes (70% OK, 20% WARNING, 10% ERROR)
            if (probabilidad < 70) {
                // Todo va bien (mantiene los valores por defecto)
                estado = "OK";
                mensaje = "Servicio operativo";
            } else if (probabilidad < 90) {
                // Advertencia de que va un poco saturado
                estado = "WARNING";
                mensaje = "Elevada carga detectada";
            } else {
                // Error crítico. Hago el print, el flush y cierro con código 1 
                // para decir que ha dado error, para que java vea que esta mal si sale ese y se cierre.
                System.out.println(nombreServicio + ";ERROR;Fallo crítico. Apagando...");
                System.out.flush();
                System.exit(1); 
            }

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
}