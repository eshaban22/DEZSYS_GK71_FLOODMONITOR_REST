# Architektur und Datenmodell (Punkt 8)

## Schichtenarchitektur

```
┌──────────────────────────────────────────────────────────┐
│ Webclient (static/: index.html + app.js + style.css)     │  Punkt 7
│ jQuery · JSON/XML via Accept-Header · Auto-Refresh       │
└───────────────────────────┬──────────────────────────────┘
                            │ HTTP (JSON / XML)
┌───────────────────────────▼──────────────────────────────┐
│ Controller: StationController (@RestController)          │  nur HTTP
│ /api/v1/stations · /measurements · /statistics · /alerts │
└───────────────────────────┬──────────────────────────────┘
                            │ Methodenaufrufe
┌───────────────────────────▼──────────────────────────────┐
│ Service: FloodMonitorService                             │  Geschaeftslogik
│ Filter · Validierung · Statistik · Warnstufen            │
│ SimulationsService (@Scheduled)                          │  Simulation
└───────────────────────────┬──────────────────────────────┘
                            │
┌───────────────────────────▼──────────────────────────────┐
│ Repository: DataRepository                               │  Datenzugriff
│ 3 Stationen + Seed-Messungen (In-Memory, CopyOnWriteArrayList) │
└──────────────────────────────────────────────────────────┘

Exception: GlobalExceptionHandler (@RestControllerAdvice)     Punkt 6
           -> ApiError als JSON oder XML
```

**Prinzip:** Der Controller enthält keine Geschäftslogik — er validiert nur die
HTTP-Syntax (Pfad-/Query-Parameter) und delegiert an den Service. Die Fachlichkeit
(Filtern, Rechnen, Warnstufen) liegt im Service, die Datenhaltung im Repository.

## Datenmodell

```
Station                          Measurement
├── id: String                   ├── stationId: String        (FK -> Station)
├── name: String                 ├── timestamp: Instant       (ISO-8601 in JSON/XML)
├── river: String                ├── waterLevel: double       cm
├── location: Location           ├── flowRate: double         m³/s
│     ├── latitude: double       ├── rainfall: double         mm/h
│     └── longitude: double      ├── temperature: double      °C
├── normalWaterLevel: double     ├── batteryLevel: int        %
├── warningWaterLevel: double    ├── status: StationStatus
├── criticalWaterLevel: double   │     (ONLINE | MAINTENANCE | OFFLINE)
└── isActive: boolean            └── warningLevel: WarningLevel
                                      (NORMAL | WARNING | CRITICAL | UNKNOWN)

StationStatistics (nur Ausgabe)
├── stationId, minWaterLevel, maxWaterLevel, avgWaterLevel, avgFlowRate
└── totalRainfall, measurementCount, warningCount, criticalWarningCount

ApiError (Fehlerformat)
└── timestamp, status, error, message, path
```

Zeitpunkte werden als `java.time.Instant` gespeichert und von Jackson
automatisch als ISO-8601-String (z. B. `2026-05-01T10:15:00Z`) ausgegeben.

## Ablauf einer Messung

1. `SimulationsService.runSimulation()` läuft alle `simulation.interval` ms.
2. Für jede **aktive** Station: letzten Messwert holen → neue Messung nach den
   [Simulationsregeln](SIMULATION.md) erzeugen → Warnstufe via Service berechnen → speichern.
3. Clients lesen über die REST-Endpunkte und wählen JSON/XML per `Accept`-Header.

## Warum diese Aufteilung?

- **Testbarkeit:** Service und Repository sind ohne HTTP testbar; MockMvc-Tests
  prüfen die Schichten zusammen (siehe [README](../README.md), Punkt 9).
- **Austauschbarkeit:** In-Memory-Repository kann später durch eine Datenbank
  ersetzt werden, ohne Controller oder Service anzufassen.
- **Fehlerkonsistenz:** Eine zentrale `@RestControllerAdvice` fängt alle Fehler
  und formatiert sie einheitlich (keine Stacktraces).
