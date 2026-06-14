package cc.fascinated.monitor.service;

import cc.fascinated.monitor.exception.impl.BadRequestException;
import cc.fascinated.monitor.model.dto.response.server.IncidentResponse;
import cc.fascinated.monitor.model.persistance.IncidentRow;
import cc.fascinated.monitor.repository.IncidentRepository;
import cc.fascinated.monitor.util.Pagination;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class IncidentService {
    private static final int MAX_PAGE_SIZE = 50;

    private final IncidentRepository incidentRepository;

    public IncidentService(IncidentRepository incidentRepository) {
        this.incidentRepository = incidentRepository;
    }

    @Transactional
    public void openOutage(long serverId, Instant startedAt) {
        if (this.incidentRepository.findByServerIdAndResolvedAtIsNull(serverId).isPresent()) {
            return;
        }
        this.incidentRepository.save(new IncidentRow(serverId, startedAt, Instant.now()));
    }

    @Transactional
    public void resolveOpenOutage(long serverId, Instant resolvedAt) {
        this.incidentRepository.findByServerIdAndResolvedAtIsNull(serverId)
                .ifPresent(incident -> incident.setResolvedAt(resolvedAt));
    }

    public Pagination.Page<IncidentResponse> listIncidents(long serverId, int page, int count) {
        if (count < 1 || count > MAX_PAGE_SIZE) {
            throw new BadRequestException("count must be between 1 and %d".formatted(MAX_PAGE_SIZE));
        }
        long total = this.incidentRepository.countByServerId(serverId);
        return new Pagination<IncidentResponse>()
                .setItemsPerPage(count)
                .setTotalItems(total)
                .getPage(page, callback -> this.incidentRepository
                        .findByServerIdOrderByStartedAtDesc(
                                serverId,
                                PageRequest.of(callback.skip() / callback.limit(), callback.limit()))
                        .stream()
                        .map(IncidentResponse::from)
                        .toList());
    }
}
