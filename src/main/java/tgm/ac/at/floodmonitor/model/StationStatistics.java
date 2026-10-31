package tgm.ac.at.floodmonitor.model;

import java.util.List;

/**
 * Statistik-Auswertung fuer eine Station (Punkt 4).
 * Enthaelt mindestens: min/max/avg Wasserstand, avg Durchflussmenge,
 * Summe Niederschlag, Anzahl Messungen, Anzahl Warnungen, Anzahl kritische Warnungen.
 */
public class StationStatistics {

    private String stationId;
    private double minWaterLevel;
    private double maxWaterLevel;
    private double avgWaterLevel;
    private double avgFlowRate;
    private double totalRainfall;
    private long measurementCount;
    private long warningCount;
    private long criticalWarningCount;

    public StationStatistics() {
    }

    /** Berechnet die Statistik aus einer Liste von Messungen (leere Liste -> alles 0). */
    public static StationStatistics of(String stationId, List<Measurement> measurements) {
        StationStatistics s = new StationStatistics();
        s.stationId = stationId;
        s.measurementCount = measurements.size();
        if (measurements.isEmpty()) {
            return s;
        }

        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        double sumWater = 0;
        double sumFlow = 0;
        double sumRain = 0;
        long warnings = 0;
        long critical = 0;

        for (Measurement m : measurements) {
            min = Math.min(min, m.getWaterLevel());
            max = Math.max(max, m.getWaterLevel());
            sumWater += m.getWaterLevel();
            sumFlow += m.getFlowRate();
            sumRain += m.getRainfall();
            if (m.getWarningLevel() == WarningLevel.WARNING) {
                warnings++;
            } else if (m.getWarningLevel() == WarningLevel.CRITICAL) {
                warnings++;
                critical++;
            }
        }

        s.minWaterLevel = round1(min);
        s.maxWaterLevel = round1(max);
        s.avgWaterLevel = round1(sumWater / measurements.size());
        s.avgFlowRate = round1(sumFlow / measurements.size());
        s.totalRainfall = round1(sumRain);
        s.warningCount = warnings;
        s.criticalWarningCount = critical;
        return s;
    }

    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    public String getStationId() { return stationId; }
    public void setStationId(String stationId) { this.stationId = stationId; }

    public double getMinWaterLevel() { return minWaterLevel; }
    public void setMinWaterLevel(double minWaterLevel) { this.minWaterLevel = minWaterLevel; }

    public double getMaxWaterLevel() { return maxWaterLevel; }
    public void setMaxWaterLevel(double maxWaterLevel) { this.maxWaterLevel = maxWaterLevel; }

    public double getAvgWaterLevel() { return avgWaterLevel; }
    public void setAvgWaterLevel(double avgWaterLevel) { this.avgWaterLevel = avgWaterLevel; }

    public double getAvgFlowRate() { return avgFlowRate; }
    public void setAvgFlowRate(double avgFlowRate) { this.avgFlowRate = avgFlowRate; }

    public double getTotalRainfall() { return totalRainfall; }
    public void setTotalRainfall(double totalRainfall) { this.totalRainfall = totalRainfall; }

    public long getMeasurementCount() { return measurementCount; }
    public void setMeasurementCount(long measurementCount) { this.measurementCount = measurementCount; }

    public long getWarningCount() { return warningCount; }
    public void setWarningCount(long warningCount) { this.warningCount = warningCount; }

    public long getCriticalWarningCount() { return criticalWarningCount; }
    public void setCriticalWarningCount(long criticalWarningCount) { this.criticalWarningCount = criticalWarningCount; }
}
