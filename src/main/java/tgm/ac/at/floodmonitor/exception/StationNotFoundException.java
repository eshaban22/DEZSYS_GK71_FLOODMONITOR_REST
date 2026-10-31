package tgm.ac.at.floodmonitor.exception;

/**
 * Wird geworfen, wenn eine Stations-ID nicht existiert (Punkt 6: 404 Not Found).
 */
public class StationNotFoundException extends RuntimeException {

    public StationNotFoundException(String stationId) {
        super("Station " + stationId + " wurde nicht gefunden");
    }
}
