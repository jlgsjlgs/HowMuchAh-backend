package com.jlgs.howmuchah.util;

import java.util.UUID;

public class InvitationLinkMapper {
    private InvitationLinkMapper() {
        // Private constructor to prevent instantiation
    }

    public static String buildLinkUrl(UUID linkId, String token, String baseUrl) {
        return String.format("%s/invite-link/%s?token=%s", baseUrl, linkId, token);
    }
}
