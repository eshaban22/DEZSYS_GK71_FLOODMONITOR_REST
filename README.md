# FloodMonitor — Hochwasser-Frühwarnsystem (DEZSYS_GK71)

REST-Schnittstelle für ein Hochwasser-Frühwarnsystem mit **JSON- und XML-Ausgabe**,
Messwert-**Simulation**, **Warnstufen-Berechnung**, **Webclient** und **OpenAPI-Doku**.

> Kurs: MidEng GK7.1 — REST, OpenAPI and Data Formats
> Stack: Java 17, Spring Boot 3.3, Maven, jQuery, springdoc-openapi

## Schnellstart

```bash
# Voraussetzung: Java 17 (JAVA_HOME setzen, falls noetig)
./mvnw spring-boot:run
```

Danach:

| Was | URL |
|---|---|
| **Webclient** | http://localhost:8080/index.html |
| **Swagger UI (OpenAPI)** | http://localhost:8080/swagger-ui.html |
| OpenAPI JSON | http://localhost:8080/v3/api-docs |
| REST-Basis | http://localhost:8080/api/v1 |

Konfiguration (`src/main/resources/application.properties`):

```properties
simulation.interval=10000   # Simulationsintervall in ms
simulation.enabled=true     # false -> keine Simulation (z. B. fuer Tests)
```

## Architektur (Punkt 8)

```
src/main/java/tgm/ac/at/floodmonitor/
├── model/                  # Datenklassen und Enums
│   ├── Station.java          (id, name, river, location, water levels, isActive)
│   ├── Location.java         (latitude, longitude)
│   ├── Measurement.java      (Messwert mit java.time.Instant)
│   ├── StationStatistics.java(Statistik-Auswertung)
│   ├── WarningLevel.java     (NORMAL, WARNING, CRITICAL, UNKNOWN)
│   └── StationStatus.java    (ONLINE, MAINTENANCE, OFFLINE)
├── repository/
│   └── DataRepository.java   # In-Memory-Daten: 3 Stationen + Seed-Messungen
├── service/
│   ├── FloodMonitorService.java # Geschaeftslogik: Filter, Validierung, Statistik, Warnstufen
│   └── SimulationsService.java  # Simulation der Messwerte (Punkt 2)
├── controller/
│   └── StationController.java   # Nur HTTP-Anbindung, keine Geschaeftslogik
├── exception/
│   ├── GlobalExceptionHandler.java (@RestControllerAdvice)
│   ├── ApiError.java             # Einheitliches Fehlerformat
│   ├── StationNotFoundException.java
│   └── InvalidRequestException.java
└── FloodmonitorApplication.java
static/                     # Webclient (Punkt 7): index.html, css/style.css, js/app.js
```

## REST-Endpunkte (Punkt 4)

| Methode | Endpunkt | Beschreibung |
|---|---|---|
| GET | `/api/v1/stations` | Alle Stationen (Filter: `river`, `isActive`, `warningLevel`) |
| GET | `/api/v1/stations/{stationId}` | Einzelne Station |
| GET | `/api/v1/stations/{stationId}/measurements/latest` | Aktueller Messwert |
| GET | `/api/v1/stations/{stationId}/measurements` | Historie (Filter: `from`, `to`, `limit`, `warningLevel`) |
| GET | `/api/v1/stations/{stationId}/statistics` | Statistik (min/max/avg, Summen, Anzahl) |
| GET | `/api/v1/alerts` | Aktuelle Warnungen (Filter: `warningLevel`, `river`) |

## Content Negotiation (Punkt 5)

Der Client waehlt das Format ueber den `Accept`-Header. Ohne Header wird JSON geliefert,
bei nicht unterstuetzten Formaten antwortet die API mit **406 Not Acceptable**.

```bash
# JSON (Standard)
curl http://localhost:8080/api/v1/stations/ST-001/measurements/latest

# XML
curl -H "Accept: application/xml" http://localhost:8080/api/v1/stations/ST-001/measurements/latest

# 406
curl -H "Accept: application/pdf" -i http://localhost:8080/api/v1/stations
```

JSON-Beispiel:

```json
{
  "stationId": "ST-001",
  "timestamp": "2026-05-01T10:15:00Z",
  "waterLevel": 318.4,
  "flowRate": 742.6,
  "rainfall": 12.7,
  "temperature": 14.3,
  "batteryLevel": 87,
  "status": "ONLINE",
  "warningLevel": "WARNING"
}
```

XML-Beispiel:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<measurement>
  <stationId>ST-001</stationId>
  <timestamp>2026-05-01T10:15:00Z</timestamp>
  <waterLevel>318.4</waterLevel>
  <flowRate>742.6</flowRate>
  <rainfall>12.7</rainfall>
  <temperature>14.3</temperature>
  <batteryLevel>87</batteryLevel>
  <status>ONLINE</status>
  <warningLevel>WARNING</warningLevel>
</measurement>
```

## Fehlerbehandlung (Punkt 6)

Zentrale Behandlung ueber `@RestControllerAdvice`. Alle Fehler haben dasselbe Format
(JSON oder XML, je nach Accept-Header) und enthalten **keine Stacktraces**:

```json
{
  "timestamp": "2026-05-01T10:20:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "Station ST-999 wurde nicht gefunden",
  "path": "/api/v1/stations/ST-999"
}
```

| Fall | Status |
|---|---|
| Erfolgreiche Anfrage | 200 OK |
| Station bekannt, aber keine Messung | 204 No Content |
| Ungueltiger Zeitraum / limit / Parameter | 400 Bad Request |
| Station nicht gefunden | 404 Not Found |
| Methode nicht erlaubt | 405 Method Not Allowed |
| Format nicht unterstuetzt | 406 Not Acceptable |
| Medientyp nicht unterstuetzt | 415 Unsupported Media Type |
| Unerwarteter Fehler | 500 (ohne Stacktrace) |

## Tests (Punkt 9)

```bash
./mvnw test
```

17 Tests decken alle geforderten Faelle ab (Stationen JSON/XML, existierende/unbekannte
Station, aktueller Messwert, Zeitraum-Filter, ungueltiger Zeitraum, alle drei Warnstufen,
406, maximales Limit). Die Simulation ist in Tests ueber `simulation.enabled=false`
deaktiviert; die Seed-Messungen im Repository sind deterministisch, damit Tests nie
zufaellig fehlschlagen.

## Webclient (Punkt 7)

Der Client unter http://localhost:8080/index.html kann:

- Stationen laden und auswaehlen,
- den aktuellen Messwert jeder aktiven Station anzeigen,
- historische Messwerte tabellarisch darstellen,
- nach Zeitraum (von/bis) und Warnstufe filtern,
- zwischen **JSON und XML** umschalten (XML wird clientseitig geparst),
- Daten **periodisch aktualisieren** (5/10/30 s, abschaltbar),
- Ladezustaende und verstaendliche Fehlermeldungen anzeigen.

Warnstufen-Farben: NORMAL **gruen**, WARNING **orange**, CRITICAL **rot**, UNKNOWN **grau**.

## Dokumentation

- [docs/AUFGABE.md](docs/AUFGABE.md) — Aufgabenstellung als Checkliste
- [docs/SIMULATION.md](docs/SIMULATION.md) — Simulations- und Warnstufenregeln (Punkt 2+3)
- [docs/ARCHITEKTUR.md](docs/ARCHITEKTUR.md) — Architektur- und Datenmodellbeschreibung
- [docs/AI_PROMPTS.md](docs/AI_PROMPTS.md) — Dokumentation der AI-Prompts
- Swagger UI: http://localhost:8080/swagger-ui.html (Punkt 10)
