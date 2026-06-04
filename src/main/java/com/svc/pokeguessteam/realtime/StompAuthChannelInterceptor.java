package com.svc.pokeguessteam.realtime;

import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.List;

/**
 * Propaga o {@code Principal} (userId) da sessão HTTP para mensagens STOMP.
 */
@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    public static final String SESSION_USER_ID = "USER_ID";

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            Object userId = accessor.getSessionAttributes() != null
                    ? accessor.getSessionAttributes().get(SESSION_USER_ID)
                    : null;
            if (userId instanceof String id && !id.isBlank()) {
                accessor.setUser(new UsernamePasswordAuthenticationToken(id, null, List.of()));
            }
        }

        if (accessor.getUser() == null && accessor.getSessionAttributes() != null) {
            Object userId = accessor.getSessionAttributes().get(SESSION_USER_ID);
            if (userId instanceof String id && !id.isBlank()) {
                accessor.setUser(new UsernamePasswordAuthenticationToken(id, null, List.of()));
            }
        }

        return message;
    }

    public static String resolveUserId(Principal principal) {
        if (principal == null) {
            return null;
        }
        return principal.getName();
    }
}
