package com.jlgs.howmuchah.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    public void notifyUserOfNewInvitation(UUID userId) {
        messagingTemplate.convertAndSendToUser(
                userId.toString(),
                "/invitations",
                new InvitationNotification("NEW_INVITATION")
        );
    }

    public record InvitationNotification(String type) {}
}