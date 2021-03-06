package guests.mail;

import lombok.SneakyThrows;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.util.FileCopyUtils;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.File;
import java.io.IOException;

public class MockMailBox extends MailBox {

    private final String environment;

    public MockMailBox(JavaMailSender mailSender, String emailFrom, String baseUrl, String scimFailureEmail, String environment) {
        super(mailSender, emailFrom, baseUrl, scimFailureEmail, environment);
        this.environment = environment;
    }

    @Override
    protected void doSendMail(MimeMessage message) {
        //nope
    }

    @Override
    protected void setText(String plainText, String htmlText, MimeMessageHelper helper) throws MessagingException {
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("mac os x") && environment.equals("local")) {
            openInBrowser(htmlText);
        }
    }

    @SneakyThrows
    private void openInBrowser(String html) {
        File tempFile = File.createTempFile("javamail", ".html");
        FileCopyUtils.copy(html.getBytes(), tempFile);
        Runtime.getRuntime().exec("open " + tempFile.getAbsolutePath());
    }
}
