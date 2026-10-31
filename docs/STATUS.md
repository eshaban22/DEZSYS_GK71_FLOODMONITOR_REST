# Projekt-Status: FloodMonitor (Stand: 13.10.2026)

## ✅ Implementierung fertig (alle technischen Punkte)

| Punkt | Umsetzung | Status |
|---|---|---|
| 1 Setup | Spring Boot 3.3, Maven, `./mvnw spring-boot:run` | ✅ |
| 2 Simulation | `SimulationsService`, 9 dokumentierte Regeln, Intervall konfigurierbar | ✅ |
| 3 Warnstufe | Serverseitig in `FloodMonitorService.calculateWarningLevel()` | ✅ |
| 4 Endpunkte | stations (+3 Filter), station, latest, measurements (+4 Filter), alerts (+2 Filter), **statistics** | ✅ |
| 5 JSON/XML | Content Negotiation über Accept-Header, 406 bei fremdem Format | ✅ |
| 6 Fehler | `GlobalExceptionHandler` + `ApiError` (400/404/405/406/415/500, JSON+XML, keine Stacktraces) | ✅ |
| 7 Webclient | `static/index.html` + jQuery: Tabelle, Filter, JSON/XML-Umschalter, Auto-Refresh, Farbcodes | ✅ |
| 8 Architektur | Model/Repository/Service/Controller/Exception/Client sauber getrennt | ✅ |
| 9 Tests | **17 MockMvc-Tests, alle grün**; Simulation via `simulation.enabled=false` deaktivierbar | ✅ |
| 10 OpenAPI | springdoc-Annotationen, Swagger UI unter `/swagger-ui.html` | ✅ |
| Abgabe-Doku | README, ARCHITEKTUR, SIMULATION, AI_PROMPTS | ✅ |

Live verifiziert (13.10.): alle Endpunkte 200/204, 400/404/406 korrekt, JSON und XML
liefern identische Fachdaten, Webclient rendert beide Formate (per DOM-Check geprüft).
`mvn test`: **17/17 grün, BUILD SUCCESS** (Exit-Status verifiziert).

## ⏳ Noch offen (für dich)

- [x] **Pkt. 12 Fragestellungen** (Theorie, mit Quellverweisen) → [FRAGEN.md](FRAGEN.md) ✅ **13.10. erstellt – von Enis durchzulesen und in eigener Formulierung zu prüfen**
- [x] **Reflexion** über aufgetretene Probleme → [REFLEXION.md](REFLEXION.md) ✅ **13.10. als Draft erstellt – Enis schläift die Formulierungen nach**
- [x] **Screenshots** (10 Motive: Webclient inkl. Filter/Zeitraum/XML/Fehler, API-XML, Swagger, 404-ApiError-XML) → [screenshots/](screenshots/) ✅ 13.10., von Enis gemacht und benannt
- [ ] **Git-Commits in logischen Schritten** (Abnahmekriterium!) → Anleitung: [GIT-ANLEITUNG.md](GIT-ANLEITUNG.md) – Commits macht Enis selbst
- [ ] Cheatsheet fürs Abgabegespräch vorbereiten

## Fürs Abgabegespräch verstehen müssen

1. Content Negotiation: `produces` am Controller + `Accept`-Header → 406
2. `@RestControllerAdvice` + eigener `ApiError`: warum zentral?
3. Simulation: Regeln 1–9 in [SIMULATION.md](SIMULATION.md) erklären können
4. Warum `Optional<Measurement>` beim latest-Endpunkt (→ 204)
5. `@JsonProperty("isActive")`: warum nötig? (boolean-Getter-Namenskonvention)
6. Warum Seeds deterministisch sind und die Simulation in Tests aus ist
