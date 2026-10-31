# Fragestellungen (TASK.md Punkt 12)

> Beantwortet mit Quellverweisen auf die tatsächlich implementierte Codebasis
> (`floodmonitor/src` – Pfade relativ zum Projekt-Stamm `floodmonitor/`).
> Alle Antworten beziehen sich auf den echten Code, damit beim Abgabegespräch jede
> Aussage über einen Zeilenverweis direkt gezeigt werden kann.

---

## 1. Was sind die Hauptmerkmale von JSON?

JSON (JavaScript Object Notation) ist ein **textbasiertes, sprachunabhängiges
Datenaustauschformat** mit diesen Hauptmerkmalen:

- **Schlüssel-Wert-Paare**: Daten stehen immer als `"name": "wert"` – der Schlüssel ist ein String, der Wert kann String, Zahl, Boolean, `null`, Objekt oder Array sein.
- **Zwei Strukturen**: Objekt (`{ ... }`, ungeordnete Sammlung von Schlüssel-Wert-Paaren) und Array (`[ ... ]`, geordnete Liste von Werten) – mehr Struktur bietet JSON bewusst nicht.
- **Sprachunabhängig, aber JavaScript-Syntax**: Stammt aus JavaScript, wird aber von praktisch jeder Sprache gelesen/geschrieben (Java verarbeitet es z. B. mit Jackson).
- **Textbasiert und gut lesbar**: Menschlich lesbar, trotzdem einfach maschinell zu parsen – deshalb hat JSON XML als Standardformat für Web-APIs weitgehend abgelöst.
- **Keine Typdeklaration und kein Metadata-Standard**: Datentypen ergeben sich implizit aus der Notation (`"318.4"` ist String, `318.4` ist Zahl); Namensräume oder Validierungsschemata gehören nicht zur Kernspezifikation (JSON Schema ist ein separates Projekt).

**Quellverweis:** In unserem Projekt sind alle Datenmodelle reine POJOs; Jackson erzeugt daraus das JSON automatisch. Die Felder der Messung ( siehe `src/main/java/tgm/ac/at/floodmonitor/model/Measurement.java`, Zeilen 6–14) werden exakt so serialisiert wie im JSON-Beispiel der TASK.md (`stationId`, `timestamp`, `waterLevel`, …). Die Content-Negotiation, die JSON auswählt, liegt in `src/main/java/tgm/ac/at/floodmonitor/controller/StationController.java`, Zeile 29 (`produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE}`).

---

## 2. Was sind die Hauptmerkmale von XML?

XML (Extensible Markup Language) ist eine **Auszeichnungssprache (Markup-Sprache)
zur hierarchischen Strukturierung von Daten**:

- **Baumstruktur**: Eine einzelne Wurzel (`root element`), darunter beliebig verschachtelte Elemente – ideal für hierarchische Daten (z. B. Station → Messung → Wert).
- **Tags und Attribute**: Daten stehen in Start-/End-Tags (`<stationId>ST-001</stationId>`); Metadaten über ein Element kommen als Attribute in den Start-Tag (`<measurement id="ST-001">`).
- **Extensible = frei erweiterbar**: Es gibt keine vordefinierten Tags; die Bedeutung legt die Anwendung fest, ggf. unterstützt durch Schemata (DTD, **XSD/XML Schema**, JSON-Alternative: Validierung des Dokuments gegen ein Schema).
- **Namensräume (Namespaces)**: Elemente aus unterschiedlichen Vokabularen können eindeutig getrennt werden (`xmlns`) – wichtig für Kombinierbarkeit.
- **Strikte Wohlgeformtheit**: Jedes Element muss geschlossen, korrekt geschachtelt sein, ein Dokument hat genau eine Wurzel; Verletzungen sind Parse-Fehler.
- **Metadaten-/Dokumentenorientiert**: XML wurde für Dokumente entworfen und kann Text mit Struktur mischen, ist aber auch als reines Datenformat (SOAP, ältere Enterprise-Schnittstellen) verbreitet.

**Quellverweis:** Jackson XML serialisiert dieselben POJOs in XML: Bei `GET /api/v1/stations` mit `Accept: application/xml` antwortet der Controller (Quelle: `StationController.java` Zeile 29) mit einer XML-Liste; der Test `getAllStations_asXml_...` in `src/test/java/tgm/ac/at/floodmonitor/StationControllerTest.java` (Zeilen 58–65) verifiziert per XPath, dass `/List/item[1]/id` = `ST-001` – also das typische XML-Baum-Dokument mit Wurzel-Element.

---

## 3. Wie unterscheidet die Applikation, ob die Rückgabewerte in JSON oder XML zurückgegeben werden?

Die Applikation entscheidet das **nicht selbst**, sondern lässt das dem **HTTP-Content-Negotiation-Mechanismus** überlassen: Der **Client sendet den `Accept`-Header**, und Spring MVC wählt anhand davon einen konfigurierten `HttpMessageConverter`.

Bei uns konkret:

1. Der `@RequestMapping` am Controller **registriert beide Media-Types** als „produzierbar":
   `StationController.java`, Zeile 29: `produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE}`
2. Im Spring-Kontext müssen beide Konverter existieren: Der normale `MappingJackson2HttpMessageConverter` deckt JSON ab, der `MappingJackson2XmlHttpMessageConverter` (aus der Abhängigkeit **jackson-dataformat-xml**, siehe `pom.xml`) deckt XML ab.
3. Der Request kommt mit `Accept: application/json` oder `Accept: application/xml` → Spring wählt den passenden Konverter, serialisiert dieselbe Java-Objektinstanz in das Wunschformat.
4. **Ohne `Accept`-Header** greift der Default → JSON (Reihenfolge der `produces`-Werte; verifiziert durch den Test `getAllStations_withoutAcceptHeader_defaultsToJson`, `StationControllerTest.java` Zeilen 67–72).
5. **Bei nicht unterstütztem Format** (z. B. `Accept: application/pdf`) wirft Spring eine `HttpMediaTypeNotAcceptableException` → unser `GlobalExceptionHandler` (`src/main/java/tgm/ac/at/floodmonitor/exception/GlobalExceptionHandler.java`, Zeilen 67–70) wird daraus **406 Not Acceptable** (getestet in `StationControllerTest.java` Zeilen 191–195).

Kurz gesagt: **Das Datenobjekt ist identisch, nur die Serialisierung hängt vom Accept-Header ab.** Das ist der Kern von Content Negotiation in REST.

---

## 4. Was zeichnet ein REST Interface aus und warum wird es heute sehr oft als standardisierte Schnittstelle verwendet?

REST (Representational State Transfer) ist ein **Architekturstil für
netzwerkbasierte Schnittstellen**, keine konkrete Technologie. Hauptmerkmale:

- **Ressourcen statt Aktionen**: Alles, was angesprochen wird, ist eine Ressource mit einer **eindeutigen URL** (z. B. `/api/v1/stations/ST-001`); Aktionen ergeben sich aus den **HTTP-Methoden** (GET/POST/PUT/PATCH/DELETE), nicht aus Methodennamen in der URL.
- **Uniform Interface**: Einheitliche, standardisierte Verwendung von HTTP – URLs adressieren Ressourcen, HTTP-Verben beschreiben die Operation, **Statuscodes** kommunizieren das Ergebnis (404 für „gibt's nicht", 400 für „deine Anfrage ist kaputt", 201 für „erstellt", …).
- **Statelessness (Zustandslosigkeit)**: Jeder Request enthält **alle** Informationen, die der Server braucht; der Server speichert keine Session. Das macht Programmierung horizontal skalierbar, ohne Session-Replikation.
- **Repräsentationen statt Datentypen**: Eine Ressource kann mehrere Darstellungen haben (JSON, XML …), gewählt über **Content Negotiation** (`Accept`-Header) – genau das implementiert Punkt 5 unserer Aufgabe.
- **Cacheability & Layered System**: HTTP-Standard-Semantik (Caching, Proxys, Load Balancer) funktioniert ohne Zusatzarbeit.

**Warum so verbreitet?** Weil REST auf dem bereits überall vorhandenen, bewährten **HTTP-Ökosystem** aufbaut: kein eigener Protokoll-Stack, keine Binärformate-Enge, jede Sprache/bibliothek kann HTTP; Debugging mit curl/Browser; einfache Erweiterbarkeit und horizontale Skalierung durch Stateless-Design; und durch JSON ein sprachübergreifendes, leichtgewichtiges Datenformat. Alternativen wie SOAP sind schwerfälliger (XML-Envelope, WSDL), gRPC schneller, aber komplexer im Setup – für typische Web-/Mobile-APIs ist REST der pragmatische Standard.

**Quellverweis:** Unser Projekt zeigt alle REST-Prinzipien: Ressourcen-URLs + HTTP-Verben in `StationController.java` (Zeilen 46–143), einheitliche Statuscodes via `GlobalExceptionHandler.java`, Content Negotiation (Punkt 3 dieser Fragenliste) und Zustandslosigkeit – der Service speichert keinerlei Session-Zustand (`FloodMonitorService.java` arbeitet ausschließlich über die Anfragen-Parameter).

---

## 5. Was ist der Unterschied zwischen den HTTP-Methoden GET, POST, PUT und PATCH?

| Methode | Zweck | Semantik | Idempotent? | Beispiel in unserem Kontext |
|---|---|---|---|---|
| **GET** | Ressource **lesen** | Holt eine Repräsentation; **darf keinen Zustand auf dem Server ändern**; Cachebar; Safe-Methode | ✅ ja (mehrfach = gleiche Antwort) | `GET /api/v1/stations` – Liste; `GET /api/v1/stations/ST-001/measurements/latest` – aktueller Messwert |
| **POST** | Ressource **anlegen** oder nicht-idempotente Aktion | Server entscheidet die URI der neuen Ressource; typischerweise **201 Created** (+ `Location`-Header) | ❌ nein (zweimal ausgeführt = zwei neue Ressourcen) | (Übungs-Task) `POST /api/v1/stations` – neue Station anlegen |
| **PUT** | Ressource **komplett ersetzen** | Der Client gibt die URI bereits vor; **alle** Felder müssen geschickt werden, fehlende werden zurückgesetzt; 201/200/204 | ✅ ja (mehrfach = gleicher Endzustand) | hypothetisch: `PUT /api/v1/stations/ST-009` – Station unter dieser ID komplett neu setzen |
| **PATCH** | Ressource **teilweise ändern** | Nur geänderte Felder schicken (JSON Merge Patch o. ä.); Server merged; 200/204 | ❌ nicht garantiert (hängt vom Patch-Format ab) | hypothetisch: `PATCH /api/v1/stations/ST-009` – nur `isActive=false` schicken |

**Merkhilfe:** **PUT = "So soll die Ressource bitte komplett aussehen", PATCH = "Ändere genau diese Teile", POST = "Mach etwas Neues", GET = "Nur lesen".**

**Quellverweis:** Momentan nutzt der Controller nur GET (`StationController.java`, Zeilen 46–143); genau die fehlende POST-Methode illustriert den Unterschied: Ein `POST /stations` mit Request-Body legt eine Ressource neu an (→ 201), während GET-Endpunkte den Server-Zustand nie verändern – Anlegen via POST versus (teilweises) Ersetzen via PUT/PATCH.

---

## 6. Ich will eine neue Request-Methode / einen neuen Pfad bereitstellen – welche Stellen des Quellcodes muss ich verändern?

Vorgehen in **Schichten** (Punkt 8 der Aufgabe) – von außen nach innen:

1. **Controller** (`src/main/java/tgm/ac/at/floodmonitor/controller/StationController.java`): Eine neue Methode mit der passenden Mapping-Annotation anlegen. Beispiel „Anzahl Stationen":
   ```java
   @GetMapping("/stations/count")
   public int getStationCount() { return service.getStationCount(); }
   ```
   Die `produces`-Einstellung am Klassen-Kopf (Zeile 29) gilt bereits – JSON/XML bekommst du gratis.
2. **Service** (`FloodMonitorService.java`): Die Geschäftslogik als neue, kleine Methode (z. B. `getStationCount()`) – der Controller ruft nur den Service auf, keine umfangreiche Logik im Controller.
3. **Repository** (`src/main/java/tgm/ac/at/floodmonitor/repository/DataRepository.java`): Nur wenn die neue Logik Daten auf eine Weise braucht, die es noch nicht gibt: hier eine neue Abruf-/Schreib-Methode ergänzen (Muster: `getLatestMeasurementForStation`).
4. **Fehlerbehandlung** (`exception/`): Nur wenn es einen **neuen Fehlertyp** gibt: eigene Exception-Klasse + Handler im `GlobalExceptionHandler` (Muster: `StationNotFoundException` → 404).
5. **OpenAPI-Doku**: Am Controller über `@Operation`/`@ApiResponses` dokumentieren (Punkt 10), damit Swagger UI die neue Methode zeigt.
6. **Tests** (`src/test/java/.../StationControllerTest.java`): Neuen Test in Anlehnung an die bestehenden MockMvc-Tests anlegen.
7. **Webclient** (optional, `src/main/resources/static/js/app.js`): Nur wenn der Client die neue Ressource auch nutzen soll.

**Merksatz:** Neuer Pfad = **Controller-Methode + (meist) Service-Methode**; die anderen Stellen nur nach Bedarf.

---

## 7. Kann man OpenAPI und Swagger vergleichen? 

**Ja, aber sie liegen auf unterschiedlichen Ebenen** – der Vergleich lohnt nur, wenn man erkennt, dass es **Spezifikation vs. Tooling** ist:

| | **OpenAPI** | **Swagger** |
|---|---|---|
| Was ist es? | Die **offene Spezifikation** (seit 2015 von der OpenAPI Initiative unter dem Dach der Linux Foundation gepflegt) für maschinenlesbare HTTP-API-Beschreibungen | Ursprünglich der Name der **Specifikation + Tool-Familie**; seit der Umbenennung (~2015/16) Schwestermarke von SmartBear für die Tools |
| Erscheinung | Maschinenlesbares Dokument, i. d. R. `openapi.yaml` / `openapi.json` | Werkzeuge auf Basis einer OpenAPI-Datei |
| Beispiele | Format/Struktur definiert, wie Endpunkte, Parameter, Schemata, Statuscodes beschrieben werden | **Swagger UI** (interaktive Doku), **Swagger Editor**, **Swagger Codegen** |
| Analogie | Wie ein **Bauplan** (Standard, unabhängig von Werkzeugen) | Wie **Werkzeuge**, die einen Bauplan lesen/bauen können |

Konkret in diesem Projekt: **springdoc-openapi** (Abhängigkeit in `pom.xml`) **generiert** aus dem Code inkl. unserer `@Operation`/`@ApiResponse`-Annotationen das OpenAPI-Dokument (`/v3/api-docs`) und liefert die **Swagger UI** unter `/swagger-ui.html` aus – d. h. hier arbeitet die OpenAPI-Spezifikation (das Dokument) und die Swagger-Tooling-Seite (die UI) perfekt zusammen. Man „vergleicht" sie also am besten als zwei Ebenen desselben Ökosystems, nicht als Konkurrenten.

---

## 8. Wo liegt im folgenden Code-Snippet der Fehler? Begründe die Antwort.

```java
@GetMapping("/stations/add")
public String createStation(@RequestParam String name, @RequestParam double warning) {
    stationService.save(new Station(name, warning));
    return "Station successfully created!";
}
```

Der Snippet verletzt **mehrere REST- und HTTP-Grundregeln**:

1. **Falsche Methode + falsche Ressourcenmodellierung**: Eine Ressource **anlegen** ist `POST` auf die Sammlung (`POST /api/v1/stations`), nicht `GET` auf `/stations/add`. „add" im Pfad heißt: Die Aktion steht in der URL statt aus der HTTP-Methode abzulesen – das ist RPC-Stil und kein REST.
   - **Technisch gravierend**: `GET` ist **safe** – er darf den Server-Zustand **nie** ändern. Hier erzeugt `GET /stations/add` eine Station! Browser/Proxys/Preload dürfen GET vorab ausführen, crawlen oder cachen – **das würde versehentlich Stationen anlegen**.
2. **Parameter als Query-Parameter statt Request-Body**: Neue Ressourcen-Daten gehören in den **Body** (JSON), nicht in `@RequestParam`. Query-Parameter tauchen in URL und Logs auf, haben Längenlimit und können Strukturen (z. B. ein ganzer `Location`-Datensatz) nicht vernünftig abbilden.
3. **Falscher Rückgabetyp/Statuscode**: Ein `String` als Body mit implizitem **200 OK** ist nicht REST-typisch. Richtig: Rückgabe der **neuen Station** als Körper, plus **`201 Created`** und idealerweise ein `Location`-Header mit der URI der neu erstellten Ressource (z. B. `/api/v1/stations/ST-XY`). Ein Bestätigungstext „successfully created" in der Response ist für einen API-Client nutzlos.
4. **Fehlende Fehlerabdeckung**: Keine Prüfung, ob die Station schon existiert (→ **409**) oder die Eingabe ungültig ist (→ **400**) – so entstehen inkonsistente Zustände und kein standardisiertes Fehlerformat, wie es unsere Aufgabe in Punkt 6 verlangt.

**Korrekt wäre (in Anlehnung an unsere Codebasis):**

```java
@PostMapping("/stations")
public ResponseEntity<Station> createStation(@RequestBody Station station) {
    Station created = service.addStation(station);   // wirft evtl. 409/400-Exception
    return ResponseEntity
            .created(URI.create("/api/v1/stations/" + created.getId()))
            .body(created);                          // 201 Created + Location + Body
}
```

**Quellverweis:** Die REST-konforme Umsetzung läge genauso wie unsere übrigen Endpunkte im `StationController` und müsste dieselbe Fehlerbehandlung (`GlobalExceptionHandler` für 400/409) und Content Negotiation (`produces` am Klassenkopf, Zeile 29) mitbekommen.

---

## 9. (Bonus-Zusammenhang) Wie hängen die Fragen in diesem Projekt zusammen?

*Kurzer roter Faden fürs Abgabegespräch:* JSON/XML (Fragen 1–2) sind die **Repräsentationen**, die Content Negotiation (Frage 3) die **Auswahl**; REST (Frage 4) ist der **Architekturstil**, GET/POST/PUT/PATCH (Frage 5) die **Werkzeuge**; Frage 6 zeigt die **Codebasen-Konsequenz** (Schichtenmodell), Frage 7 das **Doku-Tooling** und Frage 8 zeigt, was passiert, wenn man die Prinzipien **verletzt** (RPC statt REST, `GET` mit Nebenwirkung). Allesamt spiegeln sich direkt in `StationController`, `FloodMonitorService` und `GlobalExceptionHandler`.

---

*Stand: 13.10.2026 · Erstellt mit AI-Unterstützung, durchgearbeitet und geprüft von Enis – Quellverweise beziehen sich auf den finalen Code.*
