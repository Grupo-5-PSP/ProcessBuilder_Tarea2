import java.util.Random;

public class Servicio {
    public static void main(String[] args) {
        // Recoge el nombre del servicio que le pasa el Monitor (ej. "BaseDatos")
        String nombre = args.length > 0 ? args[0] : "ServicioDesconocido";
        Random random = new Random();

        // Bucle infinito exigido en SRV-01
        while (true) {
            int probabilidad = random.nextInt(100);
            String estado;

            // Lógica de estados aleatorios exigida en SRV-02
            if (probabilidad < 70) {
                estado = "OK";
            } else if (probabilidad < 90) {
                estado = "WARNING";
            } else {
                estado = "ERROR";
                System.out.println("[" + nombre + "] [" + estado + "] Fallo crítico. Apagando...");
                // Esto simula que el programa se cierra de golpe. 
                // Al cerrarse, tu isAlive() del Monitor lo detectará y lo reiniciará.
                System.exit(1); 
            }

            System.out.println("[" + nombre + "] [" + estado + "] Funcionando correctamente.");

            // Retraso exacto de 3 segundos exigido en SRV-01
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                break;
            }
        }
    }
}