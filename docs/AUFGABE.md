# DEZSYS_GK71_FLOODMONITOR_REST — Aufgabenstellung

> **Kurs:** MidEng GK7.1 — REST, OpenAPI and Data Formats (Grundlagen)
> **Geöffnet:** Di, 15.09.2026 · **Fällig:** Sa, **31.10.2026, 17:00**
> **Gruppengrösse:** 1 Person · **Abgabemodus:** Cheatsheet + kurzes Abgabegespräch
> **Original-Repo:** https://github.com/ThomasMicheler/DEZSYS_GK71_FLOODMONITOR_REST_INCOMPLETE.git

---

## Einführung

Entlang eines Flusses befinden sich mehrere Messstationen eines Hochwasser-Frühwarnsystems.
Die Stationen erfassen in regelmäßigen Zeitabständen den Wasserstand, die Fließgeschwindigkeit,
die Niederschlagsmenge und weitere Umgebungsdaten.

Die Messdaten sollen über eine standardisierte REST-Schnittstelle für unterschiedliche Zielsysteme
zur Verfügung gestellt werden. Abhängig vom Client müssen die Daten sowohl im Format **JSON**
als auch im Format **XML** abrufbar sein.

Neben den aktuellen Messwerten sollen historische Daten, Warnmeldungen und einfache statistische
Auswertungen angeboten werden. Eine Webanwendung visualisiert die Daten und ermöglicht das
Filtern nach Messstation, Zeitraum und Warnstufe.

## Voraussetzungen

- Java Programmierkenntnisse
- Verwendung von Maven oder Gradle
- Verwendung von Git
- Grundlagen dezentraler Systeme, HTTP und REST
- Grundlagen XML und JSON
- Grundlagen HTML, JavaScript und Spring Boot

## Lernziele

Nach Abschluss der Aufgabe können Sie:

- eine REST-Schnittstelle entwerfen und implementieren,
- Ressourcen und URLs sinnvoll strukturieren,
- JSON- und XML-Dokumente erzeugen und verarbeiten,
- HTTP-Methoden, Statuscodes und Content Negotiation korrekt einsetzen,
- Messdaten realistisch simulieren,
- Query-Parameter validieren und Fehler einheitlich zurückgeben,
- eine REST-Schnittstelle mit einem Webclient konsumieren,
- grundlegende Sicherheitsmaßnahmen für APIs umsetzen.

---

## Szenario und Datenmodell

Das Hochwasser-Frühwarnsystem besteht aus mehreren Messstationen. Jede Station befindet sich an
einem bestimmten Flussabschnitt und erzeugt regelmäßig Messungen.

### Eine Messung enthält mindestens:

| Eigenschaft | Beschreibung | Einheit |
|---|---|---|
| `timestamp` | Zeitpunkt der Messung | ISO-8601 |
| `waterLevel` | aktueller Wasserstand | cm |
| `flowRate` | Durchflussmenge | m³/s |
| `rainfall` | Niederschlag der letzten Stunde | mm/h |
| `temperature` | Lufttemperatur | °C |
| `batteryLevel` | Akkustand der Messstation | % |
| `status` | technischer Zustand der Station | Text |
| `warningLevel` | berechnete Hochwasserwarnstufe | Text |

### Eine Messstation enthält mindestens:

`id`, `name`, `river`, `location`, `normalWaterLevel`, `warningWaterLevel`,
`criticalWaterLevel`, `isActive` — die geografische Position besteht aus Breitengrad und Längengrad.

**Zu implementierende Klassen:** `Station`, `Location`, `Measurement`, `WarningLevel`, `StationStatus`

**Enums:**
- `WarningLevel`: `NORMAL`, `WARNING`, `CRITICAL`, `UNKNOWN`
- `StationStatus`: `ONLINE`, `MAINTENANCE`, `OFFLINE`

Zeitpunkte sollen mit einem geeigneten Java-Zeitdatentyp aus `java.time` gespeichert werden.

---

## Aufgabenstellung

### 1. Projekt einrichten

- [ ] Spring-Boot-Projekt (Empfohlen: **Spring Web**, **Jackson XML**)
- [ ] Startbar mit `mvn spring-boot:run` oder `gradle bootRun`

### 2. Messwerte simulieren

Implementiere einen Simulator, der für jede **aktive** Station regelmäßig eine neue Messung erzeugt.
Die Werte müssen realistisch sein und logisch zusammenpassen:

- [ ] Wasserstände dürfen nicht negativ sein
- [ ] Akkustand zwischen 0 und 100 %
- [ ] Starker Niederschlag erhöht den Wasserstand tendenziell
- [ ] Höherer Wasserstand → höhere Durchflussmenge
- [ ] Wasserstand ändert sich zwischen zwei Messungen nicht völlig zufällig
- [ ] Bei sehr niedrigem Akkustand: Status auf `MAINTENANCE`
- [ ] Für deaktivierte / ausgefallene Stationen werden keine Messungen erzeugt
- [ ] Vorheriger Messwert wird berücksichtigt
- [ ] Intervall konfigurierbar, z. B. `simulation.interval=10000`

### 3. Warnstufe berechnen

Die Warnstufe wird **serverseitig** berechnet und darf **nicht** vom Client vorgegeben werden.

| Bedingung | Warnstufe |
|---|---|
| Wasserstand kleiner als Warnwert | `NORMAL` |
| Wasserstand ab Warnwert | `WARNING` |
| Wasserstand ab kritischem Wert | `CRITICAL` |
| Keine aktuelle oder gültige Messung | `UNKNOWN` |

- [ ] Optional: Niederschlag und Geschwindigkeit des Wasseranstiegs berücksichtigen
- [ ] Verwendete Regeln **dokumentieren**

### 4. REST-Schnittstelle implementieren

Basis-URL: **`/api/v1`**

| Methode | Endpunkt | Beschreibung |
|---|---|---|
| GET | `/api/v1/stations` | Alle Messstationen abrufen |
| GET | `/api/v1/stations/{stationId}` | Einzelne Station abrufen |
| GET | `/api/v1/stations/{stationId}/measurements/latest` | Aktuellsten Messwert abrufen |
| GET | `/api/v1/stations/{stationId}/measurements` | Historische Messwerte abrufen |
| GET | `/api/v1/alerts` | Aktuelle Warnungen abrufen |

**Filter für Stationen:**

```
GET /api/v1/stations?river=Donau
GET /api/v1/stations?isActive=true
GET /api/v1/stations?warningLevel=CRITICAL
```

**Parameter für historische Messwerte:** `from`, `to`, `limit`, `warningLevel`

```
GET /api/v1/stations/ST-001/measurements?from=2026-05-01T08:00:00Z&to=2026-05-01T12:00:00Z&limit=100
```

**Filter für Warnungen:** mindestens `warningLevel` und `river` als optionale Filter.

**Statistik enthält mindestens:** niedrigsten und höchsten Wasserstand, durchschnittlichen
Wasserstand, durchschnittliche Durchflussmenge, Summe des Niederschlags, Anzahl der Messungen,
Anzahl der Warnungen, Anzahl der kritischen Warnungen.

### 5. JSON und XML: Content Negotiation

- [ ] Standard: JSON, zusätzlich XML unterstützt
- [ ] Client wählt Format über `Accept`-Header (`application/json` / `application/xml`)
- [ ] Ohne Accept-Header → JSON
- [ ] Nicht unterstütztes Format → **406 Not Acceptable**
- [ ] JSON und XML enthalten dieselben fachlichen Informationen

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

### 6. HTTP-Statuscodes und Fehlerbehandlung

| Situation | Statuscode |
|---|---|
| Anfrage erfolgreich | 200 OK |
| Ressource erstellt | 201 Created |
| Erfolgreich, keine Antwortdaten | 204 No Content |
| Ungültige Eingabe | 400 Bad Request |
| Anmeldung erforderlich | 401 Unauthorized |
| Zugriff nicht erlaubt | 403 Forbidden |
| Station nicht gefunden | 404 Not Found |
| Format nicht unterstützt | 406 Not Acceptable |
| Konflikt | 409 Conflict |
| Medientyp nicht unterstützt | 415 Unsupported Media Type |
| Interner Fehler | 500 Internal Server Error |

- [ ] Zentrale Fehlerbehandlung, z. B. mit `@ControllerAdvice`
- [ ] Fehler ebenfalls in **JSON oder XML** ausgeben
- [ ] Keine Stacktraces ausgeben

Fehler-Format:

```json
{
  "timestamp": "2026-05-01T10:20:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "Station ST-999 wurde nicht gefunden",
  "path": "/api/v1/stations/ST-999"
}
```

**Mindestens behandeln:**

- [ ] Unbekannte Stations-IDs
- [ ] Ungültige Zeiträume / `from` nach `to`
- [ ] Ungültige Warnstufen
- [ ] Negative oder zu große `limit`-Werte
- [ ] Ungültige Datumsformate
- [ ] Nicht unterstützte Formate

### 7. Webclient

Einfacher Client mit **HTML, CSS und jQuery**. Er muss:

- [ ] Stationen laden
- [ ] Aktuellen Messwert einer Station anzeigen
- [ ] Historische Messwerte **tabellarisch** darstellen
- [ ] Nach Zeitraum und Warnstufe filtern
- [ ] Zwischen JSON und XML umschalten
- [ ] Daten **periodisch** aktualisieren
- [ ] Ladezustände und verständliche Fehlermeldungen anzeigen

Tabelle: Station, Zeitpunkt, Wasserstand, Durchflussmenge, Niederschlag, Akkustand,
Stationsstatus, Warnstufe.

**Farbkennzeichnung:** NORMAL **grün** · WARNING **orange** · CRITICAL **rot** · UNKNOWN **grau**

Bei XML-Antworten muss der Client das XML auswerten und dieselben Informationen darstellen
wie bei JSON.

### 8. Architektur

Mindestens diese Bereiche trennen:

- **Model:** Datenklassen und Enums
- **Repository:** Zugriff auf Stationen und Messungen
- **Service:** Geschäftslogik, Simulation und Warnstufenberechnung
- **Controller:** HTTP-Anfragen
- **Exception Handling:** einheitliche Fehlerbehandlung
- **Client:** Konsumieren und Darstellen der REST-Daten

⚠️ Controller dürfen keine umfangreiche Geschäftslogik enthalten.

### 9. Automatisierte Tests

Mindestens Tests für:

- [ ] Abruf aller Stationen als JSON
- [ ] Abruf aller Stationen als XML
- [ ] Abruf einer existierenden Station
- [ ] Abruf einer unbekannten Station
- [ ] Abruf des aktuellsten Messwerts
- [ ] Filterung nach Zeitraum
- [ ] Ungültigen Zeitraum
- [ ] Warnstufe NORMAL
- [ ] Warnstufe WARNING
- [ ] Warnstufe CRITICAL
- [ ] Nicht unterstütztes Format
- [ ] Maximales Limit

Die Simulation muss für Tests **kontrollierbar oder deaktivierbar** sein.
Tests dürfen nicht zufällig fehlschlagen.

### 10. API-Dokumentation

- [ ] OpenAPI-Dokumentation mit Endpunkten, Methoden, Parametern, Request-/Response-Bodies,
      Formaten, Statuscodes, Fehlerantworten und Authentifizierung

### 11. Technische Mindestanforderungen

Java und Spring Boot · Maven oder Gradle · REST, JSON und XML ·
HTML, CSS, JavaScript und jQuery · Git · automatisierte Tests

### 12. Fragestellungen

1. Was sind die Hauptmerkmale von JSON?
2. Was sind die Hauptmerkmale von XML?
3. Wie unterscheidet die Applikation in diesem Beispiel, ob die Rückgabewerte in JSON oder XML zurückgegeben werden?
4. Was zeichnet ein REST Interface aus und warum wird es heute sehr oft als standardisierte Schnittstelle verwendet?
5. Was ist der Unterschied zwischen den HTTP Methoden GET, POST, PUT und PATCH?
6. Ich will eine neue Request Methode / Pfad in meinem REST Interface zur Verfügung stellen, welche Stellen des Quellcodes muss ich verändern?
7. Kann man OpenAPI und Swagger vergleichen? Wenn ja, dann vergleichen Sie beide Themengebiete. Wenn nein, dann begründe die Antwort.
8. Wo liegt im folgenden Code-Snippet der Fehler? Begründe die Antwort.

```java
@GetMapping("/stations/add")
public String createStation(@RequestParam String name, @RequestParam double warning) {
    stationService.save(new Station(name, warning));
    return "Station successfully created!";
}
```

> ⚠️ Antworten **nur mit Quellverweis** gültig!

---

## Bewertung

**Anforderungen „überwiegend erfüllt“**

- REST-Schnittstelle für mindestens **3 Messstationen**
- Rückgabewerte in **JSON und XML** (Pkt. 5)
- **Simulation** der Messwerte basierend auf Regeln (Pkt. 2)
- Umsetzung **aller Endpunkte** (Pkt. 4)
- Beantwortung der **Fragestellungen** (Pkt. 12, nur mit Quellverweis!)
- **AI Prompts müssen dokumentiert werden**

**Anforderungen „zur Gänze erfüllt“**

- **Filterung** der Endpunkte (Pkt. 4)
- Umsetzung des **Webclients** (Pkt. 7)
- Korrekte **Fehlerverarbeitung** bei nicht korrekten Abfragen (Pkt. 6)

---

## Abgabe (über Git-Repository)

- [ ] vollständiger Quellcode
- [ ] README.md
- [ ] Build-Konfiguration
- [ ] Konfigurationsbeispiel ohne geheime Zugangsdaten
- [ ] automatisierte Tests
- [ ] API-Dokumentation
- [ ] Architektur- und Datenmodellbeschreibung
- [ ] Dokumentation der Simulationsregeln
- [ ] Startanleitung
- [ ] Beispiele für JSON- und XML-Anfragen
- [ ] Screenshots des Webclients
- [ ] kurze Reflexion über aufgetretene Probleme

## Abnahmekriterien

- mindestens drei Messstationen vorhanden
- regelmäßig zusammenhängende Messwerte erzeugt
- Warnstufe serverseitig berechnet
- alle verpflichtenden Endpunkte funktionieren
- JSON und XML über Accept abrufbar
- ungültige Anfragen korrekt behandelt
- passende HTTP-Statuscodes verwendet
- Webclient verarbeitet JSON und XML
- Daten in einer Tabelle dargestellt
- Filterung und automatische Aktualisierung funktionieren
- automatisierte Tests vorhanden
- Projekt mit Maven oder Gradle startbar
- Projektdokumentation vorhanden
- nachvollziehbare Git-Commits vorhanden

## Links

- [Spring Boot](https://spring.io/projects/spring-boot)
- [Building an Application with Spring Boot](https://spring.io/guides/gs/spring-boot)
- [Spring Initializr](https://start.spring.io)
- [Building a RESTful Web Service](https://spring.io/guides/gs/rest-service)
- [Consuming a RESTful Web Service](https://spring.io/guides/gs/consuming-rest)
- [Swagger](https://swagger.io)
