package guests.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InvitationTest {

    @Test
    void anyRoles() {
        Invitation invitation = new Invitation();
        List<String> roles = invitation.anyRoles();
        assertEquals(0, roles.size());

        invitation.addInvitationRole(new InvitationRole(new Role()));
        roles = invitation.anyRoles();
        assertEquals(1, roles.size());
    }
}