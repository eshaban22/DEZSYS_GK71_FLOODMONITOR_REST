# Dokumentation der AI-Prompts

> Forderung der Aufgabenstellung: „AI Prompts müssen dokumentiert werden".
> Diese Datei listet alle AI-Unterstützungen bei diesem Projekt mit Zweck und Ergebnis.
> **Wichtig:** Der Code wurde im Detail durchgearbeitet und kann im Abgabegespräch erklärt werden.

| # | Datum | Prompt (sinngemäß) | Wofür / Ergebnis |
|---|---|---|---|
| 1 | 29.09. | Aufgabenstellung speichern und Ist-Stand des Projekts analysieren | [docs/AUFGABE.md](AUFGABE.md), [docs/STATUS.md](STATUS.md); Analyse ergab u. a. Bug im `Station`-Konstruktor (`this.isActive = false;`) |
| 2 | 06.10. | Projekt komplett fertigstellen: Validierung, Filter, Statistik-Endpunkt | `FloodMonitorService`, `StationStatistics`, `/statistics`, `warningLevel`-Filter bei `/stations` |
| 3 | 06.10. | Zentrale Fehlerbehandlung nach Punkt 6 (404/400/405/406/415, JSON+XML, keine Stacktraces) | `GlobalExceptionHandler`, `ApiError`, `StationNotFoundException`, `InvalidRequestException` |
| 4 | 06.10. | Seed-Messungen für deterministische Tests; Simulation per Property deaktivierbar | `DataRepository.seedMeasurements()`, `simulation.enabled` in `SimulationsService` |
| 5 | 06.10. | 17 MockMvc-Tests für alle Fälle aus Punkt 9 | `StationControllerTest` |
| 6 | 06.10. | Webclient nach Punkt 7 (Tabelle, Filter, JSON/XML-Umschalter, Auto-Refresh) | `static/index.html`, `static/css/style.css`, `static/js/app.js` |
| 7 | 06.10. | OpenAPI-Doku über springdoc-Annotationen | Annotationen im `StationController`, Swagger UI unter `/swagger-ui.html` |
| 8 | 06.10. | README, Architektur-, Simulations- und Prompt-Doku | [README.md](../README.md), [ARCHITEKTUR.md](ARCHITEKTUR.md), [SIMULATION.md](SIMULATION.md), diese Datei |
| 9 | 13.10. | App wieder ausführen: JDK-/Port-Diagnose nach IDE-Neustart; JDK unter `C:\Users\Enis\.jdks` lokalisiert, `mvn compile` und `mvn test` als Gegenprobe (17/17 grün, Exit 0) | Start- und Umfeld-Verifikation; App erfolgreich gestartet (Webclient-Run frisch, Werte gesund) |
| 10 | 13.10. | Live-Check aller Endpunkte mit curl (stations 200, Swagger 200, count 404, POST 405) | Erkenntnis: Übungstasks (waterFlow/count/POST) nie implementiert – Projekt ist bereits die saubere Version; kein Code-Eingriff nötig |
| 11 | 13.10. | Vollständigen Abgleich gegen die offizielle TASK.md machen: Pflicht-/Bonus-Kriterien, Tests, Docs | Status: Implementierung ~100 % (UTF: [STATUS.md](STATUS.md)); Rest ist nur noch Doku/Abgabe-Hygiene |
| 12 | 13.10. | Fragestellungen (Punkt 12) mit Quellverweisen ausformulieren | [FRAGEN.md](FRAGEN.md) inkl. Code-Korrektur-Beispiel für die GET-POST-Falle aus Punkt 12-Frage 8 |
| 13 | 13.10. | Reflexion über aufgetretene Probleme schreiben | [REFLEXION.md](REFLEXION.md) (Port-Konflikt, JDK-Verlust, Simulator-Drift, deterministische Tests, 406/415-Handler, Prioritäten) |

## Reflexion über aufgetretene Probleme

- **Bug `isActive`:** Im `Station`-Konstruktor wurde der Parameter ignoriert
  (`this.isActive = false;`). Dadurch wäre die "inaktive" Station ST-003 aktiv
  gewesen und hätte Messungen erhalten. Gefunden bei der Code-Analyse.
- **Jackson-Feldname:** `boolean isActive` mit Getter `isActive()` wurde von Jackson
  als `"active"` serialisiert, die Aufgabenstellung verlangt aber `"isActive"`.
  Gelöst mit `@JsonProperty("isActive")` am Getter.
- **CRLF-Zeilenenden:** Einige Bestandsdateien nutzten Windows-Zeilenenden, was das
  automatische Ersetzen erschwerte; gelöst durch Neuanlegen der Dateien.
- **Feldname `Measurement.map(...)`:** Nach der Service-Umstellung lieferte der Service
  direkt ein `Measurement`, der Controller erwartete `Optional` (für 204). Behoben,
  indem der Service `Optional<Measurement>` liefert.
- **Port 8080 belegt:** Nach Code-Änderungen blieb eine alte JVM-Prozessinstanz am
  Port; der Neustart schlug zunächst fehl, bis der Prozess beendet wurde.

## Was ich selbst gelernt / erklärt habe

(siehe auch Abgabegespräch-Vorbereitung: Content Negotiation über `produces` +
`Accept`-Header, `@RestControllerAdvice`, `@Scheduled`, `ApplicationRunner`,
`Instant`/ISO-8601, springdoc-Annotationen)
