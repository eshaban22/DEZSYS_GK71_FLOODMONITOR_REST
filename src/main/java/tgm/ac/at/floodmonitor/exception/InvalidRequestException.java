package tgm.ac.at.floodmonitor.exception;

/**
 * Wird geworfen bei ungültigen Abfrage-Parametern (Punkt 6: 400 Bad Request),
 * z. B. from nach to, negatives oder zu großes limit, ungültiger Zeitraum.
 */
public class InvalidRequestException extends RuntimeException {

    public InvalidRequestException(String message) {
        super(message);
    }
}
