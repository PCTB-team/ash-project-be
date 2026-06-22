package com.pctb.webapp.configuration;

import com.pctb.webapp.entity.StudyGroup;
import com.pctb.webapp.repository.GroupMemberRepo;
import com.pctb.webapp.repository.StudyGroupRepo;
import com.pctb.webapp.repository.UserRepo;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GroupChatWebSocketSecurityInterceptor implements ChannelInterceptor {
    JwtDecoder jwtDecoder;

    UserRepo userRepo;

    GroupMemberRepo groupMemberRepo;

    StudyGroupRepo studyGroupRepo;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            authenticateConnection(accessor);
        }

        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            authorizeGroupSubscription(accessor);
        }

        return message;
    }

    private void authenticateConnection(StompHeaderAccessor accessor) {
        String authorizationHeader = accessor.getFirstNativeHeader("Authorization");
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new AccessDeniedException("Missing websocket token");
        }

        Jwt jwt = jwtDecoder.decode(authorizationHeader.substring(7));
        String userId = jwt.getSubject();
        if (userId == null || userId.isBlank() || !userRepo.existsById(userId)) {
            throw new AccessDeniedException("Invalid websocket user");
        }

        accessor.setUser(new UsernamePasswordAuthenticationToken(userId, null, List.of()));
    }

    private void authorizeGroupSubscription(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        if (destination == null || !destination.startsWith("/topic/groups/") || !destination.endsWith("/messages")) {
            return;
        }

        if (accessor.getUser() == null || accessor.getUser().getName() == null) {
            throw new AccessDeniedException("Unauthenticated websocket subscription");
        }

        String groupId = destination
                .replaceFirst("^/topic/groups/", "")
                .replaceFirst("/messages$", "");
        String userId = accessor.getUser().getName();

        boolean isMember = groupMemberRepo.findByGroupIdAndUserId(groupId, userId).isPresent();
        boolean isOwner = studyGroupRepo.findById(groupId)
                .map(StudyGroup::getOwner)
                .map(owner -> owner.getId().equals(userId))
                .orElse(false);

        if (!isMember && !isOwner) {
            throw new AccessDeniedException("User is not a group member");
        }
    }
}
