package com.jlgs.howmuchah.exception;

public class InvalidInvitationLinkException extends RuntimeException {
    public InvalidInvitationLinkException() {
        super("This invitation link has either expired, reached the maximum use (5/5), or does not exist.");
    }
}