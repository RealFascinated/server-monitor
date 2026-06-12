package cc.fascinated.monitor.util;

import lombok.experimental.UtilityClass;

import java.util.regex.Pattern;

@UtilityClass
public class HttpPathNormalizer {
    private static final Pattern NUMERIC_SEGMENT = Pattern.compile("/\\d+");

    public static String normalize(String uri) {
        if (uri == null || uri.isEmpty()) {
            return "/";
        }
        int queryIndex = uri.indexOf('?');
        String path = queryIndex >= 0 ? uri.substring(0, queryIndex) : uri;
        return NUMERIC_SEGMENT.matcher(path).replaceAll("/{id}");
    }
}
