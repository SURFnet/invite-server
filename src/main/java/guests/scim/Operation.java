package guests.scim;

import guests.domain.UserRole;
import lombok.Getter;

import java.util.Collections;
import java.util.List;

@Getter
public class Operation {

    private final OperationType op;
    private final String path = "members";
    private final List<Member> value;

    public Operation(OperationType op, UserRole userRole) {
        this.op = op;
        this.value = Collections.singletonList(new Member(userRole.getServiceProviderId()));
    }
}
