package cc.fascinated.monitor.controller.v1;

import cc.fascinated.monitor.model.dto.request.server.ServerCreateRequest;
import cc.fascinated.monitor.model.dto.request.server.ingest.IngestServerMetrics;
import cc.fascinated.monitor.model.dto.response.server.CreatedServerResponse;
import cc.fascinated.monitor.model.dto.response.server.IngestTokenResponse;
import cc.fascinated.monitor.model.dto.response.server.ServerResponse;
import cc.fascinated.monitor.model.persistance.ServerRow;
import cc.fascinated.monitor.model.persistance.UserRow;
import cc.fascinated.monitor.service.ServerService;
import cc.fascinated.monitor.web.auth.AuthenticatedServer;
import cc.fascinated.monitor.web.auth.AuthenticatedUser;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/v1/servers")
public class ServerController {

    private final ServerService serverService;

    public ServerController(ServerService serverService) {
        this.serverService = serverService;
    }

    @GetMapping
    public List<ServerResponse> listServers(@AuthenticatedUser UserRow user) {
        return this.serverService.listServers(user);
    }

    @PostMapping(value = "/create")
    public CreatedServerResponse createServer(@AuthenticatedUser UserRow user, @RequestBody ServerCreateRequest createRequest) {
        return this.serverService.createServer(user, createRequest);
    }

    @DeleteMapping(value = "/{serverId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteServer(@AuthenticatedUser UserRow user, @PathVariable long serverId) {
        this.serverService.deleteServer(user, serverId);
    }

    @PostMapping(value = "/{serverId}/ingest-token/rotate")
    public IngestTokenResponse rotateIngestToken(@AuthenticatedUser UserRow user, @PathVariable long serverId) {
        return this.serverService.rotateIngestToken(user, serverId);
    }

    @PostMapping(value = "/ingest")
    public void ingestMetrics(@AuthenticatedServer ServerRow server, @RequestBody IngestServerMetrics metrics) {
        this.serverService.ingestMetrics(server, metrics);
    }
}
