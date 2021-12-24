package guests.mail;

import guests.AbstractMailTest;
import guests.domain.*;
import org.apache.commons.mail.util.MimeMessageParser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertTrue;


class MailBoxTest extends AbstractMailTest {

    @Autowired
    private MailBox mailBox;

    @Test
    void sendInvitation() throws Exception {
        User user = super.user();
        Institution institution = user.getInstitution();
        Invitation invitation = new Invitation();
        invitation.setHash("hash");
        invitation.setEmail("guest@example.com");
        invitation.setInstitution(institution);
        invitation.setMessage("Please join");
        invitation.setIntendedAuthority(Authority.GUEST);
        invitation.addInvitationRole(new InvitationRole(new Role("students", this.application(institution, "Canvas"))));
        invitation.addInvitationRole(new InvitationRole(new Role("students", this.application(institution, "Blackboard"))));

        mailBox.sendInviteMail(user, invitation);

        MimeMessageParser parser = mailMessage();

        String htmlContent = parser.getHtmlContent();
        assertTrue(htmlContent.contains("http://localhost:3000/invitations?h=hash"));
        assertTrue(htmlContent.contains("Canvas"));
    }
}