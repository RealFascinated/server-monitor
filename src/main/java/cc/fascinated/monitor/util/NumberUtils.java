package cc.fascinated.monitor.util;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.Nullable;

@UtilityClass
public class NumberUtils {

    @Nullable
    public static Long toLong(@Nullable Double value) {
        return value != null ? value.longValue() : null;
    }
}
