package guests.mail;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.MustacheFactory;
import guests.domain.Invitation;
import guests.domain.SCIMFailure;
import guests.domain.User;
import lombok.SneakyThrows;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

public class MailBox {

    private final JavaMailSender mailSender;
    private final String baseUrl;
    private final String emailFrom;
    private final String languageCode = "en";
    private final String scimFailureEmail;
    private final String environment;

    private static final Log LOG = LogFactory.getLog(MailBox.class);

    private final MustacheFactory mustacheFactory = new DefaultMustacheFactory("templates");

    public MailBox(JavaMailSender mailSender, String emailFrom, String baseUrl, String scimFailureEmail, String environment) {
        this.mailSender = mailSender;
        this.emailFrom = emailFrom;
        this.baseUrl = baseUrl;
        this.scimFailureEmail = scimFailureEmail;
        this.environment = environment;
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
        sendMail(String.format("invitation_%s", languageCode),
                title,
                variables,
                invitation.getEmail());
    }

    public void sendProvisioningMail(String title, String userRequest, String email) {
        LOG.info(String.format("Send email SCIM request %s %s to %s", title, userRequest, email));

        Map<String, Object> variables = new HashMap<>();
        variables.put("userRequest", userRequest);
        sendMail(String.format("scim_provisioning_%s", languageCode),
                title,
                variables,
                email);
    }

    public void sendScimFailureMail(SCIMFailure scimFailure) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("scimFailure", scimFailure);
        sendMail(String.format("scim_failure_%s", languageCode),
                String.format("SCIM failure in environment %s", environment),
                variables,
                scimFailureEmail);
    }

    @SneakyThrows
    private void sendMail(String templateName, String subject, Map<String, Object> variables, String... to) {
        String htmlText = this.mailTemplate(templateName + ".html", variables);
        String plainText = this.mailTemplate(templateName + ".txt", variables);

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        helper.setSubject(subject);
        setText(plainText, htmlText, helper);
        helper.setTo(to);
        helper.setFrom(emailFrom);
        doSendMail(message);
    }

    protected void setText(String plainText, String htmlText, MimeMessageHelper helper) throws MessagingException {
        helper.setText(plainText, htmlText);
    }

    protected void doSendMail(MimeMessage message) {
        new Thread(() -> mailSender.send(message)).start();
    }

    private String mailTemplate(String templateName, Map<String, Object> context) {
        return mustacheFactory.compile(templateName).execute(new StringWriter(), context).toString();
    }

}
