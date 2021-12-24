package guests.scim;

import lombok.Getter;

import java.io.Serializable;

@Getter
public class Member implements Serializable {

    private final String value;

    public Member(String value) {
        this.value = value;
    }
}
