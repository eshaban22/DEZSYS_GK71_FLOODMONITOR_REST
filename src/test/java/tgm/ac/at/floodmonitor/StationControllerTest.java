package tgm.ac.at.floodmonitor;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.xpath;

/**
 * Automatisierte Tests (Punkt 9).
 *
 * Die Simulation ist via simulation.enabled=false deaktiviert (Forderung Punkt 9),
 * damit die Tests deterministisch sind: Es existieren genau die Seed-Messungen
 * aus dem DataRepository - 6 pro aktiver Station (ST-001, ST-002).
 *
 * Seed-Verteilung (siehe DataRepository.seedForStation):
 *  ST-001: 200.0 -> 318.4  => 5x NORMAL, 1x WARNING (letzte Messung)
 *  ST-002: 180.0 -> 490.0  => 3x NORMAL, 2x WARNING, 1x CRITICAL (letzte Messung)
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "simulation.enabled=false"
})
class StationControllerTest {

    private static final String BASE = "/api/v1";

    @Autowired
    private MockMvc mockMvc;

    /* ---------- Punkt 9.1 + 9.2: Alle Stationen als JSON und XML ---------- */

    @Test
    void getAllStations_asJson_returnsOkAndJsonArray() throws Exception {
        mockMvc.perform(get(BASE + "/stations").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].id").value("ST-001"));
    }

    @Test
    void getAllStations_asXml_returnsOkAndXmlList() throws Exception {
        mockMvc.perform(get(BASE + "/stations").accept(MediaType.APPLICATION_XML))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML))
                .andExpect(xpath("/List/item[1]/id/text()").string("ST-001"))
                .andExpect(xpath("/List/item[3]/id/text()").string("ST-003"));
    }

    @Test
    void getAllStations_withoutAcceptHeader_defaultsToJson() throws Exception {
        mockMvc.perform(get(BASE + "/stations"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    /* ---------- Punkt 9.3 + 9.4: Existierende und unbekannte Station ---------- */

    @Test
    void getExistingStation_returnsOkWithDetails() throws Exception {
        mockMvc.perform(get(BASE + "/stations/ST-001").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("ST-001"))
                .andExpect(jsonPath("$.name").value("Station Wien Donau"))
                .andExpect(jsonPath("$.river").value("Donau"))
                .andExpect(jsonPath("$.location.latitude").value(48.2082));
    }

    @Test
    void getUnknownStation_returns404WithApiError() throws Exception {
        mockMvc.perform(get(BASE + "/stations/ST-999").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Station ST-999 wurde nicht gefunden"))
                .andExpect(jsonPath("$.path").value(BASE + "/stations/ST-999"));
    }

    /* ---------- Punkt 9.5: Aktueller Messwert ---------- */

    @Test
    void getLatestMeasurement_returnsOkWithWarningLevelWarning() throws Exception {
        mockMvc.perform(get(BASE + "/stations/ST-001/measurements/latest").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stationId").value("ST-001"))
                .andExpect(jsonPath("$.warningLevel").value("WARNING"));
    }

    /* ---------- Punkt 9.6: Filterung nach Zeitraum ---------- */

    @Test
    void getMeasurements_withTimeRange_returnsOnlyMeasurementsInRange() throws Exception {
        Instant now = Instant.now();
        // Nur die letzten 5 Minuten abfragen -> nur die juengste Seed-Messung (Zeitstempel "jetzt")
        String from = now.minus(5, ChronoUnit.MINUTES).toString();
        String to = now.plus(1, ChronoUnit.MINUTES).toString();

        mockMvc.perform(get(BASE + "/stations/ST-001/measurements")
                        .param("from", from).param("to", to)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    /* ---------- Punkt 9.7: Ungueltiger Zeitraum (from nach to) ---------- */

    @Test
    void getMeasurements_withFromAfterTo_returns400() throws Exception {
        mockMvc.perform(get(BASE + "/stations/ST-001/measurements")
                        .param("from", "2026-05-02T08:00:00Z")
                        .param("to", "2026-05-01T08:00:00Z")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void getMeasurements_withInvalidDate_returns400() throws Exception {
        mockMvc.perform(get(BASE + "/stations/ST-001/measurements")
                        .param("from", "kein-datum")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getMeasurements_withNegativeLimit_returns400() throws Exception {
        mockMvc.perform(get(BASE + "/stations/ST-001/measurements")
                        .param("limit", "-1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getMeasurements_withTooLargeLimit_returns400() throws Exception {
        mockMvc.perform(get(BASE + "/stations/ST-001/measurements")
                        .param("limit", "5000")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    /* ---------- Punkt 9.8-9.10: Warnstufen NORMAL / WARNING / CRITICAL ---------- */

    @Test
    void getMeasurements_warningLevelNormal_returnsOnlyNormalMeasurements() throws Exception {
        mockMvc.perform(get(BASE + "/stations/ST-002/measurements")
                        .param("warningLevel", "NORMAL")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$[*].warningLevel").value(everyItem(is("NORMAL"))));
    }

    @Test
    void getMeasurements_warningLevelWarning_returnsOnlyWarningMeasurements() throws Exception {
        mockMvc.perform(get(BASE + "/stations/ST-002/measurements")
                        .param("warningLevel", "WARNING")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].warningLevel").value(everyItem(is("WARNING"))));
    }

    @Test
    void getMeasurements_warningLevelCritical_returnsOnlyCriticalMeasurements() throws Exception {
        mockMvc.perform(get(BASE + "/stations/ST-002/measurements")
                        .param("warningLevel", "CRITICAL")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].warningLevel").value("CRITICAL"));
    }

    /* ---------- Punkt 9.11: Nicht unterstuetztes Format -> 406 ---------- */

    @Test
    void getStations_withUnsupportedAccept_returns406() throws Exception {
        mockMvc.perform(get(BASE + "/stations").accept("application/pdf"))
                .andExpect(status().isNotAcceptable());
    }

    /* ---------- Punkt 9.12: Maximales Limit ---------- */

    @Test
    void getMeasurements_withMaxLimit_returnsOk() throws Exception {
        mockMvc.perform(get(BASE + "/stations/ST-001/measurements")
                        .param("limit", "1000")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    /* ---------- zusaetzlich: Statistik-Endpunkt (Punkt 4) ---------- */

    @Test
    void getStatistics_returnsAllRequiredFields() throws Exception {
        mockMvc.perform(get(BASE + "/stations/ST-001/statistics").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stationId").value("ST-001"))
                .andExpect(jsonPath("$.measurementCount").value(6))
                .andExpect(jsonPath("$.warningCount").value(1))
                .andExpect(jsonPath("$.criticalWarningCount").value(0))
                .andExpect(jsonPath("$.minWaterLevel").value(200.0))
                .andExpect(jsonPath("$.maxWaterLevel").value(318.4))
                .andExpect(jsonPath("$.avgWaterLevel").exists())
                .andExpect(jsonPath("$.avgFlowRate").exists())
                .andExpect(jsonPath("$.totalRainfall").exists());
    }
}
