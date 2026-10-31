package tgm.ac.at.floodmonitor.repository;

import org.springframework.stereotype.Repository;
import tgm.ac.at.floodmonitor.model.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

@Repository
public class DataRepository {

    /** Begrenzung für das 'limit' des Historien-Endpunkts (Punkt 6: zu große limit-Werte). */
    public static final int MAX_LIMIT = 1000;

    private final List<Station> stations = new CopyOnWriteArrayList<>();
    private final List<Measurement> measurements = new CopyOnWriteArrayList<>();

    public DataRepository() {
        // Mindestens 3 Test-Stationen anlegen
        stations.add(new Station("ST-001", "Station Wien Donau", "Donau",
                new Location(48.2082, 16.3738), 200.0, 300.0, 500.0, true));

        stations.add(new Station("ST-002", "Station Linz Donau", "Donau",
                new Location(48.3069, 14.2858), 180.0, 320.0, 480.0, true));

        stations.add(new Station("ST-003", "Station Innsbruck Inn", "Inn",
                new Location(47.2692, 11.4041), 150.0, 280.0, 420.0, false)); // Deaktivierte Station

        seedMeasurements();
    }

    /**
     * Legt pro aktiver Station 6 deterministische Messungen der letzten Stunde an
     * (Abstand 10 Minuten, vom Normalwasserlevel langsam aufsteigend).
     * ST-001 endet im WARNING-Bereich, ST-002 durchlaeuft NORMAL, WARNING und CRITICAL.
     * Seed-Werte sind deterministisch (kein Zufall), damit Tests nicht zufällig fehlschlagen (Punkt 9).
     */
    private void seedMeasurements() {
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);

        seedForStation("ST-001", 200.0, 318.4, now);   // letzte Messung: WARNING (318.4 >= 300)
        seedForStation("ST-002", 180.0, 490.0, now);   // 180 -> 490: NORMAL, WARNING und CRITICAL
    }

    private void seedForStation(String stationId, double startLevel, double endLevel, Instant now) {
        int steps = 6;
        for (int i = 0; i < steps; i++) {
            double waterLevel = Math.round((startLevel + (endLevel - startLevel) * i / (steps - 1)) * 10.0) / 10.0;
            double rainfall = Math.round((4.0 + i * 1.7) * 10.0) / 10.0;
            double flowRate = Math.round((waterLevel * 2.3 + 5.0) * 10.0) / 10.0;
            Instant timestamp = now.minus((steps - 1 - i) * 10L, ChronoUnit.MINUTES);
            measurements.add(new Measurement(
                    stationId,
                    timestamp,
                    waterLevel,
                    flowRate,
                    rainfall,
                    14.5,
                    90 - i,                       // Akku sinkt langsam
                    StationStatus.ONLINE,
                    null));                       // Warnstufe wird vom Service berechnet
        }
    }

    public List<Station> getAllStations() { return stations; }

    public Optional<Station> getStationById(String id) {
        return stations.stream().filter(s -> s.getId().equalsIgnoreCase(id)).findFirst();
    }

    public void addMeasurement(Measurement measurement) {
        measurements.add(measurement);
    }

    public List<Measurement> getAllMeasurements() { return measurements; }

    public Optional<Measurement> getLatestMeasurementForStation(String stationId) {
        return measurements.stream()
                .filter(m -> m.getStationId().equalsIgnoreCase(stationId))
                .max(Comparator.comparing(Measurement::getTimestamp));
    }
}