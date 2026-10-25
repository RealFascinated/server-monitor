package cc.fascinated.monitor.alert;

import cc.fascinated.monitor.util.Constants;
import lombok.experimental.UtilityClass;
import tools.jackson.core.type.TypeReference;

import java.util.Map;
import java.util.TreeMap;

/** Canonical series identity: the label set without {@code server_id}, as sorted JSON ("" when unlabelled). */
@UtilityClass
public class SeriesKeys {
    private static final TypeReference<TreeMap<String, String>> LABELS = new TypeReference<>() {};
    private static final String SERVER_ID = "server_id";

    public static Map<String, String> seriesLabels(Map<String, String> labels) {
        if (labels.size() <= 1 && (labels.isEmpty() || labels.containsKey(SERVER_ID))) {
            return Map.of();
        }
        Map<String, String> sorted = new TreeMap<>(labels);
        sorted.remove(SERVER_ID);
        return sorted;
    }

    public static String keyOf(Map<String, String> seriesLabels) {
        return seriesLabels.isEmpty() ? "" : Constants.OBJECT_MAPPER.writeValueAsString(seriesLabels);
    }

    public static Map<String, String> labelsOf(String key) {
        return key.isEmpty() ? Map.of() : Constants.OBJECT_MAPPER.readValue(key, LABELS);
    }
}
