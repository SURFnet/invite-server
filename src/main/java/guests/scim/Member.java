package guests.scim;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;

import java.io.Serializable;

public class Member implements Serializable {

    private final String value;
    private final boolean fromExternalServiceProvider;

    public Member(String value, boolean fromExternalServiceProvider) {
        this.value = value;
        this.fromExternalServiceProvider = fromExternalServiceProvider;
    }

    public String getValue() {
        return value;
    }

    @JsonIgnore
    public boolean isFromExternalServiceProvider() {
        return fromExternalServiceProvider;
    }
}
