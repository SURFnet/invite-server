package guests.exception;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class InvitationEmailMatchingException extends AuthenticationException {
    public InvitationEmailMatchingException(String msg) {
        super(msg);
    }
}
