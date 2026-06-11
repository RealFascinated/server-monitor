package cc.fascinated.monitor.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class UserUtils {
    public static String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
}
