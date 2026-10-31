package tgm.ac.at.floodmonitor.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tgm.ac.at.floodmonitor.model.*;
import tgm.ac.at.floodmonitor.repository.DataRepository;
import tgm.ac.at.floodmonitor.service.FloodMonitorService;

import java.time.Instant;
import java.util.List;

/**
 * Controller-Schicht (Punkt 8): enthaelt NUR HTTP-Anbindung,
 * die Geschaeftslogik liegt im FloodMonitorService.
 *
 * Content Negotiation (Punkt 5): alle Endpunkte liefern JSON oder XML,
 * je nach Accept-Header (produces bei beiden Formaten registriert).
 */
@Tag(name = "Stations", description = "Messstationen, Messwerte, Warnungen und Statistik")
@RestController
@RequestMapping(value = "/api/v1", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
public class StationController {

    private final FloodMonitorService service;

    public StationController(FloodMonitorService service) {
        this.service = service;
    }

    /* ---------- GET /api/v1/stations ---------- */

    @Operation(summary = "Alle Messstationen abrufen",
            description = "Liefert alle Stationen. Optional filterbar nach Fluss, Aktiv-Status und aktueller Warnstufe.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liste der Stationen (leer, wenn kein Filter passt)"),
            @ApiResponse(responseCode = "400", description = "Ungueltiger Parameterwert (z. B. Warnstufe)", content = @Content)
    })
    @GetMapping("/stations")
    public List<Station> getStations(
            @Parameter(description = "Filter nach Fluss, z. B. 'Donau'") @RequestParam(required = false) String river,
            @Parameter(description = "Filter nach Aktiv-Status (true/false)") @RequestParam(required = false) Boolean isActive,
            @Parameter(description = "Filter nach aktueller Warnstufe der Station") @RequestParam(required = false) WarningLevel warningLevel) {
        return service.getStations(river, isActive, warningLevel);
    }

    /* ---------- GET /api/v1/stations/{stationId} ---------- */

    @Operation(summary = "Einzelne Station abrufen")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Station gefunden"),
            @ApiResponse(responseCode = "404", description = "Station nicht gefunden", content = @Content)
    })
    @GetMapping("/stations/{stationId}")
    public Station getStationById(@Parameter(description = "ID der Station, z. B. ST-001")
                                  @PathVariable String stationId) {
        return service.getStation(stationId);
    }

    /* ---------- GET /api/v1/stations/{stationId}/measurements/latest ---------- */

    @Operation(summary = "Aktuellsten Messwert einer Station abrufen",
            description = "204 No Content, wenn die Station bekannt ist, aber noch keine Messung vorliegt.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Aktueller Messwert"),
            @ApiResponse(responseCode = "204", description = "Station bekannt, aber keine Messung vorhanden"),
            @ApiResponse(responseCode = "404", description = "Station nicht gefunden", content = @Content)
    })
    @GetMapping("/stations/{stationId}/measurements/latest")
    public ResponseEntity<Measurement> getLatestMeasurement(@PathVariable String stationId) {
        return service.getLatestMeasurement(stationId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    /* ---------- GET /api/v1/stations/{stationId}/measurements ---------- */

    @Operation(summary = "Historische Messwerte einer Station abrufen",
            description = "Optional filterbar nach Zeitraum (from/to, ISO-8601), Warnstufe und limit (max. "
                    + DataRepository.MAX_LIMIT + "). Aufsteigend sortiert, aelteste zuerst.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liste der Messungen (leer, wenn nichts passt)"),
            @ApiResponse(responseCode = "400", description = "Ungueltiger Zeitraum, limit oder Parameterwert",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Station nicht gefunden", content = @Content)
    })
    @GetMapping("/stations/{stationId}/measurements")
    public List<Measurement> getMeasurements(
            @PathVariable String stationId,
            @Parameter(description = "Start des Zeitraums (inklusive), ISO-8601: 2026-05-01T08:00:00Z")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @Parameter(description = "Ende des Zeitraums (inklusive), ISO-8601: 2026-05-01T12:00:00Z")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @Parameter(description = "Maximale Anzahl Messungen (0-" + DataRepository.MAX_LIMIT + ")")
            @RequestParam(required = false) Integer limit,
            @Parameter(description = "Filter nach Warnstufe") @RequestParam(required = false) WarningLevel warningLevel) {
        return service.getMeasurements(stationId, from, to, limit, warningLevel);
    }

    /* ---------- GET /api/v1/stations/{stationId}/statistics ---------- */

    @Operation(summary = "Statistik einer Station berechnen",
            description = "Min/Max/Durchschnitt Wasserstand, Durchschnitt Durchfluss, Summe Niederschlag, "
                    + "Anzahl Messungen, Warnungen und kritische Warnungen. Optional mit Zeitraum (from/to).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Statistik der Station"),
            @ApiResponse(responseCode = "400", description = "Ungueltiger Zeitraum oder Parameterwert", content = @Content),
            @ApiResponse(responseCode = "404", description = "Station nicht gefunden", content = @Content)
    })
    @GetMapping("/stations/{stationId}/statistics")
    public StationStatistics getStatistics(
            @PathVariable String stationId,
            @Parameter(description = "Start des Zeitraums (inklusive), ISO-8601")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @Parameter(description = "Ende des Zeitraums (inklusive), ISO-8601")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return service.getStatistics(stationId, from, to);
    }

    /* ---------- GET /api/v1/alerts ---------- */

    @Operation(summary = "Aktuelle Warnungen abrufen",
            description = "Aktuellste Messung jeder aktiven Station mit Warnstufe WARNING oder CRITICAL. "
                    + "Optional filterbar nach Warnstufe und Fluss.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liste der aktuellen Warnungen"),
            @ApiResponse(responseCode = "400", description = "Ungueltiger Parameterwert", content = @Content)
    })
    @GetMapping("/alerts")
    public List<Measurement> getAlerts(
            @Parameter(description = "Filter nach Warnstufe (WARNING/CRITICAL)")
            @RequestParam(required = false) WarningLevel warningLevel,
            @Parameter(description = "Filter nach Fluss, z. B. 'Donau'")
            @RequestParam(required = false) String river) {
        return service.getAlerts(warningLevel, river);
    }
}
