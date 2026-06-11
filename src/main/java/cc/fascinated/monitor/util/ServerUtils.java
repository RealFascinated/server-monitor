package cc.fascinated.monitor.util;

import cc.fascinated.monitor.model.persistance.ServerRow;
import lombok.experimental.UtilityClass;

import java.util.Comparator;

@UtilityClass
public class ServerUtils {
    private static final Comparator<String> NAME_STRING_ORDER = (left, right) -> {
        boolean leftStartsWithLetter = startsWithLetter(left);
        boolean rightStartsWithLetter = startsWithLetter(right);
        if (leftStartsWithLetter != rightStartsWithLetter) {
            return leftStartsWithLetter ? 1 : -1;
        }
        return String.CASE_INSENSITIVE_ORDER.compare(left, right);
    };

    public static final Comparator<ServerRow> NAME_ORDER = Comparator.comparing(
            ServerRow::getServerName,
            NAME_STRING_ORDER
    );

    private static boolean startsWithLetter(String name) {
        return !name.isEmpty() && Character.isLetter(name.charAt(0));
    }
}
