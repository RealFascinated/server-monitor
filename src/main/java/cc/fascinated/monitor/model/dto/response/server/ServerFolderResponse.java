package cc.fascinated.monitor.model.dto.response.server;

import cc.fascinated.monitor.model.persistance.ServerFolderRow;

public record ServerFolderResponse(long id, String name) {
    public static ServerFolderResponse from(ServerFolderRow folder) {
        return new ServerFolderResponse(folder.getId(), folder.getName());
    }
}
