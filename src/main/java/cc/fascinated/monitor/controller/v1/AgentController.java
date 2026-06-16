package cc.fascinated.monitor.controller.v1;

import cc.fascinated.monitor.model.dto.response.agent.AgentLatestVersionResponse;
import cc.fascinated.monitor.model.dto.response.agent.AgentUpdateResponse;
import cc.fascinated.monitor.service.AgentUpdateService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/v1/agent")
public class AgentController {
    private final AgentUpdateService agentUpdateService;

    public AgentController(AgentUpdateService agentUpdateService) {
        this.agentUpdateService = agentUpdateService;
    }

    @GetMapping(value = "/latest")
    public AgentLatestVersionResponse getLatestVersion() {
        return new AgentLatestVersionResponse(this.agentUpdateService.getLatestVersion());
    }

    @GetMapping(value = "/update")
    public ResponseEntity<AgentUpdateResponse> checkUpdate(@RequestParam String version) {
        return this.agentUpdateService.checkUpdate(version)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
