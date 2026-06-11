package cc.fascinated.monitor.metrics.vm.query;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record VmQueryResponse(String status, VmQueryData data, String error) {

    public List<VmTimeSeries> timeSeries() {
        if (this.data == null || this.data.result() == null) {
            return List.of();
        }
        List<VmTimeSeries> series = new ArrayList<>(this.data.result().size());
        for (VmQueryResult result : this.data.result()) {
            Map<String, String> labels = result.metric() != null ? result.metric() : Map.of();
            if (result.values() != null) {
                series.add(new VmTimeSeries(labels, parseSamples(result.values())));
            } else if (result.value() != null) {
                series.add(new VmTimeSeries(labels, List.of(parseSample(result.value()))));
            } else {
                series.add(new VmTimeSeries(labels, List.of()));
            }
        }
        return Collections.unmodifiableList(series);
    }

    private static List<VmSample> parseSamples(List<?> values) {
        List<VmSample> samples = new ArrayList<>(values.size());
        for (Object row : values) {
            samples.add(parseSample(row));
        }
        return samples;
    }

    private static VmSample parseSample(Object row) {
        if (row instanceof List<?> list) {
            if (list.size() < 2) {
                throw new IllegalArgumentException("Expected [timestamp, value] sample pair");
            }
            Instant timestamp = Instant.ofEpochSecond(toLong(list.get(0)));
            double value = Double.parseDouble(String.valueOf(list.get(1)));
            return new VmSample(timestamp, value);
        }
        if (row instanceof Object[] array) {
            if (array.length < 2) {
                throw new IllegalArgumentException("Expected [timestamp, value] sample pair");
            }
            Instant timestamp = Instant.ofEpochSecond(toLong(array[0]));
            double value = Double.parseDouble(String.valueOf(array[1]));
            return new VmSample(timestamp, value);
        }
        throw new IllegalArgumentException("Unexpected sample row type: " + row.getClass());
    }

    private static long toLong(Object timestamp) {
        if (timestamp instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(timestamp));
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record VmQueryData(String resultType, List<VmQueryResult> result) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record VmQueryResult(Map<String, String> metric, List<?> values, List<?> value) {}
}
