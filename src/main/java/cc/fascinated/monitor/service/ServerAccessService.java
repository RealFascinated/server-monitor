package cc.fascinated.monitor.service;

import cc.fascinated.monitor.exception.impl.ConflictException;
import cc.fascinated.monitor.exception.impl.NotFoundException;
import cc.fascinated.monitor.exception.impl.UnauthorizedException;
import cc.fascinated.monitor.model.domain.server.ServerMemberRole;
import cc.fascinated.monitor.model.dto.request.server.ServerMemberInviteRequest;
import cc.fascinated.monitor.model.dto.response.server.access.*;
import cc.fascinated.monitor.model.persistance.ServerInviteRow;
import cc.fascinated.monitor.model.persistance.ServerMemberRow;
import cc.fascinated.monitor.model.persistance.ServerRow;
import cc.fascinated.monitor.model.persistance.UserRow;
import cc.fascinated.monitor.repository.ServerInviteRepository;
import cc.fascinated.monitor.repository.ServerMemberRepository;
import cc.fascinated.monitor.repository.ServerRepository;
import cc.fascinated.monitor.repository.UserRepository;
import cc.fascinated.monitor.util.AuthUtils;
import cc.fascinated.monitor.util.UserUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ServerAccessService {
    private static final int INVITE_TOKEN_BYTES = 32;
    private static final long INVITE_DURATION_DAYS = 7;

    private final ServerMemberRepository serverMemberRepository;
    private final ServerInviteRepository serverInviteRepository;
    private final ServerRepository serverRepository;
    private final UserRepository userRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public ServerAccessService(ServerMemberRepository serverMemberRepository,
                               ServerInviteRepository serverInviteRepository,
                               ServerRepository serverRepository,
                               UserRepository userRepository) {
        this.serverMemberRepository = serverMemberRepository;
        this.serverInviteRepository = serverInviteRepository;
        this.serverRepository = serverRepository;
        this.userRepository = userRepository;
    }

    public boolean isMember(long serverId, long userId) {
        return this.serverMemberRepository.existsByServerIdAndUserId(serverId, userId);
    }

    public List<Long> findMemberServerIds(long userId) {
        return this.serverMemberRepository.findServerIdsByUserId(userId);
    }

    public ServerAccessListResponse listAccess(UserRow user, ServerRow server) {
        UserRow ownerUser = this.userRepository.findById(server.getOwnerId())
                .orElseThrow(() -> new NotFoundException("Server owner not found"));

        List<ServerMemberRow> members = this.serverMemberRepository.findByServerId(server.getId());
        Map<Long, UserRow> usersById = loadUsers(members.stream().map(ServerMemberRow::getUserId).toList());

        List<ServerMemberEntryResponse> memberResponses = members.stream()
                .map(member -> ServerMemberEntryResponse.from(member, usersById.get(member.getUserId())))
                .toList();

        List<PendingInviteResponse> pendingInvites = server.getOwnerId().equals(user.getId())
                ? this.serverInviteRepository.findByServerId(server.getId()).stream()
                        .map(PendingInviteResponse::from)
                        .toList()
                : List.of();

        return new ServerAccessListResponse(
                ServerAccessUserResponse.from(ownerUser),
                memberResponses,
                pendingInvites
        );
    }

    @Transactional
    public ServerInviteCreatedResponse inviteUser(UserRow owner, ServerRow server, ServerMemberInviteRequest request) {
        requireServerOwner(owner, server);

        String email = UserUtils.normalizeEmail(request.email());
        UserRow ownerUser = this.userRepository.findById(server.getOwnerId())
                .orElseThrow(() -> new NotFoundException("Server owner not found"));

        if (email.equalsIgnoreCase(ownerUser.getEmail())) {
            throw new ConflictException("The server owner already has access");
        }

        this.userRepository.findByEmailIgnoreCase(email).ifPresent(user -> {
            if (this.serverMemberRepository.existsByServerIdAndUserId(server.getId(), user.getId())) {
                throw new ConflictException("This user already has access to the server");
            }
        });

        if (this.serverInviteRepository.existsByServerIdAndEmailIgnoreCase(server.getId(), email)) {
            throw new ConflictException("A pending invite already exists for this email");
        }

        String token = generateInviteToken();
        Instant now = Instant.now();
        ServerInviteRow invite = this.serverInviteRepository.save(new ServerInviteRow(
                server.getId(),
                email,
                ServerMemberRole.VIEWER,
                AuthUtils.hash(token),
                owner.getId(),
                now.plus(INVITE_DURATION_DAYS, ChronoUnit.DAYS),
                now
        ));

        return ServerInviteCreatedResponse.from(invite, token);
    }

    @Transactional
    public void removeMember(UserRow owner, ServerRow server, long userId) {
        requireServerOwner(owner, server);

        if (server.getOwnerId().equals(userId)) {
            throw new ConflictException("Cannot remove the server owner");
        }

        if (!this.serverMemberRepository.existsByServerIdAndUserId(server.getId(), userId)) {
            throw new NotFoundException("Member not found on this server");
        }

        this.serverMemberRepository.deleteByServerIdAndUserId(server.getId(), userId);
    }

    @Transactional
    public void revokeInvite(UserRow owner, ServerRow server, long inviteId) {
        requireServerOwner(owner, server);

        ServerInviteRow invite = this.serverInviteRepository.findById(inviteId)
                .filter(row -> row.getServerId().equals(server.getId()))
                .orElseThrow(() -> new NotFoundException("Invite not found"));

        this.serverInviteRepository.delete(invite);
    }

    public List<UserPendingInviteResponse> listPendingInvites(UserRow user) {
        List<ServerInviteRow> invites = this.serverInviteRepository.findByEmailIgnoreCaseAndExpiresAtAfter(
                user.getEmail(),
                Instant.now()
        );
        if (invites.isEmpty()) {
            return List.of();
        }

        Map<Long, ServerRow> serversById = this.serverRepository.findAllById(
                invites.stream().map(ServerInviteRow::getServerId).toList()
        ).stream().collect(Collectors.toMap(ServerRow::getId, Function.identity()));

        return invites.stream()
                .map(invite -> {
                    ServerRow server = serversById.get(invite.getServerId());
                    if (server == null) {
                        throw new NotFoundException("Server \"%s\" not found".formatted(invite.getServerId()));
                    }
                    return UserPendingInviteResponse.from(invite, server);
                })
                .toList();
    }

    @Transactional
    public ServerMemberResponse acceptInvite(UserRow user, String token) {
        ServerInviteRow invite = this.serverInviteRepository
                .findByTokenHashAndExpiresAtAfter(AuthUtils.hash(token), Instant.now())
                .orElseThrow(() -> new UnauthorizedException("Invalid or expired invite"));

        if (!invite.getEmail().equalsIgnoreCase(user.getEmail())) {
            throw new UnauthorizedException("This invite was sent to a different email");
        }

        if (this.serverMemberRepository.existsByServerIdAndUserId(invite.getServerId(), user.getId())) {
            throw new ConflictException("You already have access to this server");
        }

        Instant now = Instant.now();
        ServerMemberRow member = this.serverMemberRepository.save(new ServerMemberRow(
                invite.getServerId(),
                user.getId(),
                invite.getRole(),
                now
        ));
        this.serverInviteRepository.delete(invite);

        ServerRow server = this.serverRepository.findById(invite.getServerId())
                .orElseThrow(() -> new NotFoundException("Server \"%s\" not found".formatted(invite.getServerId())));

        return ServerMemberResponse.from(server, member);
    }

    private static void requireServerOwner(UserRow user, ServerRow server) {
        if (!server.getOwnerId().equals(user.getId())) {
            throw new UnauthorizedException("You do not own this server");
        }
    }

    private String generateInviteToken() {
        byte[] bytes = new byte[INVITE_TOKEN_BYTES];
        this.secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private Map<Long, UserRow> loadUsers(List<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return this.userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(UserRow::getId, Function.identity()));
    }
}
