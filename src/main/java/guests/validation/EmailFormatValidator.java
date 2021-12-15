package guests.validation;

import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

public class EmailFormatValidator implements FormatValidator {

    private static final Pattern pattern =
            Pattern.compile("^[A-Z0-9._%&+-]+@[A-Z0-9.-]+\\.[A-Z]{2,16}$", Pattern.CASE_INSENSITIVE);

    @Override
    public boolean isValid(String subject) {
        return !StringUtils.hasText(subject) || pattern.matcher(subject).matches();
    }

    @Override
    public String formatName() {
        return "email";
    }
}
