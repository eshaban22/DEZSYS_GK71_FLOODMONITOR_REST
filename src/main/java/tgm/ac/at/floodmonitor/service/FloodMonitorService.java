package tgm.ac.at.floodmonitor.service;

import org.springframework.stereotype.Service;
import tgm.ac.at.floodmonitor.exception.InvalidRequestException;
import tgm.ac.at.floodmonitor.exception.StationNotFoundException;
import tgm.ac.at.floodmonitor.model.*;
import tgm.ac.at.floodmonitor.repository.DataRepository;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service-Schicht (Punkt 8): enthaelt die gesamte Geschaeftslogik.
 *
 * Der Controller ruft nur noch diese Methoden auf und enthaelt selbst
 * keine fachliche Logik mehr. Der Service:
 *  - prueft Parameter und wirft bei Ungueltigkeit InvalidRequestException (Punkt 6),
 *  - wirft StationNotFoundException bei unbekannten IDs (Punkt 6),
 *  - filtert Stationen, Messungen und Warnungen (Punkt 4),
 *  - berechnet die Statistik (Punkt 4),
 *  - berechnet die Warnstufe serverseitig (Punkt 3).
 */
@Service
public class FloodMonitorService {

    private final DataRepository repository;

    public FloodMonitorService(DataRepository repository) {
        this.repository = repository;
    }

    /* ---------- Stationen (Punkt 4) ---------- */

    public List<Station> getStations(String river, Boolean isActive, WarningLevel warningLevel) {
        return repository.getAllStations().stream()
                .filter(s -> river == null || s.getRiver().equalsIgnoreCase(river))
                .filter(s -> isActive == null || s.isActive() == isActive)
                // Filter nach aktueller Warnstufe: nur aktive Stationen haben aktuelle Messungen
                .filter(s -> warningLevel == null
                        || latestWarningLevel(s.getId()) == warningLevel)
                .collect(Collectors.toList());
    }

    public Station getStation(String stationId) {
        return requireStation(stationId);
    }

    /* ---------- Messungen (Punkt 4) ---------- */

    /**
     * Liefert den aktuellsten Messwert. 204 (leeres Optional), wenn die Station
     * bekannt ist, aber noch keine Messung vorliegt; 404 bei unbekannter Station.
     */
    public Optional<Measurement> getLatestMeasurement(String stationId) {
        requireStation(stationId);
        return repository.getLatestMeasurementForStation(stationId);
    }

    public List<Measurement> getMeasurements(String stationId, Instant from, Instant to,
                                             Integer limit, WarningLevel warningLevel) {
        requireStation(stationId);
        validateMeasurementParams(from, to, limit);

        List<Measurement> result = repository.getAllMeasurements().stream()
                .filter(m -> m.getStationId().equalsIgnoreCase(stationId))
                .filter(m -> from == null || !m.getTimestamp().isBefore(from))
                .filter(m -> to == null || !m.getTimestamp().isAfter(to))
                .filter(m -> warningLevel == null || m.getWarningLevel() == warningLevel)
                // Aelteste zuerst -> der 'limit'-Parameter schneidet die aeltesten ab
                .sorted(Comparator.comparing(Measurement::getTimestamp))
                .collect(Collectors.toList());

        if (limit != null && result.size() > limit) {
            result = result.subList(result.size() - limit, result.size());
        }
        return result;
    }

    /* ---------- Warnungen (Punkt 4) ---------- */

    public List<Measurement> getAlerts(WarningLevel warningLevel, String river) {
        return repository.getAllStations().stream()
                .filter(s -> river == null || s.getRiver().equalsIgnoreCase(river))
                .map(s -> repository.getLatestMeasurementForStation(s.getId()).orElse(null))
                .filter(m -> m != null && m.getWarningLevel() != null
                        && m.getWarningLevel() != WarningLevel.NORMAL
                        && m.getWarningLevel() != WarningLevel.UNKNOWN)
                .filter(m -> warningLevel == null || m.getWarningLevel() == warningLevel)
                .collect(Collectors.toList());
    }

    /* ---------- Statistik (Punkt 4) ---------- */

    public StationStatistics getStatistics(String stationId, Instant from, Instant to) {
        requireStation(stationId);
        validateMeasurementParams(from, to, null);

        List<Measurement> measurements = repository.getAllMeasurements().stream()
                .filter(m -> m.getStationId().equalsIgnoreCase(stationId))
                .filter(m -> from == null || !m.getTimestamp().isBefore(from))
                .filter(m -> to == null || !m.getTimestamp().isAfter(to))
                .collect(Collectors.toList());

        return StationStatistics.of(stationId, measurements);
    }

    /* ---------- Warnstufe (Punkt 3, auch vom Simulator genutzt) ---------- */

    /**
     * Serverseitige Berechnung der Warnstufe (Punkt 3):
     *   waterLevel <  warningWaterLevel      -> NORMAL
     *   waterLevel >= warningWaterLevel      -> WARNING
     *   waterLevel >= criticalWaterLevel     -> CRITICAL
     *   keine Station / keine Messung        -> UNKNOWN
     */
    public WarningLevel calculateWarningLevel(Station station, Double waterLevel) {
        if (station == null || waterLevel == null) {
            return WarningLevel.UNKNOWN;
        }
        if (waterLevel >= station.getCriticalWaterLevel()) {
            return WarningLevel.CRITICAL;
        }
        if (waterLevel >= station.getWarningWaterLevel()) {
            return WarningLevel.WARNING;
        }
        return WarningLevel.NORMAL;
    }

    public WarningLevel latestWarningLevel(String stationId) {
        Optional<Measurement> latest = repository.getLatestMeasurementForStation(stationId);
        if (latest.isEmpty() || latest.get().getWarningLevel() == null) {
            return WarningLevel.UNKNOWN;
        }
        return latest.get().getWarningLevel();
    }

    /* ---------- Hilfsmethoden ---------- */

    private Station requireStation(String stationId) {
        return repository.getStationById(stationId)
                .orElseThrow(() -> new StationNotFoundException(stationId));
    }

    /** Validierung der Messungs-Parameter (Punkt 6). */
    private void validateMeasurementParams(Instant from, Instant to, Integer limit) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new InvalidRequestException("Ungueltiger Zeitraum: 'from' liegt nach 'to'");
        }
        if (limit != null) {
            if (limit < 0) {
                throw new InvalidRequestException("Ungueltiges limit: darf nicht negativ sein");
            }
            if (limit > DataRepository.MAX_LIMIT) {
                throw new InvalidRequestException(
                        "Ungueltiges limit: maximal " + DataRepository.MAX_LIMIT + " erlaubt");
            }
        }
    }
}
