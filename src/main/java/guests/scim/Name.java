package guests.scim;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@AllArgsConstructor
@Getter
public class Name implements Serializable {

    private String familyName;
    private String givenName;

}
