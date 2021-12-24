package guests.scim;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Getter
public class Email implements Serializable {

    private String type = "other";
    private String value;

    public Email(String value) {
        this.value = value;
    }
}
