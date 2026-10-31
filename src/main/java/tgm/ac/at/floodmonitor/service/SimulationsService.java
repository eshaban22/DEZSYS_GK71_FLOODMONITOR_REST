package tgm.ac.at.floodmonitor.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import tgm.ac.at.floodmonitor.model.*;
import tgm.ac.at.floodmonitor.repository.DataRepository;

import java.time.Instant;
import java.util.Random;

/**
 * Simulator (Punkt 2): erzeugt fuer jede aktive Station regelmaessig eine neue Messung.
 *
 * Regeln:
 *  1. Niederschlag: 0-25 mm/h, in "Regenphasen" gehaeuft (Wetter-Front Simulation).
 *  2. Starker Niederschlag (>= 10 mm/h) laesst den Wasserstand steigen,
 *     sonst schwankt er leicht um den bisherigen Wert (-2 bis +2 cm).
 *  3. Der Wasserstand bleibt nahe am vorherigen Wert (kein voellig zufaelliger Sprung).
 *  4. Die Durchflussmenge steigt mit dem Wasserstand (flowRate ~ 2.3 * waterLevel).
 *  5. Der Akkustand sinkt pro Intervall um 0-1 % und bleibt zwischen 0 und 100.
 *  6. Bei Akkustand unter 10 % wechselt die Station auf MAINTENANCE.
 *  7. Fuer deaktivierte Stationen (isActive=false) werden keine Messungen erzeugt.
 *  8. Der vorherige Messwert wird als Startpunkt der Berechnung verwendet.
 *
 * Das Intervall ist konfigurierbar: simulation.interval=10000 (Millisekunden).
 * Fuer Tests deaktivierbar: simulation.enabled=false (Punkt 9).
 */
@Service
public class SimulationsService implements ApplicationRunner {

    private final DataRepository repository;
    private final FloodMonitorService floodMonitorService;
    private final Random random = new Random();

    /** Regel 2: ab diesem Niederschlag steigt der Wasserstand deutlich. */
    private static final double RAIN_THRESHOLD = 10.0;
    /** Regel 6: unter diesem Akkustand wechselt die Station in die Wartung. */
    private static final int BATTERY_MAINTENANCE_THRESHOLD = 10;

    /** Kann in Tests via simulation.enabled=false deaktiviert werden (Punkt 9). */
    @Value("${simulation.enabled:true}")
    private boolean simulationEnabled;

    public SimulationsService(DataRepository repository, FloodMonitorService floodMonitorService) {
        this.repository = repository;
        this.floodMonitorService = floodMonitorService;
    }

    /**
     * Wird einmal beim Start ausgefuehrt: berechnet fuer die Seed-Messungen
     * des Repositorys die Warnstufe nach (damit keine "leeren" Daten ausgeliefert werden).
     */
    @Override
    public void run(ApplicationArguments args) {
        for (Measurement m : repository.getAllMeasurements()) {
            backfillWarningLevel(m);
        }
    }

    private void backfillWarningLevel(Measurement m) {
        if (m.getWarningLevel() == null) {
            repository.getStationById(m.getStationId()).ifPresent(station ->
                    m.setWarningLevel(floodMonitorLevel(station, m.getWaterLevel())));
        }
    }

    private WarningLevel floodMonitorLevel(Station station, Double waterLevel) {
        return floodMonitorService.calculateWarningLevel(station, waterLevel);
    }

    // Intervall ist konfigurierbar ueber application.properties (Standard: 10 Sekunden)
    @Scheduled(fixedRateString = "${simulation.interval:10000}")
    public void runSimulation() {
        if (!simulationEnabled) return; // Simulation fuer Tests deaktivierbar (Punkt 9)

        for (Station station : repository.getAllStations()) {
            if (!station.isActive()) continue; // Regel 7: keine Messungen fuer inaktive Stationen

            Measurement previous = repository.getLatestMeasurementForStation(station.getId()).orElse(null);
            Measurement next = generateNextMeasurement(station, previous);
            repository.addMeasurement(next);
        }
    }

    private Measurement generateNextMeasurement(Station station, Measurement previous) {
        double currentWater = (previous != null) ? previous.getWaterLevel() : station.getNormalWaterLevel();
        int currentBattery = (previous != null) ? previous.getBatteryLevel() : 100;
        double previousRain = (previous != null) ? previous.getRainfall() : 0.0;

        // 1. Niederschlag simulieren (0-25 mm/h), in Regenphasen gehaeuft
        double rainfall = simulateRainfall(previousRain);

        // 2. Wasserstand: starker Regen laesst ihn steigen, sonst leichte Schwankung
        double change = (rainfall >= RAIN_THRESHOLD)
                ? (rainfall * 0.8)
                : (random.nextDouble() * 4.0 - 2.0);
        double waterLevel = Math.max(0.0, Math.round((currentWater + change) * 10.0) / 10.0);

        // 3. Durchflussmenge steigt mit dem Wasserstand
        double flowRate = Math.round((waterLevel * 2.3 + random.nextDouble() * 5.0) * 10.0) / 10.0;

        // 4. Akkustand sinkt langsam, bleibt zwischen 0 und 100
        int batteryLevel = Math.max(0, Math.min(100, currentBattery - random.nextInt(2)));

        // 5. Status: bei sehr niedrigem Akku in die Wartung
        StationStatus status = (batteryLevel < BATTERY_MAINTENANCE_THRESHOLD)
                ? StationStatus.MAINTENANCE
                : StationStatus.ONLINE;

        // 6. Warnstufe serverseitig berechnen (Punkt 3)
        WarningLevel warningLevel = floodMonitorService.calculateWarningLevel(station, waterLevel);

        return new Measurement(
                station.getId(),
                Instant.now(),
                waterLevel,
                flowRate,
                rainfall,
                15.0 + Math.round(random.nextDouble() * 5.0 * 10.0) / 10.0, // Lufttemperatur 15-20 Grad
                batteryLevel,
                status,
                warningLevel
        );
    }

    /**
     * Niederschlag in Phasen: wenn es vorher geregnet hat, regnet es mit hoher
     * Wahrscheinlichkeit weiter (Wetter-Front). Sonst meist trocken.
     */
    private double simulateRainfall(double previousRain) {
        boolean wasRaining = previousRain >= RAIN_THRESHOLD;
        double base = wasRaining ? 0.7 : 0.15; // Fortdauer-Wahrscheinlichkeit
        if (random.nextDouble() > base) {
            return 0.0;
        }
        return Math.round(random.nextDouble() * 25.0 * 10.0) / 10.0;
    }
}
