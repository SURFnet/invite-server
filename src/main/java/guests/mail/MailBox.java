package guests.mail;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.MustacheFactory;
import guests.domain.Invitation;
import guests.domain.User;
import lombok.SneakyThrows;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

public class MailBox {

    private final JavaMailSender mailSender;
    private final String baseUrl;
    private final String emailFrom;
    private final String languageCode = "en";

    private final MustacheFactory mustacheFactory = new DefaultMustacheFactory("templates");

    public MailBox(JavaMailSender mailSender, String emailFrom, String baseUrl) {
        this.mailSender = mailSender;
        this.emailFrom = emailFrom;
        this.baseUrl = baseUrl;
    }

    public void sendInviteMail(User user, Invitation invitation) {
        String role = invitation.getIntendedAuthority().friendlyName();
        String title = String.format("Invitation for %s at eduID inviters", role);

        Map<String, Object> variables = new HashMap<>();
        variables.put("title", title);
        variables.put("role", role);
        variables.put("invitation", invitation);
        variables.put("user", user);
        variables.put("url", String.format("%s/invitations?h=%s", baseUrl, invitation.getHash()));
        sendMail(String.format("invitation_%s.html", languageCode),
                title,
                variables,
                invitation.getEmail());
    }

    public void sendProvisioningMail(String title, String userRequest, String email) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("userRequest", userRequest);
        sendMail(String.format("scim_provisioning_%s.html", languageCode),
                title,
                variables,
                email);
    }

    @SneakyThrows
    private void sendMail(String templateName, String subject, Map<String, Object> variables, String... to) {
        String html = this.mailTemplate(templateName, variables);

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, false);
        helper.setSubject(subject);
        helper.setTo(to);
        setText(html, helper);
        helper.setFrom(emailFrom);
        doSendMail(message);
    }

    protected void setText(String html, MimeMessageHelper helper) throws MessagingException, IOException {
        helper.setText(html, true);
    }

    protected void doSendMail(MimeMessage message) {
        new Thread(() -> mailSender.send(message)).start();
    }

    private String mailTemplate(String templateName, Map<String, Object> context) {
        return mustacheFactory.compile(templateName).execute(new StringWriter(), context).toString();
    }

}
