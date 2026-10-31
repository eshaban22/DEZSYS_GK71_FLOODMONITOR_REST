# Simulations- und Warnstufenregeln (Punkt 2 + 3)

Quelle: [`SimulationsService.java`](../src/main/java/tgm/ac/at/floodmonitor/service/SimulationsService.java)
und [`FloodMonitorService.calculateWarningLevel()`](../src/main/java/tgm/ac/at/floodmonitor/service/FloodMonitorService.java)

## Simulation (Punkt 2)

Der `SimulationsService` läuft per `@Scheduled(fixedRateString = "${simulation.interval:10000}")`
alle 10 Sekunden (konfigurierbar) und erzeugt **für jede aktive Station** eine neue Messung.

| Nr. | Regel | Umsetzung im Code |
|---|---|---|
| 1 | Niederschlag 0–25 mm/h, in „Regenphasen“ | `simulateRainfall()`: nach Regen (≥ 10 mm/h) regnet es mit 70 % Wahrscheinlichkeit weiter, sonst nur 15 % → realistische Wetter-Fronten |
| 2 | Starker Niederschlägt erhöht den Wasserstand | `change = rainfall * 0.8` wenn `rainfall >= 10`, sonst Schwankung −2…+2 cm |
| 3 | Wasserstand nicht völlig zufällig | Neuer Wert = **vorheriger Wert + kleine Änderung** (Regel 8) |
| 4 | Höherer Wasserstand → höherer Durchfluss | `flowRate ≈ 2.3 × waterLevel + Rauschen` |
| 5 | Wasserstand nie negativ | `Math.max(0.0, …)` |
| 6 | Akku zwischen 0 und 100 | `Math.max(0, Math.min(100, …))`, sinkt pro Intervall um 0–1 % |
| 7 | Niedriger Akku → MAINTENANCE | `batteryLevel < 10` → `StationStatus.MAINTENANCE` |
| 8 | Vorheriger Messwert wird berücksichtigt | `previous = repository.getLatestMeasurementForStation(...)` als Startpunkt |
| 9 | Inaktive Stationen liefern nichts | `if (!station.isActive()) continue;` |

Konfiguration: `simulation.interval=10000` (Millisekunden).
Für Tests deaktivierbar: `simulation.enabled=false` (Punkt 9) — siehe `@Value("${simulation.enabled:true}")`.

Beim Start (`ApplicationRunner.run()`) wird zusätzlich für die Seed-Messungen
des Repositorys die Warnstufe nachberechnet, damit die API nie „leere“ Daten liefert.

## Warnstufen-Berechnung (Punkt 3)

Die Warnstufe wird **ausschließlich serverseitig** berechnet
(`FloodMonitorService.calculateWarningLevel`) und nie vom Client akzeptiert:

| Bedingung | Warnstufe |
|---|---|
| `waterLevel < warningWaterLevel` | `NORMAL` |
| `waterLevel >= warningWaterLevel` | `WARNING` |
| `waterLevel >= criticalWaterLevel` | `CRITICAL` |
| keine Station / keine Messung / `null`-Wert | `UNKNOWN` |

Die Schwellen kommen aus der jeweiligen Station (`warningWaterLevel`, `criticalWaterLevel`).

### Beispielwerte der Seed-Daten

| Station | Verlauf (cm) | Schwellen | Ergebnis |
|---|---|---|---|
| ST-001 | 200 → 318.4 | Warning 300, Critical 500 | letzte Messung **WARNING** |
| ST-002 | 180 → 490 | Warning 320, Critical 480 | 3× NORMAL, 2× WARNING, letzte **CRITICAL** |
| ST-003 | keine (inaktiv) | — | keine Messungen, `latest` → leere Menge |
