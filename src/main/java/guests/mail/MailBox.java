package guests.mail;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.MustacheFactory;
import guests.domain.Invitation;
import guests.domain.User;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

public class MailBox {

    private static final String TITLE = "title";
    private static final String BASE_URL = "baseUrl";

    private JavaMailSender mailSender;
    private String baseUrl;
    private String emailFrom;

    private final MustacheFactory mustacheFactory = new DefaultMustacheFactory();

    public MailBox(JavaMailSender mailSender, String emailFrom, String baseUrl) {
        this.mailSender = mailSender;
        this.emailFrom = emailFrom;
        this.baseUrl = baseUrl;
    }

    public void sendInviteMail(Invitation invitation, User user) throws MessagingException, IOException {
//        String languageCode = invitation.getLanguage().getLanguageCode();
//
//        String title = String.format("%s %s ",
//                languageCode.equals(Language.DUTCH.getLanguageCode()) ? "Uitnodiging voor" :
//                        languageCode.equals(Language.ENGLISH.getLanguageCode()) ? "Invitation for" : "Convite para",
//                invitation.getTeam().getName());
//
//        Map<String, Object> variables = new HashMap<>();
//        variables.put(TITLE, title);
//        variables.put(FEDERATED_USER, federatedUser);
//        variables.put("invitation", invitation);
//        variables.put("invitationMessage", invitation.getLatestInvitationMessage());
//        variables.put(BASE_URL, baseUrl);
//        sendMail(
//                String.format("mail_templates/invitation_%s.html", languageCode),
//                title,
//                variables,
//                invitation.getEmail());
    }

    private void sendMail(String templateName, String subject, Map<String, Object> variables, String... to) throws MessagingException, IOException {
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

    private String mailTemplate(String templateName, Map<String, Object> context) throws IOException {
        return mustacheFactory.compile(templateName).execute(new StringWriter(), context).toString();
    }

}
