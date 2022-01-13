package guests;

import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.store.FolderException;
import com.icegreen.greenmail.util.ServerSetupTest;
import org.apache.commons.mail.util.MimeMessageParser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.test.context.ActiveProfiles;

import javax.mail.internet.MimeMessage;

import static org.awaitility.Awaitility.await;


@ActiveProfiles(value = "prod", inheritProfiles = false)
public
class AbstractMailTest extends AbstractTest {

    @RegisterExtension
    static GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP);

    @BeforeEach
    protected void beforeEach() throws FolderException {
        super.beforeEach();
        greenMail.start();
        greenMail.purgeEmailFromAllMailboxes();
    }

    @AfterEach
    void afterEach() {
        greenMail.stop();
    }

    protected MimeMessageParser mailMessage() throws Exception {
        await().until(() -> greenMail.getReceivedMessages().length != 0);
        MimeMessage mimeMessage = greenMail.getReceivedMessages()[0];
        MimeMessageParser parser = new MimeMessageParser(mimeMessage);
        return parser.parse();
    }
}