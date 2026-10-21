import java.util.Random;

public class Servicio {

    public static void main(String[] args) {
        String nombreServicio = (args.length > 0) ? args[0] : "DESCONOCIDO";
        Random random = new Random();

        String[] estados = {"OK", "WARNING", "ERROR"};
        
        // Bucle continuo (while(true) - Servicio de infraestructura IT)
        while (true) {
            int indiceEstado = random.nextInt(3);
            String estado = estados[indiceEstado];
            String mensaje = generarMensajeAleatorio(nombreServicio, estado, random);

            // Emitimos la salida en un formato estructurado para que el padre la lea
            System.out.println(estado + "|" + mensaje);
            System.out.flush();

            try {
                // Heartbeat / Estado del servicio cada 3 segundos
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    private static String generarMensajeAleatorio(String servicio, String estado, Random r) {
        switch (estado) {
            case "OK":
                return "Estado del servicio: Operativo (Ping " + (r.nextInt(20) + 5) + "ms)";
            case "WARNING":
                return "Elevado tráfico de red o uso de CPU detectado (" + (70 + r.nextInt(20)) + "%)";
            case "ERROR":
                return "Fallo en la conexión o tiempo de respuesta excedido";
            default:
                return "Respuesta procesada correctamente";
        }
    }
}
