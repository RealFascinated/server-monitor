package cc.fascinated.monitor.alert;

import cc.fascinated.monitor.model.dto.request.alert.AlertDestinationRequest;
import cc.fascinated.monitor.model.dto.request.alert.AlertDestinationUpdateRequest;
import cc.fascinated.monitor.model.dto.request.alert.AlertEnabledRequest;
import cc.fascinated.monitor.model.dto.request.alert.AlertGroupRequest;
import cc.fascinated.monitor.model.dto.request.alert.AlertGroupUpdateRequest;
import cc.fascinated.monitor.model.dto.request.alert.AlertPreviewRequest;
import cc.fascinated.monitor.model.dto.request.alert.AlertRuleRequest;
import cc.fascinated.monitor.model.dto.response.alert.AlertDestinationResponse;
import cc.fascinated.monitor.model.dto.response.alert.AlertEventResponse;
import cc.fascinated.monitor.model.dto.response.alert.AlertGroupResponse;
import cc.fascinated.monitor.model.dto.response.alert.AlertMetricResponse;
import cc.fascinated.monitor.model.dto.response.alert.AlertPreviewResponse;
import cc.fascinated.monitor.model.dto.response.alert.AlertRuleResponse;
import cc.fascinated.monitor.model.dto.response.alert.AlertSeriesResponse;
import cc.fascinated.monitor.model.persistance.UserRow;
import cc.fascinated.monitor.util.Pagination;
import cc.fascinated.monitor.web.auth.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/v1/alerts")
public class AlertController {
    private final AlertRuleService ruleService;
    private final AlertGroupService groupService;
    private final AlertDestinationService destinationService;
    private final AlertHistoryService historyService;
    private final AlertPreviewService previewService;
    private final AlertSeriesService seriesService;

    public AlertController(AlertRuleService ruleService, AlertGroupService groupService,
                           AlertDestinationService destinationService,
                           AlertHistoryService historyService, AlertPreviewService previewService,
                           AlertSeriesService seriesService) {
        this.ruleService = ruleService;
        this.groupService = groupService;
        this.destinationService = destinationService;
        this.historyService = historyService;
        this.previewService = previewService;
        this.seriesService = seriesService;
    }

    @GetMapping("/destinations")
    public List<AlertDestinationResponse> listDestinations(@AuthenticatedUser UserRow user) {
        return this.destinationService.list(user);
    }

    @PostMapping("/destinations")
    public AlertDestinationResponse createDestination(@AuthenticatedUser UserRow user,
                                                      @Valid @RequestBody AlertDestinationRequest request) {
        return this.destinationService.create(user, request);
    }

    @PatchMapping("/destinations/{id}")
    public AlertDestinationResponse updateDestination(@AuthenticatedUser UserRow user, @PathVariable long id,
                                                      @Valid @RequestBody AlertDestinationUpdateRequest request) {
        return this.destinationService.update(user, id, request);
    }

    @DeleteMapping("/destinations/{id}")
    public void deleteDestination(@AuthenticatedUser UserRow user, @PathVariable long id,
                                  @RequestParam(defaultValue = "false") boolean force) {
        this.destinationService.delete(user, id, force);
    }

    @PostMapping("/destinations/{id}/test")
    public void testDestination(@AuthenticatedUser UserRow user, @PathVariable long id) {
        this.destinationService.test(user, id);
    }

    @GetMapping("/groups")
    public List<AlertGroupResponse> listGroups(@AuthenticatedUser UserRow user) {
        return this.groupService.list(user);
    }

    @GetMapping("/groups/{id}")
    public AlertGroupResponse getGroup(@AuthenticatedUser UserRow user, @PathVariable long id) {
        return this.groupService.get(user, id);
    }

    @PostMapping("/groups")
    public AlertGroupResponse createGroup(@AuthenticatedUser UserRow user, @Valid @RequestBody AlertGroupRequest request) {
        return this.groupService.create(user, request);
    }

    @PatchMapping("/groups/{id}")
    public AlertGroupResponse updateGroup(@AuthenticatedUser UserRow user, @PathVariable long id,
                                          @Valid @RequestBody AlertGroupUpdateRequest request) {
        return this.groupService.update(user, id, request);
    }

    @DeleteMapping("/groups/{id}")
    public void deleteGroup(@AuthenticatedUser UserRow user, @PathVariable long id) {
        this.groupService.delete(user, id);
    }

    @GetMapping("/rules")
    public List<AlertRuleResponse> listRules(@AuthenticatedUser UserRow user,
                                             @RequestParam(required = false) Long groupId) {
        return this.ruleService.list(user, groupId);
    }

    @GetMapping("/rules/{id}")
    public AlertRuleResponse getRule(@AuthenticatedUser UserRow user, @PathVariable long id) {
        return this.ruleService.get(user, id);
    }

    @PostMapping("/rules")
    public AlertRuleResponse createRule(@AuthenticatedUser UserRow user, @Valid @RequestBody AlertRuleRequest request) {
        return this.ruleService.create(user, request);
    }

    @PatchMapping("/rules/{id}")
    public AlertRuleResponse updateRule(@AuthenticatedUser UserRow user, @PathVariable long id,
                                        @Valid @RequestBody AlertRuleRequest request) {
        return this.ruleService.update(user, id, request);
    }

    @PostMapping("/rules/{id}/enabled")
    public AlertRuleResponse setRuleEnabled(@AuthenticatedUser UserRow user, @PathVariable long id,
                                            @RequestBody AlertEnabledRequest request) {
        return this.ruleService.setEnabled(user, id, request.enabled());
    }

    @DeleteMapping("/rules/{id}")
    public void deleteRule(@AuthenticatedUser UserRow user, @PathVariable long id) {
        this.ruleService.delete(user, id);
    }

    @GetMapping("/events")
    public Pagination.Page<AlertEventResponse> listEvents(
            @AuthenticatedUser UserRow user,
            @RequestParam int page,
            @RequestParam int count,
            @RequestParam(required = false) Long ruleId,
            @RequestParam(required = false) Long groupId,
            @RequestParam(required = false) Long serverId,
            @RequestParam(required = false) String state
    ) {
        return this.historyService.list(user, page, count, ruleId, groupId, serverId, state);
    }

    @GetMapping("/firing-count")
    public long firingCount(@AuthenticatedUser UserRow user) {
        return this.historyService.firingCount(user);
    }

    @GetMapping("/metrics")
    public List<AlertMetricResponse> listMetrics(@AuthenticatedUser UserRow user) {
        return this.seriesService.metrics();
    }

    @GetMapping("/series")
    public AlertSeriesResponse listSeries(@AuthenticatedUser UserRow user, @RequestParam long serverId,
                                          @RequestParam String metric) {
        return this.seriesService.series(user, serverId, metric);
    }

    @PostMapping("/preview")
    public AlertPreviewResponse preview(@AuthenticatedUser UserRow user, @Valid @RequestBody AlertPreviewRequest request) {
        return this.previewService.preview(user, request);
    }
}
