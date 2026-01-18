package com.jlgs.howmuchah.exception;

public class InvalidInvitationLinkException extends RuntimeException {
    public InvalidInvitationLinkException() {
        super("This invitation link has either expired, reached its maximum uses (5/5), or does not exist.");
    }
}