/**
 * Webclient (Punkt 7) fuer das Hochwasser-Fruehwarnsystem.
 *
 * Funktionen:
 *  - laedt Stationen und befuellt das Stations-Dropdown
 *  - zeigt den aktuellen Messwert jeder aktiven Station als Karten
 *  - historische Messwerte tabellarisch (Filter: Zeitraum + Warnstufe)
 *  - Umschalter JSON <-> XML (Accept-Header), XML wird per DOMParser geparst
 *  - periodische Aktualisierung (konfigurierbar)
 *  - Ladezustaende und verstaendliche Fehlermeldungen
 */
"use strict";

const API = "/api/v1";

let stations = [];
let refreshTimer = null;

const $stationSelect = $("#stationSelect");
const $warningSelect = $("#warningSelect");
const $fromInput = $("#fromInput");
const $toInput = $("#toInput");
const $formatSelect = $("#formatSelect");
const $refreshSelect = $("#refreshSelect");

/* ---------- Lade-/Fehlerzustaende ---------- */

function setStatus($el, text, type) {
    $el.removeClass("hidden error").text(text);
    if (type === "error") $el.addClass("error");
    else if (type === "hidden") $el.addClass("hidden");
}

function showError($el, jqXHR) {
    let msg = "Unbekannter Fehler bei der Abfrage.";
    if (jqXHR.responseJSON && jqXHR.responseJSON.message) {
        msg = jqXHR.responseJSON.message;
    } else if (jqXHR.responseText) {
        try { // Fehlerkoerper kann auch XML sein
            const $m = $($.parseXML(jqXHR.responseText)).find("message");
            if ($m.length) msg = $m.text();
        } catch (e) { /* JSON-Fallback oben */ }
    }
    setStatus($el, "Fehler: " + msg, "error");
}

/* ---------- Format-Helfer (JSON und XML) ---------- */

function currentFormat() {
    return $formatSelect.val(); // "json" | "xml"
}

function acceptHeader() {
    return currentFormat() === "xml" ? "application/xml" : "application/json";
}

function dataTypeFor() {
    return currentFormat() === "xml" ? "xml" : "json";
}

/** HTML-escapes einen Wert, bevor er ins DOM eingesetzt wird. */
function esc(value) {
    return $("<div>").text(String(value)).html();
}

/**
 * Wandelt ein JSON-Objekt ODER einen XML-Knoten in ein einheitliches
 * JavaScript-Objekt um, damit der Rest des Clients formatunabhaengig arbeitet.
 */
function toMeasurement(node) {
    if (currentFormat() === "xml") {
        const $n = $(node);
        const t = (tag) => $n.find(tag).first().text();
        return {
            stationId: t("stationId"),
            timestamp: t("timestamp"),
            waterLevel: parseFloat(t("waterLevel")),
            flowRate: parseFloat(t("flowRate")),
            rainfall: parseFloat(t("rainfall")),
            temperature: parseFloat(t("temperature")),
            batteryLevel: parseInt(t("batteryLevel"), 10),
            status: t("status"),
            warningLevel: t("warningLevel"),
        };
    }
    return {
        stationId: node.stationId,
        timestamp: node.timestamp,
        waterLevel: node.waterLevel,
        flowRate: node.flowRate,
        rainfall: node.rainfall,
        temperature: node.temperature,
        batteryLevel: node.batteryLevel,
        status: node.status,
        warningLevel: node.warningLevel,
    };
}

/** Vereinheitlicht JSON-Arrays / XML-Listen zu einem Feld von Objekten. */
function extractItems(data) {
    if (currentFormat() === "xml") {
        return $(data).find("item").toArray().map((item) => toMeasurement(item));
    }
    return data.map((d) => toMeasurement(d));
}

/* ---------- API-Aufrufe ---------- */

function apiGet(url, $statusEl, onSuccess) {
    setStatus($statusEl, "Lade Daten…", null);
    $.ajax({
        url: url,
        method: "GET",
        dataType: dataTypeFor(),
        headers: { Accept: acceptHeader() },
        success: (data) => {
            setStatus($statusEl, "", "hidden");
            onSuccess(data);
        },
        error: (jqXHR) => showError($statusEl, jqXHR),
    });
}

/* ---------- Rendering ---------- */

function warningBadge(level) {
    const safe = esc(level || "UNKNOWN");
    return '<span class="badge ' + safe + '">' + safe + "</span>";
}

function formatTime(iso) {
    if (!iso) return "-";
    const d = new Date(iso);
    return isNaN(d.getTime()) ? iso : d.toLocaleString("de-AT");
}

function renderStations(list) {
    stations = list;
    $stationSelect.empty().append('<option value="">alle</option>');
    list.forEach((s) => {
        $stationSelect.append(
            '<option value="' + esc(s.id) + '">' + esc(s.name) + " (" + esc(s.river) + ")</option>");
    });
}

function latestCardHtml(m) {
    return '' +
        '<div class="card">' +
        '<h3>' + esc(m.stationId) + ' &middot; ' + esc(formatTime(m.timestamp)) + '</h3>' +
        '<p class="wl">' + esc(m.waterLevel) + ' cm &nbsp;' + warningBadge(m.warningLevel) + '</p>' +
        '<p>Durchfluss: ' + esc(m.flowRate) + ' m&sup3;/s</p>' +
        '<p>Niederschlag: ' + esc(m.rainfall) + ' mm/h &middot; Temp: ' + esc(m.temperature) + ' &deg;C</p>' +
        '<p>Akku: ' + esc(m.batteryLevel) + ' % &middot; Status: ' + esc(m.status) + '</p>' +
        '</div>';
}

function renderAlerts(data) {
    const $list = $("#alertsList");
    $list.empty();
    const items = extractItems(data);
    if (!items.length) {
        $list.html('<div class="muted">Keine Warnungen aktiv.</div>');
        return;
    }
    items.forEach((m) => {
        $list.append(
            '<div class="alert-item">' + warningBadge(m.warningLevel) +
            '<strong>' + esc(m.stationId) + '</strong>' +
            '<span>Wasserstand ' + esc(m.waterLevel) + ' cm</span>' +
            '<span class="muted">' + esc(formatTime(m.timestamp)) + '</span>' +
            '</div>');
    });
}

function loadLatest() {
    const active = stations.filter((s) => s.isActive === true || s.isActive === "true");
    const $cards = $("#latestCards");
    $cards.empty();
    if (!active.length) {
        setStatus($("#latestStatus"), "Keine aktiven Stationen.", null);
        return;
    }
    setStatus($("#latestStatus"), "Lade aktuelle Messwerte…", null);
    let loaded = 0;
    active.forEach((s) => {
        $.ajax({
            url: API + "/stations/" + s.id + "/measurements/latest",
            method: "GET",
            dataType: dataTypeFor(),
            headers: { Accept: acceptHeader() },
            success: (data) => {
                const m = currentFormat() === "xml" ? toMeasurement($(data)) : toMeasurement(data);
                $cards.append(latestCardHtml(m));
                done();
            },
            error: () => done(),
        });
    });
    function done() {
        loaded++;
        if (loaded === active.length) setStatus($("#latestStatus"), "", "hidden");
    }
}

function renderHistory(data) {
    const $body = $("#historyBody");
    $body.empty();
    const items = extractItems(data);
    if (!items.length) {
        setStatus($("#historyStatus"), "Keine Messwerte fuer die gewaehlten Filter gefunden.", null);
        return;
    }
    items.forEach((m) => {
        const level = m.warningLevel || "UNKNOWN";
        $body.append(
            '<tr class="row-' + esc(level) + '">' +
            '<td>' + esc(m.stationId) + '</td>' +
            '<td>' + esc(formatTime(m.timestamp)) + '</td>' +
            '<td>' + esc(m.waterLevel) + '</td>' +
            '<td>' + esc(m.flowRate) + '</td>' +
            '<td>' + esc(m.rainfall) + '</td>' +
            '<td>' + esc(m.batteryLevel) + '</td>' +
            '<td>' + esc(m.status) + '</td>' +
            '<td>' + warningBadge(level) + '</td>' +
            '</tr>');
    });
}

/* ---------- Laden ---------- */

function loadAlerts() {
    const params = { warningLevel: $warningSelect.val() || undefined };
    apiGet(API + "/alerts?" + $.param(params), $("#alertsStatus"), renderAlerts);
}

function loadHistory() {
    // Clientseitige Vorpruefung: 'Von' nach 'Bis' abfangen (Server validiert ebenfalls)
    const from = $fromInput.val();
    const to = $toInput.val();
    if (from && to && from > to) {
        setStatus($("#filterError"), "Ungueltiger Zeitraum: 'Von' liegt nach 'Bis'.", "error");
        return;
    }
    setStatus($("#filterError"), "", "hidden");

    const toIso = (v) => (v ? new Date(v).toISOString() : undefined);
    const stationId = $stationSelect.val() ||
        (stations.find((s) => s.isActive === true || s.isActive === "true") || { id: "ST-001" }).id;

    const params = {
        from: toIso(from),
        to: toIso(to),
        warningLevel: $warningSelect.val() || undefined,
        limit: 100,
    };
    apiGet(API + "/stations/" + stationId + "/measurements?" + $.param(params),
        $("#historyStatus"), renderHistory);
}

/* ---------- Periodische Aktualisierung ---------- */

function refreshAll() {
    $("#lastUpdate").text("Zuletzt aktualisiert: " + new Date().toLocaleTimeString("de-AT"));
    apiGet(API + "/stations", $("#alertsStatus"), (data) => {
        renderStations(extractStationList(data));
        loadAlerts();
        loadLatest();
        loadHistory();
    });
}

/** Stationen: JSON-Objekte haben kein stationId-Feld im Measurement-Sinn. */
function extractStationList(data) {
    if (currentFormat() === "xml") {
        return $(data).find("item").toArray().map((item) => {
            const $n = $(item);
            const t = (tag) => $n.find(tag).first().text();
            return {
                id: t("id"),
                name: t("name"),
                river: t("river"),
                isActive: t("isActive") === "true",
                normalWaterLevel: parseFloat(t("normalWaterLevel")),
                warningWaterLevel: parseFloat(t("warningWaterLevel")),
                criticalWaterLevel: parseFloat(t("criticalWaterLevel")),
            };
        });
    }
    return data;
}

function scheduleRefresh() {
    if (refreshTimer) clearInterval(refreshTimer);
    const ms = parseInt($refreshSelect.val(), 10);
    if (ms > 0) refreshTimer = setInterval(refreshAll, ms);
}

/* ---------- Events ---------- */

$(function () {
    $("#formatSelect").on("change", refreshAll);
    $("#applyBtn").on("click", loadHistory);
    $("#resetBtn").on("click", () => {
        $fromInput.val("");
        $toInput.val("");
        $warningSelect.val("");
        loadHistory();
    });
    $refreshSelect.on("change", scheduleRefresh);

    refreshAll();
    scheduleRefresh();
});
