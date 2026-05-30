package cc.fascinated.monitor.util;

import java.util.List;
import java.util.function.Consumer;

public final class Utils {
    private Utils() {
    }

    public static <T> void forEach(List<T> items, Consumer<T> action) {
        if (items == null) {
            return;
        }
        for (T item : items) {
            action.accept(item);
        }
    }
}
