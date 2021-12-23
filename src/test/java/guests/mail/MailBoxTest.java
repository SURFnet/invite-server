package guests.mail;

import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;
import guests.AbstractTest;
import guests.domain.*;
import lombok.SneakyThrows;
import org.apache.commons.mail.util.MimeMessageParser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

import javax.mail.internet.MimeMessage;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertTrue;


@ActiveProfiles(value = "prod", inheritProfiles = false)
class MailBoxTest extends AbstractTest {

    @Autowired
    private MailBox mailBox;

    @RegisterExtension
    static GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP);

    @SneakyThrows
    @BeforeEach
    protected void beforeEach() {
        super.beforeEach();
        greenMail.start();
        greenMail.purgeEmailFromAllMailboxes();
    }

    @AfterEach
    void afterEach() {
        greenMail.stop();
    }

    @Test
    void sendInvitation() throws Exception {
        User user = super.user();
        Institution institution = user.getInstitution();
        Invitation invitation = new Invitation();
        invitation.setHash("hash");
        invitation.setEmail("guest@example.com");
        invitation.setInstitution(institution);
        invitation.setMessage("Please join");
        invitation.setIntendedRole(Authority.GUEST);
        invitation.addInvitationRole(new InvitationRole(new Role("students", new Application(institution, "Canvas", "secret"))));
        invitation.addInvitationRole(new InvitationRole(new Role("students", new Application(institution, "Blackboard", "secret"))));

        mailBox.sendInviteMail(user, invitation);

        MimeMessage mimeMessage = mailMessage();
        MimeMessageParser parser = new MimeMessageParser(mimeMessage);
        parser.parse();

        String htmlContent = parser.getHtmlContent();
        assertTrue(htmlContent.contains("http://localhost:3000/invitations?h=hash"));
        assertTrue(htmlContent.contains("http://localhost:3000/invitations?h=hash"));
        assertTrue(htmlContent.contains("Canvas"));
    }

    private MimeMessage mailMessage() {
        await().until(() -> greenMail.getReceivedMessages().length != 0);
        return greenMail.getReceivedMessages()[0];
    }
}