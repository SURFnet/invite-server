package guests.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InvitationRequest implements Serializable {

    private Invitation invitation;
    private List<String> invites;
    private Long institutionId;
}
