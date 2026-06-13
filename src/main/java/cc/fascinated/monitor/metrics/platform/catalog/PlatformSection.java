package cc.fascinated.monitor.metrics.platform.catalog;

import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
public enum PlatformSection {
    OVERVIEW("overview"),
    FLEET("fleet"),
    INGEST("ingest"),
    VM("vm"),
    JVM("jvm"),
    HTTP("http");

    private final String jsonKey;

    PlatformSection(String jsonKey) {
        this.jsonKey = jsonKey;
    }
}
