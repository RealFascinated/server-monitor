package cc.fascinated.monitor.util;

import lombok.experimental.UtilityClass;
import ua_parser.Client;
import ua_parser.Parser;

@UtilityClass
public class UserAgentUtils {
    private static final Parser PARSER = new Parser();

    public static String formatDeviceLabel(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return "Unknown device";
        }

        Client client = PARSER.parse(userAgent);
        String browser = normalizeFamily(client.userAgent.family, "Unknown browser");
        String os = normalizeFamily(client.os.family, "Unknown OS");
        return browser + " on " + os;
    }

    private static String normalizeFamily(String family, String fallback) {
        if (family == null || family.isBlank() || "Other".equalsIgnoreCase(family)) {
            return fallback;
        }
        return family;
    }
}
