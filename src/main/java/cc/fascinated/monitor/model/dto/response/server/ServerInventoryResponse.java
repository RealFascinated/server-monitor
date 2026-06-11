package cc.fascinated.monitor.model.dto.response.server;

import cc.fascinated.monitor.model.persistance.ServerInventoryRow;

public record ServerInventoryResponse(
        String ip,
        Integer coreCount,
        Integer threadCount,
        String cpuModel,
        Integer socketCount,
        String osName,
        String osVersion
) {
    public static ServerInventoryResponse from(ServerInventoryRow inventory) {
        return new ServerInventoryResponse(
                inventory.getIp(),
                inventory.getCoreCount(),
                inventory.getThreadCount(),
                inventory.getCpuModel(),
                inventory.getSocketCount(),
                inventory.getOsName(),
                inventory.getOsVersion()
        );
    }
}
