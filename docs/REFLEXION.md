# Reflexion – aufgetretene Probleme und deren Lösung

> Kurze Auseinandersetzung mit den Problemen, die während der Entwicklung dieses
> Projekts tatsächlich aufgetreten sind (TASK.md „Abgabe": *kurze Reflexion über
> aufgetretene Probleme*).

---

## 1. „Port 8080 already in use" beim Start über IntelliJ

**Was war das Problem?**
Beim Versuch, die Anwendung in IntelliJ zu starten, kam die Fehlermeldung, Port 8080 sei bereits belegt. Der Browser zeigte aber gleichzeitig eine funktionierende App mit Live-Daten – verwirrend.

**Ursache:**
Die Applikation lief bereits als Hintergrundprozess (gestartet über das Maven-Fenster mit `spring-boot:run`). Ein Port kann nur von einem Prozess belegt werden; IntelliJ wollte ein zweites Mal einen bereits genutzten Port binden.

**Lösung:**
Alte Instanz sauber stoppen (Prozesse über `netstat -ano` identifizieren und terminieren, oder im Run-Fenster das Stop-Symbol / Strg+C verwenden), danach normal neu starten. **Gelernt:** „Port already in use" heißt nie „kaputt", sondern „läuft schon einmal".

## 2. Fehlendes Projekt-JDK nach IDE-Neustart / Workspace-Wechsel

**Was war das Problem?**
IntelliJ zeigte plötzlich hunderte rote Fehler und das Banner „Project JDK is not defined" – obwohl das Projekt zuvor einwandfrei kompiliert hatte.

**Ursache:**
Die `.idea`-Workspace-Konfiguration hatte die SDK-Referenz verloren. Das JDK war physisch vorhanden (unter `C:\Users\Enis\.jdks`), aber dem Projekt nicht mehr zugeordnet.

**Lösung:**
Über *File → Project Structure → Project → SDK* das bestehende JDK (Microsoft OpenJDK 17) wieder zuordnen. **Gelernt:** IDE-Konfiguration ist vom Code getrennt – wenn es rot wird, zuerst prüfen, ob es am Build (Maven) oder an der Konfiguration der IDE liegt. Beweis hier: `mvn test` lief vollgrün trotz der roten IDE-Anzeige – der Code selbst war in Ordnung.

## 3. Langzeit-Drift der Simulation (Wasserstand steigt endlos, Akku → 0)

**Was war das Problem?**
Nach längerer Laufzeit zeigte der Webclient unrealistische Werte (Wasserstand 1232 cm, Akku 0 %, alle Stationen im Status `MAINTENANCE`) – die Simulationsregeln der TASK.md (Punkt 2) waren streng genommen alle erfüllt, aber das Verhalten sah nicht realistisch aus.

**Ursache:**
- Regen **hebt** den Wasserstand, aber es gab keine Regel, die ihn (ohne Regen) wieder Richtung Normalwert sinken lässt.
- Der Akku verlor pro Intervall 0–1 %, bei einem 10-Sekunden-Intervall war er nach rund 17 Minuten leer.

**Lösung / Status:**
Bewusste Designentscheidung: Die Abgabekriterien der TASK.md verlangen die Regeln aus Punkt 2 (Zusammenhang: Niederschlag ↔ Wasserstand, Wasserstand ↔ Durchfluss, vorherige Messung berücksichtigen) und die Akku-Regel; die endlose Steigung ist eine Konsequenz davon, nicht ein Fehler. Die Auswirkung ist praktisch begrenzt, weil die App demo-weise läuft und nicht über Stunden betrieben werden muss. Falls gewünscht, wäre eine Erweiterung um „mean reversion" (langsame Rückkehr Richtung Normalwasserstand, sobald der Regen aufhört) sinnvoll – auch ein guter Gesprächsstoff fürs Abgabegespräch zur Simulation (Punkt 2).

## 4. Nicht-deterministische Tests durch die laufende Simulation

**Was war das Problem?**
Tests für Szenarien wie „genau 6 Messungen", „letzte Messung WARNING" oder Statistik-Zählungen schlagen fehl, wenn der Simulator während der Tests neue (Zufall-)Messungen erzeugt.

**Ursache:**
Der Simulator läuft per `@Scheduled` unabhängig von den Tests – es gibt einen Race zwischen Test-Lesen und Simulation-Schreiben.

**Lösung:**
Der Simulator wurde über die Property `simulation.enabled=false` abschaltbar gemacht
und ist im Test deaktiviert (Punkt 9: „Die Simulation muss für Tests kontrollierbar oder deaktivierbar sein").
- Zusätzlich basieren die Seed-Messungen im `DataRepository` auf **deterministischen Werte ohne Zufall**, damit die erwarteten Warnstufen- und Statistikverteilung immer gleich ist.
Ergebnis: 17/17 Tests laufen stabil. Gelernt: Test-Daten **deterministisch** statt zufällig wählen – und Simulationskomponenten sollten immer abschaltbar sein.

## 5. Content Negotiation: 406 Not Acceptable.setdefault

**Was war das Problem?**
Ein Test mit `Accept: application/pdf` erzeugte zuerst keine einheitliche Antwort, sondern teils unlesbare Fehler, statt eines strukturierten `406`-Bodys.

**Ursache:**
Spring wirft bei korrekt konfigurierter Content Negotiation eine `HttpMediaTypeNotAcceptableException` – diese wird aber nicht vom einheitlichen Fehler-Handler gefasst, wenn man nur eigene Business-Exceptions definiert.

**Lösung:**
Eigene Handler für die Spring-Standard-Exceptions ergänzen (`HttpMediaTypeNotAcceptableException` → 406, `HttpMediaTypeNotSupportedException` → 415, `HttpRequestMethodNotSupportedException` → 405, `NoResourceFoundException` → 404) in der `GlobalExceptionHandler` – so bekommt auch ein „fremder Fehler" denselben `ApiError`-Body (JSON oder XML) mit timestamp/status/error/message/path.

**Gelernt:** Bei `@RestControllerAdvice` sollte man nicht nur die eigenen Business-Exceptions handhaben, sondern auch die Spring-Rahmenwerk-Exceptions – sonst antwortet die API bei Infrastruktur-Fehlern in einem anderen Format als bei fachlichen.

## 6. Prioritätenfindung zwischen Übungs- und Original-Aufgabe

**Was war das Problem?**
Es gab ursprünglich Unklarheit über den Umfang: Die Übungs-Tasks aus dem Lehrer-Repo („Next Tasks" – waterFlow-Feld, count-Endpunkt, POST) wurden zunächst mit der eigentlichen TASK.md verwechselt, was die Prioritätenplanung unnötig erschwerte.

**Lösung/Gelernt:**
Die offizielle `TASK.md` ist die maßgebliche Quelle für die Abgabekriterien; die Übungs-Tasks sind rein optional. Für die Zeitplanung hat sich bewährt, zuerst die **Abnahmekriterien** (unten in der TASK.md) gegen die Implementierung abzugleichen – und nicht die manuelle Checklist abzuarbeiten.

---

## Kurzfazit

Der Umfang der technischen Umsetzung war gut berechenbar; die eigentlichen Hürden lagen im **Umfeld der Technologie** (IDE-Setup, Ports, JDK) und im **Software-Handwerk** (deterministische Tests, einheitliche Fehlerbehandlung). Beides ließ sich systematisch beheben und ist dokumentiert – zusammen mit den Docs in diesem Ordner ein guter Vorbereitungssatz fürs Abgabegespräch.
