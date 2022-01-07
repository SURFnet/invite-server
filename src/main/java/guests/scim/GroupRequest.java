package guests.scim;

import lombok.Getter;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

@Getter
public class GroupRequest implements Serializable {

    private final List<String> schemas = Collections.singletonList("urn:ietf:params:scim:schemas:core:2.0:Group");
    private final String externalId;
    private final String displayName;
    private final List<Member> members;

    public GroupRequest(String externalId, String displayName, List<Member> members) {
        this.externalId = externalId;
        this.displayName = displayName;
        this.members = members;
    }
}
