package guests.domain;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class InvitationUpdate {

    private Long id;
    private String message;
    private Instant expiryDate;
}
