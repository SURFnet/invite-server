package guests.scim;

import guests.domain.User;
import guests.domain.UserRole;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class UserRequest implements Serializable {

    private List<String> schemas = Collections.singletonList("urn:ietf:params:scim:schemas:core:2.0:User");
    private String externalId;
    private String userName;
    private Name name;
    private String id;
    private String displayName;
    private List<Email> emails;

    public UserRequest(User user) {
        this.externalId = user.getEduPersonPrincipalName();
        this.userName = user.getEduPersonPrincipalName();
        this.name = new Name(user.getFamilyName(), user.getGivenName());
        this.displayName = String.format("%s %s", user.getGivenName(), user.getFamilyName());
        this.emails = Collections.singletonList(new Email(user.getEmail()));
    }

    public UserRequest(User user, UserRole userRole) {
        this(user);
        this.id = userRole.getServiceProviderId();
    }
}
